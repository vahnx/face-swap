package com.faceswap;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullFaceHeadItemsTest
{
	@Test
	public void usesCacheAppearanceSuppressionInsteadOfBroadHelmetNames()
	{
		assertTrue(FullFaceHeadItems.contains(ItemID.RUNE_FULL_HELM));
		assertTrue(FullFaceHeadItems.contains(ItemID.SLAYER_HELMET));
		assertFalse(FullFaceHeadItems.contains(ItemID.CRYSTAL_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.BLUE_MOON_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.GRACEFUL_HOOD));
		assertFalse(FullFaceHeadItems.contains(ItemID.DHAROKS_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.ECLIPSE_MOON_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.BLOOD_MOON_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.ZOMBIE_HELMET));
		assertFalse(FullFaceHeadItems.contains(ItemID.RUNE_MED_HELM));
		assertFalse(FullFaceHeadItems.contains(ItemID.ARMADYL_HELMET));
		assertFalse(FullFaceHeadItems.contains(ItemID.VOID_MELEE_HELM));
	}
}
