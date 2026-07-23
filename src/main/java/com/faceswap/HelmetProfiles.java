package com.faceswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HelmetProfiles
{
	static final String RESOURCE = "/helmet_profiles.csv";
	static final String HEADER = "item_ids,item_name,model_y,model_scale,model_x,model_z,model_pitch,model_yaw,model_roll,model_width,model_face_height,model_depth,animation_frame_offset,mask_y,status,notes";
	private static final LoadedProfiles PROFILES = load();

	private HelmetProfiles()
	{
	}

	static HelmetProfile find(int itemId)
	{
		return PROFILES.byItemId.get(itemId);
	}

	static Collection<HelmetProfile> all()
	{
		return PROFILES.profiles;
	}

	private static LoadedProfiles load()
	{
		InputStream stream = HelmetProfiles.class.getResourceAsStream(RESOURCE);
		if (stream == null)
		{
			throw new IllegalStateException("Missing helmet profile resource " + RESOURCE);
		}

		Map<Integer, HelmetProfile> byItemId = new HashMap<>();
		List<HelmetProfile> profiles = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
		{
			String header = reader.readLine();
			if (!HEADER.equals(header))
			{
				throw new IllegalStateException("Unexpected helmet profile header: " + header);
			}

			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null)
			{
				lineNumber++;
				if (line.isBlank() || line.startsWith("#"))
				{
					continue;
				}

				String[] values = line.split(",", -1);
				if (values.length != 16)
				{
					throw new IllegalStateException("Expected 16 helmet profile fields on line " + lineNumber);
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
					"tested".equalsIgnoreCase(values[14]),
					"hidden".equalsIgnoreCase(values[14]));
				profiles.add(profile);

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
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to read helmet profiles", ex);
		}

		return new LoadedProfiles(
			Collections.unmodifiableMap(byItemId),
			Collections.unmodifiableList(profiles));
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
