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
        $launch = Get-Content -Raw (Join-Path $runtimeSource 'launch.json') | ConvertFrom-Json
        $classpath = @($out.FullName, $launch.classpath, (Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'))
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
        } finally { Pop-Location }
    }
} finally {
    $resolved = Get-Item -LiteralPath $out.FullName
    if ($resolved.Parent.FullName -eq $parent -and $resolved.Name -eq $id -and
        !$resolved.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $resolved.FullName -Recurse -Force
    }
}
