@echo off
setlocal EnableExtensions
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\obj-lightmap-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 7 -target 7 -d "%OUT%" ^
    "src\minecraft\zed\tekkitoverhaul\render\model\ObjModel.java" ^
    "src\minecraft\zed\tekkitoverhaul\render\model\ObjLoader.java" ^
    "src\minecraft\zed\tekkitoverhaul\render\client\ObjTessellatorRenderer.java" ^
    "zto-tools\render-tests\stubs\net\minecraft\src\Tessellator.java" ^
    "zto-tools\render-tests\stubs\org\lwjgl\opengl\GL11.java" ^
    "zto-tools\render-tests\ObjRenderLightmapSelfTest.java"
if errorlevel 1 goto :failed
java -cp "%OUT%" ObjRenderLightmapSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0

:failed
popd
exit /b 1
