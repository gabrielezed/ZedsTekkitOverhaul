@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
for %%P in (api core) do (
    if not exist "src\minecraft\buildcraft\%%P\" mkdir "src\minecraft\buildcraft\%%P"
    for %%F in ("zto-tools\buildcraft-dev-stubs\buildcraft\%%P\*.java") do (
        if not exist "src\minecraft\buildcraft\%%P\%%~nxF" (
            copy "%%~fF" "src\minecraft\buildcraft\%%P\%%~nxF" >nul
            if errorlevel 1 goto :failed
        )
    )
)
popd
exit /b 0
:failed
echo [ZTO] ERROR: Could not prepare BuildCraft 2.2.14 development placeholders.
popd
exit /b 1
