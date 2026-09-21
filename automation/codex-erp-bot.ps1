[CmdletBinding()]
param(
    [string]$TaskDirectory = '',
    [string]$CodexCommand = 'codex',
    [int]$PollSeconds = 30,
    [int]$CodexRetrySeconds = 300,
    [int]$MaxTasks = 0,
    [string]$TaskName = '',
    [switch]$RateLimitResume,
    [string]$Workstream = '',
    [ValidateSet('continuous', 'until')]
    [string]$ExecutionMode = 'continuous',
    [string]$StopAfterTask = '',
    [switch]$ListTasks,
    [switch]$Once,
    [switch]$AutoCommit,
    [switch]$AutoPush,
    [switch]$NoNotification,
    [switch]$StartPaused,
    [string]$Remote = 'origin',
    [string]$Branch = 'main'
)
# BOT_EXECUTION_MODE_VALIDATION
if (
    $ExecutionMode -eq 'until' -and
    [string]::IsNullOrWhiteSpace($StopAfterTask)
) {
    throw 'ExecutionMode until requires StopAfterTask.'
}


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
$rateLimitStatePath = Join-Path $runtimeDirectory 'codex-rate-limit.json'
$processedTasks = 0
$script:lastState = 'starting'
$script:lastMessage = 'Iniciando worker.'
$script:lastTask = ''
$taskLimit = $MaxTasks


