# PVCA Environment Diagnostic Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   PVCA - Android Environment Check     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Check Java / JDK
Write-Host "`n[1/4] Checking Java..." -NoNewline
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    Write-Host " Found java executable: $($javaCmd.Source)" -ForegroundColor Yellow
    Write-Host "   -> Note: AGP 8.5+ requires JDK 17+. Android Studio installs this automatically." -ForegroundColor Gray
} else {
    Write-Host " NOT FOUND in PATH." -ForegroundColor Yellow
}

# 2. Check Android Studio
Write-Host "`n[2/4] Checking Android Studio..." -NoNewline
$studioPaths = @(
    "C:\Program Files\Android\Android Studio",
    "$env:LOCALAPPDATA\Programs\Android Studio"
)
$studioFound = $false
foreach ($p in $studioPaths) {
    if (Test-Path $p) {
        Write-Host " OK (Found at $p)" -ForegroundColor Green
        $studioFound = $true
        break
    }
}
if (-not $studioFound) {
    Write-Host " NOT FOUND." -ForegroundColor Yellow
    Write-Host "   -> Install Android Studio from https://developer.android.com/studio" -ForegroundColor Gray
}

# 3. Check Android SDK
Write-Host "`n[3/4] Checking Android SDK..." -NoNewline
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdkPath) {
    Write-Host " OK (Found at $sdkPath)" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND." -ForegroundColor Yellow
    Write-Host "   -> Android Studio will automatically install this on first launch." -ForegroundColor Gray
}

# 4. Check ADB / Connected Devices
Write-Host "`n[4/4] Checking ADB & Connected Devices..." -NoNewline
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adbPath) {
    Write-Host " ADB Available at $adbPath" -ForegroundColor Green
} else {
    Write-Host " ADB not installed yet (installed with Android SDK Platform-Tools)." -ForegroundColor Gray
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Diagnostic complete." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

