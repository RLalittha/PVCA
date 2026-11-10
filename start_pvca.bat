@echo off
title PVCA Launcher
echo ====================================================
echo Starting PVCA (PayPal Visual Commerce Assistant)...
echo ====================================================

REM Set environment for portable Node and trusted Avast root CA
set "PATH=D:\PVCA\.tools\node-v20.18.0-win-x64;%PATH%"
set "NODE_EXTRA_CA_CERTS=D:\PVCA\.tools\avast_root.pem"

REM Launch FastAPI Backend in a new persistent console window
start "PVCA Backend (Port 8000)" cmd /k "cd /d D:\PVCA\backend && .venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload"

REM Launch Vite Web Frontend in a new persistent console window
start "PVCA Frontend (Port 5173)" cmd /k "cd /d D:\PVCA\frontend\web && set PATH=D:\PVCA\.tools\node-v20.18.0-win-x64;%%PATH%% && set NODE_EXTRA_CA_CERTS=D:\PVCA\.tools\avast_root.pem && node_modules\.bin\vite.cmd --host 0.0.0.0 --port 5173"

echo.
echo ====================================================
echo Both servers are launching in separate windows!
echo Once they finish starting up, open in Chrome:
echo.
echo   http://localhost:5173
echo.
echo Keep those two terminal windows open while testing.
echo ====================================================
ping 127.0.0.1 -n 4 >nul
