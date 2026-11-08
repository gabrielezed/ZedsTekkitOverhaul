@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "TARGET=src\minecraft\ic2\api"
if exist "%TARGET%\IEnergySink.java" (
    for %%I in (IWrenchable IEnergyEmitter IEnergySource) do (
        if not exist "%TARGET%\%%I.java" (
            copy "zto-tools\ic2-dev-stubs\ic2\api\%%I.java" "%TARGET%\" >nul
            if errorlevel 1 goto :failed
        )
    )
    findstr /L /C:"MCP-only test double" "%TARGET%\EnergyNet.java" >nul 2>&1
    if not errorlevel 1 (
        findstr /L /C:"emitEnergyFrom" "%TARGET%\EnergyNet.java" >nul
        if errorlevel 1 (
            copy /Y "zto-tools\ic2-dev-stubs\ic2\api\EnergyNet.java" "%TARGET%\" >nul
            if errorlevel 1 goto :failed
        )
    )
    echo [ZTO] IC2 API prepared. Existing external API sources were preserved.
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
