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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class TileFillerOverlay extends Overlay
{
	private final Client client;
	private final TileFillerConfig config;
	private final TileFillerPlugin plugin;

	@Inject
	private TileFillerOverlay(Client client, TileFillerConfig config, TileFillerPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Collection<ColorTileMarker> markers = plugin.getMarkers();
		if (markers.isEmpty())
		{
			return null;
		}

		final int defaultOpacity = percentToAlpha(config.fillOpacity());
		final boolean drawBorder = config.drawBorder();
		final boolean borderUseFillColor = config.borderUseFillColor();
		final Color borderColor = config.borderColor();
		final Stroke borderStroke = new BasicStroke(config.borderWidth());
		final boolean showLabels = config.showLabels();
		final Color labelColor = config.labelColor();
		final int labelFontSize = config.labelFontSize();

		for (final ColorTileMarker marker : markers)
		{
			final WorldPoint worldPoint = marker.getWorldPoint();
			if (worldPoint.getPlane() != client.getTopLevelWorldView().getPlane())
			{
				continue;
			}

			final Color baseColor = marker.getColor();
			if (baseColor == null)
			{
				continue;
			}

			final LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
			if (lp == null)
			{
				continue;
			}

			final Integer tileOpacity = marker.getOpacity();
			final int opacity = tileOpacity != null ? percentToAlpha(tileOpacity) : defaultOpacity;

			final Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				drawTile(graphics, poly, baseColor, opacity, drawBorder, borderUseFillColor, borderColor, borderStroke);
			}

			if (showLabels)
			{
				final String label = marker.getLabel();
				if (label != null && !label.isEmpty())
				{
					drawLabel(graphics, lp, label, labelColor, labelFontSize);
				}
			}
		}

		return null;
	}

	private void drawTile(Graphics2D graphics, Polygon poly, Color baseColor, int opacity,
		boolean drawBorder, boolean borderUseFillColor, Color borderColor, Stroke borderStroke)
	{
		final Color fill = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);
		graphics.setColor(fill);
		graphics.fill(poly);

		if (drawBorder)
		{
			final Color outline = borderUseFillColor
				? new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue())
				: borderColor;
			final Stroke originalStroke = graphics.getStroke();
			graphics.setColor(outline);
			graphics.setStroke(borderStroke);
			graphics.draw(poly);
			graphics.setStroke(originalStroke);
		}
	}

	private void drawLabel(Graphics2D graphics, LocalPoint lp, String label, Color labelColor, int fontSize)
	{
		final Font originalFont = graphics.getFont();
		graphics.setFont(originalFont.deriveFont(Font.BOLD, fontSize));

		final Point canvasPoint = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
		if (canvasPoint != null)
		{
			final int x = canvasPoint.getX();
			final int y = canvasPoint.getY();

			// Full outline: draw the text in black offset in every direction, then the colored
			// text on top. This keeps labels readable on any fill color, light or dark.
			graphics.setColor(Color.BLACK);
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx != 0 || dy != 0)
					{
						graphics.drawString(label, x + dx, y + dy);
					}
				}
			}

			graphics.setColor(labelColor);
			graphics.drawString(label, x, y);
		}

		graphics.setFont(originalFont);
	}

	/**
	 * Convert a 0-100 opacity percentage from config into a 0-255 alpha channel value, clamping
	 * out-of-range input defensively.
	 */
	private static int percentToAlpha(int percent)
	{
		final int clamped = Math.max(0, Math.min(percent, 100));
		return Math.round(clamped * 255f / 100f);
	}
}
