bl_info = {
    "name": "Face Swap Codex Tools",
    "author": "Face Swap",
    "version": (1, 0, 0),
    "blender": (4, 3, 0),
    "location": "3D View > Sidebar > Face Swap",
    "description": "Apply repo-local Blender edits prepared through Codex",
    "category": "Development",
}

import datetime
import pathlib
import runpy
import shutil
import traceback

import bpy


SCRIPT_NAME = "pending_changes.py"
MODEL_CONFIGS = {
    (47666, 249): ("DenseRiggedPlayerHead_Base", 153, "dense_rigged_player_head_vertices.csv"),
    (230, 249): ("RiggedPlayerHead_Base", 83, "rigged_player_head_vertices.csv"),
}


def project_directory():
    if not bpy.data.filepath:
        return None
    return pathlib.Path(bpy.data.filepath).resolve().parent


def target_object():
    for object_name, _, _ in MODEL_CONFIGS.values():
        target = bpy.data.objects.get(object_name)
        if target is not None:
            return target
    return None


class FACESWAP_OT_export_edited_head(bpy.types.Operator):
    bl_idname = "faceswap.export_edited_head"
    bl_label = "Export Edited Head"
    bl_description = "Bake object transforms and export vertices into the RuneLite plugin"

    def execute(self, context):
        directory = project_directory()
        target = target_object()
        if directory is None:
            self.report({"ERROR"}, "Save the Blender project before exporting")
            return {"CANCELLED"}
        if target is None or target.type != "MESH":
            self.report({"ERROR"}, "Missing supported Face Swap head mesh")
            return {"CANCELLED"}
        model_ids = (target.get("osrs_model_id"), target.get("osrs_secondary_model_id"))
        model_config = MODEL_CONFIGS.get(model_ids)
        if model_config is None:
            self.report({"ERROR"}, f"Unsupported OSRS model pair {model_ids}")
            return {"CANCELLED"}
        _, expected_vertex_count, output_name = model_config
        if len(target.data.vertices) != expected_vertex_count:
            self.report({"ERROR"}, f"Expected {expected_vertex_count} vertices; found {len(target.data.vertices)}")
            return {"CANCELLED"}

        repository = directory.parents[1]
        output_path = repository / "src" / "main" / "resources" / "models" / output_name
        runtime_path = repository / "build" / "resources" / "main" / "models" / output_name
        try:
            lines = [f"# models={model_ids[0]}+{model_ids[1]},vertices={expected_vertex_count}"]
            for vertex in target.data.vertices:
                world = target.matrix_world @ vertex.co
                # Inverse of the import mapping: Blender (x, -osrs_z, -osrs_y).
                lines.append(f"{world.x:.6f},{-world.z:.6f},{-world.y:.6f}")
            contents = "\n".join(lines) + "\n"
            for path in (output_path, runtime_path):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(contents, encoding="ascii")
            bpy.ops.wm.save_as_mainfile(filepath=bpy.data.filepath)
            message = f"Exported {expected_vertex_count} vertices for live reload"
            context.window_manager.face_swap_codex_status = message
            self.report({"INFO"}, f"{message} to {output_path.name}")
            return {"FINISHED"}
        except Exception as error:
            traceback.print_exc()
            context.window_manager.face_swap_codex_status = f"Error: {error}"
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class FACESWAP_OT_apply_codex_changes(bpy.types.Operator):
    bl_idname = "faceswap.apply_codex_changes"
    bl_label = "Apply Codex Changes"
    bl_description = "Back up the blend, run pending_changes.py, and save"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        directory = project_directory()
        if directory is None:
            self.report({"ERROR"}, "Save the Blender project before applying changes")
            return {"CANCELLED"}

        script_path = directory / SCRIPT_NAME
        target = target_object()
        if not script_path.is_file():
            self.report({"ERROR"}, f"Missing {script_path}")
            return {"CANCELLED"}
        if target is None:
            self.report({"ERROR"}, "Missing supported Face Swap head mesh")
            return {"CANCELLED"}

        timestamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        backup_path = directory / "backups" / f"{pathlib.Path(bpy.data.filepath).stem}-{timestamp}.blend"

        try:
            backup_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(bpy.data.filepath, backup_path)
            namespace = runpy.run_path(str(script_path))
            apply_change = namespace.get("apply")
            if not callable(apply_change):
                raise RuntimeError(f"{SCRIPT_NAME} must define apply(context, target)")

            message = apply_change(context, target) or "Changes applied"
            context.view_layer.update()
            bpy.ops.wm.save_as_mainfile(filepath=bpy.data.filepath)
            context.window_manager.face_swap_codex_status = str(message)
            self.report({"INFO"}, f"{message}; backup: {backup_path.name}")
            return {"FINISHED"}
        except Exception as error:
            traceback.print_exc()
            context.window_manager.face_swap_codex_status = f"Error: {error}"
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class FACESWAP_OT_select_base_head(bpy.types.Operator):
    bl_idname = "faceswap.select_base_head"
    bl_label = "Select Base Head"
    bl_description = "Select and frame the editable base head"

    def execute(self, context):
        target = target_object()
        if target is None:
            self.report({"ERROR"}, "Missing supported Face Swap head mesh")
            return {"CANCELLED"}

        bpy.ops.object.select_all(action="DESELECT")
        target.hide_set(False)
        target.select_set(True)
        context.view_layer.objects.active = target
        if context.area and context.area.type == "VIEW_3D":
            bpy.ops.view3d.view_selected(use_all_regions=False)
        return {"FINISHED"}


class FACESWAP_PT_codex_tools(bpy.types.Panel):
    bl_label = "Codex Mesh Tools"
    bl_idname = "FACESWAP_PT_codex_tools"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Face Swap"

    def draw(self, context):
        layout = self.layout
        layout.operator("faceswap.export_edited_head", icon="EXPORT")
        layout.operator("faceswap.apply_codex_changes", icon="FILE_REFRESH")
        layout.operator("faceswap.select_base_head", icon="RESTRICT_SELECT_OFF")
        layout.separator()
        target = target_object()
        layout.label(text=f"Target: {target.name if target else 'not found'}")
        status = context.window_manager.face_swap_codex_status
        if status:
            box = layout.box()
            box.label(text=status[:80])


CLASSES = (
    FACESWAP_OT_export_edited_head,
    FACESWAP_OT_apply_codex_changes,
    FACESWAP_OT_select_base_head,
    FACESWAP_PT_codex_tools,
)


def register():
    for cls in CLASSES:
        bpy.utils.register_class(cls)
    bpy.types.WindowManager.face_swap_codex_status = bpy.props.StringProperty(
        name="Face Swap Codex Status",
        default="Ready",
    )


def unregister():
    del bpy.types.WindowManager.face_swap_codex_status
    for cls in reversed(CLASSES):
        bpy.utils.unregister_class(cls)
