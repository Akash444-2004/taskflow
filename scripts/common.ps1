# Shared helpers for the TaskFlow run scripts.
# Credential policy:
#   * The PostgreSQL password is supplied at runtime (VS Code quick input prompt
#     or a masked terminal prompt) and only ever lives in process memory.
#   * The JWT secret is generated once and stored DPAPI-encrypted
#     (Current_user scope, bound to this Windows user and machine) under .dev/.
#     No plaintext secret is ever written to disk or committed.

$script:ProjectRoot = Split-Path $PSScriptRoot -Parent

function Convert-SecureToPlain {
    param([System.Security.SecureString]$SecureString)

    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Resolve-DatabasePassword {
    # 1. VS Code task / caller-supplied password wins (never persisted).
    if ($env:TF_DB_PASSWORD) {
        return $env:TF_DB_PASSWORD
    }

    # 2. Interactive, masked prompt.
    $secure = Read-Host -Prompt "PostgreSQL password (user 'postgres', database 'taskflow')" -AsSecureString
    $plain = Convert-SecureToPlain $secure
    if (-not $plain) {
        throw "No database password supplied. Aborting."
    }
    return $plain
}

function Resolve-JwtSecret {
    $secretFile = Join-Path $script:ProjectRoot ".dev\jwt.secret"

    if (Test-Path $secretFile) {
        $secure = (Get-Content $secretFile -Raw) | ConvertTo-SecureString
        return Convert-SecureToPlain $secure
    }

    # First run: generate a 512-bit secret for HS512 and store it encrypted.
    $bytes = New-Object byte[] 64
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $plain = [Convert]::ToBase64String($bytes)

    New-Item -ItemType Directory -Force -Path (Split-Path $secretFile) | Out-Null
    ($plain | ConvertTo-SecureString -AsPlainText -Force | ConvertFrom-SecureString) |
        Set-Content -Path $secretFile -NoNewline -Encoding ascii

    Write-Host "Generated a new JWT signing secret, stored DPAPI-encrypted at .dev\jwt.secret (git-ignored)."
    return $plain
}

function Set-TaskFlowEnvironment {
    param([string]$DatabasePassword)

    $env:DB_URL        = "jdbc:postgresql://localhost:5432/taskflow"
    $env:DB_USERNAME   = "postgres"
    $env:DB_PASSWORD   = $DatabasePassword
    $env:JWT_SECRET    = Resolve-JwtSecret
    $env:FRONTEND_URL  = "http://localhost:5173"
    $env:SERVER_PORT   = "8080"
}

function Assert-NodeToolchain {
    foreach ($cmd in @("node", "npm")) {
        if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
            throw "Required tool '$cmd' was not found on PATH. Install Node.js and reopen the terminal."
        }
    }
}

function Ensure-FrontendDependencies {
    Assert-NodeToolchain

    $frontendDir = Join-Path $script:ProjectRoot "frontend"
    if (-not (Test-Path (Join-Path $frontendDir "node_modules\vite\package.json"))) {
        Write-Host "Frontend dependencies missing. Running 'npm install'..."
        Push-Location $frontendDir
        try {
            npm install
            if ($LASTEXITCODE -ne 0) {
                throw "npm install failed. Check the output above."
            }
        }
        finally {
            Pop-Location
        }
    }
}

function Assert-PostgreSqlReachable {
    param([int]$Port = 5432)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connectTask = $client.ConnectAsync("127.0.0.1", $Port)
        if (-not $connectTask.Wait(2000) -or -not $client.Connected) {
            throw "PostgreSQL is not reachable on localhost:$Port. Start the PostgreSQL Windows service (services.msc -> postgresql) and try again."
        }
    }
    finally {
        $client.Dispose()
    }
    Write-Host "PostgreSQL is reachable on localhost:$Port."
}

function Assert-PortFree {
    param([int]$Port, [string]$Purpose)

    $existing = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($existing) {
        $owner = (Get-Process -Id ($existing | Select-Object -First 1).OwningProcess -ErrorAction SilentlyContinue).ProcessName
        throw "Port $Port is already in use by '$owner' ($Purpose). Run scripts\stop-all.ps1 first, then start again."
    }
}
