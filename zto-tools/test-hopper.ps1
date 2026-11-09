$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    New-Item -ItemType Directory -Force 'temp/hopper-tests' | Out-Null
    & javac -source 7 -target 7 -cp 'bin/minecraft;jars/bin/*' -d 'temp/hopper-tests' 'zto-tools/hopper-tests/HopperInventorySelfTest.java'
    if ($LASTEXITCODE -ne 0) { throw 'Hopper test compilation failed. Run recompile.bat first.' }
    & java -cp 'temp/hopper-tests;bin/minecraft;jars/bin/*' HopperInventorySelfTest
    if ($LASTEXITCODE -ne 0) { throw 'Hopper inventory assertions failed.' }
} finally { Pop-Location }
