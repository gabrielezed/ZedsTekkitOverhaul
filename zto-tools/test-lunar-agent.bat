@echo off
setlocal
cd /D "%~dp0.."
if not exist "temp\zto-lunar-agent" mkdir "temp\zto-lunar-agent"
javac -XDignore.symbol.file -source 8 -target 8 -d "temp\zto-lunar-agent" "zto-tools\lunar-agent\MoonAgent.java" "zto-tools\lunar-agent\MoonAgentProbe.java"
if errorlevel 1 exit /b 1
java -cp "temp\zto-lunar-agent" zed.tekkitoverhaul.lunaragent.MoonAgentProbe "bin\minecraft\net\minecraft\src\RenderGlobal.class" "jars\bin\minecraft.jar" %*
exit /b %ERRORLEVEL%
