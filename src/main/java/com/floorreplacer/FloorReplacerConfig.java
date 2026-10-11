package com.floorreplacer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(FloorReplacerConfig.GROUP)
public interface FloorReplacerConfig extends Config
{
	String GROUP = "floorreplacer";

	@ConfigItem(
		keyName = "color",
		name = "Floor colour",
		description = "The colour every floor tile is painted",
		position = 0
	)
	default Color color()
	{
		return new Color(0x64, 0x87, 0x8D);
	}

	@ConfigItem(
		keyName = "keepShading",
		name = "Keep shading",
		description = "Keep the original light and dark variation of the ground so slopes and paths stay visible. Off gives one perfectly flat colour.",
		position = 1
	)
	default boolean keepShading()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "minBrightness",
		name = "Min brightness",
		description = "With Keep shading on, no tile is drawn darker than this.",
		position = 2
	)
	default int minBrightness()
	{
		return 20;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "maxBrightness",
		name = "Max brightness",
		description = "With Keep shading on, no tile is drawn brighter than this.",
		position = 3
	)
	default int maxBrightness()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "floorObjects",
		name = "Flat floor objects",
		description = "Also recolour flat models lying on the ground: mosaics, paving, rugs, trapdoors, puddles. Anything with height is left alone.",
		position = 4
	)
	default boolean floorObjects()
	{
		return true;
	}
}
