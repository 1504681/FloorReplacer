package com.floorreplacer;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.TextureProvider;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameStateChanged;
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
	static final String VERSION = "1.0.0";

	// Vertex colour the client uses for tiles that must not be drawn (under bridges etc.)
	private static final int HIDDEN = 12345678;
	private static final int ZONE_SIZE = 8;
	private static final int ZONES = Constants.EXTENDED_SCENE_SIZE / ZONE_SIZE;
	private static final double DEFAULT_BRIGHTNESS = 0.8;
	// Objects no taller than this (in model units; a tile is 128 wide) count as floor: mosaics, rugs, paving, puddles
	private static final int FLAT_OBJECT_HEIGHT = 24;

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

	@Provides
	FloorReplacerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FloorReplacerConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invoke(this::paintAll);
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
				break;
			case LOGGED_IN:
				paintAll();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		paintScene(event.getWorldView().getScene());
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
			paintAll();
		});
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
		int reference = shade ? averageLuminance(tiles) : 0;

		int painted = 0;
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
					painted += paintTile(tile, target, shade, reference, objects);
					Tile bridge = tile.getBridge();
					if (bridge != null)
					{
						painted += paintTile(bridge, target, shade, reference, objects);
					}
				}
			}
		}

		paintedScenes.add(scene);
		invalidate(scene);
		log.debug("Painted {} floor tiles in world view {}", painted, scene.getWorldViewId());
	}

	private int paintTile(Tile tile, int target, boolean shade, int reference, boolean objects)
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
			GroundObject ground = tile.getGroundObject();
			if (ground != null)
			{
				painted += paintFlatObject(ground.getRenderable(), target, shade, reference);
			}
			GameObject[] gameObjects = tile.getGameObjects();
			if (gameObjects != null)
			{
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null)
					{
						painted += paintFlatObject(gameObject.getRenderable(), target, shade, reference);
					}
				}
			}
		}
		return painted;
	}

	/**
	 * Floors that are really models lying on the tile (mosaics, paving, rugs, trapdoors, puddles). Only static,
	 * near-flat models qualify; animated objects, actors and anything with height are left alone.
	 */
	private int paintFlatObject(Renderable renderable, int target, boolean shade, int reference)
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
		if (c1 == null || c2 == null || c3 == null || ys == null)
		{
			return 0;
		}
		int n = model.getFaceCount();
		int vertices = Math.min(model.getVerticesCount(), ys.length);
		if (!originals.containsKey(model))
		{
			float min = Float.MAX_VALUE;
			float max = -Float.MAX_VALUE;
			for (int i = 0; i < vertices; i++)
			{
				min = Math.min(min, ys[i]);
				max = Math.max(max, ys[i]);
			}
			if (vertices == 0 || max - min > FLAT_OBJECT_HEIGHT)
			{
				return 0;
			}
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
	 * offset from the scene's average floor brightness, so the lit and shadowed sides of hills still differ.
	 */
	private static int recolor(int original, int target, boolean shade, int reference)
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
		lum = Math.max(0, Math.min(JagexColor.LUMINANCE_MAX, lum));
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

		List<Scene> scenes = new ArrayList<>(paintedScenes);
		paintedScenes.clear();
		for (Scene scene : scenes)
		{
			invalidate(scene);
		}
	}

	// Tell the GPU plugin (if it is on) to re-upload the whole scene; the software renderer reads the
	// colours every frame and needs nothing.
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
