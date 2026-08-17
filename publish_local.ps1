# Local build + GitHub release for Jauml MultiLoader.
# Modrinth/CurseForge (upload-only, no CI build):
#   gh workflow run publish.yml --repo MeherBenSalem/jauml -f version=X.Y.Z
#
# Usage:
#   .\publish_local.ps1
#   .\publish_local.ps1 -Version 2.1.1 -SkipBuild

param(
    [string]$Version = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
Set-Location $root

if (-not $Version) {
    $gp = Get-Content (Join-Path $root "26.1.2\gradle.properties") | Where-Object { $_ -match '^version=' }
    if ($gp -match '^version=(.+)$') { $Version = $Matches[1].Trim() }
}
if (-not $Version) { throw "Could not resolve version from 26.1.2/gradle.properties" }

Write-Host "=== Jauml local publish v$Version ===" -ForegroundColor Green

if (-not $SkipBuild) {
    & (Join-Path $root "build_all_jars.ps1")
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$distDir = Join-Path $root "dist"
if (-not (Test-Path $distDir)) { throw "dist/ missing - run build first" }
$jars = Get-ChildItem $distDir -Filter "jauml-*.jar"
if ($jars.Count -eq 0) { throw "No jars in dist/" }

$changelog = Join-Path $root "CHANGELOG.md"
$notesFile = if (Test-Path $changelog) { $changelog } else { $null }

$tag = "v$Version"
$releaseExists = $false
try {
    gh release view $tag --repo MeherBenSalem/jauml 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $releaseExists = $true }
} catch {
    $releaseExists = $false
}

if ($releaseExists) {
    Write-Host "GitHub release $tag exists - uploading assets..." -ForegroundColor Yellow
    foreach ($jar in $jars) {
        gh release upload $tag $jar.FullName --repo MeherBenSalem/jauml --clobber
    }
} else {
    $ghArgs = @("release", "create", $tag)
    foreach ($jar in $jars) { $ghArgs += $jar.FullName }
    $ghArgs += @("--repo", "MeherBenSalem/jauml", "--title", "Jauml $Version")
    if ($notesFile) {
        $ghArgs += @("--notes-file", $notesFile)
    } else {
        $ghArgs += @("--notes", "Jauml $Version")
    }
    & gh @ghArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host ""
Write-Host "Jars in dist/ ($($jars.Count)):" -ForegroundColor Cyan
$jars | ForEach-Object { Write-Host "  $($_.Name)" }

Write-Host ""
Write-Host "Modrinth + CurseForge (upload-only workflow):" -ForegroundColor Yellow
Write-Host "  gh workflow run publish.yml --repo MeherBenSalem/jauml -f version=$Version"
Write-Host "  gh workflow run publish.yml --repo MeherBenSalem/jauml -f version=$Version -f curseforge_only=true"
