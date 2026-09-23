# Build IERAUI 0.1.12

The release source is a Forge fork pinned to upstream commit `a37a865a53280dd8ad6fad3384d69611e8c5a42f` (Forge 2.0.14), with the addon changes applied. Use the `v0.1.12` tag to reproduce this release, rather than an evolving default branch.

## Tools

- Git, JDK 17 or later, and Maven. The local release used Temurin JDK 21.0.12.1+1 and Maven 3.9.16.
- PowerShell for the packaging scripts; they use .NET's ZIP support and do not need third-party PowerShell modules.
- Network access for Maven's declared dependencies. Maven's normal local cache is supported.

```powershell
git clone --branch v0.1.12 https://github.com/Illiterally/IERAUI.git
cd IERAUI
./ieraui/scripts/Build-Addon.ps1
```

If you keep tools or Maven's cache elsewhere:

```powershell
./ieraui/scripts/Build-Addon.ps1 -Maven /path/to/mvn -MavenRepository /path/to/cache -OutputDirectory /path/to/new-output
```

Set `JAVA_HOME` to your JDK in the normal way if Maven is using the wrong Java. Do not use a JRE to compile the source.

## What the scripts do

The Maven reactor compiles Forge's six desktop-related modules and runs the focused addon regression checks. The full upstream test suite is not selected by this command.

The packaging script reads `ieraui/source-files.txt` and copies only the listed Forge classes and their nested classes from the built desktop JAR into `ieraui-addon-0.1.12.jar`. It includes the GPL license and notices, then creates the addon ZIP with the launcher, portable profile, English language resource and setup documents. It fails if an expected compiled class is missing and refuses to overwrite an existing output directory.

The launcher places the addon JAR before Forge's original JAR on the classpath. Forge still supplies the unchanged rules engine, libraries and resources. This is why the launcher requires the specific official Forge 2.0.14 JAR, and why the addon ZIP doesn't bundle those third-party libraries.

To package an already-tested build:

```powershell
./ieraui/scripts/Package-Addon.ps1 -BuiltJar ./forge-gui-desktop/target/forge-gui-desktop-2.0.14-jar-with-dependencies.jar -OutputDirectory ./new-release-output
```

JAR/ZIP metadata and build-time Forge version resources can differ across environments; don't assume a byte-identical complete Forge build. The release provides SHA-256 checksums for the published download and an inventory of the addon classes.

## Review just the addon changes

`ieraui/ieraui-0.1.12.patch` is the cumulative UI/source change against the upstream base. It does not include the repository's publication docs and packaging scripts. Apply it to a clean upstream checkout at the base commit, not to this already-patched fork. The full release source and build materials are in the release tag.

For Forge's general build instructions and contribution conventions, see the upstream [CONTRIBUTING.md](../../CONTRIBUTING.md).
