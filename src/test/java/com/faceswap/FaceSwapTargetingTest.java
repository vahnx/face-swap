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
	public void exposesRadiusAndNpcScopes()
	{
		assertEquals("Everyone in Radius", FaceSwapTargetScope.RADIUS.toString());
		assertEquals("Specific NPCs", FaceSwapNpcTargetScope.SPECIFIC_NPCS.toString());
		assertEquals("All NPCs", FaceSwapNpcTargetScope.ALL_NPCS.toString());
		assertEquals("npcTargetNames_alfie",
			FaceSwapPlugin.npcTargetNamesKey(FaceSwapHead.ALFIE));
	}
}
