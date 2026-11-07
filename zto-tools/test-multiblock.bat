@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\multiblock-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\multiblock\StructurePattern.java" ^
    "src\minecraft\zed\tekkitoverhaul\multiblock\FoundryStructureValidator.java" ^
    "zto-tools\multiblock-tests\StructurePatternSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" StructurePatternSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
