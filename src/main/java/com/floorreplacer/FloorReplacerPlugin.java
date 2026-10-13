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
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Floor Replacer",
	description = "Paints every floor tile in the scene a single colour of your choice",
	tags = {"floor", "ground", "colour", "color", "texture", "recolor", "scene", "terrain", "blindfold", "skybox", "clear", "flat", "visibility"}
)
public class FloorReplacerPlugin extends Plugin
{
	static final String VERSION = "1.1.7";

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
	// Floor objects that carry mechanic information and must keep their own look. Akkha's arena in Tombs of
	// Amascut: AKKHA_PLATFORM01-24 (the quadrant floor), AKKHA_ARENA_OUTER_LVL1/LVL2 rings, the hourglass
	// floor pieces (gameval ObjectID 45872-46067)
	private static final int AKKHA_FLOOR_FIRST = 45872;
	private static final int AKKHA_FLOOR_LAST = 46067;
	// Chambers of Xeric energy pools (RAIDS_ENERGY_POOL and its glow): flat, but you need to see them
	private static final Set<Integer> NEVER_PAINT = new HashSet<>(Arrays.asList(30066, 30067));
	// Scene.getTileHeights() is indexed in extended scene coordinates for the top-level world view, while
	// Tile.getSceneLocation() is in plain scene coordinates (same convention as the GPU plugin's uploader)
	private static final int SCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FloorReplacerConfig config;

	// Original colours of every paint/model we touched, so disabling or changing the colour restores them
	// without a scene reload. Keyed by identity: the client creates fresh objects for each scene build.
	private final Map<Object, int[]> originals = new IdentityHashMap<>();
	private final Set<Scene> paintedScenes = Collections.newSetFromMap(new IdentityHashMap<>());

	// Map region the local player stands in (template region inside instances); -1 when unknown
	private int currentRegion = -1;
	// True while the current scene carries our colours
	private boolean painted;
	// Object IDs the user never wants painted, parsed from config at each paint pass
	private Set<Integer> excludedIds = Collections.emptySet();
	// Tile last written to the debug log, so each tile is described once per visit
	private int lastDebugTile = -1;

