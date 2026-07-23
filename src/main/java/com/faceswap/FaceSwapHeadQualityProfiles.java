package com.faceswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

final class FaceSwapHeadQualityProfiles
{
	static final String RESOURCE = "/head_quality_profiles.csv";
	static final String HEADER = "head,triangles";
	private static final FaceSwapTriangleCount SAFE_FALLBACK = FaceSwapTriangleCount.TRIANGLES_4000;
	private static final Map<FaceSwapHead, FaceSwapTriangleCount> DEFAULTS = loadDefaults();

	private FaceSwapHeadQualityProfiles()
	{
	}

	static FaceSwapTriangleCount getDefault(FaceSwapHead head)
	{
		return DEFAULTS.getOrDefault(head, SAFE_FALLBACK);
	}

	static boolean hasConfiguredDefault(FaceSwapHead head)
	{
		return DEFAULTS.containsKey(head);
	}

	static FaceSwapTriangleCount resolve(FaceSwapHead head, String serializedOverrides)
	{
		return parseOverrides(serializedOverrides).getOrDefault(head, getDefault(head));
	}

	static String setOverride(
		String serializedOverrides,
		FaceSwapHead head,
		FaceSwapTriangleCount triangleCount)
	{
		Map<FaceSwapHead, FaceSwapTriangleCount> overrides = parseOverrides(serializedOverrides);
		overrides.put(head, triangleCount.resolve(head));

		StringJoiner serialized = new StringJoiner(",");
		for (FaceSwapHead candidate : FaceSwapHead.values())
		{
			FaceSwapTriangleCount value = overrides.get(candidate);
			if (value != null)
			{
				serialized.add(candidate.name() + "=" + value.name());
			}
		}
		return serialized.toString();
	}

	private static Map<FaceSwapHead, FaceSwapTriangleCount> parseOverrides(String serializedOverrides)
	{
		Map<FaceSwapHead, FaceSwapTriangleCount> overrides = new EnumMap<>(FaceSwapHead.class);
		if (serializedOverrides == null || serializedOverrides.isBlank())
		{
			return overrides;
		}

		for (String entry : serializedOverrides.split(","))
		{
			String[] fields = entry.split("=", 2);
			if (fields.length != 2)
			{
				continue;
			}
			try
			{
				FaceSwapHead head = FaceSwapHead.valueOf(fields[0].trim());
				FaceSwapTriangleCount triangleCount = FaceSwapTriangleCount.valueOf(fields[1].trim());
				if (triangleCount != FaceSwapTriangleCount.AUTO)
				{
					overrides.put(head, triangleCount);
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale entries from renamed or removed heads.
			}
		}
		return overrides;
	}

	private static Map<FaceSwapHead, FaceSwapTriangleCount> loadDefaults()
	{
		Map<FaceSwapHead, FaceSwapTriangleCount> defaults = new EnumMap<>(FaceSwapHead.class);
		try (InputStream stream = FaceSwapHeadQualityProfiles.class.getResourceAsStream(RESOURCE))
		{
			if (stream == null)
			{
				return defaults;
			}
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String header = reader.readLine();
				if (!HEADER.equals(header))
				{
					return defaults;
				}

				String line;
				while ((line = reader.readLine()) != null)
				{
					String[] fields = line.split(",", -1);
					if (fields.length != 2)
					{
						continue;
					}
					try
					{
						FaceSwapHead head = FaceSwapHead.valueOf(fields[0].trim());
						FaceSwapTriangleCount triangleCount =
							FaceSwapTriangleCount.fromTriangleCount(Integer.parseInt(fields[1].trim()));
						if (triangleCount != null)
						{
							defaults.put(head, triangleCount);
						}
					}
					catch (IllegalArgumentException ignored)
					{
						// Skip malformed rows while retaining valid defaults.
					}
				}
			}
		}
		catch (IOException ignored)
		{
			// The safe fallback keeps the plugin usable if the resource is damaged.
		}
		return defaults;
	}
}
