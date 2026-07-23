package com.faceswap;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HelmetProfilesTest
{
	@Test
	public void loadsSeededHelmetFamilies()
	{
		assertTrue(HelmetProfiles.all().size() >= 15);
		assertNotNull(HelmetProfiles.find(ItemID.ARMADYL_HELMET));
		assertNotNull(HelmetProfiles.find(ItemID.SERPENTINE_HELM));
		assertNotNull(HelmetProfiles.find(ItemID.VOID_MELEE_HELM));
	}

	@Test
	public void classifiesSeededHelmetProfiles()
	{
		HelmetProfile profile = HelmetProfiles.find(ItemID.BERSERKER_HELM);
		assertNotNull(profile);
		assertEquals("Berserker helm", profile.getName());
		assertTrue(profile.isTested());
		assertFalse(HelmetProfiles.find(ItemID.ARMADYL_HELMET).isHidden());
		assertFalse(HelmetProfiles.find(ItemID.CRYSTAL_HELM).isHidden());
		assertFalse(HelmetProfiles.find(ItemID.ZOMBIE_HELMET).isHidden());
		assertEquals(-5, HelmetProfiles.find(ItemID.LUMBERJACK_HAT).getMaskY());
		assertEquals(6, HelmetProfiles.find(ItemID.NEITIZNOT_FACEGUARD).getMaskY());
		assertNotNull(HelmetProfiles.find(ItemID.BLUE_MOON_HELM));
		assertNotNull(HelmetProfiles.find(ItemID.GRACEFUL_HOOD));
	}
}
