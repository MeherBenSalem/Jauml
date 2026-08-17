# run_all_clients.ps1
# Launches Fabric Minecraft clients for every supported workspace.
# Each client opens in its own terminal window so they run simultaneously.

Write-Host "================ JAUML – Launch All Clients ================" -ForegroundColor Green

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---------------------------------------------------------------------------
# Fabric clients for all supported Minecraft workspaces.
# ---------------------------------------------------------------------------
$clients = @(
    @{ Dir = "1.20.1";  Loader = "Fabric";   Task = ":fabric:runClient"   },

    @{ Dir = "1.21.1";  Loader = "Fabric";   Task = ":fabric:runClient"   },

    @{ Dir = "1.21.11"; Loader = "Fabric";   Task = ":fabric:runClient"   },

    @{ Dir = "26.1.2";  Loader = "Fabric";   Task = ":fabric:runClient"   },

    @{ Dir = "26.2";    Loader = "Fabric";   Task = ":fabric:runClient"   }

)

foreach ($c in $clients) {
    $dir   = Join-Path $root $c.Dir
    $title = "JAUML $($c.Dir) – $($c.Loader) Client"
    $task  = $c.Task

    if (-not (Test-Path $dir)) {
        Write-Host "  [SKIP] $dir not found." -ForegroundColor Yellow
        continue
    }

    Write-Host "  Launching [$title] -> gradlew $task" -ForegroundColor Cyan

    # Open each client in its own cmd window (stays open while game runs).
    Start-Process "cmd.exe" `
        -ArgumentList "/k", "title $title && gradlew.bat $task" `
        -WorkingDirectory $dir `
        -WindowStyle Normal
}

Write-Host "`nAll $($clients.Count) client windows have been launched." -ForegroundColor Green
Write-Host "Each window will compile then open the Minecraft client." -ForegroundColor DarkGray
