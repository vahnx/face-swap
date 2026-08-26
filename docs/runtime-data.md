# Runtime Data

Plugin Hub builds run from a JAR. Files under `src/main/resources/` are packaged into that JAR and must be treated as read-only runtime defaults; a release plugin cannot write back to its source checkout or assume that the resource directory exists on disk.

## Face Swap data directory

All mutable Face Swap data belongs under the plugin-specific directory:

`.runelite/face-swap/`

The plugin currently uses:

- `helmet_profiles.csv` for developer-only global helmet calibration overrides.
- `helmet_mesh_calibration.csv` for developer-only local landmark offsets, one row per non-zero point adjustment.
- `custom-heads/` for user-provided custom images, normalized PNG copies, and recent-image metadata.

Target-name assignments remain in the existing `targetNames_<head>` and `npcTargetNames_<head>` configuration keys. Optional future style assignments use `targetStyles_<head>` and `npcTargetStyles_<head>` maps in the form `normalized-name=style-id`; an absent entry means `default`. This schema is currently only pre-plumbing: alternate style assets, UI, and rendering are not enabled yet.

In developer mode, `Interactive Helmet Calibration` is an opt-in, client-side editing aid for 3D helmet profiles. Its draggable global handles and face landmarks update an in-memory calibration for the local player's equipped head item. The `Save preset` control writes global values to `helmet_profiles.csv` and local landmark offsets to `helmet_mesh_calibration.csv`; dragging does not write to disk. `Reset` reloads the current packaged/runtime profile and saved mesh offsets.

The bundled `src/main/resources/helmet_profiles.csv` remains the default profile set. On startup, Face Swap reads the runtime profile file when present and otherwise uses the bundled defaults. Preset writes update only the runtime file.

Custom images may be selected from any user-chosen path through the sidepanel file chooser. Face Swap reads the explicitly selected file and stores a normalized PNG copy in `.runelite/face-swap/custom-heads/`. The plugin must not scan or read arbitrary external paths without a user action.

When adding or changing runtime persistence:

1. Use `RuneLite.RUNELITE_DIR` rather than `System.getProperty("user.dir")`.
2. Keep packaged resources read-only and use a runtime override or copy for mutable data.
3. Perform disk I/O on a background executor, never on the RuneLite client thread.
4. Keep the storage path and CSV columns stable, or add an explicit migration before changing them.
