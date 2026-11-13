@echo off
setlocal
pushd "%~dp0.." || exit /b 1
set "OUT=dist-zto\lunar-tests"
if not exist "%OUT%" mkdir "%OUT%"
javac -source 8 -target 8 -d "%OUT%" src\minecraft\zed\tekkitoverhaul\lunar\*.java src\minecraft\zed\tekkitoverhaul\assault\AssaultSchedule.java src\minecraft\zed\tekkitoverhaul\assault\PatrolSchedule.java src\minecraft\zed\tekkitoverhaul\assault\OutdoorApproach.java zto-tools\lunar-tests\*.java
if errorlevel 1 goto :failed
java -cp "%OUT%" LunarCalendarSelfTest
if errorlevel 1 goto :failed
java -cp "%OUT%" zed.tekkitoverhaul.assault.AssaultScheduleSelfTest
if errorlevel 1 goto :failed
java -cp "%OUT%" zed.tekkitoverhaul.assault.OutdoorApproachSelfTest
if errorlevel 1 goto :failed
popd
exit /b 0
:failed
popd
exit /b 1
