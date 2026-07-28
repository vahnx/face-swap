package com.faceswap;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaceSwapConfigTest
{
	private final FaceSwapConfig config = new FaceSwapConfig()
	{
	};
	private final FaceSwapReleaseConfig releaseConfig = new FaceSwapReleaseConfig()
	{
	};

	@Test
	public void defaultsToMaskModeAndSardaco()
	{
		assertEquals(FaceSwapHead.SARDACO, config.selectedHead());
		assertEquals(FaceSwapHead.SARDACO, releaseConfig.selectedHead());
		assertEquals(FaceSwapHead.SARDACO, FaceSwapHead.values()[0]);
		assertEquals(FaceSwapRenderMode.MASK, FaceSwapRenderMode.values()[1]);
		assertEquals(FaceSwapRenderMode.TWO_D, FaceSwapRenderMode.values()[2]);
		assertEquals(FaceSwapRenderMode.MASK, config.renderMode());
		assertEquals(FaceSwapRenderMode.MASK, releaseConfig.renderMode());
		assertTrue(config.tabbedHeadPicker());
		assertTrue(releaseConfig.tabbedHeadPicker());
		assertFalse(config.dkMode());
		assertFalse(config.debugProjection());
		assertFalse(config.opaqueBacking());
		assertFalse(config.hideWithHeadgear());
		assertFalse(config.debugEquipment());
		assertEquals(34, config.helmetFaceDrop());
		assertEquals(1, config.wrapHeightOffset());
		assertEquals(27, config.wrapRegionHeight());
		assertEquals(6, config.wrapScreenLift());
		assertEquals(40, config.wrapTextureLift());
		assertEquals(27, config.wrapTextureXOffset());
		assertEquals(95, config.wrapTextureHeightScale());
		assertEquals(40, config.wrapTextureTopBias());
		assertEquals(12, config.wrapBackingExpansion());
		assertEquals(70, config.maskWidth());
		assertEquals(MaskTrackingMode.AUTO, config.maskTrackingMode());
		assertEquals(10, config.targetRadius());
		assertEquals(FaceSwapNpcTargetScope.DISABLED, config.npcTargetScope());
		assertEquals(0, config.maskForwardOffset());
		assertEquals(28, config.maskBacking());
		assertEquals(10, config.yOffset());
		assertEquals(0, config.maskAngle());
		assertEquals(0, config.maskYaw());
		assertEquals(0, config.maskRoll());
		assertEquals(12, config.prototype3dGlobalYShift());
		assertEquals(6, config.prototype3dY());
		assertEquals(100, config.prototype3dScale());
		assertEquals(0, config.prototype3dX());
		assertEquals(-8, config.prototype3dZ());
		assertEquals(100, config.prototype3dWidth());
		assertEquals(150, config.prototype3dTextureWidth());
		assertEquals(100, config.prototype3dFaceHeight());
		assertEquals(120, config.prototype3dDepth());
		assertEquals(50, config.prototype3dChinHeight());
		assertEquals(0, config.prototypeAnimationFrameOffset());
	}

	@Test
	public void usesSeparateNoHelmetGlobalYShift()
	{
		assertEquals(7, FaceSwapPlugin.resolvePrototypeGlobalYShift(false, 12));
		assertEquals(12, FaceSwapPlugin.resolvePrototypeGlobalYShift(true, 12));
	}

	@Test
	public void exposesOnlyReleaseControlsOutsideDeveloperMode()
	{
		assertEquals(Set.of("tabbedHeadPicker"), visibleKeys(FaceSwapReleaseConfig.class));
	}

	@Test
	public void exposesCalibrationControlsInDeveloperMode()
	{
		Set<String> visibleKeys = visibleKeys(FaceSwapConfig.class);
		assertTrue(visibleKeys.contains("prototype3dGlobalYShift"));
		assertTrue(visibleKeys.contains("prototype3dY"));
		assertTrue(visibleKeys.contains("prototype3dTextureWidth"));
		assertTrue(visibleKeys.contains("saveHelmetPreset"));
		assertTrue(visibleKeys.contains("debugProjection"));
		assertTrue(visibleKeys.contains("maskBacking"));
		assertTrue(visibleKeys.contains("maskTrackingMode"));
		assertTrue(visibleKeys.contains("maskAngle"));
		assertTrue(visibleKeys.contains("maskYaw"));
		assertTrue(visibleKeys.contains("maskRoll"));
		assertTrue(visibleKeys.size() > 40);
	}

	@Test
	public void maskTrackingModesHaveDeveloperFriendlyLabels()
	{
		assertEquals("Automatic", MaskTrackingMode.AUTO.toString());
		assertEquals("Animated Rig", MaskTrackingMode.ANIMATED_RIG.toString());
		assertEquals("Merged Model (Fallback)", MaskTrackingMode.MERGED_MODEL.toString());
		assertEquals(MaskTrackingMode.ANIMATED_RIG,
			FaceSwapPlugin.resolveMaskTrackingMode(true, MaskTrackingMode.ANIMATED_RIG));
		assertEquals(MaskTrackingMode.MERGED_MODEL,
			FaceSwapPlugin.resolveMaskTrackingMode(true, MaskTrackingMode.MERGED_MODEL));
		assertEquals(MaskTrackingMode.AUTO,
			FaceSwapPlugin.resolveMaskTrackingMode(false, MaskTrackingMode.MERGED_MODEL));
	}

	@Test
	public void textureWidthExpandsTheSampledFaceWithoutChangingTheMesh()
	{
		assertEquals(0f, FaceSwapPlugin.scalePrototypeTextureU(0f, 100), 0.001f);
		assertEquals(1f, FaceSwapPlugin.scalePrototypeTextureU(1f, 100), 0.001f);
		assertEquals(0.25f, FaceSwapPlugin.scalePrototypeTextureU(0f, 200), 0.001f);
		assertEquals(0.75f, FaceSwapPlugin.scalePrototypeTextureU(1f, 200), 0.001f);
	}

	private static Set<String> visibleKeys(Class<?> configClass)
	{
		return Arrays.stream(configClass.getMethods())
			.map(method -> method.getAnnotation(ConfigItem.class))
			.filter(item -> item != null)
			.filter(item -> !item.hidden())
			.map(ConfigItem::keyName)
			.collect(Collectors.toSet());
	}
}
