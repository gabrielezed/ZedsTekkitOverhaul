$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$source = Join-Path $root 'src/minecraft/zed/tekkitoverhaul/territory'
$classes = Join-Path $root 'bin/minecraft'
$out = Join-Path $root 'dist-zto/territory-tests'
$null = New-Item -ItemType Directory -Path $out -Force
& javac -source 8 -target 8 -cp $classes -d $out (Join-Path $source 'TerritorialActivity.java') `
    (Join-Path $source 'ThreatAssessment.java') (Join-Path $source 'TerritorialWorldState.java') `
    (Join-Path $source 'TerritorialNbtInput.java') `
    (Join-Path $PSScriptRoot 'territory-tests/TerritorialSelfTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Territorial checks require compiled MCP classes for legacy NBT.' }
& java -cp "$out;$classes" zed.tekkitoverhaul.territory.TerritorialSelfTest
if ($LASTEXITCODE -ne 0) { throw 'Territorial checks failed.' }
