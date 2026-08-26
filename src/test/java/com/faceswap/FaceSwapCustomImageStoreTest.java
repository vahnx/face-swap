package com.faceswap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FaceSwapCustomImageStoreTest
{
	@Test
	public void importsUserSelectedImagesIntoPluginDirectory() throws IOException
	{
		Path directory = Files.createTempDirectory("face-swap-custom-heads");
		Path source = Files.createTempFile("face-swap-source", ".png");
		FaceSwapCustomImageStore store = new FaceSwapCustomImageStore(directory);
		try
		{
			assertTrue(ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", source.toFile()));
			FaceSwapCustomImageStore.Entry entry = store.importImage(source);

			assertNotNull(entry);
			assertTrue(Files.isRegularFile(directory.resolve(entry.id + ".png")));
		}
		finally
		{
			store.clear();
			Files.deleteIfExists(source);
			Files.deleteIfExists(directory.resolve("recent.properties"));
			Files.deleteIfExists(directory);
		}
	}
}