if ($ExecutionMode -eq 'until' -and [string]::IsNullOrWhiteSpace($StopAfterTask)) {
    throw 'ExecutionMode until requires StopAfterTask.'
}
foreach ($directory in @($TaskDirectory, $workingDirectory, $completedDirectory, $failedDirectory, $logDirectory, $runtimeDirectory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

function Write-BotStatus([string]$state, [string]$message, [string]$taskName = '', [string]$rateLimitResetAt = '') {
    $script:lastState = $state
    $script:lastMessage = $message
    $script:lastTask = $taskName
    $status = [ordered]@{
        state = $state
        message = $message
        taskName = $taskName
        workstream = $Workstream
        executionMode = $ExecutionMode
        stopAfterTask = $StopAfterTask
        processedTasks = $processedTasks
        updatedAt = (Get-Date).ToString('o')
        pid = $PID
        rateLimitResetAt = if ($rateLimitResetAt) { $rateLimitResetAt } else { $null }
    }
    $temporary = "$statusPath.$PID.tmp"
    [System.IO.File]::WriteAllText($temporary, ($status | ConvertTo-Json), (New-Object System.Text.UTF8Encoding($false)))
    if (Test-Path -LiteralPath $statusPath) { [System.IO.File]::Replace($temporary, $statusPath, "$statusPath.previous") }
    else { [System.IO.File]::Move($temporary, $statusPath) }
}

function Get-Control {
    if (-not (Test-Path $controlPath)) { return $null }
    try { return Get-Content -Raw -Path $controlPath | ConvertFrom-Json } catch { return $null }
}

function Clear-Control { Remove-Item -Force -Path $controlPath -ErrorAction SilentlyContinue }

function Wait-BotPoll {
    for ($tick = 0; $tick -lt [math]::Max(1, $PollSeconds); $tick += 2) {
        Wait-IfPaused $script:lastTask
        Write-BotStatus $script:lastState $script:lastMessage $script:lastTask
        Start-Sleep -Seconds 2
    }
}
















function Wait-IfPaused([string]$taskName) {
    $previousState = $script:lastState
    $previousMessage = $script:lastMessage

    while ($true) {
        $control = Get-Control

        if ($control -and $control.command -eq 'stop') {
            throw 'BOT_STOP_REQUESTED'
        }

        if (-not $control -or $control.command -ne 'pause') {
            if ($control -and $control.command -eq 'resume') {
                Clear-Control
            }

            if ($script:lastState -eq 'paused') {
                Write-BotStatus $previousState $previousMessage $taskName
            }

            return
        }

        Write-BotStatus `
            'paused' `
            'Pausado pelo painel. Aguardando continuar ou parar.' `
            $taskName

        Start-Sleep -Seconds 2
    }
}

function Convert-CodexResetTextToDateTime([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }

    $normalized = [regex]::Replace(
        $text.Trim(),
        '(?i)(\d{1,2})(st|nd|rd|th)\b',
        '$1'
    )

    $culture = [System.Globalization.CultureInfo]::GetCultureInfo('en-US')
    $styles = [System.Globalization.DateTimeStyles]::AllowWhiteSpaces
    $parsed = [datetime]::MinValue

    foreach ($format in @(
        'MMM d, yyyy h:mm tt',
        'MMM d, yyyy, h:mm tt',
        'MMMM d, yyyy h:mm tt',
        'MMMM d, yyyy, h:mm tt'
    )) {
        if ([datetime]::TryParseExact(
            $normalized,
            $format,
            $culture,
            $styles,
            [ref]$parsed
        )) {
            return $parsed
        }
    }

    if ([datetime]::TryParse(
        $normalized,
        $culture,
        $styles,
        [ref]$parsed
    )) {
        return $parsed
    }

    return $null
}

function Save-CodexRateLimit(
    [datetime]$resetAt,
    [string]$displayText
) {
    $state = [ordered]@{
        resetAt = $resetAt.ToString('o')
        displayText = $displayText
        updatedAt = (Get-Date).ToString('o')
    }

    $temporary = "$rateLimitStatePath.$PID.tmp"

    [System.IO.File]::WriteAllText(
        $temporary,
        ($state | ConvertTo-Json),
        (New-Object System.Text.UTF8Encoding($false))
    )

    Move-Item -Force -Path $temporary -Destination $rateLimitStatePath
}

function Clear-CodexRateLimit {
    Remove-Item `
        -Force `
        -LiteralPath $rateLimitStatePath `
        -ErrorAction SilentlyContinue
}

function Get-ActiveCodexRateLimit {
    if (-not (Test-Path -LiteralPath $rateLimitStatePath)) {
        return $null
    }

    try {
        $state = Get-Content `
            -Raw `
            -Encoding UTF8 `
            -LiteralPath $rateLimitStatePath |
            ConvertFrom-Json

        if (-not $state.resetAt) {
            Clear-CodexRateLimit
            return $null
        }

        $resetAt = [datetime]::Parse(
            [string]$state.resetAt,
            [System.Globalization.CultureInfo]::InvariantCulture,
            [System.Globalization.DateTimeStyles]::RoundtripKind
        )

        if ((Get-Date) -ge $resetAt) {
            Clear-CodexRateLimit
            return $null
        }

        $displayText = $resetAt.ToString(
            'dd/MM/yyyy HH:mm',
            [System.Globalization.CultureInfo]::GetCultureInfo('pt-BR')
        )

        return [pscustomobject]@{
            ResetAt = $resetAt
            DisplayText = $displayText
        }
    }
    catch {
        Clear-CodexRateLimit
        return $null
    }
}

function Get-CodexResetInfo([string]$logPath) {
    if ([string]::IsNullOrWhiteSpace($logPath)) {
        return $null
    }

    $text = ''

    foreach ($path in @($logPath, "$logPath.stderr")) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        try {
            $text += "`n" + [System.IO.File]::ReadAllText($path)
        }
        catch {
        }
    }

    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }

    $text = [regex]::Replace(
        $text,
        '\x1B\[[0-?]*[ -/]*[@-~]',
        ''
    )

    $patterns = @(
        '(?i)rate limit resets?\s+on\s+([^\r\n]+)',
        '(?i)rate limit resets?\s+at\s+([^\r\n]+)',
        '(?i)resets?\s+on\s+([^\r\n]+)',
        '(?i)resets?\s+at\s+([^\r\n]+)',
        '(?i)try again at\s+([^\r\n]+)'
    )

    foreach ($pattern in $patterns) {
        $matches = [regex]::Matches($text, $pattern)

        if ($matches.Count -eq 0) {
            continue
        }

        $raw = $matches[
            $matches.Count - 1
        ].Groups[1].Value.Trim()

        $raw = $raw.TrimEnd('.', ';')

        if ([string]::IsNullOrWhiteSpace($raw)) {
            continue
        }

        $parsed = Convert-CodexResetTextToDateTime $raw

        if ($null -ne $parsed) {
            return [pscustomobject]@{
                ResetAt = $parsed
                DisplayText = $parsed.ToString(
                    'dd/MM/yyyy HH:mm',
                    [System.Globalization.CultureInfo]::GetCultureInfo('pt-BR')
                )
            }
        }
    }

    return $null
}

function Register-CodexResumeTask(
    [datetime]$resumeAt,
    [string]$resumeTaskName = ''
) {
    $now = Get-Date

    if ($resumeAt -le $now) {
        $resumeAt = $now.AddSeconds(15)
    }

    $scheduledTaskName = 'COMANDOS Codex Auto Resume'

    $arguments = @(
        '-NoProfile',
        '-NonInteractive',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"' + $PSCommandPath + '"')
    )

    if ($TaskDirectory) {
        $arguments += @('-TaskDirectory', ('"' + $TaskDirectory + '"'))
    }

    if ($CodexCommand -and $CodexCommand -ne 'codex') {
        $arguments += @('-CodexCommand', ('"' + $CodexCommand + '"'))
    }

    if ($Workstream) {
        $arguments += @('-Workstream', ('"' + $Workstream + '"'))
    }

    
    $arguments += @('-ExecutionMode', ('"' + $ExecutionMode + '"'))

    if ($StopAfterTask) {
        $arguments += @('-StopAfterTask', ('"' + $StopAfterTask + '"'))
    }
    if ($resumeTaskName) {
        $arguments += @('-TaskName', ('"' + $resumeTaskName + '"'))
        $arguments += '-RateLimitResume'
    }
    elseif ($TaskName) {
        $arguments += @('-TaskName', ('"' + $TaskName + '"'))
    }

    if ($MaxTasks -gt 0) {
        $arguments += @('-MaxTasks', [string]$MaxTasks)
    }

    if ($Once) { $arguments += '-Once' }
    if ($AutoCommit) { $arguments += '-AutoCommit' }
    if ($AutoPush) { $arguments += '-AutoPush' }
    if ($NoNotification) { $arguments += '-NoNotification' }

    if ($Remote) {
        $arguments += @('-Remote', ('"' + $Remote + '"'))
    }

    if ($Branch) {
        $arguments += @('-Branch', ('"' + $Branch + '"'))
    }

    $argumentLine = $arguments -join ' '

    try {
        Import-Module ScheduledTasks -ErrorAction Stop

        $action = New-ScheduledTaskAction `
            -Execute 'powershell.exe' `
            -Argument $argumentLine `
            -WorkingDirectory $repositoryRoot

        $trigger = New-ScheduledTaskTrigger `
            -Once `
            -At $resumeAt

        $settings = New-ScheduledTaskSettingsSet `
            -StartWhenAvailable `
            -AllowStartIfOnBatteries `
            -DontStopIfGoingOnBatteries

        Register-ScheduledTask `
            -TaskName $scheduledTaskName `
            -Action $action `
            -Trigger $trigger `
            -Settings $settings `
            -Description 'Retoma automaticamente o BOT COMANDOS quando a cota do Codex for liberada.' `
            -Force |
            Out-Null

        return $resumeAt
    }
    catch {
        Write-Warning (
            'Nao foi possivel agendar o reinicio automatico: ' +
            $_.Exception.Message
        )

        return $null
    }
}

function Stop-BotForCodexUnavailable(
    [string]$taskName,
    [string]$logPath = ''
) {
    $resetInfo = $null

    if ($logPath) {
        $resetInfo = Get-CodexResetInfo $logPath
    }

    if ($null -eq $resetInfo) {
        $resetInfo = Get-ActiveCodexRateLimit
    }

    if ($null -eq $resetInfo) {
        $fallbackAt = (Get-Date).AddSeconds(
            [Math]::Max(60, $CodexRetrySeconds)
        )

        $resetInfo = [pscustomobject]@{
            ResetAt = $fallbackAt
            DisplayText = $fallbackAt.ToString(
                'dd/MM/yyyy HH:mm',
                [System.Globalization.CultureInfo]::GetCultureInfo('pt-BR')
            )
        }
    }

    Save-CodexRateLimit `
        $resetInfo.ResetAt `
        $resetInfo.DisplayText

    $scheduledAt = Register-CodexResumeTask `
        $resetInfo.ResetAt `
        $taskName

    $message = (
        'Codex liberado para trabalhar às {0}.' -f
        $resetInfo.DisplayText
    )

    if ($null -eq $scheduledAt) {
        $message += ' A retomada automatica nao pôde ser agendada.'
    }

    Write-BotStatus `
        'waiting' `
        $message `
        $taskName `
        $resetInfo.ResetAt.ToString('o')
}
function Test-CodexAvailable {
    return $null -ne (Get-Command $CodexCommand -ErrorAction SilentlyContinue)
}

function Get-PendingTask {
    $pendingTasks = Get-ChildItem -Path $TaskDirectory -Filter '*.md' -File |
        Where-Object { $_.Name -notlike '_*' } |
        Sort-Object `
            @{ Expression = {
                if ($_.Name -match '-(\d+)-') {
                    [int]$matches[1]
                } else {
                    -1
                }
            }; Descending = $true }, `
            @{ Expression = { $_.Name }; Descending = $true }
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
    foreach ($requiredSection in @('## Objetivo', '## Escopo', '## Crit', '## Condi')) {
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
- Never claim success if any acceptance criterion is not actually implemented.
- Your final response MUST contain exactly one result marker:
  COMANDOS_TASK_RESULT: PASS
  or
  COMANDOS_TASK_RESULT: FAIL
- Use PASS only when the requested implementation is present in repository files and all applicable acceptance criteria are satisfied.
- If no repository implementation file needed to change, use FAIL and explain why.

TASK
====
$task
"@
}

function Invoke-WorkerProcess([string]$executable, [string]$arguments, [string]$directory, [string]$logPath, [string]$prompt = '') {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    if ($executable -match '\.(cmd|bat)$') {
        $info.FileName = $env:ComSpec
        $info.Arguments = '/d /s /c ""' + $executable + '" ' + $arguments + '"'
    } else { $info.FileName = $executable; $info.Arguments = $arguments }
    $info.WorkingDirectory = $directory
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    $stdout = $null
    $stderr = $null
    $started = $false
    try {
        [void]$process.Start()
        $started = $true
        $stdout = [System.IO.File]::Open($logPath, 'Create', 'Write', 'Read')
        $stderr = [System.IO.File]::Open("$logPath.stderr", 'Create', 'Write', 'Read')
        # Drain both pipes immediately. Waiting for exit before reading can deadlock.
        $outputCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdout)
        $errorCopy = $process.StandardError.BaseStream.CopyToAsync($stderr)
        if ($prompt) {
            $bytes = (New-Object System.Text.UTF8Encoding($false)).GetBytes($prompt)
            $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
        }
        $process.StandardInput.Close()
        $state = $script:lastState
        $message = $script:lastMessage
        $taskName = $script:lastTask
        while (-not $process.HasExited) {
            $control = Get-Control
            if ($control -and $control.command -eq 'stop') { throw 'BOT_STOP_REQUESTED' }
            if ($control -and $control.command -eq 'pause') {
                Write-BotStatus 'pause-requested' 'Pausa solicitada para depois da etapa atual.' $taskName
            } else {
                Write-BotStatus $state $message $taskName
            }
            Start-Sleep -Seconds 2
        }
        [void]$outputCopy.GetAwaiter().GetResult()
        [void]$errorCopy.GetAwaiter().GetResult()
        return $process.ExitCode
    } finally {
        if ($started -and -not $process.HasExited) {
            # Kill the wrapper and its children, not only cmd.exe.
            & taskkill.exe /PID $process.Id /T /F | Out-Null
            if ($LASTEXITCODE -ne 0 -and -not $process.HasExited) { throw 'Could not stop worker child processes. Check Windows process permissions.' }
            $process.WaitForExit()
        }
        if ($started -and $stdout -and $stderr) {
            [void]$outputCopy.GetAwaiter().GetResult()
            [void]$errorCopy.GetAwaiter().GetResult()
        }
        if ($stdout) { $stdout.Dispose() }
        if ($stderr) { $stderr.Dispose() }
        $process.Dispose()
    }
}

function Invoke-CodexTask([string]$taskPath, [string]$logPath) {
    $prompt = New-CodexPrompt $taskPath
    $command = Get-Command $CodexCommand -ErrorAction Stop
    if ($command.Source -match '\.ps1$') { $command = Get-Command ($command.Source -replace '\.ps1$', '.cmd') -ErrorAction Stop }
    Write-BotStatus 'working' 'Codex executando a etapa atual.' (Split-Path $taskPath -Leaf)
    $exitCode = Invoke-WorkerProcess $command.Source '--ask-for-approval never exec --sandbox workspace-write --skip-git-repo-check -' $repositoryRoot $logPath $prompt
    $outputText = (Get-Content -Raw -Encoding UTF8 -LiteralPath $logPath) + "\n" + (Get-Content -Raw -Encoding UTF8 -LiteralPath "$logPath.stderr")
    if ($exitCode -ne 0 -and $outputText -match '(?i)rate limit|usage limit|hit your usage|out of codex|resets on|try again at|add credits|temporarily unavailable') { throw 'CODEX_UNAVAILABLE' }
    if ($exitCode -ne 0) { throw "Codex exited with code $exitCode. See $logPath and $logPath.stderr" }
}

function Invoke-ProjectValidation {
    Write-BotStatus 'validating' 'Validando testes da API e lint da interface.' $script:lastTask
    if (Test-Path (Join-Path $repositoryRoot 'wr-api\mvnw.cmd')) {
        $code = Invoke-WorkerProcess (Join-Path $repositoryRoot 'wr-api\mvnw.cmd') 'test' (Join-Path $repositoryRoot 'wr-api') "$logPath.api.log"
        if ($code -ne 0) { throw 'API tests failed.' }
    }
    if (Test-Path (Join-Path $repositoryRoot 'wr-app\package.json')) {
        $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
        $code = Invoke-WorkerProcess $npm 'run lint' (Join-Path $repositoryRoot 'wr-app') "$logPath.lint.log"
        if ($code -ne 0) { throw 'Frontend lint failed.' }
    }
}


function Get-NextCommitNumber {
    Push-Location $repositoryRoot
    try {
        $subjects = & git log --format=%s -n 300
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not read git history.'
        }

        foreach ($subject in $subjects) {
            if ($subject -match '^(\d{3})\s+-\s+') {
                return ([int]$matches[1]) + 1
            }
        }

        return 1
    } finally {
        Pop-Location
    }
}

