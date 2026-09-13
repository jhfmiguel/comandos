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
$processedTasks = 0
$taskLimit = $MaxTasks
if ($taskLimit -eq 0 -and -not $Workstream -and -not $TaskName) {
    $taskLimit = 1
}

foreach ($directory in @($TaskDirectory, $workingDirectory, $completedDirectory, $failedDirectory, $logDirectory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
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
    $task = Get-Content -Raw -Path $taskPath
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
    $codexOutput = & $CodexCommand exec --approve-for-me --skip-git-repo-check $prompt 2>&1
    $exitCode = $LASTEXITCODE
    $codexOutput | Tee-Object -FilePath $logPath
    $outputText = $codexOutput -join "`n"
    if ($outputText -match '(?i)rate limit|out of codex|resets on|add credits|temporarily unavailable') {
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

if ($ListTasks) {
    Get-ChildItem -Path $TaskDirectory -Filter '*.md' -File |
        Where-Object { $_.Name -notlike '_*' } |
        Where-Object { -not $Workstream -or $_.Name -like "$Workstream-*.md" } |
        Sort-Object Name |
        Select-Object -ExpandProperty Name
    exit 0
}

while ($true) {
    $task = Get-PendingTask
    if ($null -eq $task) {
        if ($Once -or $processedTasks -gt 0) {
            Show-CompletionNotice 'COMANDOS Codex Bot finalizado' "Tarefas processadas: $processedTasks. Não há mais tarefas no escopo."
            break
        }
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    if (-not (Test-CodexAvailable)) {
        if ($Once) { break }
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    $workingTask = Join-Path $workingDirectory $task.Name
    Move-Item -Force -Path $task.FullName -Destination $workingTask
    $logPath = Join-Path $logDirectory ("{0:yyyyMMdd-HHmmss}-{1}.log" -f (Get-Date), $task.BaseName)

    try {
        Invoke-CodexTask $workingTask $logPath
        Invoke-ProjectValidation
        Complete-Task $workingTask
        $processedTasks++
        Show-CompletionNotice 'Tarefa do COMANDOS concluída' "Tarefa: $($task.Name)`nLog: $logPath"
    } catch {
        $errorText = $_ | Out-String
        $errorText | Tee-Object -FilePath $logPath -Append
        if ($errorText -match 'CODEX_UNAVAILABLE|rate limit|out of codex|resets on|add credits|temporarily unavailable') {
            Move-Item -Force -Path $workingTask -Destination (Join-Path $TaskDirectory $task.Name)
            Start-Sleep -Seconds $PollSeconds
            continue
        }
        if (Test-Path $workingTask) {
            Fail-Task $workingTask
        }
        Show-CompletionNotice 'Tarefa do COMANDOS falhou' "Tarefa: $($task.Name)`nConsulte: $logPath"
        break
    }

    if ($taskLimit -gt 0 -and $processedTasks -ge $taskLimit) { break }
}
