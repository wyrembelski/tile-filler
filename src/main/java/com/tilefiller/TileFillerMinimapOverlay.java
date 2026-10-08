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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines each filled tile on the minimap. Modeled on RuneLite's Ground Markers minimap overlay
 * (content rephrased for compliance with licensing restrictions):
 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerMinimapOverlay.java
 *
 * <p>Unlike Ground Markers' per-WorldView map iteration, this mirrors {@link TileFillerOverlay} by
 * reading the plugin's flat marker collection and filtering against the top-level world view, so
 * both overlays stay consistent.</p>
 */
class TileFillerMinimapOverlay extends Overlay
{
	private final Client client;
	private final TileFillerConfig config;
	private final TileFillerPlugin plugin;

	@Inject
	private TileFillerMinimapOverlay(Client client, TileFillerConfig config, TileFillerPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showMinimapDots())
		{
			return null;
		}

		final Collection<ColorTileMarker> markers = plugin.getMarkers();
		if (markers.isEmpty())
		{
			return null;
		}

		final WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return null;
		}

		for (final ColorTileMarker marker : markers)
		{
			final WorldPoint worldPoint = marker.getWorldPoint();
			if (worldPoint.getPlane() != wv.getPlane())
			{
				continue;
			}

			final Color color = marker.getColor();
			if (color == null)
			{
				continue;
			}

			drawOnMinimap(graphics, wv, worldPoint, color);
		}

		return null;
	}

	private void drawOnMinimap(Graphics2D graphics, WorldView wv, WorldPoint point, Color color)
	{
		final LocalPoint lp = LocalPoint.fromWorld(wv, point);
		if (lp == null)
		{
			return;
		}

		final int x = lp.getX() & -Perspective.LOCAL_TILE_SIZE;
		final int y = lp.getY() & -Perspective.LOCAL_TILE_SIZE;

		final Point mp1 = Perspective.localToMinimap(client, new LocalPoint(x, y, wv.getId()));
		final Point mp2 = Perspective.localToMinimap(client, new LocalPoint(x, y + Perspective.LOCAL_TILE_SIZE, wv.getId()));
		final Point mp3 = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y + Perspective.LOCAL_TILE_SIZE, wv.getId()));
		final Point mp4 = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y, wv.getId()));

		if (mp1 == null || mp2 == null || mp3 == null || mp4 == null)
		{
			return;
		}

		final Polygon poly = new Polygon();
		poly.addPoint(mp1.getX(), mp1.getY());
		poly.addPoint(mp2.getX(), mp2.getY());
		poly.addPoint(mp3.getX(), mp3.getY());
		poly.addPoint(mp4.getX(), mp4.getY());

		graphics.setStroke(new BasicStroke(1f));
		graphics.setColor(color);
		graphics.drawPolygon(poly);
	}
}
