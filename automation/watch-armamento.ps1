param(
    [int]$RefreshSeconds = 5,
    [switch]$Once
)

$ErrorActionPreference = 'Stop'
$automationRoot = $PSScriptRoot
$taskRoot = Join-Path $automationRoot 'tasks'
$statusPath = Join-Path $automationRoot 'runtime\status.json'

function Get-TaskDescription([string]$path) {
    $content = Get-Content -Raw -Encoding UTF8 -Path $path
    if ($content -match '(?ms)^## Objetivo\s*\r?\n+(.+?)(?:\r?\n##|\z)') {
        return (($matches[1] -replace '\s+', ' ').Trim())
    }
    return 'Descrição não informada.'
}

function Get-TaskRows {
    $groups = @(
        @{ Path = $taskRoot; State = 'Pendente' },
        @{ Path = Join-Path $taskRoot 'working'; State = 'Fazendo' },
        @{ Path = Join-Path $taskRoot 'completed'; State = 'Concluída' },
        @{ Path = Join-Path $taskRoot 'failed'; State = 'Falhou' }
    )
    foreach ($group in $groups) {
        Get-ChildItem -Path $group.Path -Filter 'armamento-*.md' -File -ErrorAction SilentlyContinue |
            Sort-Object Name |
            ForEach-Object {
                [PSCustomObject]@{
                    Nome = $_.Name
                    Estado = $group.State
                    Descricao = Get-TaskDescription $_.FullName
                    Atualizado = $_.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss')
                }
            }
    }
}

function Get-StateColor([string]$state) {
    switch ($state) {
        'Fazendo' { return 'Yellow' }
        'Concluída' { return 'Green' }
        'Falhou' { return 'Red' }
        default { return 'Gray' }
    }
}

function Write-ProgressSummary($rows) {
    $total = @($rows).Count
    $done = @($rows | Where-Object Estado -eq 'Concluída').Count
    $doing = @($rows | Where-Object Estado -eq 'Fazendo').Count
    $failed = @($rows | Where-Object Estado -eq 'Falhou').Count
    $pending = @($rows | Where-Object Estado -eq 'Pendente').Count
    $percent = if ($total -gt 0) { [math]::Round(($done / $total) * 100) } else { 0 }
    $filled = [math]::Floor($percent / 5)
    $bar = ('#' * $filled) + ('.' * (20 - $filled))

    Write-Host ('Progresso: [{0}] {1}% concluido' -f $bar, $percent) -ForegroundColor Cyan
    Write-Host ('Total: {0} | Fazendo: {1} | Pendentes: {2} | Concluidas: {3} | Falhas: {4}' -f $total, $doing, $pending, $done, $failed)
    Write-Host ''
}

while ($true) {
    Clear-Host
    Write-Host 'COMANDOS - FILA DO ARMAMENTO' -ForegroundColor Cyan
    Write-Host ('Atualizado em: {0}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Host ''
    if (Test-Path $statusPath) {
        $status = Get-Content -Raw -Encoding UTF8 $statusPath | ConvertFrom-Json
        Write-Host ('Worker: {0} | Etapa: {1} | Mensagem: {2}' -f $status.state, $status.taskName, $status.message)
    } else {
        Write-Host 'Worker: sem status ainda' -ForegroundColor Yellow
    }
    Write-Host ''
    $rows = @(Get-TaskRows)
    Write-ProgressSummary $rows
    foreach ($row in $rows) {
        $color = Get-StateColor $row.Estado
        Write-Host ('[{0,-10}] {1}' -f $row.Estado, $row.Nome) -ForegroundColor $color
        Write-Host ('             {0}' -f $row.Descricao) -ForegroundColor DarkGray
        Write-Host ('             Atualizado: {0}' -f $row.Atualizado) -ForegroundColor DarkGray
    }
    if ($Once) { break }
    Write-Host ('Atualizando a cada {0}s. Ctrl+C para sair.' -f $RefreshSeconds) -ForegroundColor DarkGray
    Start-Sleep -Seconds $RefreshSeconds
}
