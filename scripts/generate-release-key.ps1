$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$signingDirectory = Join-Path $projectRoot 'signing'
$keyStorePath = Join-Path $signingDirectory 'horacerta-release.jks'
$propertiesPath = Join-Path $signingDirectory 'keystore.properties'

if ((Test-Path -LiteralPath $keyStorePath) -or (Test-Path -LiteralPath $propertiesPath)) {
    Write-Host 'A chave de assinatura ja existe. Nenhum arquivo foi alterado.'
    exit 0
}

New-Item -ItemType Directory -Force -Path $signingDirectory | Out-Null

$randomBytes = New-Object byte[] 32
$randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $randomGenerator.GetBytes($randomBytes)
} finally {
    $randomGenerator.Dispose()
}
$password = [Convert]::ToBase64String($randomBytes).Replace('+', 'A').Replace('/', 'B').TrimEnd('=')

$keytoolPath = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\keytool.exe'
} else {
    $javaCommand = Get-Command 'java.exe' -ErrorAction Stop
    Join-Path (Split-Path $javaCommand.Source -Parent) 'keytool.exe'
}
if (-not (Test-Path -LiteralPath $keytoolPath)) {
    throw 'keytool.exe nao foi encontrado ao lado do Java instalado.'
}
$env:HORACERTA_KEYSTORE_GENERATION_PASSWORD = $password
try {
    & $keytoolPath `
        -genkeypair `
        -keystore $keyStorePath `
        -storetype PKCS12 `
        -storepass:env HORACERTA_KEYSTORE_GENERATION_PASSWORD `
        -alias 'horacerta' `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -dname 'CN=Hora Certa, OU=Mobile, O=Thiago Ventura, L=Recife, ST=Pernambuco, C=BR'
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel gerar a chave de assinatura.' }
} finally {
    Remove-Item Env:HORACERTA_KEYSTORE_GENERATION_PASSWORD
}

$properties = @(
    'storeFile=signing/horacerta-release.jks'
    "storePassword=$password"
    'keyAlias=horacerta'
    "keyPassword=$password"
)
[System.IO.File]::WriteAllLines($propertiesPath, $properties, [System.Text.UTF8Encoding]::new($false))

Write-Host 'Chave de assinatura criada. Guarde a pasta signing em um backup seguro.'
