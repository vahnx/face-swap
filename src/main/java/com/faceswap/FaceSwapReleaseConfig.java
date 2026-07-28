package com.faceswap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("faceswap")
public interface FaceSwapReleaseConfig extends Config
{
	@ConfigSection(
		name = "Sidepanel",
		description = "Face Swap sidepanel layout preferences",
		position = 0,
		closedByDefault = true
	)
	String SIDEPANEL_SECTION = "sidepanel";

	@ConfigItem(
		keyName = "selectedHead",
		name = "Selected Head",
		description = "The creator head to apply",
		hidden = true
	)
	default FaceSwapHead selectedHead()
	{
		return FaceSwapHead.SARDACO;
	}

	@ConfigItem(
		keyName = "tabbedHeadPicker",
		name = "Tabbed Head Picker",
		description = "Displays the fictional, creator, and emoji choices in tabs instead of side-by-side columns.",
		position = 0,
		section = SIDEPANEL_SECTION
	)
	default boolean tabbedHeadPicker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "renderMode",
		name = "Style",
		description = "Renders a rigged 3D head, a projected wraparound, or a front face mask with straps.",
		position = -100,
		hidden = true
	)
	default FaceSwapRenderMode renderMode()
	{
		return FaceSwapRenderMode.MASK;
	}

	@ConfigItem(
		keyName = "prototype3dTriangleOverrides",
		name = "3D Quality Overrides",
		description = "Persistent per-head quality choices managed by the Face Swap sidebar.",
		position = -99,
		hidden = true
	)
	default String prototype3dTriangleOverrides()
	{
		return "";
	}
}
