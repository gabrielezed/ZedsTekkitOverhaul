@echo off
setlocal
cd /D "%~dp0.."
if not exist "dist-zto" mkdir "dist-zto"
if not exist "temp\zto-lunar-agent" mkdir "temp\zto-lunar-agent"
javac -XDignore.symbol.file -source 8 -target 8 -d "temp\zto-lunar-agent" "zto-tools\lunar-agent\MoonAgent.java"
if errorlevel 1 exit /b 1
jar cfm "dist-zto\ZTOLunarAgent.jar" "zto-tools\lunar-agent\manifest.mf" -C "temp\zto-lunar-agent" zed
exit /b %ERRORLEVEL%
