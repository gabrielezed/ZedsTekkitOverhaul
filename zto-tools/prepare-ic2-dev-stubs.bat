@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "TARGET=src\minecraft\ic2\api"
if exist "%TARGET%\IEnergySink.java" (
    echo [ZTO] IC2 API source already exists. Nothing was overwritten.
    popd
    exit /b 0
)
if not exist "%TARGET%" mkdir "%TARGET%"
if errorlevel 1 goto :failed
copy /Y "zto-tools\ic2-dev-stubs\ic2\api\*.java" "%TARGET%\" >nul
if errorlevel 1 goto :failed
echo [ZTO] Installed minimal IC2 1.97 compile-time placeholders at "%TARGET%".
echo [ZTO] These test doubles do not provide EU in MCP. Use Prism Tekkit Dev.
popd
exit /b 0
:failed
echo [ZTO] ERROR: Could not prepare IC2 development placeholders.
popd
exit /b 1
