# Runtime Data

Plugin Hub builds run from a JAR. Files under `src/main/resources/` are packaged into that JAR and must be treated as read-only runtime defaults; a release plugin cannot write back to its source checkout or assume that the resource directory exists on disk.

## Face Swap data directory

All mutable Face Swap data belongs under the plugin-specific directory:

`.runelite/face-swap/`

The plugin currently uses:

- `helmet_profiles.csv` for developer-only global helmet calibration overrides.
- `helmet_mesh_calibration.csv` for developer-only local landmark offsets, one row per non-zero point adjustment.
- `custom-heads/` for user-provided custom images, normalized PNG copies, and recent-image metadata.

The custom picker treats a front and optional back image as one persisted pair.
Front and back replacements update that pair, selecting a pair changes the
active custom image ID, and deleting a pair removes its persisted files after
the UI confirmation. The picker remembers its last category/page state, and the
file chooser remembers its last custom-image folder; these are UI conveniences,
not additional external resource roots.

Target-name assignments remain in the existing `targetNames_<head>` and `npcTargetNames_<head>` configuration keys. When a target is added, Face Swap captures the selected creator, expression, render mode, and (for custom heads) custom image ID in the `targetStyles_<head>` / `npcTargetStyles_<head>`, `targetModes_<head>` / `npcTargetModes_<head>`, and `targetCustomImages_<head>` / `npcTargetCustomImages_<head>` maps. Later changes to the picker do not silently change existing targets; selecting that target again captures the newly selected creator, expression, render mode, and custom image. Legacy target assignments without a stored mode temporarily inherit the current global mode and are backfilled on startup.

Specific-player custom pairs are intentionally stored per player, but the UI's
`Specific Players` list is grouped by the selected head rather than by every
possible custom-pair combination. Consequently, players using different custom
pairs can appear in the same list even though their saved assignments remain
individual. This is a known UI limitation and should not be “fixed” by inventing
an unbounded head/category ID scheme.

RuneLite's plugin `Reset configuration` action clears every Face Swap configuration key, including hidden sidepanel settings, per-creator defaults, target names, captured target styles, and legacy keys. It does not delete imported custom images or runtime helmet calibration files; those have separate explicit controls or persistence files.

In developer mode, `Interactive Helmet Calibration` is an opt-in, client-side editing aid for 3D helmet profiles. Its draggable global handles and face landmarks update an in-memory calibration for the local player's equipped head item. The `Save preset` control writes global values to `helmet_profiles.csv` and local landmark offsets to `helmet_mesh_calibration.csv`; dragging does not write to disk. `Reset` reloads the current packaged/runtime profile and saved mesh offsets.

The bundled `src/main/resources/helmet_profiles.csv` remains the default profile set. On startup, Face Swap reads the runtime profile file when present and otherwise uses the bundled defaults. Preset writes update only the runtime file.

Custom images may be selected from any user-chosen path through the sidepanel file chooser. Face Swap reads the explicitly selected file and stores a normalized PNG copy in `.runelite/face-swap/custom-heads/`. The plugin must not scan or read arbitrary external paths without a user action.

When adding or changing runtime persistence:

1. Use `RuneLite.RUNELITE_DIR` rather than `System.getProperty("user.dir")`.
2. Keep packaged resources read-only and use a runtime override or copy for mutable data.
3. Perform disk I/O on a background executor, never on the RuneLite client thread.
4. Keep the storage path and CSV columns stable, or add an explicit migration before changing them.

When changing target-scope or picker behavior, verify the distinction between
the local player's current “You” selection and manually captured target
assignments. Browsing styles while target-pick mode is active must not silently
rewrite the local player's face or previously captured targets; target mode is
manual and exits with `ESC`.
