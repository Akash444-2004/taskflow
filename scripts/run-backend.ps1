# Runs the TaskFlow API (Spring Boot) against your local PostgreSQL database.
# Password flow: taken from TF_DB_PASSWORD (set by the VS Code task input) or
# asked for with a masked prompt. Never written to disk.

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

$dbPassword = Resolve-DatabasePassword
Set-TaskFlowEnvironment -DatabasePassword $dbPassword

Assert-PostgreSqlReachable -Port 5432
Assert-PortFree -Port 8080 -Purpose "the API"

Write-Host "Starting TaskFlow API on http://localhost:8080 (database: localhost:5432/taskflow)..."
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $projectRoot "backend")
& .\mvnw.cmd spring-boot:run 2>&1 | Tee-Object -FilePath (Join-Path $projectRoot ".dev\backend.out.log") -Append
