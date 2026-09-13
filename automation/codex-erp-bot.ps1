[CmdletBinding()]
param(
    [string]$TaskDirectory = '',
    [string]$CodexCommand = 'codex',
    [int]$PollSeconds = 30,
    [int]$MaxTasks = 0,
    [string]$TaskName = '',
    [string]$Workstream = '',
    [switch]$ListTasks,
    [switch]$Once,
    [switch]$AutoCommit,
    [switch]$AutoPush,
    [switch]$NoNotification,
    [string]$Remote = 'origin',
    [string]$Branch = 'main'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $TaskDirectory) {
    $TaskDirectory = Join-Path $PSScriptRoot 'tasks'
}
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workingDirectory = Join-Path $TaskDirectory 'working'
$completedDirectory = Join-Path $TaskDirectory 'completed'
$failedDirectory = Join-Path $TaskDirectory 'failed'
$logDirectory = Join-Path $PSScriptRoot 'logs'
$runtimeDirectory = Join-Path $PSScriptRoot 'runtime'
$statusPath = Join-Path $runtimeDirectory 'status.json'
$controlPath = Join-Path $runtimeDirectory 'control.json'
$processedTasks = 0
$taskLimit = $MaxTasks
if ($taskLimit -eq 0 -and -not $Workstream -and -not $TaskName) {
    $taskLimit = 1
}

