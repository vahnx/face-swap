package com.faceswap;

final class FaceSwapPanelState
{
	final FaceSwapHead selectedHead;
	final String selectedStyleId;
	final FaceSwapHeadPickerLayout headPickerLayout;
	final boolean cycleCreatorStylesWhileMoving;
	final FaceSwapTargetScope targetScope;
	final String targetNames;
	final int targetRadius;
	final FaceSwapNpcTargetScope npcTargetScope;
	final String npcTargetNames;
	final FaceSwapRenderMode renderMode;
	final int qualityLevel;
	final boolean dkMode;
	final int maskWidth;
	final boolean showMaskStrap;
	final boolean pickPlayerMode;
	final String statusText;

	FaceSwapPanelState(
		FaceSwapHead selectedHead,
		String selectedStyleId,
		FaceSwapHeadPickerLayout headPickerLayout,
		boolean cycleCreatorStylesWhileMoving,
		FaceSwapTargetScope targetScope,
		String targetNames,
		int targetRadius,
		FaceSwapNpcTargetScope npcTargetScope,
		String npcTargetNames,
		FaceSwapRenderMode renderMode,
		int qualityLevel,
		boolean dkMode,
		int maskWidth,
		boolean showMaskStrap,
		boolean pickPlayerMode,
		String statusText)
	{
		this.selectedHead = selectedHead;
		this.selectedStyleId = selectedStyleId;
		this.headPickerLayout = headPickerLayout;
		this.cycleCreatorStylesWhileMoving = cycleCreatorStylesWhileMoving;
		this.targetScope = targetScope;
		this.targetNames = targetNames;
		this.targetRadius = targetRadius;
		this.npcTargetScope = npcTargetScope;
		this.npcTargetNames = npcTargetNames;
		this.renderMode = renderMode;
		this.qualityLevel = qualityLevel;
		this.dkMode = dkMode;
		this.maskWidth = maskWidth;
		this.showMaskStrap = showMaskStrap;
		this.pickPlayerMode = pickPlayerMode;
		this.statusText = statusText;
	}
}
