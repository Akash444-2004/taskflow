$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$dataDirectory = Join-Path $projectRoot ".dev\pgdata"
$pgCtl = "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe"

if (-not (Test-Path $dataDirectory)) {
    Write-Host "No local TaskFlow database cluster exists."
    exit 0
}

& $pgCtl -D $dataDirectory status *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "TaskFlow PostgreSQL is not running."
    exit 0
}

& $pgCtl -D $dataDirectory stop -m fast
Write-Host "TaskFlow PostgreSQL stopped."
