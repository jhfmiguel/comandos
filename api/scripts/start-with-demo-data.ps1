$ErrorActionPreference = "Stop"

$previousSeed = $env:COMANDOS_DEMO_SEED
$previousCoverage = $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE

try {
    $env:COMANDOS_DEMO_SEED = "true"
    $env:COMANDOS_DEMO_REQUIRE_FULL_COVERAGE = "true"

    Write-Host ""
    Write-Host "=== COMANDOS - AMBIENTE LOCAL COM DADOS FICTICIOS ===" -ForegroundColor Cyan
    Write-Host "Banco: configuracao Oracle padrao do projeto"
    Write-Host "Carga demo: ATIVADA"
    Write-Host "Cobertura: TODAS AS ENTIDADES JPA DEVEM TER DADOS"
    Write-Host "Os dados sao complementares e podem ser executados novamente sem apagar o banco."
    Write-Host ""

    mvn spring-boot:run
}
finally {
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
}
