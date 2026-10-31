param(
    [string]$FoundationPath = $env:FARIA_MIGUEL_HOME
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$root = [System.IO.Path]::GetFullPath($root)
Set-Location $root

function Invoke-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

function Resolve-BashCommand {
    $runningOnWindows = $env:OS -eq 'Windows_NT'
    $isWindowsVariable = Get-Variable -Name IsWindows -ErrorAction SilentlyContinue
    if ($isWindowsVariable) {
        $runningOnWindows = $runningOnWindows -or [bool]$isWindowsVariable.Value
    }

    if ($runningOnWindows) {
        $gitBashCandidates = @()
        if (-not [string]::IsNullOrWhiteSpace($env:ProgramFiles)) {
            $gitBashCandidates += (Join-Path $env:ProgramFiles 'Git\bin\bash.exe')
            $gitBashCandidates += (Join-Path $env:ProgramFiles 'Git\usr\bin\bash.exe')
        }
        if (-not [string]::IsNullOrWhiteSpace(${env:ProgramFiles(x86)})) {
            $gitBashCandidates += (Join-Path ${env:ProgramFiles(x86)} 'Git\bin\bash.exe')
            $gitBashCandidates += (Join-Path ${env:ProgramFiles(x86)} 'Git\usr\bin\bash.exe')
        }

        foreach ($candidate in $gitBashCandidates) {
            if (Test-Path $candidate) {
                Write-Host "Using Git Bash for COMANDOS checks: $candidate" -ForegroundColor DarkGray
                return $candidate
            }
        }
    }

    $bash = Get-Command bash -ErrorAction SilentlyContinue
    if (-not $bash) {
        throw 'bash is required for scripts/validate-product-boundary.sh (Git Bash or WSL).'
    }

    Write-Host "Using bash from PATH for COMANDOS checks: $($bash.Source)" -ForegroundColor DarkGray
    return $bash.Source
}

if ([string]::IsNullOrWhiteSpace($FoundationPath)) {
    $FoundationPath = Join-Path $root '..\faria-miguel'
}
$FoundationPath = [System.IO.Path]::GetFullPath($FoundationPath)

Invoke-Step 'Validate and install Faria Miguel foundation' {
    Push-Location $root
    try {
        & (Join-Path $root 'scripts\bootstrap-faria-miguel.ps1') -FoundationPath $FoundationPath
    }
    finally {
        Pop-Location
    }
}

# The foundation verifier changes its own current directory. Re-anchor every
# COMANDOS validation step to the consumer repository so relative paths remain stable.
Set-Location $root
$bashCommand = Resolve-BashCommand
$boundaryScript = Join-Path $root 'scripts\validate-product-boundary.sh'
Invoke-Step 'Validate COMANDOS product-consumer boundary' {
    Push-Location $root
    try {
        & $bashCommand $boundaryScript
    }
    finally {
        Pop-Location
    }
}

Invoke-Step 'Verify COMANDOS backend against Faria Miguel artifacts' {
    Push-Location $root
    try {
        mvn -B -ntp -f (Join-Path $root 'api\pom.xml') verify
    }
    finally {
        Pop-Location
    }
}

Push-Location (Join-Path $root 'app')
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

Set-Location $root
Write-Host "`nCOMANDOS IS A VALIDATED FARIA MIGUEL CONSUMER" -ForegroundColor Green
