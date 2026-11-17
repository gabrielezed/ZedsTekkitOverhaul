param(
    [string[]]$Java = @('java'),
    [string]$PackJava,
    [switch]$Pack
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$agent = (Resolve-Path -LiteralPath (Join-Path $root 'dist-zto/ZTOAgent.jar')).Path
$runtimeSource = Join-Path $root 'dist-zto/survival-hud-runtime'
$game = Join-Path $runtimeSource 'Tekkit Dev/minecraft'
$overlay = (Resolve-Path -LiteralPath (Join-Path $game 'bin/minecraft.jar')).Path
$parent = (Resolve-Path -LiteralPath (Join-Path $root 'temp')).Path
$id = "zto-agent-probe-$([guid]::NewGuid().ToString('N'))"
$out = New-Item -ItemType Directory -Path (Join-Path $parent $id)
try {
    & javac -source 8 -target 8 -cp $agent -d $out.FullName (Join-Path $PSScriptRoot 'zto-agent/AgentProbe.java')
    if ($LASTEXITCODE) { throw 'Agent probe compilation failed.' }
    foreach ($runtime in $Java) {
        & $runtime '-Xverify:all' "-javaagent:$agent" -cp $out.FullName zed.tekkitoverhaul.agent.AgentProbe archive $overlay
        if ($LASTEXITCODE) { throw "Standalone agent probe failed for $runtime" }
    }
    if ($Pack) {
        $candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
        $headless = @($out.FullName, $overlay, $candidate, $agent, (Join-Path $root 'jars/bin/lwjgl.jar'))
        $buildCraft = @('buildcraft-client-A-core-2.2.14.zip','buildcraft-client-B-transport-2.2.14.zip') |
            ForEach-Object { Join-Path $game "mods/$_" }
        $headlessSources = @('NaturalSpawnProbe.java','EnderkinBehaviorProbe.java','ThiefBehaviorProbe.java','IndustrialTheftProbe.java') |
            ForEach-Object { Join-Path $PSScriptRoot "zto-agent/$_" }
        & javac -source 8 -target 8 -cp (($headless + $buildCraft) -join ';') -d $out.FullName @headlessSources
        if ($LASTEXITCODE) { throw 'Natural spawning probe compilation failed.' }
        $launch = Get-Content -Raw (Join-Path $runtimeSource 'launch.json') | ConvertFrom-Json
        $classpath = @($out.FullName, $launch.classpath, $candidate)
        $mods = @(Get-ChildItem -LiteralPath (Join-Path $game 'mods') -File |
            Where-Object { $_.Extension -in '.jar','.zip' -and $_.Name -notlike 'ZedsTekkitOverhaul*.jar' })
        if (!$PackJava) { $PackJava = Join-Path (Split-Path $launch.java) 'java.exe' }
        Push-Location $out.FullName
        try {
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($classpath + $mods.FullName) -join ';') zed.tekkitoverhaul.agent.AgentProbe pack
            if ($LASTEXITCODE) { throw 'Pack ABI agent probe failed.' }
            $reduced = @($mods | Where-Object Name -in 'buildcraft-client-A-core-2.2.14.zip','industrialcraft-2-client_1.97.jar')
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($classpath + $reduced.FullName) -join ';') zed.tekkitoverhaul.agent.AgentProbe reduced
            if ($LASTEXITCODE) { throw 'Reduced pack ABI agent probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') NaturalSpawnProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Natural spawning comparison failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($headless + $buildCraft) -join ';') EnderkinBehaviorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Enderkin behavior probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') ThiefBehaviorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Thief behavior probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($headless + $buildCraft) -join ';') IndustrialTheftProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Industrial theft and Ender Trail probe failed.' }
        } finally { Pop-Location }
    }
} finally {
    $resolved = Get-Item -LiteralPath $out.FullName
    if ($resolved.Parent.FullName -eq $parent -and $resolved.Name -eq $id -and
        !$resolved.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $resolved.FullName -Recurse -Force
    }
}
