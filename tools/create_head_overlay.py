#!/usr/bin/env python3
"""Create a transparent RuneLite face overlay PNG from a chroma-key source."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def is_green_key(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return g > 145 and r < 90 and b < 90 and g - r > 75 and g - b > 75


def create_overlay(input_path: Path, output_path: Path, size: int, padding: int) -> None:
    source = Image.open(input_path).convert("RGBA")
    keyed = Image.new("RGBA", source.size, (0, 0, 0, 0))
    pixels = source.load()
    keyed_pixels = keyed.load()

    bounds: list[int] | None = None
    for y in range(source.height):
        for x in range(source.width):
            pixel = pixels[x, y]
            if is_green_key(pixel):
                continue

            keyed_pixels[x, y] = pixel
            if bounds is None:
                bounds = [x, y, x, y]
            else:
                bounds[0] = min(bounds[0], x)
                bounds[1] = min(bounds[1], y)
                bounds[2] = max(bounds[2], x)
                bounds[3] = max(bounds[3], y)

    if bounds is None:
        raise ValueError("No foreground pixels found after chroma-key removal")

    left = max(0, bounds[0] - padding)
    top = max(0, bounds[1] - padding)
    right = min(source.width, bounds[2] + padding + 1)
    bottom = min(source.height, bounds[3] + padding + 1)
    cropped = keyed.crop((left, top, right, bottom))

    side = max(cropped.width, cropped.height)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.alpha_composite(cropped, ((side - cropped.width) // 2, (side - cropped.height) // 2))
    overlay = square.resize((size, size), Image.Resampling.LANCZOS)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(output_path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="Source PNG with #00ff00-style chroma background")
    parser.add_argument("output", type=Path, help="Output transparent PNG")
    parser.add_argument("--size", type=int, default=512, help="Square output size in pixels")
    parser.add_argument("--padding", type=int, default=14, help="Foreground crop padding in source pixels")
    args = parser.parse_args()

    create_overlay(args.input, args.output, args.size, args.padding)


if __name__ == "__main__":
    main()
