param()

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")

Push-Location $projectRoot
try {
    git config core.hooksPath .githooks
    Write-Output "Configured git hooks path to .githooks"
}
finally {
    Pop-Location
}
