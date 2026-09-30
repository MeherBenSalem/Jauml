Write-Host "================ JAUML JSON Library Verification Script ================" -ForegroundColor Green

$versions = @("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
$root = Get-Location

foreach ($v in $versions) {
    Write-Host "`n---------------- Verification for Version: $v ----------------" -ForegroundColor Cyan
    $dir = Join-Path $root $v
    if (-not (Test-Path $dir)) {
        Write-Host "Directory $dir does not exist, skipping." -ForegroundColor Yellow
        continue
    }

    Write-Host "Navigating to $dir" -ForegroundColor DarkGray
    Set-Location $dir

    Write-Host "1. Cleaning build directories..." -ForegroundColor DarkGray
    $cleanProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "clean" -NoNewWindow -Wait -PassThru
    if ($cleanProcess.ExitCode -ne 0) {
        Write-Host "ERROR: Clean task failed for version $v!" -ForegroundColor Red
        Set-Location $root
        exit 1
    }

    Write-Host "2. Running Java compilation and unit/integration tests..." -ForegroundColor DarkGray
    $testProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "test" -NoNewWindow -Wait -PassThru
    if ($testProcess.ExitCode -ne 0) {
        Write-Host "ERROR: Unit/Integration tests failed for version $v!" -ForegroundColor Red
        Set-Location $root
        exit 1
    }

    Write-Host "3. Building the mod jar..." -ForegroundColor DarkGray
    $buildProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "build" -NoNewWindow -Wait -PassThru
    if ($buildProcess.ExitCode -ne 0) {
        Write-Host "ERROR: Build failed for version $v!" -ForegroundColor Red
        Set-Location $root
        exit 1
    }

    Write-Host "SUCCESS: Version $v verified successfully!" -ForegroundColor Green
}

Set-Location $root
Write-Host "`n================ ALL VERSIONS VERIFIED SUCCESSFULLY! ================" -ForegroundColor Green
