# Release validation — 0.1.12

Checked on Windows on September 22, 2026, using Temurin JDK 21.0.12.1+1 and Maven 3.9.16.

## Source and build

- Prepared a clean checkout at Forge 2.0.14 (`a37a865a53280dd8ad6fad3384d69611e8c5a42f`) and applied the shipped addon source.
- Confirmed the 40 changed/new main Java files and English language resource match the locally used 0.1.12 version.
- Built all six desktop reactor modules successfully.
- Ran 66 focused checks: **zero failures, errors, or skips**. The exact selection is in `ieraui/scripts/Build-Addon.ps1`.
- Checked the staged diff and confirmed the cumulative UI patch reverses cleanly from the release source.

## Addon package

- Packaged 93 classes, including nested classes, from the 40 source files listed in `ieraui/source-files.txt`.
- Verified all 93 classes load from the addon JAR and that the unchanged game engine loads from the original Forge JAR. Reflected over their declared methods to check linkage against that distribution.
- Verified the required official Forge JAR's SHA-256: `3C0A7C385B9207E593FE149255E00D1CD7E556809F138568D07E3828B78E5DD9`.
- Launched the supplied Windows launcher in a separate folder containing the official Forge resources, original JAR, addon files, and a fresh portable profile. Confirmed the home screen and IERAUI window title.
- Started a default preconstructed match and checked the mulligan prompt, horizontal phase strip, hand/resource bar, and enlarged current power/toughness at desktop size. This was a startup smoke check, not a complete match test.
- Checked that settings were created in the isolated profile. The test did not use an existing personal profile or decks.

The launcher checksum check uses Windows PowerShell's .NET cryptography support, so it doesn't depend on PowerShell module discovery. This was corrected during the fresh-install check.

## Scope

These checks cover this Windows package and the selected UI regressions. They are not the full upstream test suite, an exhaustive card-interaction test, or validation on macOS, Linux, mobile, or another Forge version. The checksum file beside the release covers the final downloadable ZIP; its included manifest lists the actual addon classes and file hashes.
