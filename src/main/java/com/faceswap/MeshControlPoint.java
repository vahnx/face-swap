package com.faceswap;

/**
 * Stable landmarks on the procedural replacement head. The coordinates are in
 * the same local model space as the dense head before helmet transforms are
 * applied.
 */
enum MeshControlPoint
{
	CROWN("Crown", 0f, -196f, -1f),
	FOREHEAD_LEFT("Forehead L", -5f, -190f, -4f),
	FOREHEAD_CENTER("Forehead", 0f, -190f, -6f),
	FOREHEAD_RIGHT("Forehead R", 5f, -190f, -4f),
	TEMPLE_LEFT("Temple L", -9f, -182f, -2f),
	NOSE_BRIDGE("Nose bridge", 0f, -185f, -9f),
	TEMPLE_RIGHT("Temple R", 9f, -182f, -2f),
	CHEEK_LEFT("Cheek L", -9f, -174f, -4f),
	NOSE_TIP("Nose", 0f, -180f, -10f),
	CHEEK_RIGHT("Cheek R", 9f, -174f, -4f),
	MOUTH_LEFT("Mouth L", -5f, -170f, -7f),
	MOUTH_CENTER("Mouth", 0f, -170f, -8f),
	MOUTH_RIGHT("Mouth R", 5f, -170f, -7f),
	JAW_LEFT("Jaw L", -7f, -166f, -2f),
	CHIN("Chin", 0f, -164f, -3f),
	JAW_RIGHT("Jaw R", 7f, -166f, -2f);

	private final String label;
	private final float x;
	private final float y;
	private final float z;

	MeshControlPoint(String label, float x, float y, float z)
	{
		this.label = label;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	String getLabel()
	{
		return label;
	}

	float getX()
	{
		return x;
	}

	float getY()
	{
		return y;
	}

	float getZ()
	{
		return z;
	}
}
