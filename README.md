# Floor Replacer

RuneLite plugin that paints every floor tile in the loaded scene one colour of your choice. Grass, dirt, stone, sand, dungeon floors, textured floors and water all become the same colour, and so do flat models lying on the ground such as mosaics, paving and rugs. Walls, objects with height, players and the minimap are left alone.

A clean, uniform floor makes ground markers, true tile, projectiles and ground items stand out, which is what people used the Blindfold plugin plus a Skybox for. This gives you that without hiding anything else.

It rewrites the vertex colours of the scene's tiles the moment a region finishes loading, so it works with the GPU plugin on or off and costs nothing per frame. Turning the plugin off or picking another colour puts the original colours straight back, no scene reload needed.

## Settings

**Floor colour** is the colour every tile gets. Default is a soft teal (#59BDAC).

**Keep shading** (on by default) keeps each tile's light and dark variation relative to the average floor brightness, so hills, slopes and paths are still readable in the new colour. Turn it off for one perfectly flat colour with no shading at all.

**Min brightness** and **Max brightness** (20% and 80% by default) clamp the shaded result, so a sunlit plateau can't wash out to near white and a shadowed dungeon can't go near black. They only apply while Keep shading is on. 0% and 100% mean no limit.

**Flat floor objects** (on by default) also recolours floors that are really models: thin ones such as mosaics, paving, rugs, trapdoors and puddles are painted whole, and on thicker ones such as arena platforms, docks and bridge decks only the level top faces at walking height are painted. Off limits the plugin to the terrain itself.

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.
