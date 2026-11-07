@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\distillation-core-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillateType.java" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\FractionationProcess.java" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\LvHeaterBuffer.java" ^
    "zto-tools\distillation-tests\DistillationCoreSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" DistillationCoreSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
