# SPDX-License-Identifier: GPL-3.0-or-later
[CmdletBinding()]
param([string]$Maven = 'mvn', [string]$MavenRepository, [string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $repoRoot ('ieraui-build-'+(Get-Date -Format 'yyyyMMdd-HHmmss')) }
$buildArgs = @('-B','-ntp','-pl','forge-gui-desktop','-am',
    '-Dtest=LayoutFileStoreTest,PhaseInputGuardTest,InputLondonMulliganTest,PromptInteractionTest,CardActionPanelTest,PlayerStatusBarTest,CommanderCastabilityTest,PhaseTimelineTest,ResourceChangesTest,StackActivityTest,AbilityIconLayoutTest',
    '-Dsurefire.failIfNoSpecifiedTests=false','package')
if ($MavenRepository) { $buildArgs = @("-Dmaven.repo.local=$MavenRepository") + $buildArgs }
Push-Location -LiteralPath $repoRoot
try {
    & $Maven @buildArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
    & (Join-Path $PSScriptRoot 'Package-Addon.ps1') -BuiltJar (Join-Path $repoRoot 'forge-gui-desktop/target/forge-gui-desktop-2.0.15-jar-with-dependencies.jar') -OutputDirectory $OutputDirectory
} finally { Pop-Location }
