# Worker script for a dedicated dev window.
# Runs either the backend or the frontend, streaming its output to the console
# AND appending it to .dev/<target>.out.log so failures stay diagnosable.

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("backend", "frontend")]
    [string]$Target
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$logDir = Join-Path $projectRoot ".dev"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir "$Target.out.log"

Write-Host "=== TaskFlow $Target | started $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | log: $logFile ==="

if ($Target -eq "backend") {
    Set-Location (Join-Path $projectRoot "backend")
    & .\mvnw.cmd spring-boot:run 2>&1 | Tee-Object -FilePath $logFile -Append
}
else {
    Set-Location (Join-Path $projectRoot "frontend")
    npm run dev 2>&1 | Tee-Object -FilePath $logFile -Append
}

Write-Host ""
Write-Host "=== TaskFlow $Target stopped $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ==="
