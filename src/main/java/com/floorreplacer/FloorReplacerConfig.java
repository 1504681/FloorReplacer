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
		return Color.WHITE;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "strength",
		name = "Strength",
		description = "100% replaces the floor colour completely. Lower values lay the colour over the floor like a translucent layer: the floor keeps its own look underneath, softened and tinted.",
		position = 1
	)
	default int strength()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "keepShading",
		name = "Keep shading",
		description = "Keep the original light and dark variation of the ground so slopes and paths stay visible. Off gives one perfectly flat colour.",
		position = 2
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
		description = "With Keep shading on, no tile is drawn darker than this. Raise it to make the paint show on very dark floors such as boss arenas.",
		position = 3
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
		description = "With Keep shading on, no tile is drawn brighter than this. 100% is no limit.",
		position = 4
	)
	default int maxBrightness()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "floorObjects",
		name = "Flat floor objects",
		description = "Also recolour flat models lying on the ground: mosaics, paving, rugs, trapdoors, puddles. Anything with height is left alone.",
		position = 5
	)
	default boolean floorObjects()
	{
		return true;
	}

	@ConfigItem(
		keyName = "perArea",
		name = "Per-area colour and strength",
		description = "Give each area its own Floor colour and Strength, set under Per-area colour and strength below. Areas left on 'Pick a color' and -1 keep the main settings.",
		position = 6
	)
	default boolean perArea()
	{
		return false;
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

	@ConfigSection(
		name = "Per-area colour and strength",
		description = "Only used while Per-area colour and strength is on. Unset colours and -1 strengths fall back to the main settings",
		position = 11,
		closedByDefault = true
	)
	String perAreaSettings = "perAreaSettings";

	@ConfigItem(keyName = "tobMaidenColor", name = "ToB: Maiden colour", description = "Floor colour in ToB: Maiden; leave unset for the main Floor colour", section = perAreaSettings, position = 0)
	default Color tobMaidenColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobMaidenStrength", name = "ToB: Maiden strength", description = "Strength in ToB: Maiden; -1 uses the main Strength", section = perAreaSettings, position = 1)
	default int tobMaidenStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "tobBloatColor", name = "ToB: Bloat colour", description = "Floor colour in ToB: Bloat; leave unset for the main Floor colour", section = perAreaSettings, position = 2)
	default Color tobBloatColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobBloatStrength", name = "ToB: Bloat strength", description = "Strength in ToB: Bloat; -1 uses the main Strength", section = perAreaSettings, position = 3)
	default int tobBloatStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "tobNylocasColor", name = "ToB: Nylocas colour", description = "Floor colour in ToB: Nylocas; leave unset for the main Floor colour", section = perAreaSettings, position = 4)
	default Color tobNylocasColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobNylocasStrength", name = "ToB: Nylocas strength", description = "Strength in ToB: Nylocas; -1 uses the main Strength", section = perAreaSettings, position = 5)
	default int tobNylocasStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "tobSotetsegColor", name = "ToB: Sotetseg colour", description = "Floor colour in ToB: Sotetseg; leave unset for the main Floor colour", section = perAreaSettings, position = 6)
	default Color tobSotetsegColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobSotetsegStrength", name = "ToB: Sotetseg strength", description = "Strength in ToB: Sotetseg; -1 uses the main Strength", section = perAreaSettings, position = 7)
	default int tobSotetsegStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "tobXarpusColor", name = "ToB: Xarpus colour", description = "Floor colour in ToB: Xarpus; leave unset for the main Floor colour", section = perAreaSettings, position = 8)
	default Color tobXarpusColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobXarpusStrength", name = "ToB: Xarpus strength", description = "Strength in ToB: Xarpus; -1 uses the main Strength", section = perAreaSettings, position = 9)
	default int tobXarpusStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "tobVerzikColor", name = "ToB: Verzik colour", description = "Floor colour in ToB: Verzik; leave unset for the main Floor colour", section = perAreaSettings, position = 10)
	default Color tobVerzikColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "tobVerzikStrength", name = "ToB: Verzik strength", description = "Strength in ToB: Verzik; -1 uses the main Strength", section = perAreaSettings, position = 11)
	default int tobVerzikStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "toaZebakColor", name = "ToA: Crondis + Zebak colour", description = "Floor colour in ToA: Crondis + Zebak; leave unset for the main Floor colour", section = perAreaSettings, position = 12)
	default Color toaZebakColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "toaZebakStrength", name = "ToA: Crondis + Zebak strength", description = "Strength in ToA: Crondis + Zebak; -1 uses the main Strength", section = perAreaSettings, position = 13)
	default int toaZebakStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "toaKephriColor", name = "ToA: Scabaras + Kephri colour", description = "Floor colour in ToA: Scabaras + Kephri; leave unset for the main Floor colour", section = perAreaSettings, position = 14)
	default Color toaKephriColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "toaKephriStrength", name = "ToA: Scabaras + Kephri strength", description = "Strength in ToA: Scabaras + Kephri; -1 uses the main Strength", section = perAreaSettings, position = 15)
	default int toaKephriStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "toaAkkhaColor", name = "ToA: Het + Akkha colour", description = "Floor colour in ToA: Het + Akkha; leave unset for the main Floor colour", section = perAreaSettings, position = 16)
	default Color toaAkkhaColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "toaAkkhaStrength", name = "ToA: Het + Akkha strength", description = "Strength in ToA: Het + Akkha; -1 uses the main Strength", section = perAreaSettings, position = 17)
	default int toaAkkhaStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "toaBabaColor", name = "ToA: Apmeken + Ba-Ba colour", description = "Floor colour in ToA: Apmeken + Ba-Ba; leave unset for the main Floor colour", section = perAreaSettings, position = 18)
	default Color toaBabaColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "toaBabaStrength", name = "ToA: Apmeken + Ba-Ba strength", description = "Strength in ToA: Apmeken + Ba-Ba; -1 uses the main Strength", section = perAreaSettings, position = 19)
	default int toaBabaStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "toaWardensColor", name = "ToA: Wardens colour", description = "Floor colour in ToA: Wardens; leave unset for the main Floor colour", section = perAreaSettings, position = 20)
	default Color toaWardensColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "toaWardensStrength", name = "ToA: Wardens strength", description = "Strength in ToA: Wardens; -1 uses the main Strength", section = perAreaSettings, position = 21)
	default int toaWardensStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "coxColor", name = "Chambers of Xeric colour", description = "Floor colour in Chambers of Xeric; leave unset for the main Floor colour", section = perAreaSettings, position = 22)
	default Color coxColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "coxStrength", name = "Chambers of Xeric strength", description = "Strength in Chambers of Xeric; -1 uses the main Strength", section = perAreaSettings, position = 23)
	default int coxStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "infernoColor", name = "Inferno colour", description = "Floor colour in Inferno; leave unset for the main Floor colour", section = perAreaSettings, position = 24)
	default Color infernoColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "infernoStrength", name = "Inferno strength", description = "Strength in Inferno; -1 uses the main Strength", section = perAreaSettings, position = 25)
	default int infernoStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "fightCaveColor", name = "Fight Cave colour", description = "Floor colour in Fight Cave; leave unset for the main Floor colour", section = perAreaSettings, position = 26)
	default Color fightCaveColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "fightCaveStrength", name = "Fight Cave strength", description = "Strength in Fight Cave; -1 uses the main Strength", section = perAreaSettings, position = 27)
	default int fightCaveStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "colosseumColor", name = "Fortis Colosseum colour", description = "Floor colour in Fortis Colosseum; leave unset for the main Floor colour", section = perAreaSettings, position = 28)
	default Color colosseumColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "colosseumStrength", name = "Fortis Colosseum strength", description = "Strength in Fortis Colosseum; -1 uses the main Strength", section = perAreaSettings, position = 29)
	default int colosseumStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "doomColor", name = "Doom of Mokhaiotl colour", description = "Floor colour in Doom of Mokhaiotl; leave unset for the main Floor colour", section = perAreaSettings, position = 30)
	default Color doomColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "doomStrength", name = "Doom of Mokhaiotl strength", description = "Strength in Doom of Mokhaiotl; -1 uses the main Strength", section = perAreaSettings, position = 31)
	default int doomStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "nexColor", name = "Nex colour", description = "Floor colour in Nex; leave unset for the main Floor colour", section = perAreaSettings, position = 32)
	default Color nexColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "nexStrength", name = "Nex strength", description = "Strength in Nex; -1 uses the main Strength", section = perAreaSettings, position = 33)
	default int nexStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "nightmareColor", name = "Nightmare colour", description = "Floor colour in Nightmare; leave unset for the main Floor colour", section = perAreaSettings, position = 34)
	default Color nightmareColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "nightmareStrength", name = "Nightmare strength", description = "Strength in Nightmare; -1 uses the main Strength", section = perAreaSettings, position = 35)
	default int nightmareStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "yamaColor", name = "Yama colour", description = "Floor colour in Yama; leave unset for the main Floor colour", section = perAreaSettings, position = 36)
	default Color yamaColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "yamaStrength", name = "Yama strength", description = "Strength in Yama; -1 uses the main Strength", section = perAreaSettings, position = 37)
	default int yamaStrength()
	{
		return -1;
	}

	@ConfigItem(keyName = "customColor", name = "Custom regions colour", description = "Floor colour in Custom regions; leave unset for the main Floor colour", section = perAreaSettings, position = 38)
	default Color customColor()
	{
		return null;
	}

	@Range(min = -1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "customStrength", name = "Custom regions strength", description = "Strength in Custom regions; -1 uses the main Strength", section = perAreaSettings, position = 39)
	default int customStrength()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "excludedObjects",
		name = "Never paint object IDs",
		description = "Floor object IDs to leave in their original colours, comma separated. Akkha's arena floor in Tombs of Amascut and the Chambers of Xeric energy pools, herb patches, floor holes and tool spawns are always left alone.",
		position = 51
	)
	default String excludedObjects()
	{
		return "";
	}

	@ConfigItem(
		keyName = "debugLog",
		name = "Show the tile under the mouse",
		description = "Shows an overlay and writes to the client log what is on the tile under the mouse, or under you when the mouse is off the scene (terrain, floor objects, heights, painted or not). For bug reports: hover the tile and screenshot it.",
		position = 52
	)
	default boolean debugLog()
	{
		return false;
	}
}
