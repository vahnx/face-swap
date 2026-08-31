# Plugin Hub Submission Checklist

Use this checklist before opening or updating a RuneLite Plugin Hub submission.

## Source repository

- Keep the plugin source and the Plugin Hub manifest in their separate repositories.
- Add a committed root-level `icon.png` for the Plugin Hub listing. It must be a real PNG no larger than `48x72` pixels; `48x48` is the preferred size.
- Keep the runtime toolbar icon in the plugin's packaged resources. For Face Swap, the runtime icon is `src/main/resources/face_swap_icon.png`; it does not replace the root-level Plugin Hub icon.
- Verify the icon is present in the committed tree, not only in the working tree:

  ```powershell
  git ls-tree -r HEAD -- icon.png
  ```

- Run `./gradlew.bat clean test` and `git diff --check` before publishing a source commit.
- When assets are near the size budget, run `./gradlew.bat clean test jar`, inspect the packaged JAR size, and follow the compression tiers in [Face Asset Standards](asset-standards.md).
- Push the source commit first and record its full SHA.

## Package-size limits

The default Plugin Hub packager limit for the final plugin JAR is exactly
`10,485,760` bytes: `10 * 1,048,576` bytes. The limit is inclusive, so
`10,485,760` is accepted and `10,485,761` is over the limit. Face Swap does
not set a manifest-specific `jarSizeLimitMiB` override, so this default applies.
This is enforced by the [Plugin Hub packager](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java).

The tooling emits a nearing-limit warning above 80%, which is
`8,388,608` bytes. Treat that as a release warning, not extra available
capacity. The JAR limit is measured against the packaged output, including
classes and everything under `src/main/resources/`; it is not the size of the
Git repository, the working tree, or an ignored backup directory. `dev-assets/`
is excluded from the standard Plugin Hub build.

Use this local preflight from the repository root:

```powershell
./gradlew.bat clean test jar
$jar = Get-ChildItem build/libs -Filter *.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
"{0} bytes: {1}" -f $jar.Length, $jar.FullName
```

Keep the measured JAR at or below `10,485,760` bytes. Leave practical headroom
for the final Plugin Hub build because the Hub's packaged artifact, rather than
the local filename or source tree, is authoritative. Recheck the exact pinned
source commit after every asset change.

The source archive has a separate Plugin Hub source-size check. Do not confuse
that source cap with the final JAR cap; a source archive passing does not prove
that the resource-heavy plugin JAR passes.

## Plugin Hub repository

- Start the submission branch from the current upstream `master`, not an old local or fork `master`.
- Update only `plugins/face-swap` (or the relevant plugin manifest) with the full pushed source commit SHA.
- The manifest is the release pin; Plugin Hub builds the exact commit named there. The source repository's later `main` changes do not automatically update the submission.
- The manifest does not pin a local JAR. Plugin Hub rebuilds the pinned source and packages its own final artifact, so local JAR size is a preflight estimate only.
- Verify the PR changes the manifest as `MODIFIED`, rather than adding a duplicate manifest file because the fork was stale.
- A successful `build` check confirms the manifest/build path. `RuneLite Plugin Hub Checks` may remain red with `Requires maintainer review`; that is the normal maintainer-review gate and is not the same as a failed build.

## After submission

- Verify the PR is open, points to the intended fork branch, and contains the expected manifest SHA.
- Wait for maintainer review and respond to the exact current feedback. Do not assume a merged source README or source PR automatically changes the Plugin Hub submission.
