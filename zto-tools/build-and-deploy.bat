@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem ZedsTekkitOverhaul - MCP 1.2.5 build + PrismLauncher deployment.
rem Start from anywhere: the script resolves the MDK root from its own location.
rem ZTO_ARTWORK_PROFILE=public creates distribution artifacts without installing.
rem ZTO_PRISM_MODS selects a Prism mods directory or a loose staging directory.

pushd "%~dp0.." >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERROR: Unable to locate the MDK directory.
    exit /b 1
)

set "ARTWORK_PROFILE=local-dev"
if defined ZTO_ARTWORK_PROFILE set "ARTWORK_PROFILE=%ZTO_ARTWORK_PROFILE%"
set "ARTIFACT_DIR=%CD%\dist-zto"
if "%ARTWORK_PROFILE%"=="public" (
    set "ARTIFACT_DIR=%CD%\dist-zto\public"
    if defined ZTO_PRISM_MODS (
        echo [ZTO] ERROR: Clear ZTO_PRISM_MODS when building the public profile.
        goto :fail
    )
) else if not "%ARTWORK_PROFILE%"=="local-dev" (
    echo [ZTO] ERROR: Set ZTO_ARTWORK_PROFILE to local-dev or public.
    goto :fail
)
set "INSTANCE=%APPDATA%\PrismLauncher\instances\Tekkit Dev"
set "MODS_DIR=%INSTANCE%\minecraft\mods"
if defined ZTO_PRISM_MODS set "MODS_DIR=%ZTO_PRISM_MODS%"
for %%F in ("%MODS_DIR%") do set "MODS_DIR=%%~fF"
set "JAR_NAME=ZedsTekkitOverhaul.jar"
set "WORK_DIR=%CD%\dist-zto\packaging\%ARTWORK_PROFILE%"
set "BUILD_JAR=%WORK_DIR%\%JAR_NAME%"
set "DEPLOY_JAR=%MODS_DIR%\%JAR_NAME%"
set "BUILD_METADATA=%WORK_DIR%\zto-build.properties"
set "FINGERPRINTS=%WORK_DIR%\artwork.properties"

if not exist "recompile.bat" (
    echo [ZTO] ERROR: recompile.bat was not found in the MDK root.
    goto :fail
)
if not exist "reobfuscate.bat" (
    echo [ZTO] ERROR: reobfuscate.bat was not found in the MDK root.
    goto :fail
)
if "%ARTWORK_PROFILE%"=="local-dev" if not exist "%MODS_DIR%\" (
    echo [ZTO] ERROR: PrismLauncher mods directory was not found:
    echo "%MODS_DIR%"
    goto :fail
)
if not exist "src\minecraft\zed\gui\advanced_table.png" (
    echo [ZTO] ERROR: Missing src\minecraft\zed\gui\advanced_table.png
    goto :fail
)
if not exist "src\minecraft\zed\models\foundry_rotor.obj" (
    echo [ZTO] ERROR: Missing Foundry OBJ model asset.
    goto :fail
)
if not exist "src\minecraft\zed\textures\foundry_rotor.png" (
    echo [ZTO] ERROR: Missing Foundry texture asset.
    goto :fail
)
if not exist "%WORK_DIR%\" mkdir "%WORK_DIR%"
if not exist "%WORK_DIR%\" goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\check-artwork.ps1" -Profile "%ARTWORK_PROFILE%" -PrepareLocal -Fingerprints "%FINGERPRINTS%"
if errorlevel 1 goto :fail
if not exist "src\minecraft\mcmod.info" (
    echo [ZTO] ERROR: Missing mcmod.info metadata resource.
    goto :fail
)
where jar.exe >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERROR: jar.exe was not found in PATH. Configure JDK 8.
    goto :fail
)

rem Avoid loading two versions of the same ModLoader mod.
if "%ARTWORK_PROFILE%"=="local-dev" for %%F in ("%MODS_DIR%\ZedsTekkitOverhaul*.jar") do (
    if exist "%%~fF" (
        if /I not "%%~nxF"=="%JAR_NAME%" (
            echo [ZTO] ERROR: Another ZedsTekkitOverhaul JAR was found in mods: %%~nxF
            echo [ZTO] Move it out of the mods directory before deploying.
            goto :fail
        )
    )
)

