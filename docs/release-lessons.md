# Face Swap Release Lessons

This is the durable handoff for lessons learned during the work after the last
Plugin Hub submission. It complements the focused checklists in [Plugin Hub
Submission](plugin-hub-submission.md), [Face Asset Standards](asset-standards.md),
[Runtime Data](runtime-data.md), and [Development-Only Assets](development-assets.md).

## Plugin Hub and packaging

- The final JAR limit is `10,485,760` bytes (`10 MiB` using `1 MiB = 1,048,576`
  bytes), inclusive. `10,485,760` is the largest accepted value; the first
  rejected value is `10,485,761`. See the current [Plugin Hub packager
  limit check](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java).
- Plugin Hub warns above 80% of the default limit: `8,388,608` bytes. This is a
  warning threshold, not a second limit.
- Face Swap uses the default limit; there is no `jarSizeLimitMiB` manifest
  override. The final Hub-built JAR is authoritative because the Hub rebuilds
  the manifest's pinned source commit and packages its own metadata.
- All release resources under `src/main/resources/` count toward the JAR,
  including normal creator images, creator 3D/OSRS images, icons, models, and
  CSV defaults. `dev-assets/` and external backups do not belong in the release
  artifact.
- The root `icon.png` is a Plugin Hub listing icon. The runtime toolbar icon is
  `src/main/resources/face_swap_icon.png`; they have different jobs. Confirm
  the root icon in the committed tree with `git ls-tree -r HEAD -- icon.png`.
- The Plugin Hub manifest is an immutable source pin. A later source change is
  not part of a submission until the manifest's full 40-character `commit=` is
  updated. Update the existing submission PR when one exists; do not create a
  duplicate because a fork or branch is stale.
- A green normal `build` check is separate from the maintainer-review gate. A
  red `RuneLite Plugin Hub Checks` result that says review is required is not
  automatically a build failure.

## Asset and release hygiene

- Runtime heads are `512x512` transparent PNGs with a head-only crop. The
  transparency boundary is outside the subject; internal transparency through
  the face, eyes, mouth, hair, or tears is a defect.
- Front/back pairs must remain comparable in scale and placement. Creator 3D
  variants need their own complete front/back pair in
  `content_creators_3d/`; do not mirror a front image for the rear.
- Lossy compression is performed only after an external backup of the exact
  target files. Preserve paths, names, dimensions, and the alpha mask. A PNG
  can remain technically valid while looking damaged, which is what made the
  earlier Brett Dog transparency problem easy to miss.
- The creator compression tier is `pngquant --quality 0-50 --speed 1 --strip`;
  the lower `0-30` tier is reserved for emojis and fictional characters. Brett
  Dog standard, sick, alternate, and 3D/OSRS assets require representative
  visual checks after every compression pass. The same applies to the sick
  expression batch generally.
- Removing `alfie_furious_front.png` was a release-facing change: the file,
  advertised style list, and resource/style tests must agree. Never leave a
  missing file advertised just to preserve a list shape.
- Stable internal IDs and persisted keys are not display names. `JOSH_PILLAUT`
  remains the compatibility ID while the UI displays `The RS Felon`.
- A newly approved creator must have permission recorded outside the public
  repository, the creator statistics entry updated, normal and OSRS/3D assets
  checked, and picker order/style availability synchronized.

## Picker, targeting, and persistence behavior

- The sidepanel picker remembers the last category and picker position. Custom
  images are displayed as front/back pairs on one row, with fixed `Front` and
  `Back` headers, a bottom blank row for adding a pair, `Select` for inactive
  pairs, and a confirmed `-` action for deletion. The file chooser remembers
  its last folder.
- Target assignment captures the creator, expression, render mode, and custom
  image ID when a player is manually assigned. Merely browsing another style
  must not change an already captured target.
- Different custom pairs can appear under the same `Specific Players` list.
  This is a known grouping limitation; the per-player stored custom image ID is
  still distinct. Do not encode every arbitrary pair as a new permanent head
  category.
- Switching target scope must preserve the distinction between the local
  player's "You" face and manually selected target assignments. Target-pick
  mode remains manual and can be cancelled with `ESC`.
- Resetting RuneLite plugin configuration clears Face Swap configuration,
  including target assignments and picker preferences, but does not delete
  imported custom files or runtime helmet-calibration files.

## Rendering and performance boundaries

- `3D` is intended for players and humanoid single-head NPCs. Pets,
  non-humanoids, multi-head bosses, and unusual equipment can stretch or
  misplace geometry.
- `Mask` and `Wraparound` are overlays and do not participate in scene depth;
  they may appear through walls or overlap imperfectly. Wraparound also has
  actor-model sanity checks and skips its projection during known teleport
  animations, including home teleport. This suppression is wraparound-specific
  because that mode projects against transient model geometry; it is not a
  blanket suppression of every face mode or every animation.
- DK-mode hat relocation/redraw is intentionally disabled until its depth and
  positioning behavior is reliable. Do not re-enable it by changing unrelated
  global 3D calibration values.
- Mask tracking has an equipment-independent animated-rig path for players,
  with merged-model fallback for invalid/unavailable poses and normal NPC
  automatic behavior. The merged tracker remains useful for comparison and
  should not be removed without in-game validation.

## Verification and recovery

Before a release-facing change:

1. Preserve unrelated work in the dirty tree and make an external backup before
   any lossy asset rewrite.
2. Run `./gradlew.bat clean test` for code/resource consistency.
3. For asset changes, run `./gradlew.bat clean test jar`, inspect the JAR's byte
   size, and verify the exact `10,485,760`-byte ceiling.
4. Run `git diff --check` and inspect the committed tree/resource paths.
5. For visual behavior, the user must test in the RuneLite development client:
   front/back/side views, camera movement, scene changes, equipment, target
   scopes, teleport animations, plugin disable/enable, and custom-pair select,
   replace, and delete flows. A passing JVM build is not in-game verification.

If an asset pass regresses, restore only the affected files from the external
backup, rerun the asset tests/build, and recheck the JAR. Keep backups outside
`src/main/resources` and do not use broad destructive cleanup commands as a
recovery mechanism.
