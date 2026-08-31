package com.faceswap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaceSwapPluginTest
{
	@Test
	public void movementStyleSelectionNeverRepeatsCurrentStyle()
	{
		List<String> styles = List.of("default", "sad", "furious");
		assertEquals("sad", FaceSwapPlugin.nextMovementStyleId(styles, "default", 0));
		assertEquals("furious", FaceSwapPlugin.nextMovementStyleId(styles, "default", 1));
		assertEquals("default", FaceSwapPlugin.nextMovementStyleId(styles, "sad", 0));
	}

	@Test
	public void movementCycleUsesDefaultBlushingAndInLoveInOrder()
	{
		assertEquals("blushing", FaceSwapPlugin.nextMovementCycleStyleId("default"));
		assertEquals("in_love", FaceSwapPlugin.nextMovementCycleStyleId("blushing"));
		assertEquals("default", FaceSwapPlugin.nextMovementCycleStyleId("in_love"));
	}

	@Test
	public void identifiesHatLikeHeadwearByName()
	{
		assertTrue(FaceSwapPlugin.isHatLikeHeadItemName("Lumberjack hat"));
		assertTrue(FaceSwapPlugin.isHatLikeHeadItemName("Blue wizard hood"));
		assertTrue(FaceSwapPlugin.isHatLikeHeadItemName("Camo helmet"));
		assertFalse(FaceSwapPlugin.isHatLikeHeadItemName("Rune full helm"));
		assertFalse(FaceSwapPlugin.isHatLikeHeadItemName(null));
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FaceSwapPlugin.class);
		RuneLite.main(args);
	}
}
