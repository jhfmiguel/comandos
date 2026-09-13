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
    return ('Descri' + [char]0x00E7 + [char]0x00E3 + 'o nao informada.')
}

function Get-TaskProgress([string]$path, [string]$state) {
    $content = Get-Content -Raw -Encoding UTF8 -Path $path
    $criteria = [regex]::Matches($content, '(?m)^- \[([ xX])\]')
    if ($criteria.Count -gt 0) {
        $checked = @($criteria | Where-Object { $_.Groups[1].Value -match '[xX]' }).Count
        return [math]::Round(($checked / $criteria.Count) * 100)
    }
    switch ($state) {
        'Fazendo' { return 50 }
        ('Conclu' + [char]0x00ED + 'da') { return 100 }
        default { return 0 }
    }
}

function Get-TaskRows {
    $groups = @(
        @{ Path = $taskRoot; State = 'Pendente' },
        @{ Path = Join-Path $taskRoot 'working'; State = 'Fazendo' },
        @{ Path = Join-Path $taskRoot 'completed'; State = 'Conclu' + [char]0x00ED + 'da' },
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
                    Progresso = Get-TaskProgress $_.FullName $group.State
                    Atualizado = $_.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss')
                }
            }
    }
}

function Get-StateColor([string]$state) {
    switch ($state) {
        'Fazendo' { return 'Yellow' }
        ('Conclu' + [char]0x00ED + 'da') { return 'Green' }
        'Falhou' { return 'Red' }
        default { return 'Gray' }
    }
}

function Write-ProgressSummary($rows) {
    $total = @($rows).Count
    $done = @($rows | Where-Object Estado -eq ('Conclu' + [char]0x00ED + 'da')).Count
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

function Get-ShortText([string]$value, [int]$maxLength) {
    if ($value.Length -le $maxLength) { return $value }
    return $value.Substring(0, $maxLength - 3) + '...'
}

function Write-HorizontalQueue($rows) {
    $terminalWidth = 120
    try { $terminalWidth = [int]$Host.UI.RawUI.WindowSize.Width } catch { }
    $stateWidth = 9
    $nameWidth = 30
    $progressWidth = 8
    $updatedWidth = 16
    $descriptionWidth = [math]::Max(20, $terminalWidth - $stateWidth - $nameWidth - $progressWidth - $updatedWidth - 8)
    $descriptionLabel = 'Descri' + [char]0x00E7 + [char]0x00E3 + 'o'
    $header = 'Estado'.PadRight($stateWidth) + ' ' + 'Nome'.PadRight($nameWidth) + ' ' + $descriptionLabel.PadRight($descriptionWidth) + ' ' + 'Progresso'.PadRight($progressWidth) + ' ' + 'Atualizado'.PadRight($updatedWidth)
    Write-Host $header -ForegroundColor Cyan
    Write-Host ('-' * [math]::Min($terminalWidth - 1, 120)) -ForegroundColor DarkGray
    foreach ($row in $rows) {
        $state = ('{0,-9}' -f (Get-ShortText $row.Estado $stateWidth))
        $name = ('{0,-30}' -f (Get-ShortText $row.Nome $nameWidth))
        $description = (Get-ShortText $row.Descricao $descriptionWidth).PadRight($descriptionWidth)
        $progress = ('{0,6}%' -f $row.Progresso)
        $updated = ('{0,-16}' -f (Get-ShortText $row.Atualizado $updatedWidth))
        Write-Host $state -NoNewline -ForegroundColor (Get-StateColor $row.Estado)
        Write-Host (' ' + $name + ' ' + $description + ' ' + $progress + ' ' + $updated)
    }
}

while ($true) {
    Clear-Host
    Write-Host 'COMANDOS - FILA DO ARMAMENTO' -ForegroundColor Cyan
    Write-Host ('Atualizado em: {0}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Host ''
    if (Test-Path $statusPath) {
        $status = Get-Content -Raw -Encoding UTF8 $statusPath | ConvertFrom-Json
        $statusMessage = $status.message
        if ($statusMessage -match 'Codex est.*trabalhando') {
            $statusMessage = 'Codex est' + [char]0x00E1 + ' trabalhando na etapa atual.'
        }
        Write-Host ('Worker: {0} | Etapa: {1} | Mensagem: {2}' -f $status.state, $status.taskName, $statusMessage)
    } else {
        Write-Host 'Worker: sem status ainda' -ForegroundColor Yellow
    }
    Write-Host ''
    $rows = @(Get-TaskRows)
    Write-ProgressSummary $rows
    Write-HorizontalQueue $rows
    if ($Once) { break }
    Write-Host ('Atualizando a cada {0}s. Ctrl+C para sair.' -f $RefreshSeconds) -ForegroundColor DarkGray
    Start-Sleep -Seconds $RefreshSeconds
}
