# PowerShell Script to Rebuild all JAUML Jars and collect them in 'dist' directory

Write-Host "================ JAUML Rebuild and Collect Jars Script ================" -ForegroundColor Green

$versions = @("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
$root = Get-Location
$distDir = Join-Path $root "dist"

# Ensure clean dist directory
if (Test-Path $distDir) {
    Write-Host "Clearing existing dist directory..." -ForegroundColor DarkGray
    Remove-Item -Path $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir | Out-Null

foreach ($v in $versions) {
    Write-Host "`n---------------- Building Version: $v ----------------" -ForegroundColor Cyan
    $dir = Join-Path $root $v
    if (-not (Test-Path $dir)) {
        Write-Host "Directory $dir does not exist, skipping." -ForegroundColor Yellow
        continue
    }

    Write-Host "Navigating to $dir" -ForegroundColor DarkGray
    Set-Location $dir

    Write-Host "Cleaning and building all loaders for version $v..." -ForegroundColor DarkGray
    $process = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "clean", "build" -NoNewWindow -Wait -PassThru
    if ($process.ExitCode -ne 0) {
        Write-Host "ERROR: Build failed for version $v!" -ForegroundColor Red
        Set-Location $root
        exit 1
    }

    # Find and copy built jars from fabric/forge/neoforge
    $subProjects = @("fabric", "forge", "neoforge")
    foreach ($sp in $subProjects) {
        $libsDir = Join-Path (Join-Path $dir $sp) "build\libs"
        if (Test-Path $libsDir) {
            Get-ChildItem -Path $libsDir -Filter "*.jar" | ForEach-Object {
                $fileName = $_.Name
                # Exclude sources, javadoc, and dev/unobfuscated jars if they exist to keep only the clean release jars
                if ($fileName -notlike "*-sources.jar" -and $fileName -notlike "*-javadoc.jar" -and $fileName -notlike "*-dev.jar" -and $fileName -notlike "*-sources-dev.jar") {
                    Write-Host "Copying $fileName to dist/" -ForegroundColor Gray
                    Copy-Item -Path $_.FullName -Destination $distDir -Force
                }
            }
        }
    }
}

Set-Location $root
Write-Host "`n================ ALL JARS BUILT AND COLLECTED IN 'dist/' ================" -ForegroundColor Green
Get-ChildItem -Path $distDir | Select-Object Name, Length
