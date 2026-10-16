package com.floorreplacer;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Player;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.TextureProvider;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Floor Painter",
	description = "Paints every floor tile in the scene a single colour of your choice",
	tags = {"floor", "painter", "paint", "replacer", "ground", "colour", "color", "texture", "recolor", "scene", "terrain", "blindfold", "skybox", "clear", "flat", "visibility"}
)
public class FloorReplacerPlugin extends Plugin
{
	static final String VERSION = "1.2.10";

	// Vertex colour the client uses for tiles that must not be drawn (under bridges etc.)
	private static final int HIDDEN = 12345678;
	private static final int ZONE_SIZE = 8;
	private static final int ZONES = Constants.EXTENDED_SCENE_SIZE / ZONE_SIZE;
	private static final double DEFAULT_BRIGHTNESS = 0.8;
	// Objects no taller than this (in model units; a tile is 128 wide) count as floor: mosaics, rugs, paving, puddles
	private static final int FLAT_OBJECT_HEIGHT = 24;
	// For taller objects (arena platforms, docks, bridges) only faces that are level (vertex heights within
	// FACE_SLOPE) and lie at a plane's tile height (within HEIGHT_TOLERANCE) are floor: the top you walk on.
	// 48 units covers low raised slabs and steps (the Wardens obelisk platform) and stays below crate height
	private static final int FACE_SLOPE = 6;
	private static final int HEIGHT_TOLERANCE = 48;
	// A thick model whose footprint is at least 2x2 tiles is a platform, not furniture: its top surface counts
	// as floor when it sits up to this far above a plane's tile height (a crate or table is 1 tile wide)
	private static final int PLATFORM_HEIGHT = 128;
	// The level faces found on a thick model must cover at least this share of its footprint to be a floor.
	// A platform's walking surface covers nearly all of it; the odd level facet on a rock face or wall base
	// covers almost none
	private static final float MIN_COVERAGE = 0.3f;
	// Floor objects that carry mechanic information and must keep their own look. Akkha's arena in Tombs of
	// Amascut: AKKHA_PLATFORM01-24 (the quadrant floor), AKKHA_ARENA_OUTER_LVL1/LVL2 rings, the hourglass
	// floor pieces (gameval ObjectID 45872-46067)
	private static final int AKKHA_FLOOR_FIRST = 45872;
	private static final int AKKHA_FLOOR_LAST = 46067;
	// Chambers of Xeric energy pools (RAIDS_ENERGY_POOL and its glow) and herb patches (RAIDS_PATCH_EMPTY,
	// RAIDS_PATCH_GROUNDDECOR, RAIDS_PATCH_* seed/growth/fully grown) and the holes down to the next floor
	// (RAIDS_DESCENTTO1/2) and the tool spawns (RAIDS_ICEDEMON_TINDERBOX/AXE, RAIDS_STONEGUARDIANS_PICKAXE,
	// RAIDS_LASERCRABS_HAMMER): flat, but you need to see them. The cave walls RAIDS_WALL_1/2 and their flat
	// cap pieces RAIDS_WALL_TOP_CORNER_INNER/OUTER, _EDGE, _FILLED, _PLANE, _DOOR (29781-29788): a wall top is a
	// flat model, but it is not floor
	private static final Set<Integer> NEVER_PAINT = new HashSet<>(Arrays.asList(
		30066, 30067, 32542, 29734, 31634, 31862, 41754, 29711,
		29765, 29877, 29997, 29998, 29999, 30000, 30001, 30002, 30003, 30004, 30005, 30006, 30007, 30008, 30009, 30010, 30011,
		29781, 29782, 29783, 29784, 29785, 29786, 29787, 29788));
	// Objects that are floor by name, so the height rules do not apply (their level faces are painted wherever
	// they sit) and they are painted even when they SPAWN mid-fight: the Tombs of Amascut Wardens phase 3
	// arena, which floats over the void (TOA_WARDENS_WARPED_FLOORTILE*, _WARPED_LVL0_01-71,
	// _WARPED_OUTSIDE_CRUMBLE*, _P3_BROKENTILE*), and the Nightmare arena platform (NIGHTMARE_PLATFORM_LVL0-3,
	// 37646-37705). Everything else that spawns during a fight is a mechanic
	private static final Set<Integer> KNOWN_FLOOR = new HashSet<>();
	static
	{
		for (int id = 45643; id <= 45738; id++)
		{
			KNOWN_FLOOR.add(id);
		}
		for (int id = 37646; id <= 37705; id++)
		{
			KNOWN_FLOOR.add(id);
		}
	}
	// Scene.getTileHeights() is indexed in extended scene coordinates for the top-level world view, while
	// Tile.getSceneLocation() is in plain scene coordinates (same convention as the GPU plugin's uploader)
	private static final int SCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FloorReplacerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FloorReplacerOverlay overlay;

	// Original colours of every paint/model we touched, so disabling or changing the colour restores them
	// without a scene reload. Keyed by identity: the client creates fresh objects for each scene build.
	private final Map<Object, int[]> originals = new IdentityHashMap<>();
	// An animated object hands out a fresh Model per frame, but the frames share the lit model's face colour
	// arrays; the first Model seen per colour array stands for the object in originals
	private final Map<int[], Model> animatedModels = new IdentityHashMap<>();
	private final Set<Scene> paintedScenes = Collections.newSetFromMap(new IdentityHashMap<>());

