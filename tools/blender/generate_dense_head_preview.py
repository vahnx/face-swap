import math
import pathlib
import sys

import bpy


def script_args():
    separator = sys.argv.index("--") if "--" in sys.argv else len(sys.argv)
    return [pathlib.Path(value).resolve() for value in sys.argv[separator + 1 :]]


def main():
    output_path = script_args()[0]
    segments = 50
    rings = 40
    vertices = [(0.0, 0.0, 198.0)]
    for ring in range(1, rings + 1):
        theta = math.pi * ring / (rings + 1)
        vertical = math.cos(theta)
        ring_radius = math.sin(theta)
        for segment in range(segments):
            phi = math.pi * 2.0 * segment / segments
            normalized_x = ring_radius * math.sin(phi)
            normalized_z = ring_radius * math.cos(phi)
            front = max(0.0, -normalized_z)
            nose = math.exp(-((normalized_x / 0.28) ** 2) - (((vertical - 0.08) / 0.30) ** 2))
            chin = math.exp(-((normalized_x / 0.45) ** 2) - (((vertical + 0.72) / 0.22) ** 2))
            osrs_x = normalized_x * 11.0
            osrs_y = -181.0 - vertical * 17.0
            osrs_z = normalized_z * 12.0 - front * (nose * 3.0 + chin * 1.2)
            vertices.append((osrs_x, -osrs_z, -osrs_y))
    bottom = len(vertices)
    vertices.append((0.0, 0.0, 164.0))

    faces = []
    for segment in range(segments):
        faces.append((0, 1 + (segment + 1) % segments, 1 + segment))
    for ring in range(rings - 1):
        current_ring = 1 + ring * segments
        next_ring = current_ring + segments
        for segment in range(segments):
            next_segment = (segment + 1) % segments
            a = current_ring + segment
            b = current_ring + next_segment
            c = next_ring + segment
            d = next_ring + next_segment
            faces.extend(((a, b, c), (b, d, c)))
    last_ring = 1 + (rings - 1) * segments
    for segment in range(segments):
        faces.append((bottom, last_ring + segment, last_ring + (segment + 1) % segments))

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    mesh = bpy.data.meshes.new("GeneratedDenseHead_Mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new("GeneratedDenseHead_Base", mesh)
    bpy.context.scene.collection.objects.link(obj)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output_path))


if __name__ == "__main__":
    main()
