[CmdletBinding()]
param(
    [ValidateSet('exercise', 'restart', 'stress', 'dimension', 'transport', 'transport-quit', 'transport-restart', 'combat', 'terrain',
        'industrial-item', 'industrial-liquid', 'industrial-engine', 'industrial-eu', 'industrial-redpower', 'industrial-ee2',
        'industrial-eu-quit', 'industrial-eu-restart', 'industrial-eu-counterplay', 'industrial-eu-peaceful',
        'industrial-eu-unload', 'industrial-eu-replacement', 'industrial-eu-mfe', 'industrial-eu-mfsu',
        'industrial-solar', 'industrial-solar-mv', 'industrial-solar-hv', 'industrial-ee2-cover', 'industrial-ee2-mk2',
        'industrial-ee2-mk3')]
    [string]$Mode = 'exercise',
    [switch]$Bridge,
    [switch]$WithoutOptionalMods,
    [switch]$WithIndustrialAgent,
    [switch]$DiscardGame,
    [string]$CandidateJar
)

$ErrorActionPreference = 'Stop'
if ($Bridge -and $Mode -notin 'exercise', 'transport') { throw 'Use -Bridge with exercise or transport.' }
if ($WithoutOptionalMods -and ($Mode.StartsWith('transport') -or $Mode.StartsWith('industrial'))) {
    throw 'This fixture requires the corresponding native mods.'
}
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
$game = New-IsolatedRuntimeGame $runtime -WithoutOptionalMods:$WithoutOptionalMods
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$deployedSource = if ($CandidateJar) { (Resolve-Path -LiteralPath $CandidateJar).Path } else { $candidate }
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
Copy-Item -LiteralPath $deployedSource -Destination $deployed -Force
$hash = (Get-FileHash -LiteralPath $deployed).Hash
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$harness = Join-Path $game 'harness'
$report = Join-Path $root 'dist-zto/test-reports'
$null = New-Item -ItemType Directory -Path $harness, $report -Force
& javac -cp "$($launch.classpath);$deployed;$game/mods/buildcraft-client-A-core-2.2.14.zip;$game/mods/industrialcraft-2-client_1.97.jar" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/SaboteurRuntimeProbe.java') (Join-Path $PSScriptRoot 'runtime-tests/SilentRuntimeAudio.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile against the installed pack ABI.' }

