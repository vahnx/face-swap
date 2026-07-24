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

## Planned Tracker: Animated Head Rig

The preferred replacement is a lightweight, equipment-independent head rig for each
target. The rig should provide the animated head position and orientation, while the
actual rendered player model is used only for visibility and occlusion decisions.
Helmet profiles should then describe visual fit or clipping, not compensate for
tracking errors.

The new implementation must use an explicit tracking strategy boundary. The merged
model tracker documented above must remain available as a fallback while the rig is
developed and validated.

## Migration Requirements

- Keep the existing merged-model implementation intact behind a strategy or fallback
  path rather than rewriting it in place.
- Fall back automatically if the rig cannot produce a valid pose for a target.
- Retain a developer-only way to force either tracker for comparison.
- Compare both trackers across idle, running, zooming, equipment changes, region
  changes, temporary skilling actions, and representative emotes.
- Remove neither the implementation nor this documentation until the replacement is
  proven more reliable in those cases.

## Git Checkpoint

The repository tag `mask-merged-model-fallback` identifies the preserved baseline
before the equipment-independent rig refactor.
