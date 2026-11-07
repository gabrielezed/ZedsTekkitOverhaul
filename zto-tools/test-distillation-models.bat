@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\distillation-model-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\render\model\ObjModel.java" ^
    "src\minecraft\zed\tekkitoverhaul\render\model\ObjLoader.java" ^
    "zto-tools\distillation-tests\DistillationModelsSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" DistillationModelsSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
