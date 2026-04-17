@echo off
setlocal
set "BIN=%~dp0"
pushd "%BIN%.." >nul
set "ROOT=%CD%"
popd >nul
set "JAR=%ROOT%\lib\ass-kicker-svr.jar"
java %JAVA_OPTS% -jar "%JAR%" --spring.config.additional-location=optional:file:%ROOT%/conf/ %*
