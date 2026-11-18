# Runs the TaskFlow SPA (Vite dev server) on http://localhost:5173.

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

Ensure-FrontendDependencies
Assert-PortFree -Port 5173 -Purpose "the web app"

$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $projectRoot "frontend")
npm run dev 2>&1 | Tee-Object -FilePath (Join-Path $projectRoot ".dev\frontend.out.log") -Append
