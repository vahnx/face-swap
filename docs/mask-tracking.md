# Mask Tracking Architecture

## Current Fallback: Merged Model Tracking

The current mask tracker derives a head pose from the rendered actor model returned
by RuneLite. This model includes the player's body, equipped head item, weapon, and
temporary animation geometry.

The implementation is centered in
`FaceSwapOverlay.getAnimatedMaskHeadPose(...)` and uses a `MaskHeadBinding` to:

1. Detect triangles in the model's probable head region.
2. Bind to vertices near that region.
3. Track the upper portion of the bound vertices to estimate the animated head center.
4. Derive local axes for mask orientation.
5. Project the front, back, and strap geometry from that model-space pose.

Transient bindings and axis stabilization handle temporary model topology changes
caused by actions, weapons, and selected emotes. Helmet profiles can provide mask
pitch, yaw, and roll corrections.

### Strengths

- Uses RuneLite's supported actor model data.
- Follows many movement and idle animations without automating input.
- Works for players and NPCs.
- Requires no external skeletal data.

### Known Limitations

- Equipment is merged into the same model, so helmets and weapons can contaminate
  head-region detection and orientation.
- Temporary action models can replace or reorder vertices.
- Extreme emotes may stretch or displace the inferred head region.
- Item-specific corrections can interact when both a helmet and weapon are equipped.
- More per-item patches do not solve the underlying lack of an equipment-independent
  animated head anchor.

## Primary Tracker: Animated Head Rig

Mask style now uses a lightweight, equipment-independent head rig for each targeted
player. The plugin builds one shared neutral rig model and gives each actor independent
pose and action animation controllers synchronized to the actor's current animation
frames. The rig is never registered as a visible scene object.

`FaceSwapOverlay` reads the animated rig vertices through a `MaskPoseTracker` strategy
and derives the mask center and local axes from known head geometry. The actual actor
model is used for bounds validation and mask occlusion, not as the primary pose source.
Legacy helmet Y, pitch, yaw, and roll corrections are not applied to player masks
using the animated rig. Those corrections were calibrated against equipment-contaminated
merged models and remain available only to the merged-model tracker. Global developer
mask calibration controls still apply to both trackers.

### Tracking Modes

- `Automatic` uses the animated rig for players and falls back to the merged-model
  tracker when a rig or pose is unavailable or invalid.
- `Animated Rig` forces the rig in debugger launches, including for NPC experiments.
- `Merged Model (Fallback)` forces the preserved implementation for comparison.
- Normal non-debug launches always use `Automatic`, regardless of a persisted
  developer selection.

NPC animation skeletons are not universally compatible with the player rig. Automatic
mode therefore retains merged-model tracking for NPCs. The forced rig mode allows
individual humanoid NPC animations to be evaluated without changing release behavior.

## Validation Requirements

- Compare both trackers across idle, running, zooming, equipment changes, region
  changes, temporary skilling actions, and representative emotes.
- Specifically retest no equipment, Fang, Soulreaper axe, med helms, Crystal helm,
  Serpentine helm, and previously problematic utility animations.
- Keep the merged-model implementation and fallback tag until the rig has passed
  in-game validation.

## Git Checkpoint

The repository tag `mask-merged-model-fallback` identifies the preserved baseline
before the equipment-independent rig refactor.
