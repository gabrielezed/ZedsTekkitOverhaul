@echo off
setlocal
cd /D "%~dp0.."
if not exist "dist-zto" mkdir "dist-zto"
if not exist "temp\zto-drag-agent" mkdir "temp\zto-drag-agent"
javac -XDignore.symbol.file -source 8 -target 8 -d "temp\zto-drag-agent" "zto-tools\drag-agent\MouseAgent.java"
if errorlevel 1 exit /b 1
jar cfm "dist-zto\ZTOInventoryDragAgent.jar" "zto-tools\drag-agent\manifest.mf" -C "temp\zto-drag-agent" zed
exit /b %ERRORLEVEL%
