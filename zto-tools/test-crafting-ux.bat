@echo off
setlocal
cd /D "%~dp0.."
if not exist "bin\minecraft\zed\tekkitoverhaul\crafting\CraftingFill.class" (
    echo Compile the MCP client with recompile.bat first.
    exit /b 1
)
set "ZTO_UX_CLASSPATH=bin\minecraft;lib\*;jars\bin\*"
if not exist "temp\crafting-ux-test" mkdir "temp\crafting-ux-test"
javac -source 1.6 -target 1.6 -Xlint:-options -cp "%ZTO_UX_CLASSPATH%" -d "temp\crafting-ux-test" zto-tools\crafting-tests\CraftingUxTest.java
if errorlevel 1 exit /b 1
java -cp "temp\crafting-ux-test;%ZTO_UX_CLASSPATH%" zed.tekkitoverhaul.crafting.CraftingUxTest
exit /b %ERRORLEVEL%
