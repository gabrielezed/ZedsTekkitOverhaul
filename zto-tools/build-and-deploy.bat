@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem ZedsTekkitOverhaul - MCP 1.2.5 build + PrismLauncher deployment.
rem Start from anywhere: the script resolves the MDK root from its own location.
rem Optional override: set "ZTO_PRISM_MODS=C:\path\to\instance\minecraft\mods"

pushd "%~dp0.." >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERRORE: impossibile trovare la directory MDK.
    exit /b 1
)

set "MODS_DIR=%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods"
if defined ZTO_PRISM_MODS set "MODS_DIR=%ZTO_PRISM_MODS%"
set "JAR_NAME=ZedsTekkitOverhaul.jar"
set "BUILD_JAR=dist-zto\%JAR_NAME%"
set "DEPLOY_JAR=%MODS_DIR%\%JAR_NAME%"

if not exist "recompile.bat" (
    echo [ZTO] ERRORE: recompile.bat non trovato nella root MDK.
    goto :fail
)
if not exist "reobfuscate.bat" (
    echo [ZTO] ERRORE: reobfuscate.bat non trovato nella root MDK.
    goto :fail
)
if not exist "%MODS_DIR%\" (
    echo [ZTO] ERRORE: cartella mods della Prism non trovata:
    echo "%MODS_DIR%"
    goto :fail
)
if not exist "src\minecraft\zed\gui\advanced_table.png" (
    echo [ZTO] ERRORE: manca src\minecraft\zed\gui\advanced_table.png
    goto :fail
)
where jar.exe >nul 2>&1
if errorlevel 1 (
    echo [ZTO] ERRORE: jar.exe non trovato nel PATH. Imposta il JDK 8.
    goto :fail
)

rem Avoid loading two versions of the same ModLoader mod.
for %%F in ("%MODS_DIR%\ZedsTekkitOverhaul*.jar") do (
    if exist "%%~fF" (
        if /I not "%%~nxF"=="%JAR_NAME%" (
            echo [ZTO] ERRORE: altro JAR ZedsTekkitOverhaul in mods: %%~nxF
            echo [ZTO] Spostalo fuori dalla cartella mods prima del deploy.
            goto :fail
        )
    )
)

echo [ZTO] 1/4 - Compilazione MCP...
call "recompile.bat"
if errorlevel 1 (
    echo [ZTO] ERRORE: recompile.bat non riuscito.
    goto :fail
)

rem Remove only previously reobfuscated ZTO classes, never vanilla/Forge code.
if exist "reobf\minecraft\zed\" (
    rmdir /s /q "reobf\minecraft\zed"
    if exist "reobf\minecraft\zed\" (
        echo [ZTO] ERRORE: impossibile pulire reobf\minecraft\zed
        goto :fail
    )
)

echo [ZTO] 2/4 - Reobfuscation MCP...
call "reobfuscate.bat"
if errorlevel 1 (
    echo [ZTO] ERRORE: reobfuscate.bat non riuscito.
    goto :fail
)
if not exist "reobf\minecraft\zed\tekkitoverhaul\mod_ZedsTekkitOverhaul.class" (
    echo [ZTO] ERRORE: entrypoint non presente nell'output reobfuscato.
    goto :fail
)

if not exist "dist-zto\" mkdir "dist-zto"
if not exist "dist-zto\" (
    echo [ZTO] ERRORE: impossibile creare dist-zto.
    goto :fail
)

echo [ZTO] 3/4 - Creazione JAR e inclusione texture GUI...
if exist "%BUILD_JAR%" del /q "%BUILD_JAR%"
jar cf "%BUILD_JAR%" -C "reobf\minecraft" zed
if errorlevel 1 (
    echo [ZTO] ERRORE: creazione JAR fallita.
    goto :fail
)
jar uf "%BUILD_JAR%" -C "src\minecraft" zed\gui
if errorlevel 1 (
    echo [ZTO] ERRORE: aggiunta asset al JAR fallita.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.class" >nul
if errorlevel 1 (
    echo [ZTO] ERRORE: entrypoint non trovato nel JAR.
    goto :fail
)
jar tf "%BUILD_JAR%" | findstr /L /C:"zed/gui/advanced_table.png" >nul
if errorlevel 1 (
    echo [ZTO] ERRORE: texture GUI non trovata nel JAR.
    goto :fail
)

echo [ZTO] 4/4 - Deploy nella Prism...
if exist "%DEPLOY_JAR%" (
    copy /Y "%DEPLOY_JAR%" "dist-zto\ZedsTekkitOverhaul.previous.jar" >nul
    if errorlevel 1 (
        echo [ZTO] ERRORE: backup del JAR precedente fallito.
        goto :fail
    )
)
copy /Y "%BUILD_JAR%" "%DEPLOY_JAR%" >nul
if errorlevel 1 (
    echo [ZTO] ERRORE: copia nella Prism fallita.
    goto :fail
)

echo.
echo [ZTO] OK: build e deploy completati.
echo [ZTO] JAR: "%CD%\%BUILD_JAR%"
echo [ZTO] Prism: "%DEPLOY_JAR%"
echo [ZTO] Chiudi Minecraft prima di avviare la nuova build.
popd
exit /b 0

:fail
echo.
echo [ZTO] OPERAZIONE INTERROTTA. Nessun nuovo JAR installato con successo.
popd
exit /b 1
