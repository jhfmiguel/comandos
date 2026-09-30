param(
    [string]$FoundationPath = $env:FARIA_MIGUEL_HOME
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ([string]::IsNullOrWhiteSpace($FoundationPath)) {
    $FoundationPath = Join-Path $PSScriptRoot "..\..\faria-miguel"
}

$FoundationPath = [System.IO.Path]::GetFullPath($FoundationPath)
$Pom = Join-Path $FoundationPath "pom.xml"
$Verify = Join-Path $FoundationPath "scripts\verify-all.ps1"

if (-not (Test-Path $Pom)) {
    throw "Faria Miguel foundation not found at '$FoundationPath'. Set FARIA_MIGUEL_HOME or keep the repository as a sibling folder."
}
if (-not (Test-Path $Verify)) {
    throw "Faria Miguel complete verification script not found at '$Verify'."
}

Write-Host "Validating canonical Faria Miguel foundation at $FoundationPath" -ForegroundColor Cyan
& $Verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Installing validated Faria Miguel shared artifacts in local Maven repository" -ForegroundColor Cyan
& mvn -B -ntp -f $Pom -DskipTests install
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Faria Miguel shared artifacts validated and installed." -ForegroundColor Green
