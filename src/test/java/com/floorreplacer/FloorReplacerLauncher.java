package com.floorreplacer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// ./gradlew run, or run this from the IDE with -ea
public class FloorReplacerLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FloorReplacerPlugin.class);
		RuneLite.main(args);
	}
}
