$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $PSScriptRoot "oracle-step-21-inventory.sql"
$reports = Join-Path $root "reports\step-21"
New-Item -ItemType Directory -Path $reports -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$report = Join-Path $reports "oracle-inventory-$stamp.log"
$errorFile = "$report.stderr.log"
$inputFile = Join-Path $env:TEMP "comandos-oracle-step21-$stamp.sql"
try {
    if (-not (Get-Command sqlplus -ErrorAction SilentlyContinue)) { throw "sqlplus nao encontrado no PATH" }
    @("@" + '"' + $sql + '"') | Set-Content -Path $inputFile -Encoding ascii
    # SQL*Plus authenticates locally using Windows OS authentication.
    # The report contains metadata and counts, never database credentials.
    $process = Start-Process -FilePath "sqlplus.exe" -ArgumentList @("-L", "-S", '"/ as sysdba"', ('@' + $inputFile)) -PassThru -Wait -NoNewWindow -RedirectStandardOutput $report -RedirectStandardError $errorFile
    Write-Host "Relatorio: $report"
    Get-Content $report -Tail 35
    if ($process.ExitCode -ne 0) { throw "Falha no SQLPlus. Codigo: $($process.ExitCode). Verifique $errorFile" }
    Write-Host "Inventario Oracle concluido (somente leitura)." -ForegroundColor Green
} finally {
    Remove-Item $inputFile -Force -ErrorAction SilentlyContinue
}
