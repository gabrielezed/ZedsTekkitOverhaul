@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "TARGET=src\minecraft\ee"
if not exist "%TARGET%\" mkdir "%TARGET%"
if errorlevel 1 goto :failed
if not exist "%TARGET%\IEEPowerNet.java" (
    copy "zto-tools\ee2-dev-stubs\ee\IEEPowerNet.java" "%TARGET%\" >nul
    if errorlevel 1 goto :failed
)
popd
exit /b 0

:failed
popd
exit /b 1
