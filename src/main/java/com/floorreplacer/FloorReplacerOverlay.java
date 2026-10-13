package com.floorreplacer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * On-screen copy of the debug log: what is in the column the player stands in and why each piece was or was
 * not painted. Only drawn while "Log the tile under you" is on, so a screenshot carries the facts.
 */
class FloorReplacerOverlay extends OverlayPanel
{
	private final FloorReplacerPlugin plugin;
	private final FloorReplacerConfig config;

	@Inject
	FloorReplacerOverlay(FloorReplacerPlugin plugin, FloorReplacerConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.debugLog())
		{
			return null;
		}
		List<String> lines = plugin.debugLines();
		if (lines.isEmpty())
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder().text("Floor Replacer debug").build());
		for (String line : lines)
		{
			panelComponent.getChildren().add(LineComponent.builder().left(line).build());
		}
		return super.render(graphics);
	}
}
