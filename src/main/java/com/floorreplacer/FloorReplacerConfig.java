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
		return new Color(60, 140, 60);
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
}