function Get-TaskCommitDescription([string]$taskPath) {
    $taskName = Split-Path $taskPath -LeafBase
    $taskContent = Get-Content -Raw -Encoding UTF8 -Path $taskPath

    if ($taskContent -match '(?m)^#\s+(.+?)\s*$') {
        return $matches[1].Trim()
    }

    if ($taskContent -match '(?ms)^## Objetivo\s*\r?\n+(.+?)(?:\r?\n##|\z)') {
        $description = ($matches[1] -replace '\s+', ' ').Trim()
        if ($description.Length -gt 90) {
            $description = $description.Substring(0, 90).Trim()
        }
        return $description
    }

    $description = ($taskName -replace '^\d+[-_ ]*', '' -replace '[-_]+', ' ').Trim()

    if ([string]::IsNullOrWhiteSpace($description)) {
        return 'Implementa tarefa automatizada'
    }

    return $description
}






function Get-ImplementationSnapshot {
    $snapshot = New-Object 'System.Collections.Generic.Dictionary[string,string]' ([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($rootName in @('wr-api', 'wr-app')) {
        $rootPath = Join-Path $repositoryRoot $rootName

        if (-not (Test-Path -LiteralPath $rootPath)) {
            continue
        }

        Get-ChildItem -LiteralPath $rootPath -Recurse -File -Force -ErrorAction SilentlyContinue |
            Where-Object {
                $_.FullName -notmatch '[\\/](node_modules|\.next|target|dist|build|coverage)[\\/]'
            } |
            ForEach-Object {
                $relativePath = $_.FullName.Substring($repositoryRoot.Length).TrimStart('\', '/')
                $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash
                $snapshot[$relativePath] = $hash
            }
    }

    return $snapshot
}

function Get-ImplementationChanges(
    [System.Collections.Generic.Dictionary[string,string]]$before,
    [System.Collections.Generic.Dictionary[string,string]]$after
) {
    $paths = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($path in $before.Keys) {
        [void]$paths.Add($path)
    }

    foreach ($path in $after.Keys) {
        [void]$paths.Add($path)
    }

    $changes = @()

    foreach ($path in $paths) {
        $beforeHash = if ($before.ContainsKey($path)) { $before[$path] } else { $null }
        $afterHash = if ($after.ContainsKey($path)) { $after[$path] } else { $null }

        if ($beforeHash -ne $afterHash) {
            $changes += $path
        }
    }

    return @($changes | Sort-Object)
}

function Assert-CodexTaskResult([string]$logPath) {
    $combined = [System.IO.File]::ReadAllText($logPath)

    $markers = [regex]::Matches(
        $combined,
        '(?im)^\s*COMANDOS_TASK_RESULT:\s*(PASS|FAIL)\s*$'
    )

    if ($markers.Count -eq 0) {
        throw 'Codex nao informou COMANDOS_TASK_RESULT: PASS/FAIL.'
    }

    if ($markers[$markers.Count - 1].Groups[1].Value.ToUpperInvariant() -ne 'PASS') {
        throw 'Codex informou COMANDOS_TASK_RESULT: FAIL.'
    }
}

function Assert-ImplementationChanged(
    [System.Collections.Generic.Dictionary[string,string]]$before,
    [string]$logPath
) {
    $after = Get-ImplementationSnapshot
    $changes = Get-ImplementationChanges $before $after

    if ($changes.Count -eq 0) {
        throw 'Nenhum arquivo de implementacao em wr-api ou wr-app foi alterado. A tarefa nao pode ser concluida.'
    }

    $report = (
        "`r`nCOMANDOS_IMPLEMENTATION_CHANGES:`r`n- " +
        ($changes -join "`r`n- ") +
        "`r`n"
    )

    [System.IO.File]::AppendAllText(
        $logPath,
        $report,
        (New-Object System.Text.UTF8Encoding($false))
    )

    return $changes
}

function Complete-Task([string]$taskPath) {
    $destination = Join-Path $completedDirectory (Split-Path $taskPath -Leaf)
    Move-Item -Force -Path $taskPath -Destination $destination
    if ($AutoCommit) {
        Push-Location $repositoryRoot
        try {
            & git add -A
            $nextCommitNumber = Get-NextCommitNumber
            $commitDescription = Get-TaskCommitDescription $destination
            $commitMessage = ('{0:D3} - {1}' -f $nextCommitNumber, $commitDescription)
            & git commit -m $commitMessage
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
            Sort-Object `
            @{ Expression = {
                if ($_.Name -match '-(\d+)-') {
                    [int]$matches[1]
                } else {
                    -1
                }
            }; Descending = $true }, `
            @{ Expression = { $_.Name }; Descending = $true } |
            ForEach-Object { Get-TaskSummary $_ $taskGroup.State }
    }
    exit 0
}

# Holding this handle prevents concurrent workers, including simultaneous panel clicks.
$workerLock = $null
try {
    $workerLock = [System.IO.File]::Open((Join-Path $runtimeDirectory 'worker.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
} catch { Write-Error 'Another worker already owns this queue.'; exit 1 }
try {
    # Preserve tasks abandoned by a terminated worker; never overwrite another task.
    $resolvedTasks = [System.IO.Path]::GetFullPath($TaskDirectory).TrimEnd('\')
    if (-not $resolvedTasks.StartsWith($repositoryRoot.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Task directory must be inside the repository.'
    }
    foreach ($orphan in Get-ChildItem -LiteralPath $workingDirectory -Filter '*.md' -File) {
        Move-Item -LiteralPath $orphan.FullName -Destination (Join-Path $resolvedTasks $orphan.Name)
    }
    Clear-Control
    if ($StartPaused) {
        [System.IO.File]::WriteAllText($controlPath, '{"command":"pause"}', (New-Object System.Text.UTF8Encoding($false)))
    }
    Write-BotStatus 'starting' 'Worker iniciado.'
while ($true) {
    Wait-IfPaused ''
    $task = Get-PendingTask

    if ($RateLimitResume -and $null -ne $task) {
        Write-Host ("Retomando tarefa apos rate limit: {0}" -f $task.Name)
        $TaskName = ''
        $RateLimitResume = $false
    }
    if ($null -eq $task) {
        if ($Once) {
            Write-BotStatus 'completed' 'NÃ£o hÃ¡ mais tarefas no escopo.'
            Show-CompletionNotice 'COMANDOS Codex Bot finalizado' ('Tarefas processadas: ' + $processedTasks + '.')
            break
        }
        Write-BotStatus 'waiting' 'Aguardando tarefas ou disponibilidade do Codex.'
        Wait-BotPoll
        continue
    }

    if (-not (Test-CodexAvailable)) {
        Stop-BotForCodexUnavailable $task.Name
        break
    }
    $activeRateLimit = Get-ActiveCodexRateLimit

    if ($null -ne $activeRateLimit) {
        [void](Register-CodexResumeTask `
            $activeRateLimit.ResetAt `
            $task.Name)

        Write-BotStatus `
            'waiting' `
            ("Codex liberado para trabalhar às {0}." -f $activeRateLimit.DisplayText) `
            $task.Name `
            $activeRateLimit.ResetAt.ToString('o')

        break
    }
    $workingTask = Join-Path $workingDirectory $task.Name
    Move-Item -LiteralPath $task.FullName -Destination $workingTask
    $logPath = Join-Path $logDirectory ("{0:yyyyMMdd-HHmmss}-{1}.log" -f (Get-Date), $task.BaseName)

    $implementationBefore = Get-ImplementationSnapshot
    try {
        Write-BotStatus 'waiting' 'Etapa selecionada. Verificando disponibilidade do Codex.' $task.Name
        Invoke-CodexTask $workingTask $logPath
        Assert-CodexTaskResult $logPath
        [void](Assert-ImplementationChanged $implementationBefore $logPath)
        Invoke-ProjectValidation
        Complete-Task $workingTask
        $processedTasks++
        # BOT_STOP_AFTER_SELECTED_REQUIREMENT
        $reachedStopAfterTask = (
            $ExecutionMode -eq 'until' -and
            -not [string]::IsNullOrWhiteSpace($StopAfterTask) -and
            $task.Name -eq $StopAfterTask
        )
        Write-BotStatus 'completed' 'Etapa concluida e validada.' $task.Name
        Show-CompletionNotice 'Tarefa do COMANDOS concluída' "Tarefa: $($task.Name)`nLog: $logPath"
    } catch {
        $errorText = $_ | Out-String
        [System.IO.File]::AppendAllText($logPath, $errorText, (New-Object System.Text.UTF8Encoding($false)))
        if ($errorText -match 'CODEX_UNAVAILABLE|rate limit|usage limit|hit your usage|out of codex|resets on|try again at|add credits|temporarily unavailable') {
            if (Test-Path $workingTask) {
                Move-Item -Force -Path $workingTask -Destination (Join-Path $TaskDirectory $task.Name)
            }

            Stop-BotForCodexUnavailable $task.Name $logPath
            break
        }
        if ($errorText -match 'BOT_STOP_REQUESTED') {
            Move-Item -Force -Path $workingTask -Destination (Join-Path $TaskDirectory $task.Name)
            Write-BotStatus 'stopped' 'Parado pelo painel; etapa devolvida à fila.' $task.Name
            break
        }
        if (Test-Path $workingTask) {
            Fail-Task $workingTask
        }
        Write-BotStatus 'failed' ('Falha na etapa. Consulte ' + $logPath) $task.Name
        Show-CompletionNotice 'Tarefa do COMANDOS falhou' "Tarefa: $($task.Name)`nConsulte: $logPath"
        break
    }

    # selected requirement reached

    if ($reachedStopAfterTask) {

        Write-BotStatus

            'completed'

            ('Requisito limite concluido: ' + $task.Name)

            $task.Name


        Show-CompletionNotice

            'COMANDOS Codex Bot finalizado'

            ('Requisito limite concluido: ' + $task.Name)


        break

    }

    Wait-IfPaused ''

    # BOT_2_MODOS_STOP_AFTER_SELECTED
    if ($ExecutionMode -eq 'until' -and $StopAfterTask -and $task.Name -eq $StopAfterTask) {
        Write-BotStatus 'completed' ('Requisito limite concluido: ' + $task.Name) $task.Name
        Show-CompletionNotice 'COMANDOS Codex Bot finalizado' ('Requisito limite concluido: ' + $task.Name)
        break
    }

    if ($taskLimit -gt 0 -and $processedTasks -ge $taskLimit) {
        Write-BotStatus 'completed' 'Limite de etapas concluido. Inicie outra etapa pelo painel.'
        break
    }
}
} catch {
    if ($_ -match 'BOT_STOP_REQUESTED') { Write-BotStatus 'stopped' 'Worker parado pelo painel.' }
    else { Write-BotStatus 'failed' $_.Exception.Message; throw }
} finally { if ($workerLock) { $workerLock.Dispose() } }
