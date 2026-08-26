package com.faceswap;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.runelite.client.RuneLite;

final class FaceSwapCustomImageStore
{
	static final int IMAGE_SIZE = 512;
	private static final int MAX_RECENTS = 12;
	private final Path directory;
	private final Path metadataFile;
	private final List<Entry> recents = new ArrayList<>();
	private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
	private volatile String selectedId;

	FaceSwapCustomImageStore()
	{
		this(RuneLite.RUNELITE_DIR.toPath().resolve("face-swap").resolve("custom-heads"));
	}

	FaceSwapCustomImageStore(Path directory)
	{
		this.directory = directory.toAbsolutePath().normalize();
		this.metadataFile = this.directory.resolve("recent.properties");
	}

	Path getDirectory()
	{
		return directory;
	}

	void load()
	{
		try
		{
			Files.createDirectories(directory);
			Properties properties = new Properties();
			if (Files.exists(metadataFile))
			{
				try (InputStream input = Files.newInputStream(metadataFile))
				{
					properties.load(input);
				}
			}

			int count = Integer.parseInt(properties.getProperty("count", "0"));
			for (int index = 0; index < count; index++)
			{
				String id = properties.getProperty("entry." + index + ".id");
				String name = properties.getProperty("entry." + index + ".name");
				if (id != null && name != null && Files.isRegularFile(imagePath(id)))
				{
					recents.add(new Entry(id, name));
					BufferedImage image = ImageIO.read(imagePath(id).toFile());
					if (image != null)
					{
						images.put(id, image);
					}
				}
			}
			selectedId = properties.getProperty("selected");
			if (selectedId != null)
			{
				loadSelected();
			}
		}
		catch (IOException | NumberFormatException ignored)
		{
			// A damaged local history should not prevent the plugin from starting.
		}
	}

	Entry importImage(Path source) throws IOException
	{
		if (source == null)
		{
			throw new IOException("No image was selected");
		}

		Files.createDirectories(directory);
		BufferedImage decoded = ImageIO.read(source.toFile());
		if (decoded == null)
		{
			throw new IOException("Unsupported image format");
		}

		BufferedImage normalized = normalize(decoded);
		Files.createDirectories(directory);
		String id = UUID.randomUUID().toString();
		ImageIO.write(normalized, "png", imagePath(id).toFile());
		Entry entry = new Entry(id, source.getFileName().toString());
		recents.removeIf(existing -> existing.id.equals(id));
		recents.add(0, entry);
		selectedId = id;
		images.put(id, normalized);
		trimRecents();
		persist();
		FaceSwapHeadImages.setCustomImage(normalized);
		return entry;
	}

	boolean select(String id)
	{
		if (id == null || !Files.isRegularFile(imagePath(id)))
		{
			return false;
		}
		if (!images.containsKey(id))
		{
			return false;
		}
		selectedId = id;
		FaceSwapHeadImages.setCustomImage(images.get(id));
		try
		{
			persist();
		}
		catch (IOException ignored)
		{
			// The image is still usable for this session.
		}
		return true;
	}

	List<Entry> getRecents()
	{
		return Collections.unmodifiableList(new ArrayList<>(recents));
	}

	BufferedImage getImage(String id)
	{
		return images.get(id);
	}

	BufferedImage getSelectedImage()
	{
		return selectedId == null ? null : images.get(selectedId);
	}

	boolean hasSelectedImage()
	{
		return selectedId != null && images.containsKey(selectedId);
	}

	void clear()
	{
		for (Entry entry : recents)
		{
			try
			{
				Files.deleteIfExists(imagePath(entry.id));
			}
			catch (IOException ignored)
			{
				// Best-effort cleanup of user-owned cache files.
			}
		}
		recents.clear();
		images.clear();
		selectedId = null;
		FaceSwapHeadImages.setCustomImage(null);
		try
		{
			persist();
		}
		catch (IOException ignored)
		{
			// The in-memory history has still been cleared.
		}
	}

	static BufferedImage normalize(BufferedImage source)
	{
		BufferedImage normalized = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = normalized.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			double scale = Math.min(480d / Math.max(1, source.getWidth()), 480d / Math.max(1, source.getHeight()));
			int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
			graphics.drawImage(source, (IMAGE_SIZE - width) / 2, (IMAGE_SIZE - height) / 2, width, height, null);
		}
		finally
		{
			graphics.dispose();
		}
		return normalized;
	}

	private void loadSelected()
	{
		FaceSwapHeadImages.setCustomImage(images.get(selectedId));
	}

	private void trimRecents()
	{
		while (recents.size() > MAX_RECENTS)
		{
			Entry removed = recents.remove(recents.size() - 1);
			try
			{
				Files.deleteIfExists(imagePath(removed.id));
			}
			catch (IOException ignored)
			{
				// Keep the metadata bounded even if a stale file cannot be deleted.
			}
		}
	}

	private void persist() throws IOException
	{
		Files.createDirectories(directory);
		Properties properties = new Properties();
		properties.setProperty("count", Integer.toString(recents.size()));
		if (selectedId != null)
		{
			properties.setProperty("selected", selectedId);
		}
		for (int index = 0; index < recents.size(); index++)
		{
			Entry entry = recents.get(index);
			properties.setProperty("entry." + index + ".id", entry.id);
			properties.setProperty("entry." + index + ".name", entry.name);
		}
		try (OutputStream output = Files.newOutputStream(metadataFile,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
		{
			properties.store(output, "Face Swap custom image history");
		}
	}

	private Path imagePath(String id)
	{
		return directory.resolve(id + ".png");
	}

	static final class Entry
	{
		final String id;
		final String name;

		Entry(String id, String name)
		{
			this.id = id;
			this.name = name;
		}
	}
}
