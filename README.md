# Floor Replacer

RuneLite plugin that paints every floor tile in the loaded scene one colour of your choice. Grass, dirt, stone, sand, dungeon floors, textured floors and water all become the same colour, and so do flat models lying on the ground such as mosaics, paving and rugs. Walls, objects with height, players and the minimap are left alone.

A clean, uniform floor makes ground markers, true tile, projectiles and ground items stand out, which is what people used the Blindfold plugin plus a Skybox for. This gives you that without hiding anything else.

It rewrites the vertex colours of the scene's tiles the moment a region finishes loading, so it works with the GPU plugin on or off and costs nothing per frame. Turning the plugin off or picking another colour puts the original colours straight back, no scene reload needed.

## Settings

**Where** is Everywhere by default. Set it to Selected areas only and the floor is painted just in the areas ticked under Areas: each Theatre of Blood room, each Tombs of Amascut boss (with its puzzle room), Chambers of Xeric, Inferno, Fight Cave, Fortis Colosseum, Doom of Mokhaiotl, Nex, Nightmare and Yama, all on by default. **Custom region IDs** takes extra map regions, comma separated, for anywhere else. The floor switches back to normal the moment you leave a selected area.

**Floor colour** is the colour every tile gets. Default is a muted slate blue (#64878D).

**Keep shading** (on by default) keeps each tile's light and dark variation relative to the average floor brightness, so hills, slopes and paths are still readable in the new colour. Turn it off for one perfectly flat colour with no shading at all.

**Min brightness** and **Max brightness** (20% and 50% by default) clamp the shaded result, so a sunlit plateau can't wash out to near white and a shadowed dungeon can't go near black. They only apply while Keep shading is on. 0% and 100% mean no limit.

**Flat floor objects** (on by default) also recolours floors that are really models: thin ones such as mosaics, paving, rugs, trapdoors and puddles are painted whole, and on thicker ones such as arena platforms, docks and bridge decks only the level top faces at walking height are painted. Objects that spawn during a fight (acid pools, shadow markers, mechanics) are left alone. Off limits the plugin to the terrain itself.

**Log the tile under you** (off by default) writes what is on each tile you step onto, and whether it was painted, to the client log. Turn it on, stand on the floor that stayed unpainted and paste the line into a bug report.

## Changelog

1.1.3: objects spawned during a fight (acid pools, shadow markers) are no longer painted; floor objects are only painted by the scene paint on map load and login, as in 1.0.

1.1.2: scene repainted after every login (fixes instance decorations left unpainted since 1.1.0).

1.1.1: debug log of the tile under you.

1.1.0: area toggles. Paint everywhere, or only in selected raid and boss rooms plus custom region IDs.

1.0.1: default colour #64878D, default brightness clamp 20% to 50%.

1.0.0: first release. Terrain and flat floor models painted one colour, optional shading with min/max brightness, walking surfaces of thick floor models.

## License

BSD 2-Clause, see LICENSE.
