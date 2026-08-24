$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sdkRoot = Join-Path $projectRoot '.android-sdk'
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_AVD_HOME = Join-Path $projectRoot '.android-avd'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.android-user'

$emulator = Join-Path $sdkRoot 'emulator\emulator.exe'
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
$apk = Join-Path $projectRoot 'release\HoraCerta-v0.1.0.apk'
$avdName = 'HoraCerta_Pixel7_API36'
$packageName = 'com.thiagoventura.horacerta'

if (-not (Test-Path -LiteralPath $emulator)) { throw 'Android Emulator nao encontrado.' }
if (-not (Test-Path -LiteralPath $adb)) { throw 'ADB nao encontrado.' }

$emulatorDevice = (& $adb devices) |
    Select-String -Pattern '^emulator-\d+\s+device$' |
    Select-Object -First 1

if (-not $emulatorDevice) {
    Start-Process -FilePath $emulator -ArgumentList @(
        '-avd', $avdName,
        '-gpu', 'auto',
        '-timezone', 'America/Fortaleza'
    ) | Out-Null

    $booted = $false
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        $serialLine = (& $adb devices) |
            Select-String -Pattern '^emulator-\d+\s+device$' |
            Select-Object -First 1
        if ($serialLine) {
            $serial = ($serialLine.Line -split '\s+')[0]
            $boot = (& $adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
            if ($boot -eq '1') {
                $booted = $true
                break
            }
        }
        Start-Sleep -Seconds 2
    }
    if (-not $booted) { throw 'O Android virtual nao concluiu a inicializacao.' }
} else {
    $serial = ($emulatorDevice.Line -split '\s+')[0]
}

$installed = (& $adb -s $serial shell pm path $packageName 2>$null)
if (-not $installed) {
    & $adb -s $serial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel instalar o Hora Certa.' }
}

& $adb -s $serial shell am start -n "$packageName/.MainActivity" | Out-Null

