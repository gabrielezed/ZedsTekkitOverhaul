[CmdletBinding()]
param(
    [ValidateSet('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled', 'ender-first', 'ender-gibbous', 'ender-waves', 'ender-fallback', 'ender-cap', 'ender-peaceful')]
    [string[]]$Modes = @('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
$game = New-IsolatedRuntimeGame $runtime
$harness = Join-Path $game 'harness'
$report = Join-Path $root 'dist-zto/test-reports'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
$hash = (Get-FileHash -LiteralPath $candidate).Hash
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
    throw 'Deploy the candidate into the isolated Tekkit Dev copy first.'
}
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$null = New-Item -ItemType Directory -Path $harness, $report -Force
& javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/Alpha6RuntimeProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile against the installed pack ABI.' }

$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$phase = @{ first = 'FIRST_QUARTER'; gibbous = 'WAXING_GIBBOUS'; full = 'FULL_MOON'; waves = 'FULL_MOON'; quiet = 'NEW_MOON'; disabled = 'FIRST_QUARTER' }
$phase['ender-first'] = 'FIRST_QUARTER'
$phase['ender-gibbous'] = 'WAXING_GIBBOUS'
$phase['ender-waves'] = 'FULL_MOON'
$phase['ender-fallback'] = 'WAXING_GIBBOUS'
$phase['ender-cap'] = 'WAXING_GIBBOUS'
$phase['ender-peaceful'] = 'WAXING_GIBBOUS'
foreach ($mode in $Modes) {
    $id = [Guid]::NewGuid().ToString('N')
    $save = "ZTO-Test-$id"
    & (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$id" -LunarPhase $phase[$mode] -SavesDirectory (Join-Path $game 'saves') -ToolsDirectory $harness
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate a disposable save.' }
    [IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"),
        "$save -7184827518270584220`n", [Text.Encoding]::ASCII)
    $enabled = ($mode -ne 'disabled').ToString().ToLowerInvariant()
    $enderkin = $mode.StartsWith('ender-').ToString().ToLowerInvariant()
    Set-RuntimeConfig $game ([ordered]@{
        experimentalEnderkinEncounters = $enderkin
        lunarOutdoorLightPressure = $enabled
    })
    $arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2};{3}" Alpha6RuntimeProbe$Bootstrap "{4}" {5} {6}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $save, $mode
    $stdout = Join-Path $report "alpha6-$mode-$save.txt"
    $stderr = Join-Path $report "alpha6-$mode-$save.stderr.txt"
    $java = Join-Path (Split-Path $launch.java) 'java.exe'
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $timedOut = !(Wait-RuntimeClient $process 180000)
    Write-Output "ALPHA6 $mode $save SHA-256 $hash"
    Get-Content -LiteralPath $stdout | Select-String '^ALPHA6|MinecraftForge' | ForEach-Object Line
    Select-String -LiteralPath $stderr -Pattern 'Forge Mod Loader version|Forge Mod Loader load complete|\[ZTO\] PATROL' |
        ForEach-Object Line
    if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
        throw 'The deployed archive changed during the probe.'
    }
    if ($timedOut -or $process.ExitCode -ne 0 -or !(Select-String -LiteralPath $stdout -Pattern "^ALPHA6 PASS $mode$" -Quiet)) {
        Get-Content -LiteralPath $stderr -Tail 30
        throw "Runtime probe failed in $mode (timeout=$timedOut, exit=$($process.ExitCode)). See $stdout and $stderr."
    }
}
