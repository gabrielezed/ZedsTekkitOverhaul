@echo off
setlocal
cd /D "%~dp0.."
if not exist "dist-zto" mkdir "dist-zto"
if not exist "temp\zto-industrial-agent" mkdir "temp\zto-industrial-agent"
javac -XDignore.symbol.file -source 8 -target 8 -d "temp\zto-industrial-agent" "zto-tools\industrial-agent\IndustrialAgent.java"
if errorlevel 1 exit /b 1
jar cfm "dist-zto\ZTOIndustrialAgent.jar" "zto-tools\industrial-agent\manifest.mf" -C "temp\zto-industrial-agent" zed
exit /b %ERRORLEVEL%
