# Ensouled Elf Head Blender Base

Run `gradlew exportElfHead` to refresh the raw cache exports. The Blender project is generated from
`ensouled_elf_head.json`, which preserves the original cache vertex and triangle ordering.

The `FRONT_DIRECTION` wireframe marker points toward the front of the head. You can move vertices in
Edit or Sculpt mode. Keep the original topology when the result needs to round-trip into the plugin:
do not add, delete, merge, subdivide, or reorder vertices or faces.

## Codex Sidebar

The development add-on under `addons/face_swap_codex` adds **Face Swap** to the 3D View sidebar
(press `N` if the sidebar is hidden). **Apply Codex Changes** executes `pending_changes.py`, creates a
timestamped copy under `backups/`, and saves the updated project. The script must expose
`apply(context, target)` and should preserve the base mesh topology unless a change explicitly requires
otherwise.

**Export Edited Head** bakes the object's current transform, converts the vertices back to OSRS axes,
and writes `src/main/resources/models/rigged_player_head_vertices.csv`. The 3D prototype loads that
resource over cache model `230`. During development it also updates `build/resources/main`, so toggle
the 3D prototype off and on after export to reload the mesh without restarting RuneLite. A packaged JAR
still requires rebuilding and restarting because resources inside a JAR cannot be changed in place.

## Rigged Base

`dense_rigged_player_head.blend` is the production editing base. It combines selectable head kit `205`
model `47666` with jaw kit `10` model `249`, and retains four OSRS animation groups covering all 153
vertices. Keep the `OSRS_GROUP_*` assignments and topology unchanged. `rigged_player_head.blend` remains
available as the original 83-vertex fallback. The older Ensouled elf project is retained only as prototype
history and is not compatible with player skeletal animation.
