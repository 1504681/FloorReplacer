<!-- managed by ~/homelab/bin/claude-md; hand-written notes go below the line -->
# FloorReplacer

RuneLite plugin that paints every floor tile in the loaded scene one colour of your choice. Grass, dirt, stone, sand, dungeon floors, textured floors and water all become the same colour; walls, objects, players and the minimap are left alone.

## Run
- `./gradlew run` launches a RuneLite client with the plugin loaded; `./gradlew build` must pass before any hub PR (the hub builds your exact commit).

## Test
- `./gradlew test`

## Where things live
- Git: https://github.com/1504681/FloorReplacer.git (PUBLIC).

## Conventions
Box-wide conventions and the port map: ~/homelab (BOX.md is imported into every session). Registry: add/edit services.toml then bin/apply. No Claude attribution or session links in anything pushed.

<!-- hand-written notes below this line are preserved by bin/claude-md -->
---
- Hub process + auto-merge update path: ~/homelab/runbooks/runelite-plugin.md. Verify API symbols with `javap -cp ~/.gradle/caches/modules-2/files-2.1/net.runelite/runelite-api/<ver>/*/runelite-api-<ver>.jar <class>`.
- How it works: `GameStateChanged LOGGED_IN` → rewrite `SceneTilePaint` corner colours / `SceneTileModel` triangle colour arrays (packed HSL, keep 12345678 = hidden), set textures to -1, then `DrawCallbacks.invalidateZone` on all 23×23 zones so the GPU plugin re-uploads. Originals kept in an IdentityHashMap and written back on disable/config change.
