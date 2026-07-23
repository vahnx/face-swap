# Development-Only Assets

This policy applies to creator or fictional-character assets that are available only in local development/debug launches.

## Required Layout

- Release-approved assets belong in `src/main/resources/heads/content_creators/` or `src/main/resources/heads/fictional_characters/`.
- Development-only assets belong under the matching `dev-assets/heads/<category>/` directory.
- `dev-assets/` is intentionally gitignored. Its contents must not be committed, pushed, or included in a Plugin Hub submission.
- Never rely on hiding a picker entry to prevent publication. Plugin Hub packages everything under `src/main/resources`.

The local Gradle test source set includes `dev-assets/`, so `gradlew.bat run` and tests resolve category paths such as `/heads/content_creators/<name>_front.png`. Plugin Hub uses `build=standard` and packages main resources only, excluding the local development directory.

## Head Availability

Declare availability in `FaceSwapHead`:

- `releaseAvailable=true` exposes the head in release and debug builds.
- `releaseAvailable=false, debugAvailable=true` makes the head debug-only.
- Both values `false` retain code/config compatibility without exposing the head.

Every debug-only head must have its assets outside `src/main/resources`. `FaceSwapReleaseAssetsTest` enforces this separation.

## Before Submission

1. Run `.\gradlew.bat clean test`.
2. Confirm `FaceSwapReleaseAssetsTest` passes.
3. Confirm `git status --short` does not show `dev-assets/`.
4. Inspect `src/main/resources/heads/` and verify every bundled likeness has documented release permission.
5. Do not force-add files from `dev-assets/`.
