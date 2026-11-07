@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\distillation-layout-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillationPartType.java" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillationLayout.java" ^
    "zto-tools\distillation-tests\DistillationLayoutSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" DistillationLayoutSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
