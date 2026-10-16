# Floor Painter

RuneLite plugin that paints every floor tile in the loaded scene one colour of your choice. Grass, dirt, stone, sand, dungeon floors, textured floors and water all become the same colour, and so do flat models lying on the ground such as mosaics, paving and rugs. Walls, objects with height, players and the minimap are left alone.

A clean, uniform floor makes ground markers, true tile, projectiles and ground items stand out, which is what people used the Blindfold plugin plus a Skybox for. This gives you that without hiding anything else.

It rewrites the vertex colours of the scene's tiles the moment a region finishes loading, so it works with the GPU plugin on or off and costs nothing per frame. Turning the plugin off or picking another colour puts the original colours straight back, no scene reload needed.

## Settings

**Where** is Everywhere by default. Set it to Selected areas only and the floor is painted just in the areas ticked under Areas: each Theatre of Blood room, each Tombs of Amascut boss (with its puzzle room), Chambers of Xeric, Inferno, Fight Cave, Fortis Colosseum, Doom of Mokhaiotl, Nex, Nightmare and Yama, all on by default. **Custom region IDs** takes extra map regions, comma separated, for anywhere else. The floor switches back to normal the moment you leave a selected area.

**Floor colour** is the colour every tile gets. Default is white (#FFFFFF).

**Strength** (20% by default) is how much of that colour goes on. At 100% the floor is replaced outright. Lower values lay the colour over the floor like a translucent layer, so the ground keeps its own look underneath, softened and tinted.

**Keep shading** (on by default) keeps each tile's light and dark variation relative to the average floor brightness, so hills, slopes and paths are still readable in the new colour. Turn it off for one perfectly flat colour with no shading at all.

**Min brightness** and **Max brightness** (20% and 50% by default) clamp the shaded result, so a sunlit plateau can't wash out to near white and a shadowed dungeon can't go near black. They only apply while Keep shading is on. 0% and 100% mean no limit.

**Flat floor objects** (on by default) also recolours floors that are really models: thin ones such as mosaics, paving, rugs, trapdoors and puddles are painted whole, and on thicker ones such as arena platforms, docks and bridge decks only the level top faces at walking height are painted, plus the top surface of wide platforms (2x2 tiles or more) that sit up to a tile above the floor, and only when those faces cover a real share of the object's footprint, so a stray level facet on a rock face or wall base is left alone. Animated floor objects present when the map loads (the Wardens pool channels) are painted by the same rules. Objects that spawn during a fight (acid pools, shadow markers, mechanics) are left alone, with one exception: the Wardens phase 3 arena floor, which is a known list of floor pieces. Off limits the plugin to the terrain itself.

**Never paint object IDs** takes floor object IDs, comma separated, that keep their original colours. Akkha's arena floor in Tombs of Amascut (the quadrant platform, outer rings and hourglass tiles) and the energy pools, herb patches, floor holes and tool spawns (tinderbox, axe, pickaxe, hammer) in Chambers of Xeric are always left alone because you need to see them.

**Show the tile under the mouse** (off by default) shows an overlay, and writes to the client log, what is on the tile under the mouse (or under you when the mouse is off the scene), on every plane of that column, whether it is walkable and whether it was painted. Turn it on, hover the floor or wall that looks wrong and screenshot it for a bug report.

## Changelog

1.2.11: turning the plugin off restores every tile even if one piece or one world view fails on the way.

1.2.10: flat objects on tiles whose own terrain is hidden are the floor there and are painted at any height (Nightmare arena, other platforms over the void); the Nightmare platform pieces join the known-floor list. Fixes the Nightmare arena, bare since 1.2.3.

1.2.9: renamed to Floor Painter (settings carry over); plugin icon.

1.2.8: scenes of sub world views (moving platforms, the Wardens phase 3 arena) are painted and re-uploaded with the right zone count; the debug overlay names the world view and reports its own errors.

1.2.7: the Wardens phase 3 arena floor is painted wherever it floats; its pieces are floor by name and skip the height rules that 1.2.3 added for flat objects.

1.2.6: the Wardens phase 3 arena floor in Tombs of Amascut, which the server lays down mid-fight, is painted as it appears (an allowlist of its floor pieces; other mid-fight spawns stay untouched).

1.2.5: animated floor objects (the Wardens energy pool channels in Tombs of Amascut) are painted too, through the lit model their animation frames share.

1.2.4: defaults are now white at 20% Strength, a soft brightening wash; set Strength to 100% for the old solid replacement.

1.2.3: flat floor objects must rest at a plane's tile height (Chambers of Xeric wall caps are flat but a storey up); the cave walls and wall tops there are never painted; the debug overlay describes the tile under the mouse.

1.2.2: level facets on rock faces and wall bases are no longer painted (the painted faces must cover at least 30% of the object's footprint); the Skip walls and cliffs setting is gone, it skipped real hills and missed the cave walls.

1.2.1: Strength blends like a translucent layer (RGB) instead of through the hue wheel; wall tops no longer count as floor (only wide platforms may match the height of the plane above).

1.2.0: Strength setting to soften the floor instead of replacing it; steep terrain (cave walls, cliffs) is skipped by default.

1.1.11: Chambers of Xeric tool spawns (tinderbox, bronze axe, pickaxe, hammer) are never painted.

1.1.10: tops of wide raised platforms (2x2 tiles or more, up to a tile above the floor) are painted; the tile debug view is also an on-screen overlay.

1.1.9: Chambers of Xeric holes down to the next floor are never painted.

1.1.8: Chambers of Xeric herb patches are never painted.

1.1.7: Chambers of Xeric energy pools are never painted.

1.1.6: low raised slabs and steps (up to 48 units above the tile) count as walking surface, for the rest of the Wardens obelisk platform.

1.1.5: Akkha's arena floor is never painted; Never paint object IDs setting for other floors you want left alone.

1.1.4: walking-height check for thick floor models read the wrong column of the height map (fixes platforms left unpainted); the debug log now describes every plane of the column you stand in.

1.1.3: objects spawned during a fight (acid pools, shadow markers) are no longer painted; floor objects are only painted by the scene paint on map load and login, as in 1.0.

1.1.2: scene repainted after every login (fixes instance decorations left unpainted since 1.1.0).

1.1.1: debug log of the tile under you.

1.1.0: area toggles. Paint everywhere, or only in selected raid and boss rooms plus custom region IDs.

1.0.1: default colour #64878D, default brightness clamp 20% to 50%.

1.0.0: first release. Terrain and flat floor models painted one colour, optional shading with min/max brightness, walking surfaces of thick floor models.

## License

BSD 2-Clause, see LICENSE.
