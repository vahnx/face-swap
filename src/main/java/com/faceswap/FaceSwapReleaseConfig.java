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
