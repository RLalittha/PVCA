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

REM 2. Check for staged changes
set HAS_STAGED=0
git diff --staged --quiet
if errorlevel 1 set HAS_STAGED=1

if "!HAS_STAGED!"=="1" (
    echo Staged changes to be committed:
    echo ----------------------------------------------------
    git diff --staged --name-status
    echo ----------------------------------------------------
    echo.
    
    set /p COMMIT_MSG="Enter commit message (press Enter for 'PVCA update'): "
    if "!COMMIT_MSG!"=="" set COMMIT_MSG=PVCA update

    echo.
    echo Proposed Commit Message: "!COMMIT_MSG!"
    echo Target: origin/!CURRENT_BRANCH! (Standard push, NEVER forced)
    echo.

    set /p CONFIRM="Are you sure you want to commit and push? (y/N): "
    if /i not "!CONFIRM!"=="y" (
        echo.
        echo Push cancelled by user. No changes were committed or pushed.
        pause
        exit /b 0
    )

    echo.
    echo Committing staged changes...
    git commit -m "!COMMIT_MSG!"
    if errorlevel 1 (
        echo Error: Git commit failed.
        pause
        exit /b 1
    )
) else (
    echo No newly staged files found. Checking for unpushed commits...
    echo.
    echo Recent local commits:
    git log --oneline -3
    echo.
    set /p CONFIRM="Push pending commits to origin/!CURRENT_BRANCH!? (y/N): "
    if /i not "!CONFIRM!"=="y" (
        echo.
        echo Push cancelled by user.
        pause
        exit /b 0
    )
)

REM 3. Safe push (never force)
echo.
echo Pushing to origin/!CURRENT_BRANCH!...
echo (If a GitHub login dialog appears, please complete sign-in in your browser)
echo.
git push -u origin !CURRENT_BRANCH!
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
