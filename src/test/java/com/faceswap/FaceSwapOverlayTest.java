package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaceSwapOverlayTest
{
	@Test
	public void detectsRaspberryAnimationIds()
	{
		assertTrue(FaceSwapOverlay.isRaspberryAnimation(
			net.runelite.api.gameval.AnimationID.EMOTE_YA_BOO_SUCKS));
		assertTrue(FaceSwapOverlay.isRaspberryAnimation(
			net.runelite.api.gameval.AnimationID.EMOTE_YA_BOO_SUCKS_LOOP));
		assertFalse(FaceSwapOverlay.isRaspberryAnimation(
			net.runelite.api.gameval.AnimationID.EMOTE_WAVE));
	}

	@Test
	public void detectsBunnyHopAnimationId()
	{
		assertTrue(FaceSwapOverlay.isBunnyHopAnimation(
			net.runelite.api.gameval.AnimationID.RABBIT_EMOTE));
		assertFalse(FaceSwapOverlay.isBunnyHopAnimation(
			net.runelite.api.gameval.AnimationID.EMOTE_JUMP_WITH_JOY));
	}
}
