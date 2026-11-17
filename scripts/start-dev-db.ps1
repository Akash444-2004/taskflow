$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$dataDirectory = Join-Path $projectRoot ".dev\pgdata"
$logFile = Join-Path $projectRoot ".dev\postgres.log"
$pgCtl = "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe"

if (-not (Test-Path $pgCtl)) {
    throw "PostgreSQL 18 was not found at $pgCtl. Update the path in this script."
}

if (-not (Test-Path $dataDirectory)) {
    throw "The local TaskFlow database cluster is missing. See README.md for initialization steps."
}

& $pgCtl -D $dataDirectory status *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "TaskFlow PostgreSQL is already running on localhost:5433."
    exit 0
}

& $pgCtl -D $dataDirectory -l $logFile -o "-p 5433" start
if ($LASTEXITCODE -ne 0) {
    throw "PostgreSQL failed to start. Check $logFile."
}

Write-Host "TaskFlow PostgreSQL started on localhost:5433."
