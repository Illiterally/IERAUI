# SPDX-License-Identifier: GPL-3.0-or-later
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$BuiltJar,
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$builtPath = (Resolve-Path -LiteralPath $BuiltJar).Path
$outputPath = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $outputPath) { throw 'Choose a new output directory; existing releases are never overwritten.' }
New-Item -ItemType Directory -Path $outputPath | Out-Null
$payload = Join-Path $outputPath 'payload'
New-Item -ItemType Directory -Path $payload | Out-Null
$addonPath = Join-Path $payload 'ieraui-addon-0.1.12.jar'
$sources = @(Get-Content -LiteralPath (Join-Path $repoRoot 'ieraui/source-files.txt') | Where-Object { $_.Trim() })
$inputZip = [IO.Compression.ZipFile]::OpenRead($builtPath)
$outputZip = [IO.Compression.ZipFile]::Open($addonPath, [IO.Compression.ZipArchiveMode]::Create)
$classNames = [Collections.Generic.HashSet[string]]::new()
try {
    foreach ($source in $sources) {
        if ($source -notmatch '/src/main/java/(.+)\.java$') { throw "Unexpected source path: $source" }
        $classRoot = $Matches[1]
        $entries = @($inputZip.Entries | Where-Object {
            $_.FullName -eq ($classRoot + '.class') -or
            ($_.FullName.StartsWith($classRoot + '$', [StringComparison]::Ordinal) -and $_.FullName.EndsWith('.class'))
        })
        if (-not ($entries | Where-Object { $_.FullName -eq ($classRoot + '.class') })) { throw "Missing compiled source: $source" }
        foreach ($entry in $entries) {
            if (-not $classNames.Add($entry.FullName)) { continue }
            $dest = $outputZip.CreateEntry($entry.FullName, [IO.Compression.CompressionLevel]::Optimal)
            $dest.LastWriteTime = [DateTimeOffset]::new(2026,9,22,0,0,0,[TimeSpan]::Zero)
            $readStream = $entry.Open()
            $writeStream = $dest.Open()
            try { $readStream.CopyTo($writeStream) } finally { $readStream.Dispose(); $writeStream.Dispose() }
        }
    }
    foreach ($notice in @(@('LICENSE','META-INF/LICENSE'),@('ieraui/NOTICE.md','META-INF/NOTICE.md'))) {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($outputZip, (Join-Path $repoRoot $notice[0]), $notice[1], [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $inputZip.Dispose(); $outputZip.Dispose() }
New-Item -ItemType Directory -Path (Join-Path $payload 'res/languages'),(Join-Path $payload 'IERAUI'),(Join-Path $payload 'data/preferences') | Out-Null
Copy-Item -LiteralPath (Join-Path $repoRoot 'ieraui/release/Play IERAUI.cmd') -Destination $payload
Copy-Item -LiteralPath (Join-Path $repoRoot 'ieraui/release/forge.profile.properties') -Destination $payload
Copy-Item -LiteralPath (Join-Path $repoRoot 'ieraui/release/forge.preferences') -Destination (Join-Path $payload 'data/preferences/forge.preferences')
Copy-Item -LiteralPath (Join-Path $repoRoot 'forge-gui/res/languages/en-US.properties') -Destination (Join-Path $payload 'res/languages/en-US.properties')
Copy-Item -LiteralPath (Join-Path $repoRoot 'LICENSE') -Destination (Join-Path $payload 'IERAUI/LICENSE.txt')
Copy-Item -LiteralPath (Join-Path $repoRoot 'ieraui/NOTICE.md') -Destination (Join-Path $payload 'IERAUI/NOTICE.md')
foreach ($doc in @('SETUP.md','FEATURES.md','BUILD.md','ROADMAP.md','VALIDATION.md')) {
    Copy-Item -LiteralPath (Join-Path $repoRoot ('ieraui/docs/'+$doc)) -Destination (Join-Path $payload ('IERAUI/'+$doc))
}
$sourceText = @'
IERAUI 0.1.12 corresponding source and build instructions:
https://github.com/Illiterally/IERAUI/tree/v0.1.12
https://github.com/Illiterally/IERAUI/archive/refs/tags/v0.1.12.zip

The full Forge-based source is in that release tag. Build instructions are in
ieraui/docs/BUILD.md. The upstream base is Forge 2.0.14, commit
a37a865a53280dd8ad6fad3384d69611e8c5a42f. The addon is GPL-3.0-or-later.
'@
[IO.File]::WriteAllText((Join-Path $payload 'IERAUI/SOURCE.txt'),$sourceText,[Text.UTF8Encoding]::new($false))
$inventory = [ordered]@{
    addon = 'IERAUI'; version = '0.1.12'; forgeVersion = '2.0.14'
    upstreamCommit = 'a37a865a53280dd8ad6fad3384d69611e8c5a42f'
    requiredForgeJarSha256 = '3C0A7C385B9207E593FE149255E00D1CD7E556809F138568D07E3828B78E5DD9'
    addonJarSha256 = (Get-FileHash -LiteralPath $addonPath).Hash
    languageSha256 = (Get-FileHash -LiteralPath (Join-Path $payload 'res/languages/en-US.properties')).Hash
    compiledSourceCount = $sources.Count; classCount = $classNames.Count
    classes = @($classNames | Sort-Object)
}
[IO.File]::WriteAllText((Join-Path $payload 'IERAUI/manifest.json'),($inventory|ConvertTo-Json -Depth 4)+"`n",[Text.UTF8Encoding]::new($false))
$zipPath = Join-Path $outputPath 'IERAUI-0.1.12-addon.zip'
[IO.Compression.ZipFile]::CreateFromDirectory($payload,$zipPath,[IO.Compression.CompressionLevel]::Optimal,$false)
$zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $outputPath 'SHA256SUMS.txt'), "$zipHash  IERAUI-0.1.12-addon.zip`n",[Text.UTF8Encoding]::new($false))
Write-Output "Packaged $($classNames.Count) Forge classes from $($sources.Count) source files: $zipPath"
