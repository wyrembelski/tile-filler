/*
 * Copyright (c) 2026, Tile Filler contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DAMAGES ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE.
 */
package com.tilefiller;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.Runnables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Tile Filler",
	description = "Fills tiles completely with a color and opacity of your choosing",
	tags = {"tile", "marker", "ground", "fill", "color", "overlay"}
)
public class TileFillerPlugin extends Plugin
{
	private static final String CONFIG_GROUP = TileFillerConfig.CONFIG_GROUP;
	private static final String REGION_PREFIX = "region_";

	// Region-level sharing is attached to the world-map orb widget (the same component Ground
	// Markers uses) so it is always reachable, not just when hovering a filled tile.
	private static final WidgetMenuOption EXPORT_TILES_OPTION =
		new WidgetMenuOption("Export", "Fill tiles", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);
	private static final WidgetMenuOption IMPORT_TILES_OPTION =
		new WidgetMenuOption("Import", "Fill tiles", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);
	private static final WidgetMenuOption CLEAR_TILES_OPTION =
		new WidgetMenuOption("Clear", "Fill tiles", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);

	/**
	 * Rendered markers resolved to the current world view(s). Rebuilt from persisted points
	 * whenever regions load so the overlay can stay a dumb, fast reader each frame.
	 */
	private final ListMultimap<WorldView, ColorTileMarker> markers = ArrayListMultimap.create();

	@Inject
	private Client client;

	@Inject
	private TileFillerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TileFillerOverlay overlay;

	@Inject
	private TileFillerMinimapOverlay minimapOverlay;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private MenuManager menuManager;

	@Inject
	private Gson gson;

	private boolean sharingMenuAdded;

