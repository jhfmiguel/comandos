param(
    [string]$FoundationPath = $env:FARIA_MIGUEL_HOME
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Invoke-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

if ([string]::IsNullOrWhiteSpace($FoundationPath)) {
    $FoundationPath = Join-Path $root '..\faria-miguel'
}
$FoundationPath = [System.IO.Path]::GetFullPath($FoundationPath)

Invoke-Step 'Validate and install Faria Miguel foundation' {
    & (Join-Path $root 'scripts\bootstrap-faria-miguel.ps1') -FoundationPath $FoundationPath
}

$bash = Get-Command bash -ErrorAction SilentlyContinue
if (-not $bash) {
    throw 'bash is required for scripts/validate-product-boundary.sh (Git Bash or WSL).'
}
Invoke-Step 'Validate COMANDOS product-consumer boundary' {
    bash scripts/validate-product-boundary.sh
}

Invoke-Step 'Verify COMANDOS backend against Faria Miguel artifacts' {
    mvn -B -ntp -f api/pom.xml verify
}

Push-Location app
try {
    Invoke-Step 'Install COMANDOS frontend dependencies' { yarn install --non-interactive }
    Invoke-Step 'COMANDOS frontend lint' { yarn lint }
    Invoke-Step 'COMANDOS frontend platform boundary audit' { yarn platform:audit }
    Invoke-Step 'COMANDOS frontend i18n audit' { yarn i18n:audit }
    Invoke-Step 'COMANDOS Armamento layout audit' { yarn validate:armamento }
    Invoke-Step 'COMANDOS commercial dependency audit' { yarn licenses:audit }
    Invoke-Step 'COMANDOS production build' { yarn build }
}
finally {
    Pop-Location
}

Write-Host "`nCOMANDOS IS A VALIDATED FARIA MIGUEL CONSUMER" -ForegroundColor Green
