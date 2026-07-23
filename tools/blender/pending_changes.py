"""Codex-prepared edits for the currently open Face Swap base mesh.

Codex updates this file when a requested mesh operation is suitable for automation.
The Blender sidebar add-on calls apply() and creates a backup before execution.
"""


def apply(context, target):
    context.view_layer.objects.active = target
    target.select_set(True)
    target.rotation_euler = (0.0, 0.0, 0.0)
    target.location = (0.0, 0.0, 0.0)
    target.scale = (1.0, 1.0, 1.0)
    return "Reset base head transform for live rotation calibration"