	@Provides
	TileFillerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TileFillerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);
		if (config.showImportExport())
		{
			addSharingMenuOptions();
		}
		loadPoints();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);
		removeSharingMenuOptions();
		markers.clear();
	}

	/**
	 * Attach the region-level Export / Import / Clear options to the world-map orb widget via
	 * {@link MenuManager}, mirroring RuneLite's Ground Markers plugin. Idempotent.
	 */
	private void addSharingMenuOptions()
	{
		if (sharingMenuAdded)
		{
			return;
		}

		menuManager.addManagedCustomMenu(EXPORT_TILES_OPTION, this::exportTiles);
		menuManager.addManagedCustomMenu(IMPORT_TILES_OPTION, this::importTiles);
		menuManager.addManagedCustomMenu(CLEAR_TILES_OPTION, this::clearTiles);
		sharingMenuAdded = true;
	}

	private void removeSharingMenuOptions()
	{
		if (!sharingMenuAdded)
		{
			return;
		}

		menuManager.removeManagedCustomMenu(EXPORT_TILES_OPTION);
		menuManager.removeManagedCustomMenu(IMPORT_TILES_OPTION);
		menuManager.removeManagedCustomMenu(CLEAR_TILES_OPTION);
		sharingMenuAdded = false;
	}

	/**
	 * Flattened snapshot of every resolved marker across loaded world views, consumed by the overlay.
	 */
	Collection<ColorTileMarker> getMarkers()
	{
		return markers.values();
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		loadPoints();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("showImportExport".equals(event.getKey()))
		{
			if (config.showImportExport())
			{
				addSharingMenuOptions();
			}
			else
			{
				removeSharingMenuOptions();
			}
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		loadPoints(event.getWorldView());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		markers.removeAll(event.getWorldView());
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final MenuAction menuAction = event.getMenuEntry().getType();
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		if (!hotKeyPressed || (menuAction != MenuAction.WALK && menuAction != MenuAction.SET_HEADING))
		{
			return;
		}

		final int worldId = event.getMenuEntry().getWorldViewId();
		final WorldView wv = client.getWorldView(worldId);
		if (wv == null)
		{
			return;
		}

		// Tile-specific entries need a selected scene tile. Region-level import/export/clear are
		// attached to the world-map orb (see addSharingMenuOptions) so they are always reachable.
		final Tile selectedSceneTile = wv.getSelectedSceneTile();
		if (selectedSceneTile != null)
		{
			final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedSceneTile.getLocalLocation());
			final int regionId = worldPoint.getRegionID();
			final Collection<TileFillerPoint> regionPoints = getPoints(regionId);
			final boolean marked = regionPoints.stream().anyMatch(p ->
				p.getRegionX() == worldPoint.getRegionX()
					&& p.getRegionY() == worldPoint.getRegionY()
					&& p.getZ() == worldPoint.getPlane());

			client.createMenuEntry(-1)
				.setOption(marked ? "Unfill" : "Fill")
				.setTarget("Tile")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> toggleTile(worldPoint));

			if (marked)
			{
				client.createMenuEntry(-2)
					.setOption("Pick color")
					.setTarget("Tile")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> pickColor(worldPoint));

				client.createMenuEntry(-3)
					.setOption("Set opacity")
					.setTarget("Tile")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> setOpacity(worldPoint));

				client.createMenuEntry(-4)
					.setOption("Label")
					.setTarget("Tile")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> labelTile(worldPoint));
			}

			if (!regionPoints.isEmpty())
			{
				client.createMenuEntry(-5)
					.setOption("Reset all")
					.setTarget("Tile")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> resetRegion(regionId, regionPoints.size()));
			}
		}
	}

	private void toggleTile(WorldPoint worldPoint)
	{
		final int regionId = worldPoint.getRegionID();
		final TileFillerPoint point = new TileFillerPoint(
			regionId,
			worldPoint.getRegionX(),
			worldPoint.getRegionY(),
			worldPoint.getPlane(),
			config.fillColor(),
			null,
			config.fillOpacity());

		final List<TileFillerPoint> regionPoints = new ArrayList<>(getPoints(regionId));
		final boolean removed = regionPoints.removeIf(p ->
			p.getRegionX() == point.getRegionX()
				&& p.getRegionY() == point.getRegionY()
				&& p.getZ() == point.getZ());

		if (!removed)
		{
			regionPoints.add(point);
		}

		savePoints(regionId, regionPoints);
		loadPoints();
	}

	private void pickColor(WorldPoint worldPoint)
	{
		final TileFillerPoint existing = findPoint(worldPoint);
		if (existing == null)
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			final RuneliteColorPicker colorPicker = colorPickerManager.create(
				client,
				existing.getColor(),
				"Tile fill color",
				false);
			colorPicker.setOnClose(c -> colorTile(existing, c));
			colorPicker.setVisible(true);
		});
	}

	private void labelTile(WorldPoint worldPoint)
	{
		final TileFillerPoint existing = findPoint(worldPoint);
		if (existing == null)
		{
			return;
		}

		chatboxPanelManager.openTextInput("Tile label")
			.value(Optional.ofNullable(existing.getLabel()).orElse(""))
			.onDone((input) ->
			{
				final String label = Strings.emptyToNull(input);
				final TileFillerPoint updated = new TileFillerPoint(
					existing.getRegionId(),
					existing.getRegionX(),
					existing.getRegionY(),
					existing.getZ(),
					existing.getColor(),
					label,
					existing.getOpacity());
				replacePoint(existing, updated);
			})
			.build();
	}

	private void setOpacity(WorldPoint worldPoint)
	{
		final TileFillerPoint existing = findPoint(worldPoint);
		if (existing == null)
		{
			return;
		}

		chatboxPanelManager.openTextInput("Tile opacity (0-100, blank = default)")
			.value(Optional.ofNullable(existing.getOpacity()).map(String::valueOf).orElse(""))
			.onDone((input) ->
			{
				final String trimmed = input == null ? "" : input.trim();
				final Integer opacity;
				if (trimmed.isEmpty())
				{
					opacity = null;
				}
				else
				{
					final int parsed;
					try
					{
						parsed = Integer.parseInt(trimmed);
					}
					catch (NumberFormatException ex)
					{
						log.debug("Ignoring invalid tile opacity input: {}", trimmed);
						return;
					}
					opacity = Math.max(0, Math.min(parsed, 100));
				}

				final TileFillerPoint updated = new TileFillerPoint(
					existing.getRegionId(),
					existing.getRegionX(),
					existing.getRegionY(),
					existing.getZ(),
					existing.getColor(),
					existing.getLabel(),
					opacity);
				replacePoint(existing, updated);
			})
			.build();
	}

	private void resetRegion(int regionId, int count)
	{
		chatboxPanelManager.openTextMenuInput("Clear all " + count + " filled tiles in this region?")
			.option("Yes", () ->
			{
				savePoints(regionId, null);
				loadPoints();
			})
			.option("No", Runnables.doNothing())
			.build();
	}

	private void colorTile(TileFillerPoint existing, Color newColor)
	{
		final TileFillerPoint updated = new TileFillerPoint(
			existing.getRegionId(),
			existing.getRegionX(),
			existing.getRegionY(),
			existing.getZ(),
			newColor,
			existing.getLabel(),
			existing.getOpacity());
		replacePoint(existing, updated);
	}

	private TileFillerPoint findPoint(WorldPoint worldPoint)
	{
		return getPoints(worldPoint.getRegionID()).stream()
			.filter(p -> p.getRegionX() == worldPoint.getRegionX()
				&& p.getRegionY() == worldPoint.getRegionY()
				&& p.getZ() == worldPoint.getPlane())
			.findFirst()
			.orElse(null);
	}

	/**
	 * Replace the stored point at {@code updated}'s coordinates with {@code updated}, then rebuild
	 * the rendered markers.
	 */
	private void replacePoint(TileFillerPoint existing, TileFillerPoint updated)
	{
		final List<TileFillerPoint> regionPoints = new ArrayList<>(getPoints(existing.getRegionId()));
		regionPoints.removeIf(p ->
			p.getRegionX() == updated.getRegionX()
				&& p.getRegionY() == updated.getRegionY()
				&& p.getZ() == updated.getZ());
		regionPoints.add(updated);

		savePoints(existing.getRegionId(), regionPoints);
		loadPoints();
	}

	private void loadPoints()
	{
		markers.clear();

		final WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}

		loadPoints(wv);

		for (final WorldEntity we : wv.worldEntities())
		{
			loadPoints(we.getWorldView());
		}
	}

	private void loadPoints(WorldView wv)
	{
		markers.removeAll(wv);

		final int[] regions = wv.getMapRegions();
		if (regions == null)
		{
			return;
		}

		for (final int regionId : regions)
		{
			final Collection<TileFillerPoint> regionPoints = getPoints(regionId);
			markers.putAll(wv, translateToColorTileMarker(wv, regionPoints));
		}
	}

	/**
	 * Resolve region-relative points to concrete world points for the given view, expanding each to
	 * every local instance occurrence so markers render correctly inside instanced areas.
	 */
	private Collection<ColorTileMarker> translateToColorTileMarker(WorldView wv, Collection<TileFillerPoint> points)
	{
		if (points.isEmpty())
		{
			return Collections.emptyList();
		}

		return points.stream()
			.map(point -> new ColorTileMarker(
				WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()),
				point.getColor(), point.getLabel(), point.getOpacity()))
			.flatMap(marker ->
			{
				final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(wv, marker.getWorldPoint());
				return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, marker.getColor(), marker.getLabel(), marker.getOpacity()));
			})
			.collect(Collectors.toList());
	}

	private Collection<TileFillerPoint> getPoints(int regionId)
	{
		final String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		return gson.fromJson(json, new TypeToken<List<TileFillerPoint>>(){}.getType());
		// CHECKSTYLE:ON
	}

	private void savePoints(int regionId, Collection<TileFillerPoint> points)
	{
		if (points == null || points.isEmpty())
		{
			configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
			return;
		}

		final String json = gson.toJson(points);
		configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId, json);
	}

	/**
	 * Serialize every tile in the currently loaded regions to a base64 share code and place it on
	 * the system clipboard. Modeled on RuneLite's Ground Markers sharing manager (content rephrased
	 * for compliance with licensing restrictions):
	 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerSharingManager.java
	 */
	private void exportTiles(MenuEntry menuEntry)
	{
		final int[] regions = client.getMapRegions();
		if (regions == null)
		{
			return;
		}

		final List<TileFillerPoint> points = Arrays.stream(regions)
			.mapToObj(regionId -> getPoints(regionId).stream())
			.flatMap(Function.identity())
			.collect(Collectors.toList());

		if (points.isEmpty())
		{
			sendChatMessage("You have no filled tiles to export.");
			return;
		}

		final String json = gson.toJson(points);
		final String code = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(new StringSelection(code), null);

		log.debug("Exported {} filled tile(s) from loaded regions", points.size());
		sendChatMessage(points.size() + " filled tile(s) copied to your clipboard.");
	}

	/**
	 * Read a share code from the system clipboard, decode it, and merge the tiles into their
	 * respective regions. Any malformed clipboard content is reported via chat and never thrown to
	 * the client.
	 */
	private void importTiles(MenuEntry menuEntry)
	{
		final String clipboardText;
		try
		{
			clipboardText = Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.getData(DataFlavor.stringFlavor)
				.toString();
		}
		catch (IOException | UnsupportedFlavorException ex)
		{
			log.warn("error reading clipboard", ex);
			sendChatMessage("Unable to read your system clipboard.");
			return;
		}

		if (Strings.isNullOrEmpty(clipboardText))
		{
			sendChatMessage("Your clipboard does not contain a tile share code.");
			return;
		}

		final String json;
		try
		{
			json = new String(Base64.getDecoder().decode(clipboardText.trim()), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex)
		{
			log.debug("Clipboard is not a valid tile share code", ex);
			sendChatMessage("Clipboard does not contain a valid tile share code.");
			return;
		}

		final List<TileFillerPoint> importPoints;
		try
		{
			// CHECKSTYLE:OFF
			importPoints = gson.fromJson(json, new TypeToken<List<TileFillerPoint>>(){}.getType());
			// CHECKSTYLE:ON
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Malformed tile share code", ex);
			sendChatMessage("Clipboard does not contain a valid tile share code.");
			return;
		}

		if (importPoints == null || importPoints.isEmpty())
		{
			sendChatMessage("Clipboard does not contain any tiles to import.");
			return;
		}

		final Map<Integer, List<TileFillerPoint>> grouped = importPoints.stream()
			.collect(Collectors.groupingBy(TileFillerPoint::getRegionId));

		grouped.forEach((regionId, groupedPoints) ->
		{
			final List<TileFillerPoint> merged = new ArrayList<>(getPoints(regionId));
			for (final TileFillerPoint point : groupedPoints)
			{
				final boolean duplicate = merged.stream().anyMatch(p ->
					p.getRegionX() == point.getRegionX()
						&& p.getRegionY() == point.getRegionY()
						&& p.getZ() == point.getZ());
				if (!duplicate)
				{
					merged.add(point);
				}
			}
			savePoints(regionId, merged);
		});

		loadPoints();
		sendChatMessage(importPoints.size() + " filled tile(s) imported from the clipboard.");
	}

	/**
	 * Clear every filled tile in the currently loaded regions after a confirmation prompt.
	 */
	private void clearTiles(MenuEntry menuEntry)
	{
		final int[] regions = client.getMapRegions();
		if (regions == null)
		{
			return;
		}

		final long count = Arrays.stream(regions)
			.mapToLong(regionId -> getPoints(regionId).size())
			.sum();

		if (count == 0)
		{
			sendChatMessage("You have no filled tiles to clear.");
			return;
		}

		chatboxPanelManager.openTextMenuInput("Clear the " + count + " filled tile(s) in the loaded regions?")
			.option("Yes", () ->
			{
				for (final int regionId : regions)
				{
					savePoints(regionId, null);
				}

				loadPoints();
				sendChatMessage(count + " filled tile(s) cleared.");
			})
			.option("No", Runnables.doNothing())
			.build();
	}

	private void sendChatMessage(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}
}
