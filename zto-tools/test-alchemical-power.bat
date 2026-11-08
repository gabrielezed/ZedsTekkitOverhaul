@echo off
setlocal
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\alchemical-power-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 1.6 -target 1.6 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\power\CollectorFace.java" ^
    "src\minecraft\zed\tekkitoverhaul\power\AlchemicalBuffer.java" ^
    "zto-tools\power-tests\AlchemicalBufferSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" zed.tekkitoverhaul.power.AlchemicalBufferSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0

:failed
popd
exit /b 1
