param(
    [switch]$SkipTests,
    [int]$MinimumRows = 3
)

$ErrorActionPreference = "Stop"

$apiPath = Join-Path $PSScriptRoot "..\api"
$previousSeed = $env:COMANDOS_DEMO_SEED
$previousCoverage = $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE
$previousMinimumRows = $env:COMANDOS_DEMO_MINIMUM_ROWS

try {
    $env:COMANDOS_DEMO_SEED = "true"
    $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE = "true"
    $env:COMANDOS_DEMO_MINIMUM_ROWS = [string][Math]::Max(1, $MinimumRows)

    Push-Location $apiPath
    Write-Host "COMANDOS - Oracle local com massa ficticia" -ForegroundColor Cyan
    Write-Host "Carga demo: ativada"
    Write-Host "Cobertura: todas as entidades JPA devem possuir dados"
    Write-Host "Minimo solicitado por entidade: $($env:COMANDOS_DEMO_MINIMUM_ROWS)"
    Write-Host "A carga e idempotente e nao apaga o banco." -ForegroundColor DarkGray

    if ($SkipTests) {
        mvn -DskipTests spring-boot:run
    }
    else {
        mvn spring-boot:run
    }
}
finally {
    Pop-Location

    if ($null -eq $previousSeed) {
        Remove-Item Env:COMANDOS_DEMO_SEED -ErrorAction SilentlyContinue
    }
    else {
        $env:COMANDOS_DEMO_SEED = $previousSeed
    }

    if ($null -eq $previousCoverage) {
        Remove-Item Env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE -ErrorAction SilentlyContinue
    }
    else {
        $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE = $previousCoverage
    }

    if ($null -eq $previousMinimumRows) {
        Remove-Item Env:COMANDOS_DEMO_MINIMUM_ROWS -ErrorAction SilentlyContinue
    }
    else {
        $env:COMANDOS_DEMO_MINIMUM_ROWS = $previousMinimumRows
    }
}
