$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$source = Join-Path $root 'src/minecraft/zed/tekkitoverhaul'
$test = Join-Path $root 'dist-zto/config-tests'
$null = New-Item -ItemType Directory -Path $test -Force
& javac -source 8 -target 8 -d $test (Join-Path $source 'config/ZtoConfig.java') `
    (Join-Path $source 'config/ZtoConfigLoader.java') (Join-Path $source 'distillation/DistillateType.java') `
    (Join-Path $source 'distillation/DistillationPartType.java') (Join-Path $PSScriptRoot 'config-tests/ZtoConfigSelfTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Configuration probe did not compile.' }
& java -cp $test ZtoConfigSelfTest $test
if ($LASTEXITCODE -ne 0) { throw 'Configuration probe failed.' }
