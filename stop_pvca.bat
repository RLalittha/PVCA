@echo off
title Stop PVCA Servers
echo Stopping PVCA servers on ports 8000 and 5173...

for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8000 ^| findstr LISTENING') do (
    echo Terminating PID %%a on port 8000
    taskkill /f /pid %%a >nul 2>&1
)

for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5173 ^| findstr LISTENING') do (
    echo Terminating PID %%a on port 5173
    taskkill /f /pid %%a >nul 2>&1
)

echo Done. PVCA servers stopped.
ping 127.0.0.1 -n 3 >nul
