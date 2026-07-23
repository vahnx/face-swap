import math
import pathlib
import sys

import bpy
from mathutils import Vector


def script_args():
    separator = sys.argv.index("--") if "--" in sys.argv else len(sys.argv)
    return [pathlib.Path(value).resolve() for value in sys.argv[separator + 1 :]]


def main():
    input_path, output_path = script_args()
    bpy.ops.wm.open_mainfile(filepath=str(input_path))
    target = next(obj for obj in bpy.data.objects if obj.type == "MESH" and obj.name.endswith("Head_Base"))
    coordinates = [vertex.co.copy() for vertex in target.data.vertices]
    minimum = Vector(tuple(min(coordinate[axis] for coordinate in coordinates) for axis in range(3)))
    maximum = Vector(tuple(max(coordinate[axis] for coordinate in coordinates) for axis in range(3)))
    source_center = (minimum + maximum) * 0.5
    source_radius = (maximum - minimum) * 0.5

    # Blender axes are OSRS X, -Z, -Y. This matches the neutral player-head bounds.
    destination_center = Vector((0.0, 8.0, 181.0))
    destination_radius = Vector((11.0, 12.0, 17.0))
    for vertex in target.data.vertices:
        relative = vertex.co - source_center
        normalized = Vector(tuple(relative[axis] / max(0.001, source_radius[axis]) for axis in range(3)))
        if normalized.length_squared < 0.0001:
            normalized = Vector((0.0, 1.0, 0.0))
        normalized.normalize()
        reshaped = destination_center + Vector(tuple(normalized[axis] * destination_radius[axis] for axis in range(3)))

        # Build a modest nose and chin into the otherwise neutral ellipsoid.
        front = max(0.0, normalized.y)
        nose = math.exp(-((normalized.x / 0.28) ** 2) - (((normalized.z - 0.08) / 0.30) ** 2))
        chin = math.exp(-((normalized.x / 0.45) ** 2) - (((normalized.z + 0.72) / 0.22) ** 2))
        reshaped.y += front * (nose * 3.0 + chin * 1.2)
        vertex.co = reshaped

    target.data.update()
    target.name = "DenseFaceHead_Base"
    target.data.name = "DenseFaceHead_Mesh"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output_path))


if __name__ == "__main__":
    main()
