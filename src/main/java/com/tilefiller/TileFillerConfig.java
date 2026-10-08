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

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(TileFillerConfig.CONFIG_GROUP)
public interface TileFillerConfig extends Config
{
	String CONFIG_GROUP = "tile-filler";

	@ConfigItem(
		keyName = "fillColor",
		name = "Fill color",
		description = "Default color used to fill newly marked tiles. Opacity is controlled by the Fill opacity slider below.",
		position = 1
	)
	default Color fillColor()
	{
		return Color.YELLOW;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "fillOpacity",
		name = "Fill opacity",
		description = "How opaque the tile fill is (0% = invisible, 100% = fully solid).",
		position = 2
	)
	@Units(Units.PERCENT)
	default int fillOpacity()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "drawBorder",
		name = "Draw border",
		description = "Draw a solid outline around each filled tile.",
		position = 3
	)
	default boolean drawBorder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "borderUseFillColor",
		name = "Border matches fill",
		description = "Use each tile's fill color for its border instead of the border color below.",
		position = 4
	)
	default boolean borderUseFillColor()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "borderColor",
		name = "Border color",
		description = "Color of the tile border, used when \"Border matches fill\" is off.",
		position = 5
	)
	default Color borderColor()
	{
		return Color.WHITE;
	}

	@Range(
		min = 1,
		max = 8
	)
	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Thickness of the tile border, when the border is enabled.",
		position = 6
	)
	default int borderWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "showLabels",
		name = "Show labels",
		description = "Draw the text label on tiles that have one.",
		position = 7
	)
	default boolean showLabels()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "labelColor",
		name = "Label color",
		description = "Color of tile labels.",
		position = 8
	)
	default Color labelColor()
	{
		return Color.WHITE;
	}

	@Range(
		min = 8,
		max = 24
	)
	@ConfigItem(
		keyName = "labelFontSize",
		name = "Label font size",
		description = "Point size of tile labels.",
		position = 9
	)
	default int labelFontSize()
	{
		return 14;
	}

	@ConfigItem(
		keyName = "showMinimapDots",
		name = "Show minimap dots",
		description = "Outline each filled tile on the minimap.",
		position = 10
	)
	default boolean showMinimapDots()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showImportExport",
		name = "Import / export menu",
		description = "Add Export / Import / Clear options to the world-map orb for sharing codes.",
		position = 11
	)
	default boolean showImportExport()
	{
		return true;
	}
}
