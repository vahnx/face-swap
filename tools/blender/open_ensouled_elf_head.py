import json
import math
import pathlib
import sys

import bpy


def script_args():
    separator = sys.argv.index("--") if "--" in sys.argv else len(sys.argv)
    return [pathlib.Path(value) for value in sys.argv[separator + 1 :]]


def jagex_hsl_to_rgba(value):
    hue = ((value >> 10) & 63) / 64.0
    saturation = ((value >> 7) & 7) / 8.0
    lightness = (value & 127) / 128.0

    if saturation == 0:
        return (lightness, lightness, lightness, 1.0)

    q = lightness * (1.0 + saturation) if lightness < 0.5 else lightness + saturation - lightness * saturation
    p = 2.0 * lightness - q

    def channel(offset):
        sample = (hue + offset) % 1.0
        if sample < 1.0 / 6.0:
            return p + (q - p) * 6.0 * sample
        if sample < 0.5:
            return q
        if sample < 2.0 / 3.0:
            return p + (q - p) * (2.0 / 3.0 - sample) * 6.0
        return p

    return (channel(1.0 / 3.0), channel(0.0), channel(-1.0 / 3.0), 1.0)


def add_direction_marker(collection, radius):
    bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=radius * 0.12, radius2=0.0, depth=radius * 0.45)
    marker = bpy.context.object
    marker.name = "FRONT_DIRECTION"
    marker.rotation_euler = (math.radians(90), 0.0, 0.0)
    marker.location = (0.0, -radius * 1.2, 0.0)
    marker.display_type = "WIRE"
    marker.hide_render = True
    for candidate in list(marker.users_collection):
        candidate.objects.unlink(marker)
    collection.objects.link(marker)


def main():
    json_path, blend_path = script_args()
    data = json.loads(json_path.read_text(encoding="utf-8"))

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    collection = bpy.data.collections.new("Face Swap Source Mesh")
    bpy.context.scene.collection.children.link(collection)

    # OSRS uses Y as vertical and Z as depth. Blender uses Z as vertical.
    vertices = [(x, -z, -y) for x, y, z in data["vertices"]]
    mesh = bpy.data.meshes.new("EnsouledElfHead_Base_Mesh")
    mesh.from_pydata(vertices, [], data["faces"])
    mesh.update()

    obj = bpy.data.objects.new("EnsouledElfHead_Base", mesh)
    collection.objects.link(obj)
    obj["osrs_item_id"] = data["itemId"]
    obj["osrs_model_id"] = data["modelId"]
    obj["topology_must_remain_ordered"] = True

    vertex_ids = mesh.attributes.new("osrs_vertex_index", "INT", "POINT")
    for index, entry in enumerate(vertex_ids.data):
        entry.value = index

    materials = {}
    for polygon, color_value in zip(mesh.polygons, data["faceColors"]):
        material = materials.get(color_value)
        if material is None:
            material = bpy.data.materials.new(f"OSRS_HSL_{color_value:05d}")
            material.diffuse_color = jagex_hsl_to_rgba(color_value)
            material.roughness = 0.8
            mesh.materials.append(material)
            materials[color_value] = material
        polygon.material_index = list(materials).index(color_value)

    radius = max((sum(component * component for component in vertex) ** 0.5 for vertex in vertices), default=100.0)
    add_direction_marker(collection, radius)

    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.context.scene["face_swap_note"] = "Edit geometry freely, but retain vertex/triangle order for round-trip export."
    bpy.context.scene.tool_settings.transform_pivot_point = "MEDIAN_POINT"

    blend_path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_path))


if __name__ == "__main__":
    main()
