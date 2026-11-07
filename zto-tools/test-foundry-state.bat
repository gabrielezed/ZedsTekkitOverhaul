@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\foundry-state-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" "src\minecraft\zed\tekkitoverhaul\machine\FoundryRotorState.java" "zto-tools\state-tests\FoundryRotorStateSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" FoundryRotorStateSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
