$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $PSScriptRoot "oracle-step-21-automated-audit.sql"
$reports = Join-Path $root "reports\step-21"
New-Item -ItemType Directory -Force -Path $reports | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$report = Join-Path $reports "oracle-full-audit-$stamp.log"
$stderr = "$report.stderr.log"
$inputFile = Join-Path $env:TEMP "comandos-oracle-full-audit-$stamp.sql"
try {
    if (-not (Get-Command sqlplus.exe -ErrorAction SilentlyContinue)) {
        throw "sqlplus.exe nao encontrado no PATH"
    }
    ('@"' + $sql + '"') | Set-Content -Path $inputFile -Encoding ascii
    $p = Start-Process -FilePath "sqlplus.exe" -ArgumentList @("-L", "-S", '"/ as sysdba"', ('@' + $inputFile)) -PassThru -Wait -NoNewWindow -RedirectStandardOutput $report -RedirectStandardError $stderr
    Write-Host "Relatorio completo: $report"
    if ($p.ExitCode -ne 0) {
        Get-Content $report -Tail 40
        throw "Auditoria Oracle falhou. Codigo: $($p.ExitCode). Erros: $stderr"
    }
    $data = @(Get-Content $report)
    $summary = @($data | Select-String -Pattern 'ERP_TOTAL_ROWS=|FM_TOTAL_ROWS=|POPULATED_TABLES=|COUNT_ERRORS=|COUNT_ERROR ')
    foreach ($match in $summary) { Write-Host $match.Line }
    $required = @("ERP_TOTAL_ROWS=", "FM_TOTAL_ROWS=", "POPULATED_TABLES=", "COUNT_ERRORS=")
    $missing = @($required | Where-Object { $prefix = $_; -not @($data | Where-Object { $_.Trim().StartsWith($prefix) }).Count })
    if ($missing.Count -gt 0) {
        Write-Warning ("Marcadores ausentes: " + ($missing -join ", "))
        Write-Host "--- INICIO DO RELATORIO ---"
        $data | Select-Object -First 35
        Write-Host "--- FINAL DO RELATORIO ---"
        $data | Select-Object -Last 35
        throw "Auditoria incompleta; nao considerar aprovada. Relatorio: $report"
    }
    if ($data -match '^COUNT_ERRORS=[1-9]') {
        throw "Existem tabelas que nao puderam ser contadas. Consulte $report"
    }
    Write-Host "Auditoria estrutural e contagens concluidas. Paridade de dominio ainda nao comprovada." -ForegroundColor Green
} finally {
    Remove-Item $inputFile -Force -ErrorAction SilentlyContinue
}
