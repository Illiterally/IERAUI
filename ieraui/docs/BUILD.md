# Build IERAUI 0.1.14 for Forge 2.0.15

This local update is based on official Forge tag `forge-2.0.15`, commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, and retains the IERAUI desktop UI changes, including the Commander availability cue.

The source is on the local branch `ieraui-forge-2.0.15`. This update has not been published to GitHub; public v0.1.12 remains a separate release for Forge 2.0.14.

Requirements: JDK 17 or later, Maven, Git, and PowerShell. This update was built with Temurin JDK 21.0.12.1+1 and Maven 3.9.16.

From this source checkout:

```powershell
./ieraui/scripts/Build-Addon.ps1 -Maven /path/to/mvn.cmd -MavenRepository /path/to/cache -OutputDirectory /path/to/new-output
```

The script builds the six desktop modules and runs the focused IERAUI regression suite. It packages the 42 modified/added source classes and their nested classes, the English language file, notices, and launcher. The remaining engine classes and resources come from official Forge 2.0.15.

To package an already-tested build:

```powershell
./ieraui/scripts/Package-Addon.ps1 -BuiltJar ./forge-gui-desktop/target/forge-gui-desktop-2.0.15-jar-with-dependencies.jar -OutputDirectory ./new-release-output
```

The launcher verifies the official base JAR's SHA-256 before loading the addon. Keep this source checkout and `ieraui/ieraui-0.1.14.patch` with the local update. The historical 0.1.12 patch remains for reproducing that older release.

See [VALIDATION.md](VALIDATION.md) for verification and [Forge's contribution guide](../../CONTRIBUTING.md) for upstream conventions.
