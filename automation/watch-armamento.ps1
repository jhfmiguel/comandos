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
    Get-TaskRows | Format-Table -Wrap -AutoSize
    if ($Once) { break }
    Write-Host ('Atualizando a cada {0}s. Ctrl+C para sair.' -f $RefreshSeconds) -ForegroundColor DarkGray
    Start-Sleep -Seconds $RefreshSeconds
}
