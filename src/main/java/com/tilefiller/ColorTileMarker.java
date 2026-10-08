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
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * A marked tile resolved to a concrete {@link WorldPoint} in the current world view, ready to be
 * rendered. Produced from a {@link TileFillerPoint} after translating region-relative coordinates
 * into (possibly instanced) world coordinates.
 */
@Value
class ColorTileMarker
{
	private WorldPoint worldPoint;
	private Color color;
	private String label;
	private Integer opacity;
}