$id = [Guid]::NewGuid().ToString('N')
$save = "ZTO-Test-$id"
& (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$id" -LunarPhase NEW_MOON -SavesDirectory (Join-Path $game 'saves') -ToolsDirectory $harness
if ($LASTEXITCODE -ne 0) { throw 'Could not generate a disposable save.' }
[IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"),
    "$save -7184827518270584220`n", [Text.Encoding]::ASCII)
Set-RuntimeConfig $game ([ordered]@{ playerAssault = 'false'; lunarPatrols = 'false' })
$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
if ($Mode.StartsWith('industrial') -or $WithIndustrialAgent) {
    $industrialAgent = Join-Path $root 'dist-zto/ZTOIndustrialAgent.jar'
    if (!(Test-Path -LiteralPath $industrialAgent)) { throw 'Build ZTOIndustrialAgent.jar before running the industrial probes.' }
    $agents += ' "-javaagent:' + $industrialAgent + '"'
    Write-Output "SABOTEUR industrial agent SHA-256 $((Get-FileHash -LiteralPath $industrialAgent).Hash)"
}
$java = Join-Path (Split-Path $launch.java) 'java.exe'

function Read-ProbeBridge([Diagnostics.Process]$Process) {
    $cli = Join-Path $PSScriptRoot 'zed-dev.ps1'
    $windup = $cancelled = $applied = $expired = $blocked = $false
    $transport = $Mode -eq 'transport'
    $targetY = if ($transport) { 122 } else { 121 }
    $targetType = if ($transport) { 'BUILDCRAFT_RECEIVER' } else { 'HOPPER' }
    $deadline = [DateTime]::UtcNow.AddSeconds(95)
    while (!$Process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        $discovery = Get-ChildItem -LiteralPath $harness -Filter 'zto-dev-bridge-*.json' | Select-Object -First 1
        if ($discovery) {
            $reply = & $cli query saboteur.nearby -Discovery $discovery.FullName -Json | ConvertFrom-Json
            if ($reply.ok) {
                foreach ($mob in $reply.data.saboteurs) {
                    $windup = $windup -or ($mob.windupRemaining -gt 0 -and $null -ne $mob.target -and $mob.targetType -eq $targetType)
                    $cancelled = $cancelled -or ($mob.cancelled -gt 0)
                }
                $machine = & $cli query machine.inspect -Discovery $discovery.FullName -Json `
                    -WorldId $reply.data.worldId -X 40 -Y $targetY -Z 215 | ConvertFrom-Json
                if ($machine.ok) {
                    $applied = $applied -or ($machine.data.disruptionTicks -gt 0)
                    $expired = $expired -or ($applied -and $machine.data.disruptionTicks -eq 0)
                    $blocked = $blocked -or $machine.data.blockedOffers -gt 0
                }
                if ($windup -and $applied -and $expired -and (($transport -and $blocked) -or (!$transport -and $cancelled))) {
                    Write-Output "SABOTEUR Bridge observed $targetType windup, active lease, expiration, cancelled=$cancelled, blocked=$blocked"
                    return
                }
            }
        }
        Start-Sleep -Milliseconds 150
    }
    throw "Missing Bridge observation windup=$windup cancelled=$cancelled applied=$applied expired=$expired blocked=$blocked"
}

function Invoke-Probe([string]$Phase, [int]$ExpectedExit) {
    $stdout = Join-Path $report "saboteur-$Phase-$save.txt"
    $stderr = Join-Path $report "saboteur-$Phase-$save.stderr.txt"
    $enabled = $Bridge.IsPresent.ToString().ToLowerInvariant()
    $arguments = '-Xmx1G -Dzto.dev={7} -Dzto.devBridge.enabled={7} "-Djava.io.tmpdir={3}" "-Djava.library.path={0}" {1} -cp "{2};{3}" SaboteurRuntimeProbe$Bootstrap "{4}" {5} {6}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $save, $Phase, $enabled
    $process = Start-BackgroundMinecraft -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $bridgeFailure = $null
    try {
        if ($Bridge) { Read-ProbeBridge $process }
    } catch {
        $bridgeFailure = $_
    } finally {
        if (!(Wait-RuntimeClient $process 120000)) { throw "Probe timed out. See $stdout and $stderr" }
    }
    Get-Content -LiteralPath $stdout | Select-String '^SABOTEUR|MinecraftForge' | ForEach-Object Line
    if ($process.ExitCode -ne $ExpectedExit -or !(Select-String -LiteralPath $stdout -Pattern "^SABOTEUR PASS $Phase$" -Quiet)) {
        Get-Content -LiteralPath $stderr -Tail 30
        throw "Probe failed. See $stdout and $stderr"
    }
    if ($bridgeFailure) { throw $bridgeFailure }
}

Write-Output "SABOTEUR $Mode $save SHA-256 $hash"
if ($WithoutOptionalMods) { Write-Output 'SABOTEUR optional mods omitted from the disposable mods directory' }
Write-Output "SABOTEUR game $game"
try {
    if ($Mode -eq 'restart') {
        Invoke-Probe 'halt' 23
        Invoke-Probe 'reopen' 0
    } elseif ($Mode -eq 'transport-restart') {
        Invoke-Probe 'transport-halt' 23
        Invoke-Probe 'transport-reopen' 0
    } elseif ($Mode -in 'industrial-eu-quit', 'industrial-eu-restart') {
        if ($Mode -eq 'industrial-eu-quit') {
            Invoke-Probe 'industrial-eu-quit' 0
        } else {
            Invoke-Probe 'industrial-eu-halt' 23
        }
        Invoke-Probe 'industrial-eu-reopen' 0
    } else {
        Invoke-Probe $Mode 0
    }
    if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) { throw 'The deployed archive changed during the probe.' }
} finally {
    if ($DiscardGame) {
        $run = (Get-Item -LiteralPath $game).Parent.Parent.Parent
        $runs = (Resolve-Path -LiteralPath (Join-Path $runtime 'runs')).Path
        if ($run.Parent.FullName -ne $runs -or $run.Name -notmatch '^[a-f0-9]{32}$' -or
            $run.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
            throw "Unexpected disposable test path $($run.FullName)"
        }
        Remove-Item -LiteralPath $run.FullName -Recurse -Force
    }
}
