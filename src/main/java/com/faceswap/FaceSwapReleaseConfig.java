package com.faceswap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("faceswap")
public interface FaceSwapReleaseConfig extends Config
{
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
		position = 0
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
