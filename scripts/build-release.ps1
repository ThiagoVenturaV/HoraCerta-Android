$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
$localGradle = Join-Path $projectRoot '.tooling\gradle\gradle-9.4.1\bin\gradle.bat'
$gradle = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { Join-Path $projectRoot 'gradlew.bat' }

if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'signing\keystore.properties'))) {
    & (Join-Path $PSScriptRoot 'generate-release-key.ps1')
}

Push-Location $projectRoot
try {
    & $gradle ':app:testDebugUnitTest' ':app:lintRelease' ':app:assembleRelease'
    if ($LASTEXITCODE -ne 0) { throw "A compilacao falhou com codigo $LASTEXITCODE." }

    $releaseDirectory = Join-Path $projectRoot 'release'
    New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
    Copy-Item `
        -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk') `
        -Destination (Join-Path $releaseDirectory 'HoraCerta-v0.2.0.apk') `
        -Force
    Write-Host "APK pronto em $releaseDirectory\HoraCerta-v0.2.0.apk"
} finally {
    Pop-Location
}
