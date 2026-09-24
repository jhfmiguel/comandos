param(
    [switch]$SkipTests
)

$apiPath = Join-Path $PSScriptRoot "..\api"
$previousSeed = $env:COMANDOS_DEMO_SEED
$previousCoverage = $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE

try {
    $env:COMANDOS_DEMO_SEED = "true"
    $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE = "true"
    Push-Location $apiPath

    Write-Host "COMANDOS - Oracle local com massa ficticia" -ForegroundColor Cyan
    Write-Host "A carga e idempotente: registros de referencia existentes nao sao duplicados." -ForegroundColor DarkGray

    if ($SkipTests) {
        mvn -DskipTests spring-boot:run
    } else {
        mvn spring-boot:run
    }
}
finally {
    Pop-Location
    if ($null -eq $previousSeed) {
        Remove-Item Env:COMANDOS_DEMO_SEED -ErrorAction SilentlyContinue
    } else {
        $env:COMANDOS_DEMO_SEED = $previousSeed
    }

    if ($null -eq $previousCoverage) {
        Remove-Item Env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE -ErrorAction SilentlyContinue
    } else {
        $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE = $previousCoverage
    }
}
