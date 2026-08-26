package com.faceswap;

import java.util.Locale;

/**
 * The persisted assignment boundary for a target. The style ID is intentionally
 * a string so future facial styles can be added without changing the assignment
 * or config schema again.
 */
final class FaceSwapAssignment
{
	static final String DEFAULT_STYLE_ID = "default";

	private final FaceSwapHead head;
	private final String styleId;

	FaceSwapAssignment(FaceSwapHead head, String styleId)
	{
		this.head = head;
		this.styleId = normalizeStyleId(styleId);
	}

	static FaceSwapAssignment defaultStyle(FaceSwapHead head)
	{
		return new FaceSwapAssignment(head, DEFAULT_STYLE_ID);
	}

	FaceSwapHead getHead()
	{
		return head;
	}

	String getStyleId()
	{
		return styleId;
	}

	static String normalizeStyleId(String styleId)
	{
		if (styleId == null || styleId.trim().isEmpty())
		{
			return DEFAULT_STYLE_ID;
		}
		return styleId.trim().toLowerCase(Locale.ROOT);
	}
}
