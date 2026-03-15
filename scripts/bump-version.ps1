param(
    [string]$BaseUrl = ""
)

$ErrorActionPreference = "Stop"

function Read-PropertiesFile {
    param([string]$Path)

    $properties = @{}
    if (-not (Test-Path $Path)) {
        return $properties
    }

    Get-Content -Path $Path | ForEach-Object {
        if (($_ -notmatch '^\s*#') -and ($_ -match '^\s*([^=]+?)\s*=\s*(.*)\s*$')) {
            $properties[$matches[1].Trim()] = $matches[2].Trim()
        }
    }

    return $properties
}

function Write-PropertiesFile {
    param(
        [string]$Path,
        [hashtable]$Properties
    )

    $lines = @(
        "VERSION_CODE=$($Properties['VERSION_CODE'])"
        "VERSION_NAME=$($Properties['VERSION_NAME'])"
    )

    Set-Content -Path $Path -Value $lines -Encoding ascii
}

function Resolve-SiteUrl {
    param(
        [string]$ProjectRoot,
        [string]$CliBaseUrl
    )

    if (-not [string]::IsNullOrWhiteSpace($CliBaseUrl)) {
        return $CliBaseUrl
    }

    $localPropertiesPath = Join-Path $ProjectRoot "local.properties"
    $localProperties = Read-PropertiesFile -Path $localPropertiesPath
    $siteUrl = $localProperties["update.siteUrl"]

    if ([string]::IsNullOrWhiteSpace($siteUrl)) {
        throw "update.siteUrl is missing in local.properties."
    }

    return $siteUrl.Replace("\:", ":").Replace("\\", "\")
}

function Resolve-ReleaseDownloadUrl {
    param(
        [string]$ProjectRoot,
        [string]$VersionName
    )

    try {
        Push-Location $ProjectRoot
        $remoteUrl = (git remote get-url origin).Trim()
    }
    catch {
        return ""
    }
    finally {
        Pop-Location
    }

    if ([string]::IsNullOrWhiteSpace($remoteUrl)) {
        return ""
    }

    $normalizedRemoteUrl = $remoteUrl
    if ($normalizedRemoteUrl -match '^git@github\.com:(.+?)/(.+?)(\.git)?$') {
        $owner = $matches[1]
        $repo = $matches[2]
        return "https://github.com/$owner/$repo/releases/download/v$VersionName/some-$VersionName.apk"
    }

    if ($normalizedRemoteUrl -match '^https://github\.com/(.+?)/(.+?)(\.git)?$') {
        $owner = $matches[1]
        $repo = $matches[2]
        return "https://github.com/$owner/$repo/releases/download/v$VersionName/some-$VersionName.apk"
    }

    return ""
}

function Get-NextVersionName {
    param([string]$CurrentVersionName)

    if ($CurrentVersionName -match '^(\d+)\.(\d+)\.(\d+)$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        $patch = [int]$matches[3] + 1
        return "$major.$minor.$patch"
    }

    if ($CurrentVersionName -match '^(\d+)\.(\d+)$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        return "$major.$minor.1"
    }

    return "1.0.1"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$versionPropertiesPath = Join-Path $projectRoot "version.properties"
$prepareScriptPath = Join-Path $scriptDir "prepare-distribution-site.ps1"

if (-not (Test-Path $versionPropertiesPath)) {
    throw "version.properties file was not found: $versionPropertiesPath"
}

$versionProperties = Read-PropertiesFile -Path $versionPropertiesPath
$currentVersionCode = [int]($versionProperties["VERSION_CODE"])
$currentVersionName = $versionProperties["VERSION_NAME"]

if ([string]::IsNullOrWhiteSpace($currentVersionName)) {
    throw "VERSION_NAME is missing in version.properties."
}

$nextVersionCode = $currentVersionCode + 1
$nextVersionName = Get-NextVersionName -CurrentVersionName $currentVersionName

$versionProperties["VERSION_CODE"] = "$nextVersionCode"
$versionProperties["VERSION_NAME"] = $nextVersionName
Write-PropertiesFile -Path $versionPropertiesPath -Properties $versionProperties

$resolvedBaseUrl = Resolve-SiteUrl -ProjectRoot $projectRoot -CliBaseUrl $BaseUrl
$downloadUrl = Resolve-ReleaseDownloadUrl -ProjectRoot $projectRoot -VersionName $nextVersionName

if ([string]::IsNullOrWhiteSpace($downloadUrl)) {
    powershell -ExecutionPolicy Bypass -File $prepareScriptPath -BaseUrl $resolvedBaseUrl -SkipApkCopy
}
else {
    powershell -ExecutionPolicy Bypass -File $prepareScriptPath -BaseUrl $resolvedBaseUrl -DownloadUrl $downloadUrl -SkipApkCopy
}

Write-Output "Bumped version to $nextVersionName ($nextVersionCode)"
