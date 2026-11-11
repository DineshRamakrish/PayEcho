$ErrorActionPreference = 'Stop'

$signingDirectory = Join-Path $env:LOCALAPPDATA 'PayEcho'
$keystorePath = Join-Path $signingDirectory 'PayEcho-release.p12'
$passwordPath = Join-Path $signingDirectory 'release-password.dpapi'
$sdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$originalAndroidHome = $env:ANDROID_HOME
$originalAndroidSdkRoot = $env:ANDROID_SDK_ROOT

if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath $sdkPath)) {
    $env:ANDROID_HOME = $sdkPath
}
if (-not $env:ANDROID_SDK_ROOT -and (Test-Path -LiteralPath $sdkPath)) {
    $env:ANDROID_SDK_ROOT = $sdkPath
}

Push-Location -LiteralPath $PSScriptRoot
try {
    if (-not (Test-Path -LiteralPath $keystorePath)) {
        throw "Release keystore not found: $keystorePath"
    }
    if (-not (Test-Path -LiteralPath $passwordPath)) {
        throw "Release signing password not found: $passwordPath"
    }

    $encryptedPassword = Get-Content -LiteralPath $passwordPath -Raw
    $securePassword = ConvertTo-SecureString -String $encryptedPassword
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
        $env:PAYECHO_KEYSTORE_PATH = $keystorePath
        $env:PAYECHO_STORE_PASSWORD = $password
        $env:PAYECHO_KEY_PASSWORD = $password
        $env:PAYECHO_KEY_ALIAS = 'payecho'

        if (-not (Test-Path -LiteralPath '.\gradlew.bat')) {
            throw 'Gradle wrapper is missing from the project.'
        }
        & .\gradlew.bat assembleRelease
        if ($LASTEXITCODE -ne 0) {
            throw "Release build failed with exit code $LASTEXITCODE"
        }

        Copy-Item -LiteralPath '.\app\build\outputs\apk\release\app-release.apk' `
            -Destination '.\PayEcho.apk' -Force
    } finally {
        if ($passwordPointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
        Remove-Item Env:PAYECHO_KEYSTORE_PATH -ErrorAction SilentlyContinue
        Remove-Item Env:PAYECHO_STORE_PASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:PAYECHO_KEY_PASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:PAYECHO_KEY_ALIAS -ErrorAction SilentlyContinue
        $password = $null
        }
} finally {
        if ($null -eq $originalAndroidHome) {
            Remove-Item Env:ANDROID_HOME -ErrorAction SilentlyContinue
        } else {
            $env:ANDROID_HOME = $originalAndroidHome
        }
        if ($null -eq $originalAndroidSdkRoot) {
            Remove-Item Env:ANDROID_SDK_ROOT -ErrorAction SilentlyContinue
        } else {
            $env:ANDROID_SDK_ROOT = $originalAndroidSdkRoot
        }
    Pop-Location
}
