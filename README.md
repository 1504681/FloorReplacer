# Floor Replacer

RuneLite plugin that paints every floor tile in the loaded scene one colour of your choice. Grass, dirt, stone, sand, dungeon floors, textured floors and water all become the same colour, and so do flat models lying on the ground such as mosaics, paving and rugs. Walls, objects with height, players and the minimap are left alone.

It rewrites the vertex colours of the scene's tiles the moment a region finishes loading, so it works with the GPU plugin on or off and costs nothing per frame. Turning the plugin off or picking another colour puts the original colours straight back, no scene reload needed.

## Settings

**Floor colour** is the colour every tile gets. Default is a plain green.

**Keep shading** (on by default) keeps each tile's light and dark variation relative to the average floor brightness, so hills, slopes and paths are still readable in the new colour. Turn it off for one perfectly flat colour with no shading at all.

**Flat floor objects** (on by default) also recolours ground models no taller than about a fifth of a tile: mosaics, paving, rugs, trapdoors, puddles. Off limits the plugin to the terrain itself.

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.
