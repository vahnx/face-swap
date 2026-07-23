package com.faceswap;

public enum FaceSwapTriangleCount
{
	AUTO("Auto (Per Head)", 0, 0, 0, false),
	TRIANGLES_2000("2,000", 3, 50, 20, false),
	TRIANGLES_3000("3,000", 4, 50, 30, false),
	TRIANGLES_4000("4,000", 5, 50, 40, true),
	TRIANGLES_5000("5,000", 6, 50, 50, true),
	TRIANGLES_6000("6,000", 7, 60, 50, true),
	TRIANGLES_7000("7,000", 9, 70, 50, true),
	TRIANGLES_8000("8,000", 10, 80, 50, true);

	private final String displayName;
	private final int donorCopies;
	private final int segments;
	private final int rings;
	private final boolean enhancedSampling;

	FaceSwapTriangleCount(String displayName, int donorCopies, int segments, int rings,
		boolean enhancedSampling)
	{
		this.displayName = displayName;
		this.donorCopies = donorCopies;
		this.segments = segments;
		this.rings = rings;
		this.enhancedSampling = enhancedSampling;
	}

	int getDonorCopies()
	{
		return donorCopies;
	}

	int getSegments()
	{
		return segments;
	}

	int getRings()
	{
		return rings;
	}

	int getTriangleCount()
	{
		return segments * rings * 2;
	}

	boolean usesEnhancedSampling()
	{
		return enhancedSampling;
	}

	FaceSwapTriangleCount resolve(FaceSwapHead head)
	{
		return this == AUTO ? FaceSwapHeadQualityProfiles.getDefault(head) : this;
	}

	static FaceSwapTriangleCount fromTriangleCount(int triangleCount)
	{
		for (FaceSwapTriangleCount preset : values())
		{
			if (preset != AUTO && preset.getTriangleCount() == triangleCount)
			{
				return preset;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
