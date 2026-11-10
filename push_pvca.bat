@echo off
setlocal enabledelayedexpansion
title PVCA Git Push Helper
echo ====================================================
echo PVCA - Safe Git Push Helper
echo ====================================================
echo.

cd /d D:\PVCA

REM 1. Show current branch and remote
for /f "tokens=*" %%i in ('git branch --show-current') do set CURRENT_BRANCH=%%i
for /f "tokens=*" %%i in ('git remote get-url origin 2^>nul') do set REMOTE_URL=%%i

echo Current branch: !CURRENT_BRANCH!
echo Remote origin:  !REMOTE_URL!
echo.

REM 2. Display staged files
echo Staged changes to be committed:
echo ----------------------------------------------------
git diff --staged --name-status
if errorlevel 1 (
    echo Error reading git status.
    pause
    exit /b 1
)
echo ----------------------------------------------------
echo.

REM Check if there is anything staged
git diff --staged --quiet
if not errorlevel 1 (
    echo No changes are currently staged for commit.
    echo Use 'git add <files>' before running this script.
    echo.
    pause
    exit /b 0
)

REM 3. Ask for commit message
set /p COMMIT_MSG="Enter commit message (press Enter for 'PVCA update'): "
if "!COMMIT_MSG!"=="" set COMMIT_MSG=PVCA update

echo.
echo Proposed Commit Message: "!COMMIT_MSG!"
echo Target: origin/!CURRENT_BRANCH! (Standard push, NEVER forced)
echo.

REM 4. Request explicit confirmation
set /p CONFIRM="Are you sure you want to commit and push? (y/N): "
if /i not "!CONFIRM!"=="y" (
    echo.
    echo Push cancelled by user. No changes were committed or pushed.
    pause
    exit /b 0
)

REM 5. Commit
echo.
echo Committing staged changes...
git commit -m "!COMMIT_MSG!"
if errorlevel 1 (
    echo Error: Git commit failed.
    pause
    exit /b 1
)

REM 6. Safe push (never force)
echo.
echo Pushing to origin/!CURRENT_BRANCH!...
git push origin !CURRENT_BRANCH!
if errorlevel 1 (
    echo.
    echo ====================================================
    echo Push failed. Please check your network connection
    echo or GitHub authentication credentials.
    echo ====================================================
    pause
    exit /b 1
)

echo.
echo ====================================================
echo Successfully pushed to !REMOTE_URL! (!CURRENT_BRANCH!)
echo ====================================================
pause
