package com.faceswap;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FaceSwapTargetingTest
{
	@Test
	public void normalizesNamesToUniqueLines()
	{
		assertEquals("Alice\nBob",
			FaceSwapPlugin.normalizeTargetNames(" Alice, Bob\r\nalice "));
	}

	@Test
	public void usesSeparatePersistentKeyForEachHead()
	{
		assertEquals("targetNames_king_condor",
			FaceSwapPlugin.targetNamesKey(FaceSwapHead.KING_CONDOR));
		assertEquals("targetNames_alfie",
			FaceSwapPlugin.targetNamesKey(FaceSwapHead.ALFIE));
		assertEquals("targetStyles_alfie",
			FaceSwapPlugin.targetStylesKey(FaceSwapHead.ALFIE));
		assertEquals("npcTargetStyles_alfie",
			FaceSwapPlugin.npcTargetStylesKey(FaceSwapHead.ALFIE));
		assertEquals("targetModes_alfie",
			FaceSwapPlugin.targetModesKey(FaceSwapHead.ALFIE));
		assertEquals("npcTargetModes_alfie",
			FaceSwapPlugin.npcTargetModesKey(FaceSwapHead.ALFIE));
		assertEquals("targetCustomImages_alfie",
			FaceSwapPlugin.targetCustomImagesKey(FaceSwapHead.ALFIE));
		assertEquals("npcTargetCustomImages_alfie",
			FaceSwapPlugin.npcTargetCustomImagesKey(FaceSwapHead.ALFIE));
	}

	@Test
	public void exposesAllPlayersScope()
	{
		assertEquals("Disabled", FaceSwapTargetScope.DISABLED.toString());
		assertEquals("All Players", FaceSwapTargetScope.ALL_PLAYERS.toString());
	}

	@Test
	public void exposesRuneLiteListScopes()
	{
		assertEquals("Chat Channel", FaceSwapTargetScope.CHAT_CHANNEL.toString());
		assertEquals("Ignore List", FaceSwapTargetScope.IGNORE_LIST.toString());
	}

	@Test
	public void parsesAndSerializesCapturedTargetStyles()
	{
		assertEquals("alternate", FaceSwapPlugin.parseTargetStyles(
			"Alice=alternate,bob=default").get("alice"));
		assertEquals("alice=alternate,bob=default",
			FaceSwapPlugin.serializeTargetStyles(FaceSwapPlugin.parseTargetStyles(
				"Alice=alternate,bob=default")));
	}

	@Test
	public void parsesAndSerializesTargetCustomImages()
	{
		Map<String, String> images = FaceSwapPlugin.parseTargetCustomImages(
			"Alice=front-one,bob=front-two");

		assertEquals("front-one", images.get("alice"));
		assertEquals("alice=front-one,bob=front-two",
			FaceSwapPlugin.serializeTargetCustomImages(images));
	}

	@Test
	public void assignmentDefaultsToTheDefaultStyle()
	{
		FaceSwapAssignment assignment = FaceSwapAssignment.defaultStyle(FaceSwapHead.ALFIE);

		assertEquals(FaceSwapHead.ALFIE, assignment.getHead());
		assertEquals(FaceSwapAssignment.DEFAULT_STYLE_ID, assignment.getStyleId());
		assertEquals(null, assignment.getRenderMode());
		assertEquals(null, assignment.getCustomImageId());
	}

	@Test
	public void assignmentRetainsItsCustomImageId()
	{
		FaceSwapAssignment assignment = new FaceSwapAssignment(
			FaceSwapHead.CUSTOM, "default", FaceSwapRenderMode.TWO_D, "front-one");

		assertEquals("front-one", assignment.getCustomImageId());
	}

	@Test
	public void specificPlayersKeepYouAssignmentForExplicitlyTargetedLocalPlayer()
	{
		FaceSwapAssignment selected = new FaceSwapAssignment(
			FaceSwapHead.SARDACO, "default", FaceSwapRenderMode.MASK);
		FaceSwapAssignment target = new FaceSwapAssignment(
			FaceSwapHead.BRETTDOG, "alternate", FaceSwapRenderMode.THREE_D);

		assertSame(selected,
			FaceSwapPlugin.resolveSpecificPlayerAssignment(true, selected, target));
		assertSame(target,
			FaceSwapPlugin.resolveSpecificPlayerAssignment(false, selected, target));
	}

	@Test
	public void targetPickingKeepsTheLocalPlayersExistingAssignment()
	{
		FaceSwapAssignment selected = new FaceSwapAssignment(
			FaceSwapHead.SARDACO, "default", FaceSwapRenderMode.MASK);
		FaceSwapAssignment target = new FaceSwapAssignment(
			FaceSwapHead.BRETTDOG, "alternate", FaceSwapRenderMode.THREE_D);

		assertSame(target,
			FaceSwapPlugin.resolveSpecificPlayerAssignment(true, true, selected, target));
		assertEquals(null,
			FaceSwapPlugin.resolveSpecificPlayerAssignment(true, true, selected, null));
	}

	@Test
	public void specificPlayersDoNotRenderLocalPlayerUnlessExplicitlyTargeted()
	{
		FaceSwapAssignment selected = new FaceSwapAssignment(
			FaceSwapHead.SARDACO, "default", FaceSwapRenderMode.MASK);

		assertEquals(null,
			FaceSwapPlugin.resolveSpecificPlayerAssignment(true, selected, null));
	}

	@Test
	public void parsesAndSerializesCapturedTargetModes()
	{
		assertEquals(FaceSwapRenderMode.MASK, FaceSwapPlugin.parseTargetModes(
			"Alice=MASK,bob=THREE_D").get("alice"));
		assertEquals("alice=MASK,bob=THREE_D",
			FaceSwapPlugin.serializeTargetModes(FaceSwapPlugin.parseTargetModes(
				"Alice=MASK,bob=THREE_D")));
	}

	@Test
	public void exposesRadiusAndNpcScopes()
	{
		assertEquals("Everyone in Radius", FaceSwapTargetScope.RADIUS.toString());
		assertEquals("Specific NPCs", FaceSwapNpcTargetScope.SPECIFIC_NPCS.toString());
		assertEquals("All NPCs", FaceSwapNpcTargetScope.ALL_NPCS.toString());
		assertEquals("npcTargetNames_alfie",
			FaceSwapPlugin.npcTargetNamesKey(FaceSwapHead.ALFIE));
	}
}
