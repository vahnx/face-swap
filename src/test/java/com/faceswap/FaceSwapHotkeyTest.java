package com.faceswap;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import net.runelite.api.GameState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaceSwapHotkeyTest
{
	@Test
	public void escapeCancelsTargetPicking()
	{
		FaceSwapPlugin plugin = new FaceSwapPlugin();
		plugin.setPickPlayerMode(true);

		KeyEvent event = keyEvent(KeyEvent.VK_ESCAPE);

		assertTrue(plugin.handleKeyPressed(GameState.LOGGED_IN, event));
		assertFalse(plugin.handleKeyPressed(GameState.LOGGED_IN, event));
	}

	@Test
	public void inactiveEscapeAndOtherKeysPassThrough()
	{
		FaceSwapPlugin plugin = new FaceSwapPlugin();

		assertFalse(plugin.handleKeyPressed(GameState.LOGGED_IN, keyEvent(KeyEvent.VK_ESCAPE)));
		assertFalse(plugin.handleKeyPressed(GameState.LOGGED_IN, keyEvent(KeyEvent.VK_ENTER)));
	}

	@Test
	public void escapePassesThroughOutsideTheLoggedInGame()
	{
		FaceSwapPlugin plugin = new FaceSwapPlugin();
		plugin.setPickPlayerMode(true);

		assertFalse(plugin.handleKeyPressed(GameState.LOGIN_SCREEN, keyEvent(KeyEvent.VK_ESCAPE)));
		assertTrue(plugin.handleKeyPressed(GameState.LOGGED_IN, keyEvent(KeyEvent.VK_ESCAPE)));
	}

	private static KeyEvent keyEvent(int keyCode)
	{
		return new KeyEvent(
			new Canvas(),
			KeyEvent.KEY_PRESSED,
			System.currentTimeMillis(),
			0,
			keyCode,
			KeyEvent.CHAR_UNDEFINED);
	}

}