	// Map region the local player stands in (template region inside instances); -1 when unknown
	private int currentRegion = -1;
	// True while the current scene carries our colours
	private boolean painted;
	// Object IDs the user never wants painted, parsed from config at each paint pass
	private Set<Integer> excludedIds = Collections.emptySet();
	// Tile last written to the debug log, so each tile is described once per visit, and what was written
	private int lastDebugTile = -1;
	private final List<String> debugLines = new ArrayList<>();
	// Colour parameters of the last scene pass, reused for floor pieces that spawn later
	private int paintTarget;
	private boolean paintShade;
	private int paintReference;
	// Something was painted outside a scene pass; the GPU plugin needs a re-upload on the next client tick
	private boolean dirty;

	// Luminance clamp (0..127) applied when shading is kept; set from config at the start of each paint pass
	// How far each vertex colour moves towards the target: 100 replaces it, less lays the colour over the floor
	// like a translucent layer (blended in RGB with the client's own palette formula, cached per packed colour)
	private int strength = 100;
	private int targetRgb;
	private double brightness = DEFAULT_BRIGHTNESS;
	private int[] blendCache;
	private int minLum = 0;
	private int maxLum = JagexColor.LUMINANCE_MAX;

	@Provides
	FloorReplacerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FloorReplacerConfig.class);
	}

	@Override
	protected void startUp()
	{
		currentRegion = -1;
		overlayManager.add(overlay);
		clientThread.invoke(this::evaluate);
	}

	List<String> debugLines()
	{
		return debugLines;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		debugLines.clear();
		clientThread.invoke(this::restoreAll);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
			case LOGIN_SCREEN:
			case HOPPING:
				// The scene is about to be rebuilt; the old paints/models are garbage now
				originals.clear();
				animatedModels.clear();
				paintedScenes.clear();
				painted = false;
				currentRegion = -1;
				break;
			case LOGGED_IN:
				// Repaint even if WorldViewLoaded already did: objects the server places right after the map
				// rebuild are on the tiles by now, and a second pass over painted tiles is a no-op
				painted = false;
				evaluate();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		if (wanted())
		{
			paintScene(event.getWorldView().getScene());
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		Scene scene = event.getWorldView() != null ? event.getWorldView().getScene() : null;
		if (scene != null)
		{
			paintedScenes.remove(scene);
		}
	}

	// Objects that spawn later (acid pools, shadow markers, arena mechanics) are deliberately left alone: they
	// are gameplay, not floor. Only the scene paint passes (map load, login) touch floor objects, with one
	// exception: the KNOWN_FLOOR allowlist, floors the server lays down mid-fight.

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		if (object != null && spawnIsFloor(object.getId()))
		{
			boolean wide = isWide(object);
			paintSpawn(event.getTile(), object.getRenderable(), object.getZ(), wide, footprint(object));
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		GroundObject object = event.getGroundObject();
		if (object != null && spawnIsFloor(object.getId()))
		{
			paintSpawn(event.getTile(), object.getRenderable(), object.getZ(), false, 1);
		}
	}

	private boolean spawnIsFloor(int id)
	{
		return painted && config.floorObjects() && KNOWN_FLOOR.contains(id) && !excluded(id);
	}

	private void paintSpawn(Tile tile, Renderable renderable, int objectZ, boolean wide, int footprint)
	{
		Player local = client.getLocalPlayer();
		WorldView view = local != null ? local.getWorldView() : client.getTopLevelWorldView();
		Scene scene = view != null ? view.getScene() : null;
		if (tile == null || scene == null)
		{
			return;
		}
		if (!paintedScenes.contains(scene))
		{
			paintedScenes.add(scene);
		}
		int offset = heightOffset(scene);
		int[] levels = walkableHeights(tile, scene.getTileHeights(), offset, wide);
		if (paintFloorObject(renderable, objectZ, levels, wide, footprint, true, hiddenTerrain(tile), paintTarget, paintShade, paintReference) > 0)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (dirty)
		{
			dirty = false;
			for (Scene scene : paintedScenes)
			{
				invalidate(scene);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		evaluate();
		if (config.debugLog())
		{
			try
			{
				debugTile();
			}
			catch (RuntimeException e)
			{
				debugLines.clear();
				debugLines.add("v" + VERSION + " debug failed: " + e);
				log.warn("Floor Painter: debug failed", e);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FloorReplacerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		clientThread.invoke(() ->
		{
			restoreAll();
			evaluate();
			if (!config.debugLog())
			{
				debugLines.clear();
			}
			lastDebugTile = -1;
		});
	}

	/**
	 * Paint or restore the scene according to where the player is. Cheap enough to run every tick: it only acts
	 * when the wanted state differs from the current one.
	 */
	private void evaluate()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		Player local = client.getLocalPlayer();
		if (local != null && local.getLocalLocation() != null)
		{
			currentRegion = WorldPoint.fromLocalInstance(client, local.getLocalLocation()).getRegionID();
		}
		boolean want = wanted();
		if (want && !painted)
		{
			paintAll();
		}
		else if (!want && painted)
		{
			restoreAll();
		}
	}

	private boolean wanted()
	{
		if (config.areaMode() == FloorReplacerConfig.AreaMode.EVERYWHERE)
		{
			return true;
		}
		return currentRegion != -1 && regionEnabled(currentRegion);
	}

	// Region IDs cross-checked against the Watchdog, Blert and Automatic Low Detail plugins
	private boolean regionEnabled(int region)
	{
		switch (region)
		{
			case 12613:
				return config.tobMaiden();
			case 13125:
				return config.tobBloat();
			case 13122:
				return config.tobNylocas();
			case 13123:
			case 13379:
				return config.tobSotetseg();
			case 12612:
				return config.tobXarpus();
			case 12611:
				return config.tobVerzik();
			case 15698:
			case 15700:
				return config.toaZebak();
			case 14162:
			case 14164:
				return config.toaKephri();
			case 14674:
			case 14676:
				return config.toaAkkha();
			case 15186:
			case 15188:
				return config.toaBaba();
			case 15184:
			case 15696:
				return config.toaWardens();
			case 13136:
			case 13137:
			case 13393:
			case 13138:
			case 13394:
			case 13139:
			case 13395:
			case 13140:
			case 13396:
			case 13141:
			case 13397:
			case 13145:
			case 13401:
			case 12889:
				return config.cox();
			case 9043:
				return config.inferno();
			case 9551:
				return config.fightCave();
			case 7216:
				return config.colosseum();
			case 5269:
			case 13668:
			case 14180:
				return config.doom();
			case 11601:
				return config.nex();
			case 15515:
				return config.nightmare();
			case 5789:
			case 6045:
				return config.yama();
			default:
				return customRegions().contains(region);
		}
	}

	private boolean excluded(int objectId)
	{
		return (objectId >= AKKHA_FLOOR_FIRST && objectId <= AKKHA_FLOOR_LAST)
			|| NEVER_PAINT.contains(objectId)
			|| excludedIds.contains(objectId);
	}

	private Set<Integer> customRegions()
	{
		return parseIds(config.customRegions());
	}

	private static Set<Integer> parseIds(String list)
	{
		Set<Integer> out = new HashSet<>();
		for (String part : list.split("[,\\s]+"))
		{
			try
			{
				if (!part.isEmpty())
				{
					out.add(Integer.parseInt(part.trim()));
				}
			}
			catch (NumberFormatException ignored)
			{
				// typo in the list; skip it
			}
		}
		return out;
	}

	private void paintAll()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return;
		}
		paintScene(top.getScene());
		// Sub world views: moving platforms and the like carry their own scene (the Wardens phase 3 arena)
		for (WorldEntity entity : top.worldEntities())
		{
			WorldView sub = entity != null ? entity.getWorldView() : null;
			if (sub != null)
			{
				paintScene(sub.getScene());
			}
		}
	}

	private void paintScene(Scene scene)
	{
		if (scene == null)
		{
			return;
		}
		Tile[][][] tiles = scene.getExtendedTiles();
		if (tiles == null)
		{
			return;
		}

		TextureProvider textures = client.getTextureProvider();
		brightness = textures != null ? textures.getBrightness() : DEFAULT_BRIGHTNESS;
		targetRgb = config.color().getRGB() & 0xFFFFFF;
		int target = JagexColor.rgbToHSL(targetRgb, brightness) & 0xFFFF;
		boolean shade = config.keepShading();
		boolean objects = config.floorObjects();
		strength = Math.max(0, Math.min(100, config.strength()));
		if (strength < 100)
		{
			if (blendCache == null)
			{
				blendCache = new int[1 << 16];
			}
			Arrays.fill(blendCache, -1);
		}
		minLum = JagexColor.LUMINANCE_MAX * Math.max(0, Math.min(100, config.minBrightness())) / 100;
		maxLum = JagexColor.LUMINANCE_MAX * Math.max(0, Math.min(100, config.maxBrightness())) / 100;
		if (maxLum < minLum)
		{
			maxLum = minLum;
		}
		int reference = shade ? averageLuminance(tiles) : 0;
		excludedIds = parseIds(config.excludedObjects());
		paintTarget = target;
		paintShade = shade;
		paintReference = reference;

		int[][][] heights = scene.getTileHeights();
		int offset = heightOffset(scene);
		int count = 0;
		for (Tile[][] plane : tiles)
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					count += paintTile(tile, heights, offset, target, shade, reference, objects);
					Tile bridge = tile.getBridge();
					if (bridge != null)
					{
						count += paintTile(bridge, heights, offset, target, shade, reference, objects);
					}
				}
			}
		}

		paintedScenes.add(scene);
		painted = true;
		invalidate(scene);
		log.debug("Painted {} floor tiles in world view {}", count, scene.getWorldViewId());
	}

	private int paintTile(Tile tile, int[][][] heights, int offset, int target, boolean shade, int reference, boolean objects)
	{
		int painted = 0;
		SceneTilePaint paint = tile.getSceneTilePaint();
		if (paint != null)
		{
			paintPaint(paint, target, shade, reference);
			painted++;
		}
		SceneTileModel model = tile.getSceneTileModel();
		if (model != null)
		{
			paintModel(model, target, shade, reference);
			painted++;
		}
		if (objects)
		{
			// A wall is one tile thick and a storey tall, so its top sits exactly at the plane above's height map.
			// Only wide platforms may match heights of planes above their own; everything else uses its own plane
			// and the ones below (a sunken arena floor seen from the plane above still counts).
			int[] own = walkableHeights(tile, heights, offset, false);
			int[] all = walkableHeights(tile, heights, offset, true);
			boolean bare = hiddenTerrain(tile);
			GroundObject ground = tile.getGroundObject();
			if (ground != null && !excluded(ground.getId()))
			{
				painted += paintFloorObject(ground.getRenderable(), ground.getZ(), own, false, 1, KNOWN_FLOOR.contains(ground.getId()), bare, target, shade, reference);
			}
			GameObject[] gameObjects = tile.getGameObjects();
			if (gameObjects != null)
			{
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null && !excluded(gameObject.getId()))
					{
						boolean wide = isWide(gameObject);
						painted += paintFloorObject(gameObject.getRenderable(), gameObject.getZ(), wide ? all : own, wide, footprint(gameObject), KNOWN_FLOOR.contains(gameObject.getId()), bare, target, shade, reference);
					}
				}
			}
		}
		return painted;
	}

	private static int heightOffset(Scene scene)
	{
		return scene.getWorldViewId() == WorldView.TOPLEVEL ? SCENE_OFFSET : 0;
	}

	/** Number of tiles the object stands on. */
	private static int footprint(GameObject object)
	{
		Point min = object.getSceneMinLocation();
		Point max = object.getSceneMaxLocation();
		if (min == null || max == null)
		{
			return 1;
		}
		return Math.max(1, (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1));
	}

	/** Share of the footprint (in tiles) covered by the given faces, seen from above. Can exceed 1 for overlapping faces. */
	private static float coverage(Model model, boolean[] faces, int footprint)
	{
		float[] xs = model.getVerticesX();
		float[] zs = model.getVerticesZ();
		int[] f1 = model.getFaceIndices1();
		int[] f2 = model.getFaceIndices2();
		int[] f3 = model.getFaceIndices3();
		if (xs == null || zs == null || f1 == null || f2 == null || f3 == null)
		{
			return 1f;
		}
		int vertices = Math.min(model.getVerticesCount(), Math.min(xs.length, zs.length));
		double area = 0;
		for (int i = 0; i < faces.length; i++)
		{
			if (!faces[i] || f1[i] >= vertices || f2[i] >= vertices || f3[i] >= vertices)
			{
				continue;
			}
			double ax = xs[f2[i]] - xs[f1[i]];
			double az = zs[f2[i]] - zs[f1[i]];
			double bx = xs[f3[i]] - xs[f1[i]];
			double bz = zs[f3[i]] - zs[f1[i]];
			area += Math.abs(ax * bz - az * bx) / 2.0;
		}
		return (float) (area / (footprint * 128.0 * 128.0));
	}

	/** True when the object covers at least 2x2 tiles: a platform or deck rather than a crate or table. */
	private static boolean isWide(GameObject object)
	{
		Point min = object.getSceneMinLocation();
		Point max = object.getSceneMaxLocation();
		return min != null && max != null && max.getX() - min.getX() >= 1 && max.getY() - min.getY() >= 1;
	}

	/** True when a level face at this absolute height counts as floor: at a plane's tile height, or, for a wide
	 * platform, the top surface sitting up to PLATFORM_HEIGHT above one. */
	private static boolean isFloorHeight(float absolute, int[] levels, boolean topOfPlatform)
	{
		for (int level : levels)
		{
			float above = level - absolute; // model Y grows downwards, so a raised top is below the tile height
			if (Math.abs(above) <= HEIGHT_TOLERANCE || (topOfPlatform && above > 0 && above <= PLATFORM_HEIGHT))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Tile heights at this tile's column: the tile's own plane and those below, plus the planes above when
	 * upper is set. Empty when the tile is outside the height map.
	 */
	private static int[] walkableHeights(Tile tile, int[][][] heights, int offset, boolean upper)
	{
		Point loc = tile.getSceneLocation();
		if (heights == null || loc == null)
		{
			return new int[0];
		}
		int sx = loc.getX() + offset;
		int sy = loc.getY() + offset;
		int top = upper ? heights.length - 1 : Math.min(heights.length - 1, tile.getPlane());
		int[] out = new int[top + 1];
		int count = 0;
		for (int z = 0; z <= top; z++)
		{
			int[][] plane = heights[z];
			if (plane != null && sx >= 0 && sx < plane.length && plane[sx] != null && sy >= 0 && sy < plane[sx].length)
			{
				out[count++] = plane[sx][sy];
			}
		}
		return count == out.length ? out : Arrays.copyOf(out, count);
	}

	/**
	 * The model to recolour for a scene object. A static object is its own Model. An animated object (the Tombs
	 * of Amascut Wardens pool channels, glowing floor plates) builds a Model per animation frame from one lit
	 * base model; the frames share that base model's face colour and texture arrays, so recolouring the arrays
	 * through any frame recolours the object until the client rebuilds the base model. Actors and anything else
	 * give null.
	 */
	private Model modelOf(Renderable renderable)
	{
		if (renderable instanceof Model)
		{
			return (Model) renderable;
		}
		if (!(renderable instanceof DynamicObject))
		{
			return null;
		}
		Model frame = renderable.getModel();
		int[] colors = frame != null ? frame.getFaceColors1() : null;
		if (colors == null)
		{
			return null;
		}
		Model first = animatedModels.putIfAbsent(colors, frame);
		return first != null ? first : frame;
	}

	/**
	 * Floors that are really models on the tile. A near-flat model (mosaic, paving, rug, trapdoor, puddle) resting
	 * at a plane's tile height is painted whole. A taller static model (arena platform, dock, bridge deck) only has its level faces painted,
	 * and only those sitting at a plane's tile height: the surface you walk on, not the sides or the underside.
	 * Animated objects and actors are left alone.
	 */
	/**
	 * True when the tile has no visible terrain of its own: no paint or model, or one hidden with the 12345678
	 * colour. Jagex hides the terrain where an object is the floor (boss arenas, platforms over the void), so
	 * a flat object on such a tile is the floor whatever height it floats at.
	 */
	private static boolean hiddenTerrain(Tile tile)
	{
		SceneTilePaint paint = tile.getSceneTilePaint();
		if (paint != null)
		{
			return paint.getNeColor() == HIDDEN;
		}
		SceneTileModel model = tile.getSceneTileModel();
		if (model != null)
		{
			int[] colors = model.getTriangleColorA();
			if (colors == null)
			{
				return true;
			}
			for (int color : colors)
			{
				if (color != HIDDEN)
				{
					return false;
				}
			}
		}
		return true;
	}

	private int paintFloorObject(Renderable renderable, int objectZ, int[] levels, boolean wide, int footprint, boolean known, boolean bare, int target, boolean shade, int reference)
	{
		Model model = modelOf(renderable);
		if (model == null)
		{
			return 0;
		}
		int[] c1 = model.getFaceColors1();
		int[] c2 = model.getFaceColors2();
		int[] c3 = model.getFaceColors3();
		float[] ys = model.getVerticesY();
		int[] f1 = model.getFaceIndices1();
		int[] f2 = model.getFaceIndices2();
		int[] f3 = model.getFaceIndices3();
		if (c1 == null || c2 == null || c3 == null || ys == null || f1 == null || f2 == null || f3 == null)
		{
			return 0;
		}
		int n = model.getFaceCount();
		int vertices = Math.min(model.getVerticesCount(), ys.length);
		if (vertices == 0 || n == 0)
		{
			return 0;
		}
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < vertices; i++)
		{
			min = Math.min(min, ys[i]);
			max = Math.max(max, ys[i]);
		}

		if (levels.length == 0 && !known && !bare)
		{
			return 0;
		}
		boolean[] mask = null;
		if (max - min <= FLAT_OBJECT_HEIGHT)
		{
			// A flat model is only floor when it rests on one. Cave wall caps, roof slabs and ledges are flat
			// too, but they hang a storey above the tile height. A known floor, or a flat object on a tile
			// whose own terrain is hidden (the object IS the floor there), may float wherever it likes
			if (!known && !bare && !isFloorHeight(objectZ + max, levels, false))
			{
				return 0;
			}
		}
		else
		{
			mask = new boolean[n];
			boolean any = false;
			for (int i = 0; i < n; i++)
			{
				if (f1[i] >= vertices || f2[i] >= vertices || f3[i] >= vertices)
				{
					continue;
				}
				float y1 = ys[f1[i]];
				float y2 = ys[f2[i]];
				float y3 = ys[f3[i]];
				float lo = Math.min(y1, Math.min(y2, y3));
				float hi = Math.max(y1, Math.max(y2, y3));
				if (hi - lo > FACE_SLOPE)
				{
					continue;
				}
				float average = (y1 + y2 + y3) / 3f;
				boolean top = wide && average - min <= FACE_SLOPE;
				if (known || isFloorHeight(objectZ + average, levels, top))
				{
					mask[i] = true;
					any = true;
				}
			}
			if (!any || (!known && coverage(model, mask, footprint) < MIN_COVERAGE))
			{
				// a few level facets on a rock face or wall base are not a floor
				return 0;
			}
		}

		if (!originals.containsKey(model))
		{
			short[] tex = model.getFaceTextures();
			int[] copy = new int[tex != null ? 4 * n : 3 * n];
			System.arraycopy(c1, 0, copy, 0, n);
			System.arraycopy(c2, 0, copy, n, n);
			System.arraycopy(c3, 0, copy, 2 * n, n);
			if (tex != null)
			{
				for (int i = 0; i < n; i++)
				{
					copy[3 * n + i] = tex[i];
				}
			}
			originals.put(model, copy);
		}
		int[] orig = originals.get(model);
		short[] tex = model.getFaceTextures();
		for (int i = 0; i < n; i++)
		{
			if (mask != null && !mask[i])
			{
				continue;
			}
			c1[i] = recolor(orig[i], target, shade, reference);
			c2[i] = recolor(orig[n + i], target, shade, reference);
			// colour3 of -1 means flat-shaded face, -2 means hidden face; both must survive
			c3[i] = recolor(orig[2 * n + i], target, shade, reference);
			if (tex != null)
			{
				tex[i] = -1;
			}
		}
		return 1;
	}

	private void paintPaint(SceneTilePaint paint, int target, boolean shade, int reference)
	{
		if (!originals.containsKey(paint))
		{
			originals.put(paint, new int[]{
				paint.getSwColor(), paint.getSeColor(), paint.getNeColor(), paint.getNwColor(), paint.getTexture()
			});
		}
		int[] orig = originals.get(paint);
		paint.setSwColor(recolor(orig[0], target, shade, reference));
		paint.setSeColor(recolor(orig[1], target, shade, reference));
		paint.setNeColor(recolor(orig[2], target, shade, reference));
		paint.setNwColor(recolor(orig[3], target, shade, reference));
		paint.setTexture(-1);
	}

	private void paintModel(SceneTileModel model, int target, boolean shade, int reference)
	{
		int[] a = model.getTriangleColorA();
		int[] b = model.getTriangleColorB();
		int[] c = model.getTriangleColorC();
		int[] tex = model.getTriangleTextureId();
		if (a == null || b == null || c == null)
		{
			return;
		}
		int n = a.length;
		if (!originals.containsKey(model))
		{
			int[] copy = new int[tex != null ? 4 * n : 3 * n];
			System.arraycopy(a, 0, copy, 0, n);
			System.arraycopy(b, 0, copy, n, n);
			System.arraycopy(c, 0, copy, 2 * n, n);
			if (tex != null)
			{
				System.arraycopy(tex, 0, copy, 3 * n, n);
			}
			originals.put(model, copy);
		}
		int[] orig = originals.get(model);
		for (int i = 0; i < n; i++)
		{
			a[i] = recolor(orig[i], target, shade, reference);
			b[i] = recolor(orig[n + i], target, shade, reference);
			c[i] = recolor(orig[2 * n + i], target, shade, reference);
			if (tex != null)
			{
				tex[i] = -1;
			}
		}
	}

	/**
	 * Replace one packed HSL vertex colour with the target. With shading on, the vertex keeps its brightness
	 * offset from the scene's average floor brightness, so the lit and shadowed sides of hills still differ,
	 * clamped to the configured min/max brightness so no area gets washed out or too dark.
	 */
	private int recolor(int original, int target, boolean shade, int reference)
	{
		if (original == HIDDEN || original < 0)
		{
			return original;
		}
		if (strength < 100)
		{
			return overlay(original);
		}
		int lum = JagexColor.unpackLuminance((short) target);
		if (shade)
		{
			lum += JagexColor.unpackLuminance((short) original) - reference;
		}
		lum = Math.max(minLum, Math.min(maxLum, lum));
		return JagexColor.packHSL(
			JagexColor.unpackHue((short) target),
			JagexColor.unpackSaturation((short) target),
			lum) & 0xFFFF;
	}

	/**
	 * Partial strength: the chosen colour laid over the original like a translucent layer. Blended in RGB, so a
	 * brown floor heading for blue goes through grey-blue, never through green or purple.
	 */
	private int overlay(int original)
	{
		int key = original & 0xFFFF;
		int cached = blendCache[key];
		if (cached >= 0)
		{
			return cached;
		}
		int rgb = hslToRgb(key, brightness);
		int r = mix(rgb >> 16 & 0xFF, targetRgb >> 16 & 0xFF);
		int g = mix(rgb >> 8 & 0xFF, targetRgb >> 8 & 0xFF);
		int b = mix(rgb & 0xFF, targetRgb & 0xFF);
		int packed = JagexColor.rgbToHSL(r << 16 | g << 8 | b, brightness) & 0xFFFF;
		int lum = Math.max(minLum, Math.min(maxLum, JagexColor.unpackLuminance((short) packed)));
		packed = JagexColor.packHSL(JagexColor.unpackHue((short) packed), JagexColor.unpackSaturation((short) packed), lum) & 0xFFFF;
		blendCache[key] = packed;
		return packed;
	}

	private int mix(int from, int to)
	{
		return from + Math.round((to - from) * strength / 100f);
	}

	/** The client's own palette formula: packed HSL to the RGB it is drawn with at this brightness setting. */
	private static int hslToRgb(int hsl, double brightness)
	{
		double hue = JagexColor.unpackHue((short) hsl) / 64.0 + 0.0078125;
		double sat = JagexColor.unpackSaturation((short) hsl) / 8.0 + 0.0625;
		double lum = JagexColor.unpackLuminance((short) hsl) / 128.0;
		double r = lum;
		double g = lum;
		double b = lum;
		if (sat != 0.0)
		{
			double q = lum < 0.5 ? lum * (1.0 + sat) : lum + sat - lum * sat;
			double p = 2.0 * lum - q;
			double hr = hue + 1.0 / 3.0;
			if (hr > 1.0)
			{
				hr -= 1.0;
			}
			double hb = hue - 1.0 / 3.0;
			if (hb < 0.0)
			{
				hb += 1.0;
			}
			r = channel(p, q, hr);
			g = channel(p, q, hue);
			b = channel(p, q, hb);
		}
		return gamma(r, brightness) << 16 | gamma(g, brightness) << 8 | gamma(b, brightness);
	}

	private static double channel(double p, double q, double h)
	{
		if (6.0 * h < 1.0)
		{
			return p + (q - p) * 6.0 * h;
		}
		if (2.0 * h < 1.0)
		{
			return q;
		}
		if (3.0 * h < 2.0)
		{
			return p + (q - p) * (2.0 / 3.0 - h) * 6.0;
		}
		return p;
	}

	private static int gamma(double value, double brightness)
	{
		int c = (int) (value * 256.0);
		c = (int) (Math.pow(c / 256.0, brightness) * 256.0);
		return Math.max(0, Math.min(255, c));
	}

	private int averageLuminance(Tile[][][] tiles)
	{
		long sum = 0;
		int count = 0;
		for (Tile[][] plane : tiles)
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					SceneTilePaint paint = tile.getSceneTilePaint();
					if (paint == null)
					{
						continue;
					}
					int[] orig = originals.get(paint);
					int[] colors = orig != null
						? orig
						: new int[]{paint.getSwColor(), paint.getSeColor(), paint.getNeColor(), paint.getNwColor()};
					for (int i = 0; i < 4; i++)
					{
						if (colors[i] != HIDDEN)
						{
							sum += JagexColor.unpackLuminance((short) colors[i]);
							count++;
						}
					}
				}
			}
		}
		return count == 0 ? JagexColor.LUMINANCE_MAX / 2 : (int) (sum / count);
	}

	private void restoreAll()
	{
		for (Map.Entry<Object, int[]> entry : originals.entrySet())
		{
			Object key = entry.getKey();
			int[] orig = entry.getValue();
			if (key instanceof SceneTilePaint)
			{
				SceneTilePaint paint = (SceneTilePaint) key;
				paint.setSwColor(orig[0]);
				paint.setSeColor(orig[1]);
				paint.setNeColor(orig[2]);
				paint.setNwColor(orig[3]);
				paint.setTexture(orig[4]);
			}
			else if (key instanceof Model)
			{
				Model model = (Model) key;
				int n = model.getFaceCount();
				System.arraycopy(orig, 0, model.getFaceColors1(), 0, n);
				System.arraycopy(orig, n, model.getFaceColors2(), 0, n);
				System.arraycopy(orig, 2 * n, model.getFaceColors3(), 0, n);
				short[] tex = model.getFaceTextures();
				if (tex != null && orig.length == 4 * n)
				{
					for (int i = 0; i < n; i++)
					{
						tex[i] = (short) orig[3 * n + i];
					}
				}
			}
			else if (key instanceof SceneTileModel)
			{
				SceneTileModel model = (SceneTileModel) key;
				int[] a = model.getTriangleColorA();
				int n = a.length;
				System.arraycopy(orig, 0, a, 0, n);
				System.arraycopy(orig, n, model.getTriangleColorB(), 0, n);
				System.arraycopy(orig, 2 * n, model.getTriangleColorC(), 0, n);
				int[] tex = model.getTriangleTextureId();
				if (tex != null && orig.length == 4 * n)
				{
					System.arraycopy(orig, 3 * n, tex, 0, n);
				}
			}
		}
		originals.clear();
		animatedModels.clear();
		painted = false;

		List<Scene> scenes = new ArrayList<>(paintedScenes);
		paintedScenes.clear();
		for (Scene scene : scenes)
		{
			invalidate(scene);
		}
	}

	// Tell the GPU plugin (if it is on) to re-upload the whole scene; the software renderer reads the
	// colours every frame and needs nothing.
	/**
	 * Describe the column the player stands in, every plane of it, in the client log and on the debug overlay:
	 * terrain, every object and why each was or was not painted. The floor you see is often a lower plane than
	 * the one you stand on.
	 */
	private void debugTile()
	{
		Player local = client.getLocalPlayer();
		WorldView view = local != null ? local.getWorldView() : null;
		LocalPoint lp = local != null ? local.getLocalLocation() : null;
		Scene scene = view != null ? view.getScene() : null;
		if (scene == null || lp == null)
		{
			return;
		}
		int plane = view.getPlane();
		int sx = lp.getSceneX();
		int sy = lp.getSceneY();
		String where = "standing on";
		// The tile under the mouse wins, so a wall or ledge you cannot stand on can be inspected too
		Tile hovered = view.getSelectedSceneTile();
		Point hoverLoc = hovered != null ? hovered.getSceneLocation() : null;
		if (hoverLoc != null)
		{
			plane = hovered.getRenderLevel();
			sx = hoverLoc.getX();
			sy = hoverLoc.getY();
			where = "hovering";
		}
		int key = (plane << 16) | ((sx & 0xFF) << 8) | (sy & 0xFF);
		if (key == lastDebugTile)
		{
			return;
		}
		lastDebugTile = key;
		Tile[][][] tiles = scene.getTiles();
		if (tiles == null || sx < 0 || sy < 0)
		{
			return;
		}
		int[][][] heights = scene.getTileHeights();
		int offset = heightOffset(scene);
		List<String> lines = new ArrayList<>();
		lines.add(String.format("v%s %s plane %d scene %d,%d region %d view %s painted=%b (scene %s)", VERSION, where, plane, sx, sy, currentRegion,
			view.isTopLevel() ? "top" : "sub " + view.getId() + " " + view.getSizeX() + "x" + view.getSizeY(), painted,
			paintedScenes.contains(scene) ? "painted" : "not painted"));
		CollisionData[] collision = view.getCollisionMaps();
		for (int z = 0; z < tiles.length; z++)
		{
			if (sx >= tiles[z].length || sy >= tiles[z][sx].length || tiles[z][sx][sy] == null)
			{
				continue;
			}
			Tile tile = tiles[z][sx][sy];
			describeTile(tile, "plane " + z + walkability(collision, z, sx, sy), heights, offset, lines);
			if (tile.getBridge() != null)
			{
				describeTile(tile.getBridge(), "plane " + z + " bridge", heights, offset, lines);
			}
		}
		debugLines.clear();
		debugLines.addAll(lines);
		for (String line : lines)
		{
			log.info("Floor Painter: {}", line);
		}
	}

	/** " blocked" / " blocked by object" / "" for the debug line, from the collision map. */
	private static String walkability(CollisionData[] collision, int plane, int sx, int sy)
	{
		if (collision == null || plane >= collision.length || collision[plane] == null)
		{
			return "";
		}
		int[][] flags = collision[plane].getFlags();
		if (flags == null || sx >= flags.length || flags[sx] == null || sy >= flags[sx].length)
		{
			return "";
		}
		int f = flags[sx][sy];
		if ((f & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
		{
			return " blocked";
		}
		if ((f & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0)
		{
			return " blocked by object";
		}
		return "";
	}

	private void describeTile(Tile tile, String label, int[][][] heights, int offset, List<String> lines)
	{
		int[] own = walkableHeights(tile, heights, offset, false);
		int[] levels = walkableHeights(tile, heights, offset, true);
		StringBuilder sb = new StringBuilder(label).append(" heights=").append(Arrays.toString(levels));
		SceneTilePaint paint = tile.getSceneTilePaint();
		if (paint != null)
		{
			sb.append(" paint tex=").append(paint.getTexture())
				.append(paint.getNeColor() == HIDDEN ? " hidden" : "")

				.append(originals.containsKey(paint) ? " (painted)" : " (not painted)");
		}
		SceneTileModel model = tile.getSceneTileModel();
		if (model != null)
		{
			int[] a = model.getTriangleColorA();
			sb.append(" tile model tris=").append(a != null ? a.length : -1)
				.append(originals.containsKey(model) ? " (painted)" : " (not painted)");
		}
		if (paint == null && model == null)
		{
			sb.append(" no terrain");
		}
		lines.add(sb.toString());
		GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			lines.add(describe("ground", ground.getId(), ground.getRenderable(), ground.getZ(), own, false, "1x1", 1));
		}
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null)
				{
					Point min = gameObject.getSceneMinLocation();
					Point max = gameObject.getSceneMaxLocation();
					String size = min != null && max != null
						? (max.getX() - min.getX() + 1) + "x" + (max.getY() - min.getY() + 1)
						: "?";
					boolean wide = isWide(gameObject);
					lines.add(describe("object", gameObject.getId(), gameObject.getRenderable(), gameObject.getZ(), wide ? levels : own, wide, size, footprint(gameObject)));
				}
			}
		}
		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null)
		{
			lines.add(describe("decor", deco.getId(), deco.getRenderable(), deco.getZ(), own, false, "1x1", 1));
			if (deco.getRenderable2() != null)
			{
				lines.add(describe("decor2", deco.getId(), deco.getRenderable2(), deco.getZ(), own, false, "1x1", 1));
			}
		}
	}

	private String describe(String kind, int id, Renderable renderable, int objectZ, int[] levels, boolean wide, String size, int footprint)
	{
		StringBuilder sb = new StringBuilder("  ").append(kind).append(' ').append(id).append(' ').append(size).append(' ');
		if (excluded(id))
		{
			sb.append("excluded ");
		}
		if (KNOWN_FLOOR.contains(id))
		{
			sb.append("known-floor ");
		}
		if (renderable == null)
		{
			return sb.append("null").toString();
		}
		if (renderable instanceof DynamicObject)
		{
			sb.append("animated ");
		}
		Model model = modelOf(renderable);
		if (model == null)
		{
			return sb.append(renderable.getClass().getSimpleName()).toString();
		}
		float[] ys = model.getVerticesY();
		int[] f1 = model.getFaceIndices1();
		int[] f2 = model.getFaceIndices2();
		int[] f3 = model.getFaceIndices3();
		int n = model.getFaceCount();
		if (ys == null || f1 == null || f2 == null || f3 == null)
		{
			return sb.append("model without geometry").toString();
		}
		int vertices = Math.min(model.getVerticesCount(), ys.length);
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < vertices; i++)
		{
			min = Math.min(min, ys[i]);
			max = Math.max(max, ys[i]);
		}
		int level = 0;
		int floor = 0;
		float nearest = Float.MAX_VALUE;
		boolean[] mask = new boolean[n];
		for (int i = 0; i < n; i++)
		{
			if (f1[i] >= vertices || f2[i] >= vertices || f3[i] >= vertices)
			{
				continue;
			}
			float y1 = ys[f1[i]];
			float y2 = ys[f2[i]];
			float y3 = ys[f3[i]];
			if (Math.max(y1, Math.max(y2, y3)) - Math.min(y1, Math.min(y2, y3)) > FACE_SLOPE)
			{
				continue;
			}
			level++;
			float average = (y1 + y2 + y3) / 3f;
			float absolute = objectZ + average;
			for (int h : levels)
			{
				nearest = Math.min(nearest, Math.abs(absolute - h));
			}
			if (isFloorHeight(absolute, levels, wide && average - min <= FACE_SLOPE))
			{
				floor++;
				mask[i] = true;
			}
		}
		sb.append(String.format("faces=%d level=%d floor=%d cover=%.0f%% thick=%b y=%.0f..%.0f z=%d nearest=%s tex=%b %s",
			n, level, floor, coverage(model, mask, footprint) * 100, max - min > FLAT_OBJECT_HEIGHT, min, max, objectZ,
			nearest == Float.MAX_VALUE ? "-" : String.format("%.0f", nearest),
			model.getFaceTextures() != null, originals.containsKey(model) ? "(painted)" : "(not painted)"));
		return sb.toString();
	}

	private void invalidate(Scene scene)
	{
		DrawCallbacks callbacks = client.getDrawCallbacks();
		if (callbacks == null)
		{
			return;
		}
		// The GPU plugin keeps ZONES x ZONES zones for the top-level scene but only size/8 for a sub world view;
		// asking for a zone it does not have throws and the scene never gets re-uploaded
		int zonesX = ZONES;
		int zonesY = ZONES;
		if (scene.getWorldViewId() != WorldView.TOPLEVEL)
		{
			WorldView view = client.getWorldView(scene.getWorldViewId());
			if (view == null)
			{
				return;
			}
			zonesX = view.getSizeX() >> 3;
			zonesY = view.getSizeY() >> 3;
		}
		try
		{
			for (int x = 0; x < zonesX; x++)
			{
				for (int y = 0; y < zonesY; y++)
				{
					callbacks.invalidateZone(scene, x, y);
				}
			}
		}
		catch (RuntimeException e)
		{
			log.warn("Floor Painter: could not invalidate world view {} ({}x{} zones)", scene.getWorldViewId(), zonesX, zonesY, e);
		}
	}
}
