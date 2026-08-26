package com.faceswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.RuneLite;

final class HelmetProfiles
{
	static final String RESOURCE = "/helmet_profiles.csv";
	static final String HEADER = "item_ids,item_name,model_y,model_scale,model_x,model_z,model_pitch,model_yaw,model_roll,model_width,model_face_height,model_depth,animation_frame_offset,mask_y,mask_pitch,mask_yaw,mask_roll,status,notes";
	static final Path RUNTIME_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("face-swap").resolve("helmet_profiles.csv");
	private static volatile LoadedProfiles profiles = loadResource();

	private HelmetProfiles()
	{
	}

	static HelmetProfile find(int itemId)
	{
		return profiles.byItemId.get(itemId);
	}

	static Collection<HelmetProfile> all()
	{
		return profiles.profiles;
	}

	static void loadRuntimeProfiles() throws IOException
	{
		if (!Files.isRegularFile(RUNTIME_FILE))
		{
			return;
		}

		profiles = loadFile(RUNTIME_FILE);
	}

	static List<String> readProfileLines() throws IOException
	{
		if (Files.isRegularFile(RUNTIME_FILE))
		{
			return new ArrayList<>(Files.readAllLines(RUNTIME_FILE, StandardCharsets.UTF_8));
		}

		return readResourceLines();
	}

	static void writeRuntimeProfiles(List<String> lines) throws IOException
	{
		LoadedProfiles validated = parse(lines);
		Path directory = RUNTIME_FILE.getParent();
		Files.createDirectories(directory);
		Path temporary = Files.createTempFile(directory, "helmet_profiles", ".tmp");
		try
		{
			Files.write(temporary, lines, StandardCharsets.UTF_8);
			try
			{
				Files.move(temporary, RUNTIME_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ex)
			{
				Files.move(temporary, RUNTIME_FILE, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temporary);
		}
		profiles = validated;
	}

	private static LoadedProfiles loadResource()
	{
		try
		{
			return parse(readResourceLines());
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to read helmet profiles", ex);
		}
	}

	private static LoadedProfiles loadFile(Path path) throws IOException
	{
		return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
	}

	private static List<String> readResourceLines() throws IOException
	{
		try (InputStream stream = HelmetProfiles.class.getResourceAsStream(RESOURCE))
		{
			if (stream == null)
			{
				throw new IOException("Missing helmet profile resource " + RESOURCE);
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				List<String> lines = new ArrayList<>();
				String line;
				while ((line = reader.readLine()) != null)
				{
					lines.add(line);
				}
				return lines;
			}
		}
	}

	private static LoadedProfiles parse(List<String> lines)
	{
		if (lines.isEmpty() || !HEADER.equals(lines.get(0)))
		{
			throw new IllegalStateException("Unexpected helmet profile header: "
				+ (lines.isEmpty() ? "" : lines.get(0)));
		}

		Map<Integer, HelmetProfile> byItemId = new HashMap<>();
		List<HelmetProfile> loadedProfiles = new ArrayList<>();
		for (int index = 1; index < lines.size(); index++)
		{
			String line = lines.get(index);
			int lineNumber = index + 1;
			if (line.isBlank() || line.startsWith("#"))
			{
				continue;
			}

			String[] values = line.split(",", -1);
			if (values.length != 19)
			{
				throw new IllegalStateException("Expected 19 helmet profile fields on line " + lineNumber);
			}

			HelmetProfile profile = new HelmetProfile(
				values[1],
				parseInt(values[2], lineNumber),
				parseInt(values[3], lineNumber),
				parseInt(values[4], lineNumber),
				parseInt(values[5], lineNumber),
				parseInt(values[6], lineNumber),
				parseInt(values[7], lineNumber),
				parseInt(values[8], lineNumber),
				parseInt(values[9], lineNumber),
				parseInt(values[10], lineNumber),
				parseInt(values[11], lineNumber),
				parseInt(values[12], lineNumber),
				parseInt(values[13], lineNumber),
				parseInt(values[14], lineNumber),
				parseInt(values[15], lineNumber),
				parseInt(values[16], lineNumber),
				"tested".equalsIgnoreCase(values[17]),
				"hidden".equalsIgnoreCase(values[17]));
			loadedProfiles.add(profile);

			for (String itemIdValue : values[0].split(";"))
			{
				int itemId = parseInt(itemIdValue, lineNumber);
				HelmetProfile existing = byItemId.put(itemId, profile);
				if (existing != null)
				{
					throw new IllegalStateException("Duplicate helmet item ID " + itemId + " on line " + lineNumber);
				}
			}
		}

		return new LoadedProfiles(
			Collections.unmodifiableMap(byItemId),
			Collections.unmodifiableList(loadedProfiles));
	}

	private static int parseInt(String value, int lineNumber)
	{
		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalStateException("Invalid number on helmet profile line " + lineNumber, ex);
		}
	}

	private static final class LoadedProfiles
	{
		private final Map<Integer, HelmetProfile> byItemId;
		private final List<HelmetProfile> profiles;

		private LoadedProfiles(Map<Integer, HelmetProfile> byItemId, List<HelmetProfile> profiles)
		{
			this.byItemId = byItemId;
			this.profiles = profiles;
		}
	}
}
