package com.faceswap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.RuneLite;

/** Runtime-only persistence for local helmet mesh offsets. */
final class HelmetMeshCalibrations
{
	static final String HEADER = "item_id,point_id,x,y,z";
	static final Path RUNTIME_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("face-swap")
		.resolve("helmet_mesh_calibration.csv");
	private static final Map<Integer, HelmetMeshCalibration> CALIBRATIONS = new HashMap<>();

	private HelmetMeshCalibrations()
	{
	}

	static synchronized void loadRuntime() throws IOException
	{
		CALIBRATIONS.clear();
		if (!Files.isRegularFile(RUNTIME_FILE))
		{
			return;
		}

		Map<Integer, HelmetMeshCalibration> loaded = parse(Files.readAllLines(RUNTIME_FILE, StandardCharsets.UTF_8));
		CALIBRATIONS.putAll(loaded);
	}

	static synchronized HelmetMeshCalibration get(int itemId)
	{
		HelmetMeshCalibration calibration = CALIBRATIONS.get(itemId);
		return calibration == null ? new HelmetMeshCalibration() : calibration.copy();
	}

	static synchronized void save(int itemId, HelmetMeshCalibration calibration) throws IOException
	{
		if (calibration == null || calibration.isDefault())
		{
			CALIBRATIONS.remove(itemId);
		}
		else
		{
			CALIBRATIONS.put(itemId, calibration.copy());
		}

		List<String> lines = new ArrayList<>();
		lines.add(HEADER);
		List<Integer> itemIds = new ArrayList<>(CALIBRATIONS.keySet());
		Collections.sort(itemIds);
		for (int storedItemId : itemIds)
		{
			HelmetMeshCalibration stored = CALIBRATIONS.get(storedItemId);
			for (MeshControlPoint point : MeshControlPoint.values())
			{
				int x = stored.getX(point);
				int y = stored.getY(point);
				int z = stored.getZ(point);
				if (x != 0 || y != 0 || z != 0)
				{
					lines.add(storedItemId + "," + point.name() + "," + x + "," + y + "," + z);
				}
			}
		}

		Path directory = RUNTIME_FILE.getParent();
		Files.createDirectories(directory);
		Path temporary = Files.createTempFile(directory, "helmet_mesh_calibration", ".tmp");
		try
		{
			Files.write(temporary, lines, StandardCharsets.UTF_8);
			try
			{
				Files.move(temporary, RUNTIME_FILE, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);
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
	}

	private static Map<Integer, HelmetMeshCalibration> parse(List<String> lines)
	{
		if (lines.isEmpty() || !HEADER.equals(lines.get(0)))
		{
			throw new IllegalStateException("Unexpected helmet mesh calibration header");
		}

		Map<Integer, HelmetMeshCalibration> parsed = new HashMap<>();
		for (int index = 1; index < lines.size(); index++)
		{
			String line = lines.get(index);
			int lineNumber = index + 1;
			if (line.isBlank() || line.startsWith("#"))
			{
				continue;
			}
			String[] values = line.split(",", -1);
			if (values.length != 5)
			{
				throw new IllegalStateException("Expected 5 helmet mesh calibration fields on line " + lineNumber);
			}
			int itemId = parseInt(values[0], lineNumber);
			MeshControlPoint point;
			try
			{
				point = MeshControlPoint.valueOf(values[1].trim());
			}
			catch (IllegalArgumentException ex)
			{
				throw new IllegalStateException("Invalid helmet mesh control point on line " + lineNumber, ex);
			}
			HelmetMeshCalibration calibration = parsed.computeIfAbsent(itemId, ignored -> new HelmetMeshCalibration());
			calibration.setOffset(point,
				parseInt(values[2], lineNumber),
				parseInt(values[3], lineNumber),
				parseInt(values[4], lineNumber));
		}
		return parsed;
	}

	private static int parseInt(String value, int lineNumber)
	{
		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalStateException("Invalid number on helmet mesh calibration line " + lineNumber, ex);
		}
	}
}
