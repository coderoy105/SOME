param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl
)

$ErrorActionPreference = "Stop"

function Escape-PropertiesValue {
    param([string]$Value)

    return $Value.Replace("\", "\\").Replace(":", "\:")
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$localPropertiesPath = Join-Path $projectRoot "local.properties"

if (-not (Test-Path $localPropertiesPath)) {
    throw "local.properties file was not found: $localPropertiesPath"
}

$normalizedBaseUrl = $BaseUrl.Trim()
if (-not $normalizedBaseUrl.EndsWith("/")) {
    $normalizedBaseUrl += "/"
}

$feedUrl = "$normalizedBaseUrl" + "latest.json"
$escapedFeedUrl = Escape-PropertiesValue -Value $feedUrl
$escapedSiteUrl = Escape-PropertiesValue -Value $normalizedBaseUrl

$lines = Get-Content -Path $localPropertiesPath | Where-Object {
    ($_ -notmatch '^update\.feedUrl=') -and ($_ -notmatch '^update\.siteUrl=')
}

$lines += "update.feedUrl=$escapedFeedUrl"
$lines += "update.siteUrl=$escapedSiteUrl"

Set-Content -Path $localPropertiesPath -Value $lines -Encoding ascii

Write-Output "Configured local.properties"
Write-Output "update.feedUrl=$feedUrl"
Write-Output "update.siteUrl=$normalizedBaseUrl"
