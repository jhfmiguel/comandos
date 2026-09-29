param(
    [string]$FoundationPath = $env:FARIA_MIGUEL_HOME
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($FoundationPath)) {
    $FoundationPath = Join-Path $PSScriptRoot "..\..\faria-miguel"
}

$FoundationPath = [System.IO.Path]::GetFullPath($FoundationPath)
$Pom = Join-Path $FoundationPath "pom.xml"

if (-not (Test-Path $Pom)) {
    throw "Faria Miguel foundation not found at '$FoundationPath'. Set FARIA_MIGUEL_HOME or keep the repository as a sibling folder."
}

Write-Host "Installing Faria Miguel shared artifacts from $FoundationPath"
& mvn -f $Pom -DskipTests install
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Faria Miguel shared artifacts installed in the local Maven repository."
