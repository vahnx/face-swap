package com.faceswap.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.KitDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.KitLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

public final class ExportRiggedHead
{
	private static final int HEAD_KIT_ID = 0;
	private static final int HEAD_MODEL_ID = 230;
	private static final int JAW_KIT_ID = 10;
	private static final int JAW_MODEL_ID = 249;

	private ExportRiggedHead()
	{
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 2 && args.length != 7)
		{
			throw new IllegalArgumentException("Usage: ExportRiggedHead <cache-directory> <output-directory> "
				+ "[head-kit head-model jaw-kit jaw-model output-stem]");
		}

		int headKitId = args.length == 7 ? Integer.parseInt(args[2]) : HEAD_KIT_ID;
		int headModelId = args.length == 7 ? Integer.parseInt(args[3]) : HEAD_MODEL_ID;
		int jawKitId = args.length == 7 ? Integer.parseInt(args[4]) : JAW_KIT_ID;
		int jawModelId = args.length == 7 ? Integer.parseInt(args[5]) : JAW_MODEL_ID;
		String outputStem = args.length == 7 ? args[6] : "rigged_player_head";
		Path output = Path.of(args[1]);
		Files.createDirectories(output);
		try (Store store = new Store(Path.of(args[0]).toFile()))
		{
			store.load();
			KitDefinition headKit = loadKit(store, headKitId);
			KitDefinition jawKit = loadKit(store, jawKitId);
			validateKitModel(headKit, headModelId);
			validateKitModel(jawKit, jawModelId);

			ModelDefinition head = loadModel(store, headModelId);
			ModelDefinition jaw = loadModel(store, jawModelId);
			applyKitRecolors(head, headKit);
			applyKitRecolors(jaw, jawKit);
			writeJson(output.resolve(outputStem + ".json"), headKitId, jawKitId, head, jaw,
				args.length == 7 ? "DenseRiggedPlayerHead_Base" : "RiggedPlayerHead_Base");
			writeObj(output.resolve(outputStem + ".obj"), head, jaw);
			writeVerticesCsv(output.resolve(outputStem + "_vertices.csv"), head, jaw);

			System.out.printf("Exported models %d + %d: %d vertices, %d triangles, 4 animation groups%n",
				head.id, jaw.id, head.vertexCount + jaw.vertexCount, head.faceCount + jaw.faceCount);
		}
	}

	private static void validateKitModel(KitDefinition kit, int modelId) throws IOException
	{
		if (kit.models == null || kit.models.length != 1 || kit.models[0] != modelId)
		{
			throw new IOException("Kit " + kit.getId() + " no longer maps exclusively to model " + modelId);
		}
	}

	private static KitDefinition loadKit(Store store, int kitId) throws IOException
	{
		Index configs = store.getIndex(IndexType.CONFIGS);
		Archive archive = configs.getArchive(ConfigType.IDENTKIT.getId());
		ArchiveFiles files = archive.getFiles(store.getStorage().loadArchive(archive));
		for (FSFile file : files.getFiles())
		{
			if (file.getFileId() == kitId)
			{
				return new KitLoader().load(kitId, file.getContents());
			}
		}
		throw new IOException("Identity kit " + kitId + " was not found");
	}

	private static ModelDefinition loadModel(Store store, int modelId) throws IOException
	{
		Index models = store.getIndex(IndexType.MODELS);
		Archive archive = models.getArchive(modelId);
		if (archive == null)
		{
			throw new IOException("Model " + modelId + " was not found");
		}

		byte[] packed = store.getStorage().loadArchive(archive);
		return new ModelLoader().load(modelId, archive.decompress(packed));
	}

	private static void applyKitRecolors(ModelDefinition model, KitDefinition kit)
	{
		if (kit.recolorToFind != null)
		{
			for (int i = 0; i < kit.recolorToFind.length; i++)
			{
				model.recolor(kit.recolorToFind[i], kit.recolorToReplace[i]);
			}
		}
	}

	private static void writeJson(Path path, int headKitId, int jawKitId, ModelDefinition head,
		ModelDefinition jaw, String objectName) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			writer.write("{\n  \"kitId\": " + headKitId + ",\n");
			writer.write("  \"jawKitId\": " + jawKitId + ",\n");
			writer.write("  \"modelId\": " + head.id + ",\n");
			writer.write("  \"secondaryModelId\": " + jaw.id + ",\n");
			writer.write("  \"objectName\": \"" + objectName + "\",\n");
			writer.write("  \"vertices\": [\n");
			writeVertices(writer, head, false);
			writeVertices(writer, jaw, true);
			writer.write("  ],\n  \"faces\": [\n");
			writeFaces(writer, head, 0, false);
			writeFaces(writer, jaw, head.vertexCount, true);
			writer.write("  ],\n  \"faceColors\": [");
			writeFaceColors(writer, head, false);
			writeFaceColors(writer, jaw, true);
			writer.write("],\n  \"vertexGroups\": [\n");
			int[][] headGroups = head.getVertexGroups();
			int[][] jawGroups = jaw.getVertexGroups();
			int groupCount = Math.max(headGroups.length, jawGroups.length);
			for (int group = 0; group < groupCount; group++)
			{
				writer.write("    [");
				boolean comma = false;
				if (group < headGroups.length)
				{
					for (int vertex : headGroups[group])
					{
						writer.write((comma ? ", " : "") + vertex);
						comma = true;
					}
				}
				if (group < jawGroups.length)
				{
					for (int vertex : jawGroups[group])
					{
						writer.write((comma ? ", " : "") + (head.vertexCount + vertex));
						comma = true;
					}
				}
				writer.write(group + 1 == groupCount ? "]\n" : "],\n");
			}
			writer.write("  ]\n}\n");
		}
	}

	private static void writeVertices(BufferedWriter writer, ModelDefinition model, boolean lastModel)
		throws IOException
	{
		for (int i = 0; i < model.vertexCount; i++)
		{
			boolean last = lastModel && i + 1 == model.vertexCount;
			writer.write(String.format("    [%d, %d, %d]%s%n", model.vertexX[i], model.vertexY[i],
				model.vertexZ[i], last ? "" : ","));
		}
	}

	private static void writeFaces(BufferedWriter writer, ModelDefinition model, int vertexOffset,
		boolean lastModel) throws IOException
	{
		for (int i = 0; i < model.faceCount; i++)
		{
			boolean last = lastModel && i + 1 == model.faceCount;
			writer.write(String.format("    [%d, %d, %d]%s%n", model.faceIndices1[i] + vertexOffset,
				model.faceIndices2[i] + vertexOffset, model.faceIndices3[i] + vertexOffset, last ? "" : ","));
		}
	}

	private static void writeFaceColors(BufferedWriter writer, ModelDefinition model, boolean lastModel)
		throws IOException
	{
		for (int i = 0; i < model.faceCount; i++)
		{
			writer.write(Integer.toString(model.faceColors[i] & 0xffff));
			if (!lastModel || i + 1 != model.faceCount)
			{
				writer.write(", ");
			}
		}
	}

	private static void writeObj(Path path, ModelDefinition head, ModelDefinition jaw) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			writer.write("# Rigged player head cache models " + head.id + " + " + jaw.id + "\n");
			writer.write("# Preserve vertex order and animation groups.\n");
			writer.write("o RiggedPlayerHead_Base\n");
			for (ModelDefinition model : new ModelDefinition[]{head, jaw})
			{
				for (int i = 0; i < model.vertexCount; i++)
				{
					writer.write(String.format("v %d %d %d%n", model.vertexX[i], -model.vertexZ[i], -model.vertexY[i]));
				}
			}
			int offset = 0;
			for (ModelDefinition model : new ModelDefinition[]{head, jaw})
			{
				for (int i = 0; i < model.faceCount; i++)
				{
					writer.write(String.format("f %d %d %d%n", model.faceIndices1[i] + offset + 1,
						model.faceIndices2[i] + offset + 1, model.faceIndices3[i] + offset + 1));
				}
				offset += model.vertexCount;
			}
		}
	}

	private static void writeVerticesCsv(Path path, ModelDefinition head, ModelDefinition jaw) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.US_ASCII))
		{
			writer.write("# models=" + head.id + "+" + jaw.id + ",vertices="
				+ (head.vertexCount + jaw.vertexCount) + "\n");
			for (ModelDefinition model : new ModelDefinition[]{head, jaw})
			{
				for (int vertex = 0; vertex < model.vertexCount; vertex++)
				{
					writer.write(model.vertexX[vertex] + "," + model.vertexY[vertex] + ","
						+ model.vertexZ[vertex] + "\n");
				}
			}
		}
	}
}
