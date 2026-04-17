@echo off
setlocal
pushd "%~dp0.." >nul
set "ROOT=%CD%"
popd >nul
set "PIDFILE=%ROOT%\run\ass-kicker-manager.pid"

if not exist "%PIDFILE%" (
  echo pid file not found %PIDFILE%
  exit /b 1
)

set "PID="
for /f "usebackq delims=" %%a in ("%PIDFILE%") do set "PID=%%a"
if not defined PID (
  echo pid file empty
  exit /b 1
)

tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
if errorlevel 1 (
  echo stale pid file removing
  del "%PIDFILE%"
  exit /b 0
)

taskkill /PID %PID% /T
timeout /t 2 /nobreak >nul
tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
if not errorlevel 1 (
  taskkill /PID %PID% /T /F
)
del "%PIDFILE%" 2>nul
echo stopped %PID%
