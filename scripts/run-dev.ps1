# Full stack launcher.
# Asks once for the PostgreSQL password (masked or via the VS Code quick input),
# runs pre-flight checks, then starts the backend and the frontend in separate
# windows. Both stream their output to .dev\*.out.log, so a crashed process is
# always diagnosable. This terminal is free to close once everything is up.

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

Write-Host "== TaskFlow full stack =="

# 1. Credentials (password stays in memory only).
$dbPassword = Resolve-DatabasePassword
Set-TaskFlowEnvironment -DatabasePassword $dbPassword

# 2. Pre-flight checks: fail fast with a clear message instead of a broken stack.
Assert-PostgreSqlReachable -Port 5432
Ensure-FrontendDependencies
Assert-PortFree -Port 8080 -Purpose "the API"
Assert-PortFree -Port 5173 -Purpose "the web app"

# 3. Start each side in its own window (survives this terminal closing).
$worker = Join-Path $PSScriptRoot "dev-process.ps1"

Write-Host "Opening backend window (http://localhost:8080)..."
Start-Process -FilePath "powershell" -ArgumentList @(
    "-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass",
    "-Command", "& '$worker' -Target backend"
)

Write-Host "Opening frontend window (http://localhost:5173)..."
Start-Process -FilePath "powershell" -ArgumentList @(
    "-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass",
    "-Command", "& '$worker' -Target frontend"
)

# 4. Wait for the API to come up and report.
Write-Host "Waiting for the API to become healthy (this can take a minute on the first run)..."
$deadline = (Get-Date).AddSeconds(150)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $healthy = $true; break }
    }
    catch {
        # Not up yet; keep waiting.
    }
}

Write-Host ""
if ($healthy) {
    Write-Host "Backend:  UP   -> http://localhost:8080" -ForegroundColor Green
    Write-Host "Frontend:      -> http://localhost:5173" -ForegroundColor Green
    Write-Host "You can close this terminal. Use scripts\stop-all.ps1 to stop both."
}
else {
    Write-Host "The API did not become healthy in time. Check the backend window or .dev\backend.out.log." -ForegroundColor Yellow
}
