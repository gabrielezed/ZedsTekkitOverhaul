@echo off
setlocal
pushd "%~dp0.."
set "OUT=dist-zto\emc-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 1.6 -target 1.6 -cp "bin\minecraft;lib;lib\*;jars\bin\*" -d "%OUT%" zto-tools\emc-tests\EmcSelfTest.java
if errorlevel 1 goto :fail
java -cp "%OUT%;bin\minecraft;lib;lib\*;jars\bin\*" zed.tekkitoverhaul.emc.EmcSelfTest
if errorlevel 1 goto :fail
popd
exit /b 0

:fail
popd
exit /b 1