foreach ($directory in @($TaskDirectory, $workingDirectory, $completedDirectory, $failedDirectory, $logDirectory, $runtimeDirectory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

function Write-BotStatus([string]$state, [string]$message, [string]$taskName = '') {
    $status = [ordered]@{
        state = $state
        message = $message
        taskName = $taskName
        workstream = $Workstream
        processedTasks = $processedTasks
        updatedAt = (Get-Date).ToString('o')
        pid = $PID
    }
    $status | ConvertTo-Json | Set-Content -Path $statusPath -Encoding UTF8
}

function Get-Control {
    if (-not (Test-Path $controlPath)) { return $null }
    try { return Get-Content -Raw -Path $controlPath | ConvertFrom-Json } catch { return $null }
}

function Clear-Control { Remove-Item -Force -Path $controlPath -ErrorAction SilentlyContinue }

function Wait-IfPaused([string]$taskName) {
    while ($true) {
        $control = Get-Control
        if ($control -and $control.command -eq 'stop') { throw 'BOT_STOP_REQUESTED' }
        if (-not $control -or $control.command -ne 'pause') { return }
        Write-BotStatus 'paused' 'Pausado pelo painel. Aguardando continuar ou parar.' $taskName
        Start-Sleep -Seconds 2
    }
}

function Test-CodexAvailable {
    return $null -ne (Get-Command $CodexCommand -ErrorAction SilentlyContinue)
}

function Get-PendingTask {
    $pendingTasks = Get-ChildItem -Path $TaskDirectory -Filter '*.md' -File |
        Where-Object { $_.Name -notlike '_*' } |
        Sort-Object Name
    if ($Workstream) {
        $pendingTasks = $pendingTasks | Where-Object { $_.Name -like "$Workstream-*.md" }
    }
    if ($TaskName) {
        return $pendingTasks | Where-Object { $_.Name -eq $TaskName } | Select-Object -First 1
    }
    return $pendingTasks | Select-Object -First 1
}

function New-CodexPrompt([string]$taskPath) {
    $task = Get-Content -Raw -Encoding UTF8 -Path $taskPath
    foreach ($requiredSection in @('## Objetivo', '## Escopo', '## Critérios de aceite', '## Condição de parada')) {
        if ($task -notmatch [regex]::Escape($requiredSection)) {
            throw "Task '$taskPath' does not contain the required section '$requiredSection'."
        }
    }
    return @"
You are the implementation agent for the COMANDOS ERP repository.
Repository: $repositoryRoot
Task file: $taskPath

Read the task below and implement it completely in the repository.
- Inspect nearby code and existing conventions before editing.
- Keep the change focused on this task.
- Run the narrowest relevant tests or validation commands.
- Do not commit or push; the worker handles that when configured.
- Stop exactly at the scope and stop condition in the task. Do not start another feature.
- If this task belongs to a workstream, continue only through the next task in that same workstream.
- At the end, report changed files, validation results, and any blocker.

TASK
====
$task
"@
}

function Invoke-CodexTask([string]$taskPath, [string]$logPath) {
    $prompt = New-CodexPrompt $taskPath
    $command = Get-Command "$CodexCommand.cmd" -ErrorAction SilentlyContinue
    if (-not $command) { $command = Get-Command $CodexCommand -ErrorAction Stop }
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = $command.Source
    $processInfo.Arguments = 'exec --approve-for-me --skip-git-repo-check -'
    $processInfo.WorkingDirectory = $repositoryRoot
    $processInfo.UseShellExecute = $false
    $processInfo.CreateNoWindow = $true
    $processInfo.RedirectStandardInput = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $processInfo
    [void]$process.Start()
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $promptBytes = $utf8.GetBytes($prompt)
    $process.StandardInput.BaseStream.Write($promptBytes, 0, $promptBytes.Length)
    $process.StandardInput.BaseStream.Flush()
    $process.StandardInput.Close()
    Write-BotStatus 'working' 'Codex está trabalhando na etapa atual.' (Split-Path $taskPath -Leaf)
    try {
        while (-not $process.HasExited) {
            $control = Get-Control
            if ($control -and $control.command -eq 'stop') {
                $process.Kill()
                throw 'BOT_STOP_REQUESTED'
            }
            if ($control -and $control.command -eq 'pause') {
                Write-BotStatus 'pause-requested' 'Pausa solicitada; aguardando o Codex finalizar o comando atual.' (Split-Path $taskPath -Leaf)
            }
            Start-Sleep -Seconds 2
        }
        $codexOutput = @($process.StandardOutput.ReadToEnd(), $process.StandardError.ReadToEnd())
        $exitCode = $process.ExitCode
    } finally {
        $process.Dispose()
    }
    $codexOutput | Tee-Object -FilePath $logPath
    $outputText = (Get-Content -Raw -Encoding UTF8 -Path $logPath) + "`n" + ($codexOutput -join "`n")
    if ($outputText -match '(?i)rate limit|usage limit|hit your usage|out of codex|resets on|try again at|add credits|temporarily unavailable') {
        throw 'CODEX_UNAVAILABLE'
    }
    if ($exitCode -ne 0) {
        throw "Codex exited with code $exitCode. See $logPath"
    }
}

function Invoke-ProjectValidation {
    Push-Location $repositoryRoot
    try {
        & git status --short
        if (Test-Path (Join-Path $repositoryRoot 'wr-api\mvnw.cmd')) {
            Push-Location (Join-Path $repositoryRoot 'wr-api')
            try { & .\mvnw.cmd test } finally { Pop-Location }
            if ($LASTEXITCODE -ne 0) { throw 'API tests failed.' }
        }
        if (Test-Path (Join-Path $repositoryRoot 'wr-app\package.json')) {
            Push-Location (Join-Path $repositoryRoot 'wr-app')
            try { & npm.cmd run lint } finally { Pop-Location }
            if ($LASTEXITCODE -ne 0) { throw 'Frontend lint failed.' }
        }
    } finally {
        Pop-Location
    }
}

function Complete-Task([string]$taskPath) {
    $destination = Join-Path $completedDirectory (Split-Path $taskPath -Leaf)
    Move-Item -Force -Path $taskPath -Destination $destination
    if ($AutoCommit) {
        Push-Location $repositoryRoot
        try {
            & git add -A
            & git commit -m "BOT - Implementa tarefa $(Split-Path $destination -LeafBase)"
            if ($LASTEXITCODE -ne 0) { throw 'Git commit failed.' }
            if ($AutoPush) {
                & git push $Remote $Branch
                if ($LASTEXITCODE -ne 0) { throw 'Git push failed.' }
            }
        } finally {
            Pop-Location
        }
    }
}

function Fail-Task([string]$taskPath) {
    Move-Item -Force -Path $taskPath -Destination (Join-Path $failedDirectory (Split-Path $taskPath -Leaf))
}

function Show-CompletionNotice([string]$title, [string]$message) {
    $noticePath = Join-Path $logDirectory 'last-completion.txt'
    "$(Get-Date -Format s) - $title`r`n$message" | Set-Content -Path $noticePath
    if ($NoNotification) { return }
    try {
        Add-Type -AssemblyName System.Windows.Forms
        Add-Type -AssemblyName System.Drawing
        $notifyIcon = New-Object System.Windows.Forms.NotifyIcon
        $notifyIcon.Icon = [System.Drawing.SystemIcons]::Information
        $notifyIcon.Visible = $true
        $notifyIcon.BalloonTipTitle = $title
        $notifyIcon.BalloonTipText = $message
        $notifyIcon.ShowBalloonTip(8000)
        Start-Sleep -Seconds 8
        $notifyIcon.Dispose()
    } catch {
        Write-Warning "Could not show Windows notification: $($_.Exception.Message)"
    }
}

function Get-TaskSummary([System.IO.FileInfo]$taskFile, [string]$taskState) {
    $content = Get-Content -Raw -Encoding UTF8 -Path $taskFile.FullName
    $description = 'Descrição não informada.'
    if ($content -match '(?ms)^## Objetivo\s*\r?\n+(.+?)(?:\r?\n##|\z)') {
        $description = ($matches[1] -replace '\s+', ' ').Trim()
    }
    Write-Output ("{0} | {1} | {2} | {3:yyyy-MM-dd HH:mm:ss}" -f $taskFile.Name, $taskState, $description, $taskFile.LastWriteTime)
}

if ($ListTasks) {
    $taskGroups = @(
        @{ Path = $TaskDirectory; State = 'Pendente' },
        @{ Path = $workingDirectory; State = 'Fazendo' },
        @{ Path = $completedDirectory; State = 'Concluída' },
        @{ Path = $failedDirectory; State = 'Falhou' }
    )
    foreach ($taskGroup in $taskGroups) {
        Get-ChildItem -Path $taskGroup.Path -Filter '*.md' -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '_*' } |
            Where-Object { -not $Workstream -or $_.Name -like "$Workstream-*.md" } |
            Sort-Object Name |
            ForEach-Object { Get-TaskSummary $_ $taskGroup.State }
    }
    exit 0
}

while ($true) {
    Wait-IfPaused ''
    $task = Get-PendingTask
    if ($null -eq $task) {
        if ($Once -or $processedTasks -gt 0) {
            Write-BotStatus 'completed' 'Não há mais tarefas no escopo.'
            Show-CompletionNotice 'COMANDOS Codex Bot finalizado' "Tarefas processadas: $processedTasks. Não há mais tarefas no escopo."
            break
        }
        Write-BotStatus 'waiting' 'Aguardando tarefas ou disponibilidade do Codex.'
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    if (-not (Test-CodexAvailable)) {
        Write-BotStatus 'waiting' 'Codex indisponível; aguardando o próximo ciclo.' $task.Name
        if ($Once) { break }
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    $workingTask = Join-Path $workingDirectory $task.Name
    Move-Item -Force -Path $task.FullName -Destination $workingTask
    $logPath = Join-Path $logDirectory ("{0:yyyyMMdd-HHmmss}-{1}.log" -f (Get-Date), $task.BaseName)
    Clear-Control

    try {
        Write-BotStatus 'working' 'Iniciando etapa selecionada.' $task.Name
        Invoke-CodexTask $workingTask $logPath
        Invoke-ProjectValidation
        Complete-Task $workingTask
        $processedTasks++
        Show-CompletionNotice 'Tarefa do COMANDOS concluída' "Tarefa: $($task.Name)`nLog: $logPath"
    } catch {
        $errorText = $_ | Out-String
        $errorText | Tee-Object -FilePath $logPath -Append
        if ($errorText -match 'CODEX_UNAVAILABLE|rate limit|usage limit|hit your usage|out of codex|resets on|try again at|add credits|temporarily unavailable') {
            Move-Item -Force -Path $workingTask -Destination (Join-Path $TaskDirectory $task.Name)
            Write-BotStatus 'waiting' 'Rate limit do Codex; etapa preservada na fila.' $task.Name
            Start-Sleep -Seconds $PollSeconds
            continue
        }
        if ($errorText -match 'BOT_STOP_REQUESTED') {
            Move-Item -Force -Path $workingTask -Destination (Join-Path $TaskDirectory $task.Name)
            Write-BotStatus 'stopped' 'Parado pelo painel; etapa devolvida à fila.' $task.Name
            break
        }
        if (Test-Path $workingTask) {
            Fail-Task $workingTask
        }
        Show-CompletionNotice 'Tarefa do COMANDOS falhou' "Tarefa: $($task.Name)`nConsulte: $logPath"
        break
    }

    if ($taskLimit -gt 0 -and $processedTasks -ge $taskLimit) { break }
}