	// Luminance clamp (0..127) applied when shading is kept; set from config at the start of each paint pass
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
		clientThread.invoke(this::evaluate);
	}

	@Override
	protected void shutDown()
	{
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

	// Objects that spawn later (acid pools, shadow markers, arena mechanics) are deliberately left alone: they
	// are gameplay, not floor. Only the scene paint passes (map load, login) touch floor objects.

	@Subscribe
	public void onGameTick(GameTick event)
	{
		evaluate();
		if (config.debugLog())
		{
			debugTile();
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
		if (top != null)
		{
			paintScene(top.getScene());
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
		double brightness = textures != null ? textures.getBrightness() : DEFAULT_BRIGHTNESS;
		int target = JagexColor.rgbToHSL(config.color().getRGB() & 0xFFFFFF, brightness) & 0xFFFF;
		boolean shade = config.keepShading();
		boolean objects = config.floorObjects();
		minLum = JagexColor.LUMINANCE_MAX * Math.max(0, Math.min(100, config.minBrightness())) / 100;
		maxLum = JagexColor.LUMINANCE_MAX * Math.max(0, Math.min(100, config.maxBrightness())) / 100;
		if (maxLum < minLum)
		{
			maxLum = minLum;
		}
		int reference = shade ? averageLuminance(tiles) : 0;
		excludedIds = parseIds(config.excludedObjects());

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
			int[] levels = walkableHeights(tile, heights, offset);
			GroundObject ground = tile.getGroundObject();
			if (ground != null && !excluded(ground.getId()))
			{
				painted += paintFloorObject(ground.getRenderable(), ground.getZ(), levels, target, shade, reference);
			}
			GameObject[] gameObjects = tile.getGameObjects();
			if (gameObjects != null)
			{
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null && !excluded(gameObject.getId()))
					{
						painted += paintFloorObject(gameObject.getRenderable(), gameObject.getZ(), levels, target, shade, reference);
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

	/** Tile heights of every plane at this tile's column, or empty when the tile is outside the height map. */
	private static int[] walkableHeights(Tile tile, int[][][] heights, int offset)
	{
		Point loc = tile.getSceneLocation();
		if (heights == null || loc == null)
		{
			return new int[0];
		}
		int sx = loc.getX() + offset;
		int sy = loc.getY() + offset;
		int[] out = new int[heights.length];
		int count = 0;
		for (int[][] plane : heights)
		{
			if (plane != null && sx >= 0 && sx < plane.length && plane[sx] != null && sy >= 0 && sy < plane[sx].length)
			{
				out[count++] = plane[sx][sy];
			}
		}
		return count == out.length ? out : java.util.Arrays.copyOf(out, count);
	}

	/**
	 * Floors that are really models on the tile. A near-flat model (mosaic, paving, rug, trapdoor, puddle) is
	 * painted whole. A taller static model (arena platform, dock, bridge deck) only has its level faces painted,
	 * and only those sitting at a plane's tile height: the surface you walk on, not the sides or the underside.
	 * Animated objects and actors are left alone.
	 */
	private int paintFloorObject(Renderable renderable, int objectZ, int[] levels, int target, boolean shade, int reference)
	{
		if (!(renderable instanceof Model))
		{
			return 0;
		}
		Model model = (Model) renderable;
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

		boolean[] mask = null;
		if (max - min > FLAT_OBJECT_HEIGHT)
		{
			if (levels.length == 0)
			{
				return 0;
			}
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
				float absolute = objectZ + (y1 + y2 + y3) / 3f;
				for (int level : levels)
				{
					if (Math.abs(absolute - level) <= HEIGHT_TOLERANCE)
					{
						mask[i] = true;
						any = true;
						break;
					}
				}
			}
			if (!any)
			{
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
		if (!shade)
		{
			return target;
		}
		int lum = JagexColor.unpackLuminance((short) target) + (JagexColor.unpackLuminance((short) original) - reference);
		lum = Math.max(minLum, Math.min(maxLum, lum));
		return JagexColor.packHSL(
			JagexColor.unpackHue((short) target),
			JagexColor.unpackSaturation((short) target),
			lum) & 0xFFFF;
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
	 * Describe the column the player stands in, every plane of it, in the client log: terrain, every object and
	 * why each was or was not painted. The floor you see is often a lower plane than the one you stand on.
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
		log.info("Floor Replacer: standing on plane {} scene {},{} region {} painted={} v{}", plane, sx, sy, currentRegion, painted, VERSION);
		for (int z = 0; z < tiles.length; z++)
		{
			if (sx >= tiles[z].length || sy >= tiles[z][sx].length || tiles[z][sx][sy] == null)
			{
				continue;
			}
			Tile tile = tiles[z][sx][sy];
			describeTile(tile, "plane " + z, heights, offset);
			if (tile.getBridge() != null)
			{
				describeTile(tile.getBridge(), "plane " + z + " bridge", heights, offset);
			}
		}
	}

	private void describeTile(Tile tile, String label, int[][][] heights, int offset)
	{
		int[] levels = walkableHeights(tile, heights, offset);
		StringBuilder sb = new StringBuilder("Floor Replacer:   ").append(label)
			.append(" heights=").append(Arrays.toString(levels));
		SceneTilePaint paint = tile.getSceneTilePaint();
		if (paint != null)
		{
			sb.append(" | paint tex=").append(paint.getTexture()).append(" sw=").append(paint.getSwColor())
				.append(paint.getNeColor() == HIDDEN ? " hidden" : "")
				.append(originals.containsKey(paint) ? " (painted)" : " (not painted)");
		}
		SceneTileModel model = tile.getSceneTileModel();
		if (model != null)
		{
			int[] a = model.getTriangleColorA();
			sb.append(" | tile model tris=").append(a != null ? a.length : -1)
				.append(originals.containsKey(model) ? " (painted)" : " (not painted)");
		}
		GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			describe(sb, "ground", ground.getId(), ground.getRenderable(), ground.getZ(), levels);
		}
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null)
				{
					describe(sb, "object", gameObject.getId(), gameObject.getRenderable(), gameObject.getZ(), levels);
				}
			}
		}
		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null)
		{
			describe(sb, "decor", deco.getId(), deco.getRenderable(), deco.getZ(), levels);
			if (deco.getRenderable2() != null)
			{
				describe(sb, "decor2", deco.getId(), deco.getRenderable2(), deco.getZ(), levels);
			}
		}
		if (paint == null && model == null && ground == null && gameObjects == null && deco == null)
		{
			sb.append(" | empty");
		}
		log.info(sb.toString());
	}

	private void describe(StringBuilder sb, String kind, int id, Renderable renderable, int objectZ, int[] levels)
	{
		sb.append(" | ").append(kind).append(' ').append(id).append(' ');
		if (excluded(id))
		{
			sb.append("excluded ");
		}
		if (renderable == null)
		{
			sb.append("null");
			return;
		}
		if (!(renderable instanceof Model))
		{
			sb.append(renderable instanceof DynamicObject ? "animated (skipped)" : renderable.getClass().getSimpleName());
			return;
		}
		Model model = (Model) renderable;
		float[] ys = model.getVerticesY();
		int[] f1 = model.getFaceIndices1();
		int[] f2 = model.getFaceIndices2();
		int[] f3 = model.getFaceIndices3();
		int n = model.getFaceCount();
		if (ys == null || f1 == null || f2 == null || f3 == null)
		{
			sb.append("model without geometry");
			return;
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
		int atFloor = 0;
		float nearest = Float.MAX_VALUE;
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
			float absolute = objectZ + (y1 + y2 + y3) / 3f;
			for (int h : levels)
			{
				float diff = Math.abs(absolute - h);
				nearest = Math.min(nearest, diff);
			}
			for (int h : levels)
			{
				if (Math.abs(absolute - h) <= HEIGHT_TOLERANCE)
				{
					atFloor++;
					break;
				}
			}
		}
		sb.append(String.format("model faces=%d levelFaces=%d atFloor=%d y=%.0f..%.0f z=%d nearest=%s textured=%b %s",
			n, level, atFloor, min, max, objectZ, nearest == Float.MAX_VALUE ? "-" : String.format("%.0f", nearest),
			model.getFaceTextures() != null, originals.containsKey(model) ? "(painted)" : "(not painted)"));
	}

	private void invalidate(Scene scene)
	{
		DrawCallbacks callbacks = client.getDrawCallbacks();
		if (callbacks == null)
		{
			return;
		}
		for (int x = 0; x < ZONES; x++)
		{
			for (int y = 0; y < ZONES; y++)
			{
				callbacks.invalidateZone(scene, x, y);
			}
		}
	}
}
