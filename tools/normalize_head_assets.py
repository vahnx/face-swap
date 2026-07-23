#!/usr/bin/env python3
"""Normalize transparent Face Swap head assets to a consistent runtime canvas."""

import argparse
import os
import shutil
from pathlib import Path

from PIL import Image


DEFAULT_SIZE = 512
DEFAULT_PADDING = 16
ALPHA_BOUNDS_THRESHOLD = 8


def normalize_asset(source_path: Path, size: int, padding: int, backup_dir: Path | None) -> None:
    with Image.open(source_path) as source:
        image = source.convert("RGBA")

    alpha = image.getchannel("A")
    visible_alpha = alpha.point(lambda value: 255 if value > ALPHA_BOUNDS_THRESHOLD else 0)
    content_bounds = visible_alpha.getbbox()
    if content_bounds is None:
        raise ValueError(f"{source_path.name} has no visible pixels")

    if backup_dir is not None:
        backup_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_path, backup_dir / source_path.name)

    content = image.crop(content_bounds)
    available = size - padding * 2
    scale = min(available / content.width, available / content.height)
    output_width = max(1, round(content.width * scale))
    output_height = max(1, round(content.height * scale))
    resized = content.resize(
        (output_width, output_height),
        Image.Resampling.LANCZOS,
        reducing_gap=3.0,
    )

    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(
        resized,
        ((size - output_width) // 2, (size - output_height) // 2),
        resized,
    )

    temporary_path = source_path.with_suffix(".normalized.png")
    output.save(temporary_path, format="PNG", optimize=True, compress_level=9)
    os.replace(temporary_path, source_path)
    print(
        f"{source_path.name}: {image.width}x{image.height} "
        f"content {content.width}x{content.height} -> {output_width}x{output_height}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("asset_dir", type=Path)
    parser.add_argument("--size", type=int, default=DEFAULT_SIZE)
    parser.add_argument("--padding", type=int, default=DEFAULT_PADDING)
    parser.add_argument("--backup-dir", type=Path)
    args = parser.parse_args()

    if args.size <= 0 or args.padding < 0 or args.padding * 2 >= args.size:
        parser.error("size and padding do not leave a positive content area")

    asset_paths = sorted(args.asset_dir.glob("*.png"))
    if not asset_paths:
        parser.error(f"no PNG assets found under {args.asset_dir}")

    for asset_path in asset_paths:
        normalize_asset(asset_path, args.size, args.padding, args.backup_dir)


if __name__ == "__main__":
    main()
