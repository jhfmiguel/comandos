$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$reportDir = Join-Path $root "reports\step-21"
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$report = Join-Path $reportDir "bridge-consumers-$stamp.txt"
$summaryCsv = Join-Path $reportDir "bridge-consumers-$stamp.csv"
$terms = @(
    "MasterDataMigrationService",
    "MasterDataParityService",
    "MasterDataReferenceService",
    "ProductMasterDataReferenceSynchronizer",
    "ProductMasterDataReferenceParityService",
    "MasterDataCutoverStatusService",
    "legacyId",
    "LegacyId",
    "shadowWrite",
    "shadow-write",
    "delete-shadow",
    "crosswalk",
    "backfill"
)
Push-Location $root
try {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw "Git nao encontrado" }
    $lines = New-Object System.Collections.Generic.List[string]
    $rows = New-Object System.Collections.Generic.List[object]
    $lines.Add("ETAPA 21 - MAPA DE CONSUMIDORES DE BRIDGES")
    $lines.Add("Gerado em: $(Get-Date -Format s)")
    $lines.Add("Repositorio: $root")
    $lines.Add("Commit: $(git rev-parse --short HEAD)")
    $lines.Add("OBS: inventario estatico; nao comprova equivalencia funcional ou cutover.")
    $lines.Add("")
    foreach ($term in $terms) {
        $lines.Add("===== $term =====")
        # git grep scans tracked working-tree files. Literal strings avoid regex surprises.
        $matches = @(git grep -n -F -I -- "$term" -- ':(glob)**/*.java' ':(glob)**/*.kt' ':(glob)**/*.ts' ':(glob)**/*.tsx' ':(glob)**/*.sql' ':(glob)**/*.json' ':(glob)**/*.md' ':(glob)**/*.yml' ':(glob)**/*.yaml' ':(glob)**/*.properties' 2>$null)
        $gitExit = $LASTEXITCODE
        if ($gitExit -gt 1) { throw "git grep falhou para $term (codigo $gitExit)" }
        if ($gitExit -eq 1) {
            $lines.Add("(nenhuma referencia)")
            $rows.Add([pscustomobject]@{ Termo = $term; Arquivo = ""; Linha = ""; Texto = ""; Classificacao = "SEM_REFERENCIAS" })
            continue
        }
        foreach ($match in $matches) {
            $lines.Add($match)
            if ($match -match '^(.+?):([0-9]+):(.*)$') {
                $path = $Matches[1]
                $lineNo = $Matches[2]
                $body = $Matches[3].Trim()
                $kind = if ($path -match '(^|[/\\])src[/\\]test[/\\]') { "TESTE" } elseif ($path -match '\.java$') { "JAVA" } elseif ($path -match '(docs|architecture|README)') { "DOCUMENTACAO" } else { "OUTRO" }
                $rows.Add([pscustomobject]@{ Termo = $term; Arquivo = $path; Linha = $lineNo; Texto = $body; Classificacao = $kind })
            }
        }
        $lines.Add("")
    }
    $lines | Set-Content -Path $report -Encoding UTF8
    $rows | Export-Csv -Path $summaryCsv -NoTypeInformation -Encoding UTF8
    Write-Host "Relatorio: $report"
    Write-Host "CSV: $summaryCsv"
    $rows | Group-Object Termo | ForEach-Object {
        $hitCount = @($_.Group | Where-Object { $_.Classificacao -ne "SEM_REFERENCIAS" }).Count
        Write-Host ("{0}: {1} referencias" -f $_.Name, $hitCount)
    }
    Write-Host "Mapa gerado. Nenhum arquivo de codigo foi modificado." -ForegroundColor Green
} finally {
    Pop-Location
}
