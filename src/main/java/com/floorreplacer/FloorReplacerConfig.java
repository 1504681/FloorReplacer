package com.floorreplacer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(FloorReplacerConfig.GROUP)
public interface FloorReplacerConfig extends Config
{
	String GROUP = "floorreplacer";

	enum AreaMode
	{
		EVERYWHERE("Everywhere"),
		SELECTED_AREAS("Selected areas only");

		private final String label;

		AreaMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "areaMode",
		name = "Where",
		description = "Paint the floor everywhere, or only in the areas ticked below",
		position = -1
	)
	default AreaMode areaMode()
	{
		return AreaMode.EVERYWHERE;
	}

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

	@ConfigSection(
		name = "Areas",
		description = "Which areas get the painted floor when Where is set to Selected areas only",
		position = 10,
		closedByDefault = true
	)
	String areas = "areas";

	@ConfigItem(keyName = "tobMaiden", name = "ToB: Maiden", description = "Paint the floor in Maiden", section = areas, position = 0)
	default boolean tobMaiden()
	{
		return true;
	}

	@ConfigItem(keyName = "tobBloat", name = "ToB: Bloat", description = "Paint the floor in Bloat", section = areas, position = 1)
	default boolean tobBloat()
	{
		return true;
	}

	@ConfigItem(keyName = "tobNylocas", name = "ToB: Nylocas", description = "Paint the floor in Nylocas", section = areas, position = 2)
	default boolean tobNylocas()
	{
		return true;
	}

	@ConfigItem(keyName = "tobSotetseg", name = "ToB: Sotetseg", description = "Paint the floor in Sotetseg", section = areas, position = 3)
	default boolean tobSotetseg()
	{
		return true;
	}

	@ConfigItem(keyName = "tobXarpus", name = "ToB: Xarpus", description = "Paint the floor in Xarpus", section = areas, position = 4)
	default boolean tobXarpus()
	{
		return true;
	}

	@ConfigItem(keyName = "tobVerzik", name = "ToB: Verzik", description = "Paint the floor in Verzik", section = areas, position = 5)
	default boolean tobVerzik()
	{
		return true;
	}

	@ConfigItem(keyName = "toaZebak", name = "ToA: Crondis + Zebak", description = "Paint the floor in Crondis + Zebak", section = areas, position = 6)
	default boolean toaZebak()
	{
		return true;
	}

	@ConfigItem(keyName = "toaKephri", name = "ToA: Scabaras + Kephri", description = "Paint the floor in Scabaras + Kephri", section = areas, position = 7)
	default boolean toaKephri()
	{
		return true;
	}

	@ConfigItem(keyName = "toaAkkha", name = "ToA: Het + Akkha", description = "Paint the floor in Het + Akkha", section = areas, position = 8)
	default boolean toaAkkha()
	{
		return true;
	}

	@ConfigItem(keyName = "toaBaba", name = "ToA: Apmeken + Ba-Ba", description = "Paint the floor in Apmeken + Ba-Ba", section = areas, position = 9)
	default boolean toaBaba()
	{
		return true;
	}

	@ConfigItem(keyName = "toaWardens", name = "ToA: Wardens", description = "Paint the floor in Wardens", section = areas, position = 10)
	default boolean toaWardens()
	{
		return true;
	}

	@ConfigItem(keyName = "cox", name = "Chambers of Xeric", description = "Paint the floor in Chambers of Xeric", section = areas, position = 11)
	default boolean cox()
	{
		return true;
	}

	@ConfigItem(keyName = "inferno", name = "Inferno", description = "Paint the floor in Inferno", section = areas, position = 12)
	default boolean inferno()
	{
		return true;
	}

	@ConfigItem(keyName = "fightCave", name = "Fight Cave", description = "Paint the floor in Fight Cave", section = areas, position = 13)
	default boolean fightCave()
	{
		return true;
	}

	@ConfigItem(keyName = "colosseum", name = "Fortis Colosseum", description = "Paint the floor in Fortis Colosseum", section = areas, position = 14)
	default boolean colosseum()
	{
		return true;
	}

	@ConfigItem(keyName = "doom", name = "Doom of Mokhaiotl", description = "Paint the floor in Doom of Mokhaiotl", section = areas, position = 15)
	default boolean doom()
	{
		return true;
	}

	@ConfigItem(keyName = "nex", name = "Nex", description = "Paint the floor in Nex", section = areas, position = 16)
	default boolean nex()
	{
		return true;
	}

	@ConfigItem(keyName = "nightmare", name = "Nightmare", description = "Paint the floor in Nightmare", section = areas, position = 17)
	default boolean nightmare()
	{
		return true;
	}

	@ConfigItem(keyName = "yama", name = "Yama", description = "Paint the floor in Yama", section = areas, position = 18)
	default boolean yama()
	{
		return true;
	}

	@ConfigItem(
		keyName = "customRegions",
		name = "Custom region IDs",
		description = "Extra map regions to paint, comma separated (the ::loc command or the Region Locker plugin show the ID you stand in). Instanced areas use their template region.",
		section = areas,
		position = 50
	)
	default String customRegions()
	{
		return "";
	}

	@ConfigItem(
		keyName = "excludedObjects",
		name = "Never paint object IDs",
		description = "Floor object IDs to leave in their original colours, comma separated. Akkha's arena floor in Tombs of Amascut and the Chambers of Xeric energy pools and herb patches are always left alone.",
		position = 51
	)
	default String excludedObjects()
	{
		return "";
	}

	@ConfigItem(
		keyName = "debugLog",
		name = "Log the tile under you",
		description = "Writes what is on the tile you step onto (terrain, floor objects, heights, painted or not) to the client log. For bug reports about floors that stay unpainted.",
		position = 52
	)
	default boolean debugLog()
	{
		return false;
	}
}
