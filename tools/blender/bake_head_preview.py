import pathlib
import sys

import bpy


def script_args():
    separator = sys.argv.index("--") if "--" in sys.argv else len(sys.argv)
    return [pathlib.Path(value).resolve() for value in sys.argv[separator + 1 :]]


def sample(image, u, v):
    width, height = image.size
    center_x = max(0, min(width - 1, round(u * (width - 1))))
    center_y = max(0, min(height - 1, round(v * (height - 1))))
    channels = [0.0, 0.0, 0.0]
    samples = 0
    for source_y in range(max(0, center_y - 1), min(height - 1, center_y + 1) + 1):
        pixel_y = height - 1 - source_y
        for x in range(max(0, center_x - 1), min(width - 1, center_x + 1) + 1):
            offset = (pixel_y * width + x) * 4
            if image.pixels[offset + 3] <= 0.09:
                continue
            for channel in range(3):
                channels[channel] += image.pixels[offset + channel]
            samples += 1
    if not samples:
        return (0.68, 0.43, 0.36, 1.0)
    return tuple(channel / samples for channel in channels) + (1.0,)


def main():
    blend_path, front_path, back_path, output_path = script_args()
    bpy.ops.wm.open_mainfile(filepath=str(blend_path))
    target = next(obj for obj in bpy.data.objects if obj.type == "MESH" and obj.name.endswith("Head_Base"))
    front_image = bpy.data.images.load(str(front_path), check_existing=False)
    back_image = bpy.data.images.load(str(back_path), check_existing=False)
    osrs_vertices = [(vertex.co.x, -vertex.co.z, -vertex.co.y) for vertex in target.data.vertices]
    min_x = min(vertex[0] for vertex in osrs_vertices)
    max_x = max(vertex[0] for vertex in osrs_vertices)
    min_y = min(vertex[1] for vertex in osrs_vertices)
    max_y = max(vertex[1] for vertex in osrs_vertices)
    min_z = min(vertex[2] for vertex in osrs_vertices)
    max_z = max(vertex[2] for vertex in osrs_vertices)
    center_z = (min_z + max_z) * 0.5
    materials = {}
    for polygon in target.data.polygons:
        points = [osrs_vertices[index] for index in polygon.vertices]
        face_x = sum(point[0] for point in points) / 3.0
        face_y = sum(point[1] for point in points) / 3.0
        face_z = sum(point[2] for point in points) / 3.0
        front = face_z <= center_z
        u = (face_x - min_x) / (max_x - min_x)
        if not front:
            u = 1.0 - u
        v = (face_y - min_y) / (max_y - min_y)
        color = sample(front_image if front else back_image, u, v)
        key = tuple(round(channel * 31) for channel in color[:3])
        material_index = materials.get(key)
        if material_index is None:
            material = bpy.data.materials.new(f"Baked_{key[0]}_{key[1]}_{key[2]}")
            material.diffuse_color = color
            material.roughness = 0.8
            target.data.materials.append(material)
            material_index = len(target.data.materials) - 1
            materials[key] = material_index
        polygon.material_index = material_index
    bpy.ops.wm.save_as_mainfile(filepath=str(output_path))


if __name__ == "__main__":
    main()