echo [ZTO] Preparing local IC2 1.97 compile-time placeholders...
call "zto-tools\prepare-ic2-dev-stubs.bat"
if errorlevel 1 goto :fail
call "zto-tools\prepare-buildcraft-dev-stubs.bat"
if errorlevel 1 goto :fail
call "zto-tools\prepare-ee2-dev-stubs.bat"
if errorlevel 1 goto :fail
echo [ZTO] 1/4 - Compiling with MCP...
if not exist "dist-zto\" mkdir "dist-zto"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\build-provenance.ps1" -Metadata "%BUILD_METADATA%" -ArtworkFingerprints "%FINGERPRINTS%"
if errorlevel 1 goto :fail
call "recompile.bat" > "dist-zto\mcp-compile.log" 2>&1
set "MCP_EXIT=%ERRORLEVEL%"
type "dist-zto\mcp-compile.log"
if not "%MCP_EXIT%"=="0" goto :compile_failed
rem MCP 6.2 catches javac errors internally. Its wrapper can return success with partial classes.
findstr /L /C:"== ERRORS FOUND ==" /C:"FATAL ERROR" "dist-zto\mcp-compile.log" >nul
if not errorlevel 1 goto :compile_failed
findstr /B /L /C:"- Done in " "dist-zto\mcp-compile.log" >nul
if errorlevel 1 goto :compile_failed
copy /y "%BUILD_METADATA%" "bin\minecraft\zto-build.properties" >nul
if errorlevel 1 goto :fail

rem Remove only previously reobfuscated ZTO classes, never vanilla/Forge code.
if exist "reobf\minecraft\zed\" (
    rmdir /s /q "reobf\minecraft\zed"
    if exist "reobf\minecraft\zed\" (
        echo [ZTO] ERROR: Unable to clean reobf\minecraft\zed
        goto :fail
    )
)

echo [ZTO] 2/4 - Reobfuscating with MCP...
call "reobfuscate.bat"
if errorlevel 1 (
    echo [ZTO] ERROR: reobfuscate.bat failed.
    goto :fail
)
if not exist "reobf\minecraft\zed\tekkitoverhaul\mod_ZedsTekkitOverhaul.class" (
    echo [ZTO] ERROR: Mod entry point is missing from reobfuscated output.
    goto :fail
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\build-zto-agent.ps1" -ArtifactDirectory "%WORK_DIR%"
if errorlevel 1 goto :fail

if not exist "dist-zto\" mkdir "dist-zto"
if not exist "dist-zto\" (
    echo [ZTO] ERROR: Unable to create dist-zto.
    goto :fail
)

