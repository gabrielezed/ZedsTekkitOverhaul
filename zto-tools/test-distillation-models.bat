@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
rem Historical entry point for Distillation rendering checks.
set "OUT=dist-zto\distillation-texture-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillationPartType.java" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillationLayout.java" ^
    "src\minecraft\zed\tekkitoverhaul\distillation\DistillationTextures.java" ^
    "zto-tools\distillation-tests\DistillationTexturesSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" zed.tekkitoverhaul.distillation.DistillationTexturesSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
