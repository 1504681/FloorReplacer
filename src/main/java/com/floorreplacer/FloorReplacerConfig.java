package com.floorreplacer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
		return new Color(0x59, 0xBD, 0xAC);
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

	@ConfigItem(
		keyName = "floorObjects",
		name = "Flat floor objects",
		description = "Also recolour flat models lying on the ground: mosaics, paving, rugs, trapdoors, puddles. Anything with height is left alone.",
		position = 2
	)
	default boolean floorObjects()
	{
		return true;
	}
}