echo [ZTO] 3/4 - Packaging JAR and GUI texture...
if exist "%BUILD_JAR%" del /q "%BUILD_JAR%"
jar cf "%BUILD_JAR%" -C "reobf\minecraft" zed
if errorlevel 1 (
    echo [ZTO] ERROR: JAR packaging failed.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "src\minecraft" zed\gui
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to add GUI assets to the JAR.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "src\minecraft" zed\models
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to add model assets to the JAR.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "src\minecraft" zed\textures
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to add texture assets to the JAR.
    goto :fail
)
for %%S in (ambient1 ambient2 warning dive hurt death) do (
    if not exist "src\minecraft\zed\sounds\aerial\%%S.wav" (
        echo [ZTO] ERROR: Missing aerial predator sound %%S.
        goto :fail
    )
)
for %%S in (warning fault recovery) do (
    if not exist "src\minecraft\zed\sounds\jetpack\%%S.wav" (
        echo [ZTO] ERROR: Missing jetpack weather sound %%S.
        goto :fail
    )
)
for %%S in (ambient1 ambient2 warning slash hurt death) do (
    if not exist "src\minecraft\zed\sounds\skelebird\%%S.wav" (
        echo [ZTO] ERROR: Missing Skelebird sound %%S.
        goto :fail
    )
)
if not exist "src\minecraft\zed\textures\skelebird.png" (
    echo [ZTO] ERROR: Missing Skelebird texture.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "src\minecraft" zed\sounds
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to add predator sounds to the JAR.
    goto :fail
)
for %%S in (start running complete) do (
    jar tf "%BUILD_JAR%" | findstr /L /X /C:"zed/sounds/distillation/%%S.wav" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: Missing Distillation sound %%S in the archive.
        goto :fail
    )
)
jar tf "%BUILD_JAR%" | findstr /L /X /C:"zed/sounds/motorboat/running.wav" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Missing Motorboat engine sound in the archive.
    goto :fail
)
for %%S in (ambient warning lunge hurt death) do (
    jar tf "%BUILD_JAR%" | findstr /L /X /C:"zed/sounds/guardian/%%S.wav" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: Missing Abyssal Guardian sound %%S in the archive.
        goto :fail
    )
)
for %%S in (ambient charge release hurt death) do (
    jar tf "%BUILD_JAR%" | findstr /L /X /C:"zed/sounds/abyssalenderkin/%%S.wav" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: Missing Abyssal Enderkin sound %%S in the archive.
        goto :fail
    )
)
jar uf "%BUILD_JAR%" -C "src\minecraft" mcmod.info
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to add mcmod.info to the JAR.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "%WORK_DIR%" zto-build.properties
if errorlevel 1 goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\preserve-local-assets.ps1" -Profile "%ARTWORK_PROFILE%" -Candidate "%BUILD_JAR%" -Installed "%DEPLOY_JAR%"
if errorlevel 1 goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\check-artwork.ps1" -Profile "%ARTWORK_PROFILE%" -Archive "%BUILD_JAR%" -Fingerprints "%FINGERPRINTS%"
if errorlevel 1 goto :fail
for %%U in (UnderwaterRegistry ItemOxygenTank TileAirCompressor ContainerAirCompressor GuiAirCompressor UnderwaterHooks) do (
    jar tf "%BUILD_JAR%" | findstr /L /X /C:"zed/tekkitoverhaul/exploration/underwater/%%U.class" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: Missing underwater feature class %%U in the archive.
        goto :fail
    )
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.class" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Mod entry point was not found in the JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/gui/advanced_table.png" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: GUI texture was not found in the JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/models/foundry_rotor.obj" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Foundry OBJ model was not found in the JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/textures/foundry_rotor.png" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Foundry texture was not found in the JAR.
    goto :fail
)
for %%F in (gasoline kerosene diesel heavy_gas_oil) do (
    jar tf "%BUILD_JAR%" | findstr /L /C:"zed/textures/%%F_fluid.png" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: %%F fluid texture missing from JAR.
        goto :fail
    )
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/tekkitoverhaul/distillation/TileEntityDistillationReceiver.class" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: BuildCraft receiver missing from JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"mcmod.info" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: mcmod.info missing from JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/tekkitoverhaul/power/TileAlchemicalConverter.class" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Alchemical converter missing from JAR.
    goto :fail
)

echo [ZTO] 4/4 - Publishing the verified %ARTWORK_PROFILE% artifact pair...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\build-provenance.ps1" -Metadata "%BUILD_METADATA%" -Archive "%BUILD_JAR%"
if errorlevel 1 goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\build-provenance.ps1" -Metadata "%BUILD_METADATA%" -Archive "%WORK_DIR%\ZTOAgent.jar"
if errorlevel 1 goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\publish-zto-build.ps1" -CandidateDirectory "%WORK_DIR%" -ArtifactDirectory "%ARTIFACT_DIR%"
if errorlevel 1 goto :fail

if "%ARTWORK_PROFILE%"=="local-dev" (
    if defined ZTO_PRISM_MODS (
        powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\install-zto-prism.ps1" -ModsDirectory "%MODS_DIR%" -ArtifactDirectory "%ARTIFACT_DIR%"
        if errorlevel 1 goto :fail
    ) else (
        powershell.exe -NoProfile -ExecutionPolicy Bypass -File "zto-tools\install-zto-prism.ps1" -Instance "%INSTANCE%" -ArtifactDirectory "%ARTIFACT_DIR%"
        if errorlevel 1 goto :fail
    )
)

echo.
echo [ZTO] SUCCESS: Verified %ARTWORK_PROFILE% build.
echo [ZTO] JAR: "%ARTIFACT_DIR%\%JAR_NAME%"
echo [ZTO] Compatibility launcher artifact: "%ARTIFACT_DIR%\ZTOAgent.jar"
if "%ARTWORK_PROFILE%"=="public" echo [ZTO] Public build completed without installing to Prism.
popd
exit /b 0

:compile_failed
echo [ZTO] ERROR: MCP did not complete compilation. See dist-zto\mcp-compile.log.
:fail
echo.
echo [ZTO] OPERATION STOPPED. The new JAR was not deployed successfully.
popd
exit /b 1
