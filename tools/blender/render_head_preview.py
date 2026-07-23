import mathutils
import pathlib
import sys

import bpy


def script_args():
    separator = sys.argv.index("--") if "--" in sys.argv else len(sys.argv)
    return [pathlib.Path(value).resolve() for value in sys.argv[separator + 1 :]]


def point_camera(camera, target):
    camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()


def main():
    blend_path, output_directory = script_args()
    bpy.ops.wm.open_mainfile(filepath=str(blend_path))
    target = next(obj for obj in bpy.data.objects if obj.type == "MESH" and obj.name.endswith("Head_Base"))

    corners = [target.matrix_world @ mathutils.Vector(corner) for corner in target.bound_box]
    center = sum(corners, mathutils.Vector()) / len(corners)
    size = max(max(corner[i] for corner in corners) - min(corner[i] for corner in corners) for i in range(3))

    camera_data = bpy.data.cameras.new("PreviewCamera")
    camera = bpy.data.objects.new("PreviewCamera", camera_data)
    bpy.context.scene.collection.objects.link(camera)
    bpy.context.scene.camera = camera
    camera_data.type = "ORTHO"
    camera_data.ortho_scale = size * 1.25

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_shadows = True
    scene.display.shading.show_cavity = True
    scene.display.shading.cavity_type = "WORLD"
    scene.render.resolution_x = 512
    scene.render.resolution_y = 512
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.film_transparent = True
    output_directory.mkdir(parents=True, exist_ok=True)

    views = {
        "front": mathutils.Vector((0.0, -size * 4.0, 0.0)),
        "side": mathutils.Vector((size * 4.0, 0.0, 0.0)),
        "back": mathutils.Vector((0.0, size * 4.0, 0.0)),
    }
    for name, offset in views.items():
        camera.location = center + offset
        point_camera(camera, center)
        scene.render.filepath = str(output_directory / f"dense_head_{name}.png")
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()
