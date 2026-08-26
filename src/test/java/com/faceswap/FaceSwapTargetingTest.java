package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	public void parsesAndSerializesOptionalTargetStyles()
	{
		assertEquals("alternate", FaceSwapPlugin.parseTargetStyles(
			"Alice=alternate,bob=default").get("alice"));
		assertEquals("alice=alternate",
			FaceSwapPlugin.serializeTargetStyles(FaceSwapPlugin.parseTargetStyles(
				"Alice=alternate,bob=default")));
	}

	@Test
	public void assignmentDefaultsToTheDefaultStyle()
	{
		FaceSwapAssignment assignment = FaceSwapAssignment.defaultStyle(FaceSwapHead.ALFIE);

		assertEquals(FaceSwapHead.ALFIE, assignment.getHead());
		assertEquals(FaceSwapAssignment.DEFAULT_STYLE_ID, assignment.getStyleId());
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
