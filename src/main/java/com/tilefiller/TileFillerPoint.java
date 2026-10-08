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

/**
 * A persisted, region-relative marked tile. Stored as JSON in the plugin config, one list per
 * region id. Region-relative coordinates (rather than absolute world coordinates) keep markers
 * stable and allow correct rendering inside instanced areas.
 *
 * <p>{@code opacity} is a nullable per-tile override (0-100). When {@code null}, the tile uses the
 * global fill opacity from the config. It is the last field so that older persisted JSON written
 * without it still deserializes cleanly (Gson leaves the absent field {@code null}).</p>
 */
@Value
class TileFillerPoint
{
	private int regionId;
	private int regionX;
	private int regionY;
	private int z;
	private Color color;
	private String label;
	private Integer opacity;
}
