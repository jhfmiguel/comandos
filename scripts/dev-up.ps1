param(
    [switch]$Messaging,
    [switch]$Cache
)

$profiles = @()
if ($Messaging) { $profiles += "--profile"; $profiles += "messaging" }
if ($Cache) { $profiles += "--profile"; $profiles += "cache" }

docker compose -f "$PSScriptRoot\..\infrastructure\docker\compose.yml" @profiles up -d
