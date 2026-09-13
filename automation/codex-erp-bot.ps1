[CmdletBinding()]
param(
    [string]$TaskDirectory = (Join-Path $PSScriptRoot 'tasks'),
    [string]$CodexCommand = 'codex',
    [int]$PollSeconds = 30,
    [int]$MaxTasks = 0,
    [switch]$Once,
    [switch]$AutoCommit,
    [switch]$AutoPush,
    [string]$Remote = 'origin',
    [string]$Branch = 'main'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workingDirectory = Join-Path $TaskDirectory 'working'
$completedDirectory = Join-Path $TaskDirectory 'completed'
$failedDirectory = Join-Path $TaskDirectory 'failed'
$logDirectory = Join-Path $PSScriptRoot 'logs'
$processedTasks = 0

foreach ($directory in @($TaskDirectory, $workingDirectory, $completedDirectory, $failedDirectory, $logDirectory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

function Test-CodexAvailable {
    return $null -ne (Get-Command $CodexCommand -ErrorAction SilentlyContinue)
}

function Get-PendingTask {
    return Get-ChildItem -Path $TaskDirectory -Filter '*.md' -File |
        Where-Object { $_.Name -notlike '_*' } |
        Sort-Object Name |
        Select-Object -First 1
}

function New-CodexPrompt([string]$taskPath) {
    $task = Get-Content -Raw -Path $taskPath
    return @"
You are the implementation agent for the COMANDOS ERP repository.
Repository: $repositoryRoot
Task file: $taskPath

Read the task below and implement it completely in the repository.
- Inspect nearby code and existing conventions before editing.
- Keep the change focused on this task.
- Run the narrowest relevant tests or validation commands.
- Do not commit or push; the worker handles that when configured.
- At the end, report changed files, validation results, and any blocker.

TASK
====
$task
"@
}

function Invoke-CodexTask([string]$taskPath, [string]$logPath) {
    $prompt = New-CodexPrompt $taskPath
    $codexOutput = & $CodexCommand exec --full-auto --skip-git-repo-check $prompt 2>&1
    $exitCode = $LASTEXITCODE
    $codexOutput | Tee-Object -FilePath $logPath
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

while ($true) {
    $task = Get-PendingTask
    if ($null -eq $task) {
        if ($Once) { break }
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
    } catch {
        $_ | Out-String | Tee-Object -FilePath $logPath -Append
        if (Test-Path $workingTask) {
            Fail-Task $workingTask
        }
    }

    $processedTasks++
    if ($MaxTasks -gt 0 -and $processedTasks -ge $MaxTasks) { break }
}
