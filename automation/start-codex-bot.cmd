@echo off
setlocal
cd /d "%~dp0.."
start "COMANDOS Codex Bot" /min powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\automation\codex-erp-bot.ps1"
endlocal
