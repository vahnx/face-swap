package com.faceswap.tools;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

public final class ExportDenseDonorModel
{
	private ExportDenseDonorModel()
	{
	}

	public static void main(String[] args) throws Exception
	{
		Path output = Path.of(args[1]);
		int modelId = Integer.parseInt(args[2]);
		String outputStem = args[3];
		Files.createDirectories(output);
		try (Store store = new Store(Path.of(args[0]).toFile()))
		{
			store.load();
			Index models = store.getIndex(IndexType.MODELS);
			Archive archive = models.getArchive(modelId);
			byte[] packed = store.getStorage().loadArchive(archive);
			ModelDefinition model = new ModelLoader().load(modelId, archive.decompress(packed));
			writeJson(output.resolve(outputStem + ".json"), model);
			System.out.printf("Exported donor model %d: %d vertices, %d triangles, %d groups%n",
				model.id, model.vertexCount, model.faceCount, model.getVertexGroups().length);
		}
	}

	private static void writeJson(Path path, ModelDefinition model) throws Exception
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			writer.write("{\n  \"kitId\": -1,\n  \"jawKitId\": -1,\n");
			writer.write("  \"modelId\": " + model.id + ",\n  \"secondaryModelId\": -1,\n");
			writer.write("  \"objectName\": \"DenseDonorHead_Base\",\n  \"vertices\": [\n");
			for (int vertex = 0; vertex < model.vertexCount; vertex++)
			{
				writer.write(String.format("    [%d, %d, %d]%s%n", model.vertexX[vertex],
					model.vertexY[vertex], model.vertexZ[vertex], vertex + 1 == model.vertexCount ? "" : ","));
			}
			writer.write("  ],\n  \"faces\": [\n");
			for (int face = 0; face < model.faceCount; face++)
			{
				writer.write(String.format("    [%d, %d, %d]%s%n", model.faceIndices1[face],
					model.faceIndices2[face], model.faceIndices3[face], face + 1 == model.faceCount ? "" : ","));
			}
			writer.write("  ],\n  \"faceColors\": [");
			for (int face = 0; face < model.faceCount; face++)
			{
				writer.write((model.faceColors[face] & 0xffff) + (face + 1 == model.faceCount ? "" : ", "));
			}
			writer.write("],\n  \"vertexGroups\": [\n");
			int[][] groups = model.getVertexGroups();
			for (int group = 0; group < groups.length; group++)
			{
				writer.write("    [");
				for (int vertex = 0; vertex < groups[group].length; vertex++)
				{
					writer.write(groups[group][vertex] + (vertex + 1 == groups[group].length ? "" : ", "));
				}
				writer.write(group + 1 == groups.length ? "]\n" : "],\n");
			}
			writer.write("  ]\n}\n");
		}
	}
}
