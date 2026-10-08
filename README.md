# Tile Filler

A RuneLite plugin that fills tiles completely with a solid, translucent color of your
choosing — think of it as the filled-in version of the built-in Ground Markers / Tile
Markers plugin.

## Features

- **Shift + right-click a tile → Fill / Unfill** to toggle a filled tile.
- **Shift + right-click a filled tile → Pick color** to recolor just that tile.
- **Shift + right-click a filled tile → Label** to add a text label to it.
- **Shift + right-click a filled tile → Set opacity** to give that tile its own opacity
  (0–100%), overriding the global fill opacity. Leave the input blank to go back to the
  global value.
- **Shift + right-click → Reset all** to clear every filled tile in the current region
  (asks for confirmation first).
- **Right-click the world-map orb → Export / Import / Clear** to copy every filled tile in
  your loaded regions to the clipboard as a share code, paste one back on another client or
  profile, or clear the loaded regions. These options live on the minimap's world-map orb,
  so they are always reachable even on empty ground.
- Optional **minimap outlines** so filled tiles are visible on the minimap too.
- Choose a **default fill color** in the plugin settings. Opacity is set solely by the Fill
  opacity slider — the fill color picker no longer has its own opacity slider.
- Choose the **fill opacity** (0–100%) independently of the color. Newly filled tiles
  capture the current fill opacity at creation, so an exported tile reproduces the exact
  fill you saw when you placed it.
- Optional **border** around each filled tile, with a configurable color (or match the
  fill color) and width.
- Optional **text labels** with a configurable color.
- Markers are saved per region and persist across sessions. They also render correctly
  inside instanced areas.

## Usage

1. Enable **Tile Filler** in the RuneLite plugin list.
2. Hold **Shift**, right-click the ground, and choose **Fill**.
3. Adjust the default color, opacity, border, and labels in the plugin's settings panel.
4. To change one tile's color, hold Shift, right-click it, and choose **Pick color**.
5. To label a tile, hold Shift, right-click it, and choose **Label**.
6. To give one tile its own opacity, hold Shift, right-click it, choose **Set opacity**,
   and enter a value from 0–100 (blank clears the override).
7. To share tiles, right-click the **world-map orb** on the minimap and choose **Export**
   to copy a share code for all your loaded regions to the clipboard, **Import** to paste
   one back in, or **Clear** to remove the loaded regions' tiles. These work even when no
   tile is filled nearby.

## Settings

| Setting | Description |
|---------|-------------|
| Fill color | Default color applied to newly filled tiles. Opacity is controlled by the Fill opacity slider, not this picker. |
| Fill opacity | How solid the fill appears (0% = invisible, 100% = fully opaque). New tiles capture this value at creation. |
| Draw border | Draw a solid outline around each filled tile. |
| Border matches fill | Use each tile's fill color for its border. |
| Border color | Color of the border when it does not match the fill. |
| Border width | Thickness of that outline. |
| Show labels | Draw text labels on tiles that have one. |
| Label color | Color of tile labels. |
| Label font size | Point size of tile labels. |
| Show minimap outlines | Outline each filled tile on the minimap. |
| Import / export menu | Add Export / Import / Clear options to the world-map orb for sharing codes. |

## Building & running

Requires JDK 11.

```bash
./gradlew run     # launch a developer RuneLite client with the plugin loaded
./gradlew build   # compile and run tests
```

## License

BSD 2-Clause. See [LICENSE](LICENSE).
