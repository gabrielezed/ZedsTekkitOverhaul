@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem ZedsTekkitOverhaul - MCP 1.2.5 build + PrismLauncher deployment.
rem Start from anywhere: the script resolves the MDK root from its own location.
rem Optional override: set "ZTO_PRISM_MODS=C:\path\to\instance\minecraft\mods"

pushd "%~dp0.." >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERROR: Unable to locate the MDK directory.
    exit /b 1
)

set "MODS_DIR=%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods"
if defined ZTO_PRISM_MODS set "MODS_DIR=%ZTO_PRISM_MODS%"
set "JAR_NAME=ZedsTekkitOverhaul.jar"
set "BUILD_JAR=dist-zto\%JAR_NAME%"
set "DEPLOY_JAR=%MODS_DIR%\%JAR_NAME%"

if not exist "recompile.bat" (
    echo [ZTO] ERROR: recompile.bat was not found in the MDK root.
    goto :fail
)
if not exist "reobfuscate.bat" (
    echo [ZTO] ERROR: reobfuscate.bat was not found in the MDK root.
    goto :fail
)
if not exist "%MODS_DIR%\" (
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
where jar.exe >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERROR: jar.exe was not found in PATH. Configure JDK 8.
    goto :fail
)

rem Avoid loading two versions of the same ModLoader mod.
for %%F in ("%MODS_DIR%\ZedsTekkitOverhaul*.jar") do (
    if exist "%%~fF" (
        if /I not "%%~nxF"=="%JAR_NAME%" (
            echo [ZTO] ERROR: Another ZedsTekkitOverhaul JAR was found in mods: %%~nxF
            echo [ZTO] Move it out of the mods directory before deploying.
            goto :fail
        )
    )
)

echo [ZTO] 1/4 - Compiling with MCP...
call "recompile.bat"
if errorlevel 1 (
    echo [ZTO] ERROR: recompile.bat failed.
    goto :fail
)

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

echo [ZTO] 4/4 - Deploying to PrismLauncher...
if exist "%DEPLOY_JAR%" (
    copy /Y "%DEPLOY_JAR%" "dist-zto\ZedsTekkitOverhaul.previous.jar" >nul
    if errorlevel 1 (
        echo [ZTO] ERROR: Failed to back up the previous JAR.
        goto :fail
    )
)
copy /Y "%BUILD_JAR%" "%DEPLOY_JAR%" >nul
if errorlevel 1 (
    echo [ZTO] ERROR: Failed to copy the JAR into PrismLauncher.
    goto :fail
)

echo.
echo [ZTO] SUCCESS: Build and deployment completed.
echo [ZTO] JAR: "%CD%\%BUILD_JAR%"
echo [ZTO] Prism: "%DEPLOY_JAR%"
echo [ZTO] Close Minecraft before starting the updated build.
popd
exit /b 0

:fail
echo.
echo [ZTO] OPERATION STOPPED. The new JAR was not deployed successfully.
popd
exit /b 1
