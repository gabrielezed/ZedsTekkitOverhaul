param(
    [string] $PrismRoot = (Join-Path $env:APPDATA 'PrismLauncher'),
    [string] $ModJar
)
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    & cmd /d /c zto-tools\build-drag-agent.bat
    if ($LASTEXITCODE) { throw 'Drag agent build failed.' }
    $agent = (Resolve-Path 'dist-zto/ZTOInventoryDragAgent.jar').Path
    $out = (Resolve-Path 'temp/zto-drag-agent').Path
    $libraries = Join-Path $PrismRoot 'libraries'
    $pack = Join-Path $PrismRoot 'instances/Tekkit Dev/minecraft/bin/modpack.jar'
    $merged = Join-Path $PrismRoot 'instances/Tekkit Dev/minecraft/bin/minecraft.jar'
    $client = Join-Path $libraries 'com/mojang/minecraft/1.2.5/minecraft-1.2.5-client.jar'
    $lwjgl = Join-Path $libraries 'org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar'
    foreach ($artifact in @($pack, $client, $lwjgl)) {
        if (!(Test-Path -LiteralPath $artifact -PathType Leaf)) { throw "Missing artifact: $artifact" }
    }
    & javac -source 8 -target 8 -cp $agent -d $out 'zto-tools/drag-agent/MouseAgentProbe.java'
    if ($LASTEXITCODE) { throw 'Drag agent probe compilation failed.' }
    $java = Join-Path $PrismRoot 'java/jre-legacy/bin/java.exe'
    if (!(Test-Path -LiteralPath $java -PathType Leaf)) { $java = 'java' }
    if (Test-Path -LiteralPath $merged -PathType Leaf) {
        $classpath = "$out;$merged;$lwjgl"
    } else {
        # Prism removes the base JAR's signing metadata when creating the merged client.
        # The workstation JDK 8 rejects the legacy SHA-1 signatures, unlike Prism's older Java 8.
        $java = 'java'
        $classpath = "$out;$pack;$client;$lwjgl"
    }
    if ($ModJar) {
        $classpath += ';' + (Resolve-Path -LiteralPath $ModJar).Path
        & $java '-Xverify:all' "-javaagent:$agent" -cp $classpath MouseAgentProbe verify-mod
    } else {
        & $java '-Xverify:all' "-javaagent:$agent" -cp $classpath MouseAgentProbe
    }
    if ($LASTEXITCODE) { throw 'Production drag dispatcher verification failed.' }
} finally {
    Pop-Location
}
