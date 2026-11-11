# Build IERAUI 0.1.21 for Forge 2.0.16

This update is based on official Forge tag `forge-2.0.16`, commit `583714b8bfbd9cb04f8bc39ad95861190caf7691`, and retains the IERAUI desktop UI changes, including the Commander availability cue.

Corresponding source: https://github.com/Illiterally/IERAUI/tree/v0.1.21.

Requirements: JDK 17 or later, Maven, Git, and PowerShell. This update was built with Temurin JDK 21.0.12.1+1 and Maven 3.9.16.

From this source checkout:

```powershell
./ieraui/scripts/Build-Addon.ps1 -Maven /path/to/mvn.cmd -MavenRepository /path/to/cache -OutputDirectory /path/to/new-output
```

The script builds the six desktop modules and runs the focused IERAUI regression suite. It packages the 66 modified/added source classes and their nested classes, the English language file, notices, and launcher. This includes the bestow view refresh, AI evaluation and blocking-check performance changes, and combat-result reporting. Damage rules, achievement thresholds, card scripts, and shuffling are unchanged. The remaining engine classes and resources come from official Forge 2.0.16.

To package an already-tested build:

```powershell
./ieraui/scripts/Package-Addon.ps1 -BuiltJar ./forge-gui-desktop/target/forge-gui-desktop-2.0.16-jar-with-dependencies.jar -OutputDirectory ./new-release-output
```

The launcher verifies the official base JAR's SHA-256 before loading the addon. Keep this source checkout and `ieraui/ieraui-0.1.21.patch` with the local update. The historical 0.1.12 patch remains for reproducing that older release.

See [VALIDATION.md](VALIDATION.md) for verification and [Forge's contribution guide](../../CONTRIBUTING.md) for upstream conventions.
