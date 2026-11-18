# Stops the TaskFlow dev processes: whatever is listening on the API (8080)
# and web app (5173) ports. PostgreSQL (5432) is intentionally never touched.

$ErrorActionPreference = "Continue"

$targets = @(
    @{ Port = 8080; Name = "API (backend)" },
    @{ Port = 5173; Name = "Web app (frontend)" }
)

foreach ($target in $targets) {
    $connections = Get-NetTCPConnection -LocalPort $target.Port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        Write-Host "$($target.Name): not running (nothing on port $($target.Port))."
        continue
    }

    $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $pids) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $processId -Force
            Write-Host "$($target.Name): stopped $($process.ProcessName) (PID $processId)."
        }
    }
}

Write-Host "Done."
