param([string]$JdkBin)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$source = Join-Path $root 'src/minecraft/zed/tekkitoverhaul/devbridge'
$test = Join-Path $PSScriptRoot 'dev-bridge-tests/DevBridgeTest.java'
$output = Join-Path $root 'dist-zto/dev-bridge-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$javac = 'javac'
$java = 'java'
if ($JdkBin) {
    $javac = Join-Path $JdkBin 'javac.exe'
    $java = Join-Path $JdkBin 'java.exe'
}

$lunar = @(Get-ChildItem (Join-Path (Split-Path $source) 'lunar') -Filter '*.java' -File | ForEach-Object FullName)
& $javac -d $output "$source/BridgeProtocol.java" "$source/BridgeDispatcher.java" "$source/BridgeServer.java" "$source/LunarObserver.java" @lunar $test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp $output zed.tekkitoverhaul.devbridge.DevBridgeTest
exit $LASTEXITCODE
