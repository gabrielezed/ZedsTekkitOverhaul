[CmdletBinding()]
param(
    [ValidateSet('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled', 'ender-first', 'ender-gibbous', 'ender-waves', 'ender-fallback', 'ender-cap', 'ender-peaceful', 'assault-recurrence', 'territory-legacy', 'territory-threat', 'territory-elevated', 'territory-both',
        'composition-disabled', 'composition-quiet', 'composition-ordinary', 'composition-elevated', 'composition-both',
        'composition-quiet-enderkin', 'composition-ordinary-enderkin', 'composition-elevated-enderkin',
        'composition-hard', 'composition-hard-iron', 'composition-hard-powered', 'composition-hard-open',
        'composition-hard-baseline-open', 'composition-hard-balanced', 'composition-hard-retreat', 'composition-quantum')]
    [string[]]$Modes = @('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled'),
    [string]$CandidateJar
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
$game = New-IsolatedRuntimeGame $runtime
$harness = Join-Path $game 'harness'
$report = Join-Path $root 'dist-zto/test-reports'
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
if ($CandidateJar) {
    $candidate = (Resolve-Path -LiteralPath $CandidateJar).Path
    Copy-Item -LiteralPath $candidate -Destination $deployed -Force
}
$hash = (Get-FileHash -LiteralPath $candidate).Hash
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
    throw 'Deploy the candidate into the isolated Tekkit Dev copy first.'
}
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$null = New-Item -ItemType Directory -Path $harness, $report -Force
$sources = 'Alpha6RuntimeProbe', 'SilentRuntimeAudio', 'AssaultRecurrenceProbe', 'TerritorialRuntimeProbe', 'AssaultPressureProbe' |
    ForEach-Object { Join-Path $PSScriptRoot "runtime-tests/$_.java" }
& javac -cp "$($launch.classpath);$candidate" -d $harness @sources
if ($LASTEXITCODE -ne 0) { throw 'Could not compile against the installed pack ABI.' }

$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$phase = @{ first = 'FIRST_QUARTER'; gibbous = 'WAXING_GIBBOUS'; full = 'FULL_MOON'; waves = 'FULL_MOON'; quiet = 'NEW_MOON'; disabled = 'FIRST_QUARTER' }
$phase['ender-first'] = 'FIRST_QUARTER'
$phase['ender-gibbous'] = 'WAXING_GIBBOUS'
$phase['ender-waves'] = 'FULL_MOON'
$phase['ender-fallback'] = 'WAXING_GIBBOUS'
$phase['ender-cap'] = 'WAXING_GIBBOUS'
$phase['ender-peaceful'] = 'WAXING_GIBBOUS'
$phase['assault-recurrence'] = 'FULL_MOON'
$phase['territory-legacy'] = $phase['territory-threat'] = $phase['territory-both'] = 'FULL_MOON'
$phase['territory-elevated'] = 'FULL_MOON'
. (Join-Path $PSScriptRoot 'runtime-tests/TerritoryHttp.ps1')
foreach ($mode in $Modes) {
    $composition = $mode.StartsWith('composition-')
    if ($composition) { $phase[$mode] = 'FULL_MOON' }
    $id = [Guid]::NewGuid().ToString('N')
    $save = "ZTO-Test-$id"
    & (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$id" -LunarPhase $phase[$mode] -SavesDirectory (Join-Path $game 'saves') -ToolsDirectory $harness
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate a disposable save.' }
    [IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"),
        "$save -7184827518270584220`n", [Text.Encoding]::ASCII)
    if ($mode -eq 'territory-threat') {
        $fresh = Join-Path $game "saves/$save-Fresh"
        $null = New-Item -ItemType Directory -Path $fresh
        [IO.File]::WriteAllText((Join-Path $fresh '.zto-disposable-test'), "$save-Fresh 911`n", [Text.Encoding]::ASCII)
    }
    $enabled = ($mode -ne 'disabled').ToString().ToLowerInvariant()
    $enderkin = ($mode.StartsWith('ender-') -or $mode.EndsWith('-enderkin') -or
        $mode -in 'territory-both', 'composition-both').ToString().ToLowerInvariant()
    $threat = ($mode -in 'territory-threat', 'territory-elevated', 'territory-both' -or
        ($composition -and $mode -ne 'composition-disabled')).ToString().ToLowerInvariant()
    $useBridge = $composition -or $mode -in 'territory-threat', 'territory-elevated'
    $bridgeFlag = $useBridge.ToString().ToLowerInvariant()
    Set-RuntimeConfig $game ([ordered]@{
        experimentalEnderkinEncounters = $enderkin
        experimentalTerritorialThreat = $threat
        lunarOutdoorLightPressure = $enabled
    })
    $arguments = '-Xmx1G -Dzto.dev={7} -Dzto.devBridge.enabled={7} "-Djava.io.tmpdir={4}" "-Djava.library.path={0}" {1} -cp "{2};{3}" Alpha6RuntimeProbe$Bootstrap "{4}" {5} {6}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $save, $mode, $bridgeFlag
    $stdout = Join-Path $report "alpha6-$mode-$save.txt"
    $stderr = Join-Path $report "alpha6-$mode-$save.stderr.txt"
    $java = Join-Path (Split-Path $launch.java) 'java.exe'
    $process = Start-BackgroundMinecraft -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    if ($useBridge) {
        $prefix = if ($composition) { 'alpha4' } else { 'alpha2' }
        Test-TerritoryHttp $process $game $save $mode $stdout (Join-Path $report "$prefix-http-$save.json")
    }
    $timedOut = !(Wait-RuntimeClient $process 180000)
    if (!$useBridge -and (Select-String -LiteralPath $stderr -Pattern 'Dev Bridge enabled. Discovery:' -Quiet)) {
        throw 'The probe found a listener announcement with Dev Bridge disabled.'
    }
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
