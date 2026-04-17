@echo off
setlocal EnableDelayedExpansion
pushd "%~dp0.." >nul
set "ROOT=%CD%"
popd >nul
set "JAR=%ROOT%\lib\ass-kicker-manager.jar"
set "RUN=%ROOT%\run"
set "LOG=%ROOT%\logs"
set "PIDFILE=%RUN%\ass-kicker-manager.pid"
set "OUT=%LOG%\ass-kicker-manager.out"
if not exist "%RUN%" mkdir "%RUN%"
if not exist "%LOG%" mkdir "%LOG%"

if exist "%PIDFILE%" (
  for /f "usebackq delims=" %%a in ("%PIDFILE%") do (
    tasklist /FI "PID eq %%a" 2>nul | find "%%a" >nul
    if !errorlevel! equ 0 (
      echo already running pid %%a
      exit /b 1
    )
  )
  del "%PIDFILE%" 2>nul
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; ^
   $root=[System.IO.Path]::GetFullPath('%ROOT%'); ^
   $jar=[System.IO.Path]::GetFullPath('%JAR%'); ^
   $pidf=[System.IO.Path]::GetFullPath('%PIDFILE%'); ^
   $out=[System.IO.Path]::GetFullPath('%OUT%'); ^
   $arg='--spring.config.additional-location=optional:file:' + $root + '/conf/'; ^
   $p=Start-Process -FilePath 'java' -WorkingDirectory $root -ArgumentList @('-jar',$jar,$arg) -RedirectStandardOutput $out -RedirectStandardError $out -PassThru -WindowStyle Hidden; ^
   [System.IO.File]::WriteAllText($pidf, [string]$p.Id); ^
   Write-Host ('started pid ' + $p.Id + ' log ' + $out)"
