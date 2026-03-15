param(
    [Parameter(Mandatory = $true)]
    [string]$KeystorePath
)

$ErrorActionPreference = "Stop"

$resolvedPath = Resolve-Path $KeystorePath
$bytes = [System.IO.File]::ReadAllBytes($resolvedPath)
$base64 = [System.Convert]::ToBase64String($bytes)

Write-Output $base64
