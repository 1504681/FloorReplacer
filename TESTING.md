# In-game checks owed

- ToA Wardens: the obelisk platform, its low steps and the top of the wide platform are all painted (1.1.6, 1.1.10); the floor around them too.
- ToA Akkha: the arena floor keeps its own look while the corridor outside is painted (1.1.5).
- CoX: energy pools, herb patches and the holes down to the next floor keep their own look, the surrounding floor is painted (1.1.7 to 1.1.9).
- Mid-fight spawns (Zebak acid, Verzik/Xarpus shadows) stay unpainted after a scene repaint on login (1.1.3).
- Never paint object IDs: add one id, that object is left alone; tile debug overlay shows on screen only when its setting is on (1.1.10).
- CoX: the cave walls and their flat tops stay in their own colours while the room floor is painted; Strength below 100 looks like a tint, not new hues (1.2.1 to 1.2.3). Confirmed so far: Akkha, pools, patches, holes, tool spawns (1.1.9 feedback), "this is better" on 1.2.1.
- Debug overlay follows the mouse: hover a wall tile, the header says "hovering" and the plane line says "blocked"/"blocked by object" (1.2.3).
- Rugs and mosaics outdoors (Varrock, GE) are still painted: flat objects now need to rest at the tile height (1.2.3).
- ToA Wardens: the two dark-red pool channel pieces (TOA_WARDENS_POOL_CHANNEL_CONNECTOR01 45594, animated 3x5) take the floor colour, and keep it through the fight (1.2.5).
- ToA Wardens P3: the warped arena floor (TOA_WARDENS_WARPED_FLOORTILE/LVL0/P3_BROKENTILE 45643-45738) takes the floor colour as it appears; shadows and other P3 spawns do not (1.2.6).
- ToA Wardens P3 (1.2.8): the debug header says 'view sub N' if the arena is its own world view; its floor is painted; the overlay never goes blank there.
- Nightmare: the arena platform is painted again (hidden terrain + NIGHTMARE_PLATFORM 37646-37705 known floor), bare on hub 1.2.4 (1.2.9).
- Turning the plugin off (or Where = Selected areas and walking out) restores every tile, including with a boat or other world entity in the scene (1.2.11).
