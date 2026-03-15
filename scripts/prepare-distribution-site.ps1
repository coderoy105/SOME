param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,
    [string]$ApkSourcePath = "app/build/outputs/apk/debug/app-debug.apk",
    [switch]$SkipApkCopy
)

$ErrorActionPreference = "Stop"

function Read-PropertiesFile {
    param([string]$Path)

    $properties = @{}
    Get-Content -Path $Path | ForEach-Object {
        if (($_ -notmatch '^\s*#') -and ($_ -match '^\s*([^=]+?)\s*=\s*(.*)\s*$')) {
            $properties[$matches[1].Trim()] = $matches[2].Trim()
        }
    }

    return $properties
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$versionPropertiesPath = Join-Path $projectRoot "version.properties"
$siteDirectory = Join-Path $projectRoot "distribution-site"
$apkDirectory = Join-Path $siteDirectory "apk"
$apkSource = Join-Path $projectRoot $ApkSourcePath

if (-not (Test-Path $versionPropertiesPath)) {
    throw "version.properties file was not found: $versionPropertiesPath"
}

if ((-not $SkipApkCopy) -and (-not (Test-Path $apkSource))) {
    throw "APK file was not found: $apkSource"
}

$versionProperties = Read-PropertiesFile -Path $versionPropertiesPath
$rawVersionCode = $versionProperties["VERSION_CODE"]
if ([string]::IsNullOrWhiteSpace($rawVersionCode)) {
    $rawVersionCode = "1"
}
$versionCode = [int]$rawVersionCode
$versionName = $versionProperties["VERSION_NAME"]

if ([string]::IsNullOrWhiteSpace($versionName)) {
    throw "VERSION_NAME is missing in version.properties."
}

$normalizedBaseUrl = $BaseUrl.Trim()
if (-not $normalizedBaseUrl.EndsWith("/")) {
    $normalizedBaseUrl += "/"
}

New-Item -ItemType Directory -Path $apkDirectory -Force | Out-Null

$apkFileName = "some-$versionName.apk"
$apkTarget = Join-Path $apkDirectory $apkFileName
if (-not $SkipApkCopy) {
    Copy-Item -Path $apkSource -Destination $apkTarget -Force
}

$apkUrl = "$normalizedBaseUrl" + "apk/$apkFileName"
$pageUrl = $normalizedBaseUrl

$latestJson = [ordered]@{
    versionCode = $versionCode
    versionName = $versionName
    apkUrl = $apkUrl
    pageUrl = $pageUrl
    force = $false
    message = "A new SOME update is ready."
} | ConvertTo-Json -Depth 4

$indexHtml = @"
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>SOME Download</title>
  <style>
    :root {
      --pink: #f05d9b;
      --pink-soft: #f8d6ea;
      --bg: #fff7fc;
      --text: #35253a;
      --muted: #6f6172;
      --card: rgba(255,255,255,.96);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", "Apple SD Gothic Neo", sans-serif;
      background:
        radial-gradient(circle at top right, #ffe7f4 0%, transparent 35%),
        radial-gradient(circle at bottom left, #f2e8ff 0%, transparent 30%),
        var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
    }
    .card {
      width: min(560px, 100%);
      background: var(--card);
      border: 1px solid rgba(240,93,155,.18);
      border-radius: 28px;
      box-shadow: 0 24px 70px rgba(99, 54, 95, .14);
      padding: 28px;
      backdrop-filter: blur(12px);
    }
    h1 {
      margin: 0 0 10px;
      text-align: center;
      font-size: 40px;
      letter-spacing: -0.03em;
    }
    p {
      margin: 0;
      line-height: 1.6;
      color: var(--muted);
    }
    .row {
      margin-top: 22px;
      padding-top: 18px;
      border-top: 1px solid rgba(112, 91, 114, .12);
    }
    .meta {
      margin-top: 12px;
      padding: 14px 16px;
      border-radius: 18px;
      background: linear-gradient(135deg, var(--pink-soft), #fff);
      color: var(--text);
      font-weight: 600;
    }
    .button {
      display: block;
      width: 100%;
      text-decoration: none;
      text-align: center;
      margin-top: 14px;
      padding: 15px 18px;
      border-radius: 18px;
      color: #1f1522;
      font-weight: 700;
      background: linear-gradient(135deg, var(--pink-soft), #fff);
      border: 1px solid rgba(240,93,155,.22);
    }
    .hint {
      margin-top: 12px;
      font-size: 14px;
    }
  </style>
</head>
<body>
  <main class="card">
    <h1>SOME</h1>
    <p>This page hosts the latest SOME APK. On Android, you may need to allow installs from unknown apps before opening the file.</p>

    <section class="row">
      <div class="meta">Latest version: $versionName</div>
      <a class="button" href="./apk/$apkFileName">Download latest APK</a>
      <p class="hint">This page and its version metadata are refreshed automatically by the deployment workflow.</p>
    </section>
  </main>
</body>
</html>
"@

Set-Content -Path (Join-Path $siteDirectory "latest.json") -Value $latestJson -Encoding ascii
Set-Content -Path (Join-Path $siteDirectory "index.html") -Value $indexHtml -Encoding ascii

Write-Output "Prepared distribution site"
Write-Output "Version: $versionName ($versionCode)"
Write-Output "Page URL: $pageUrl"
Write-Output "APK URL: $apkUrl"
