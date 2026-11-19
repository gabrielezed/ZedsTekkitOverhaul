[CmdletBinding()]
param(
    [ValidateSet('exercise', 'restart', 'stress', 'dimension', 'transport', 'transport-quit', 'transport-restart', 'combat', 'terrain',
        'smoke', 'theft-cable', 'industrial-item', 'industrial-redpower', 'industrial-redpower-quit',
        'industrial-power', 'industrial-power-quit',
        'direct-item', 'direct-liquid', 'direct-engine', 'direct-eu', 'direct-eu-mfe', 'direct-eu-mfsu',
        'direct-eu-quit', 'direct-eu-restart', 'direct-eu-counterplay', 'direct-eu-peaceful', 'direct-eu-unload',
        'direct-eu-replacement', 'direct-solar', 'direct-solar-mv', 'direct-solar-hv', 'direct-redpower',
        'direct-ee2', 'direct-ee2-mk2', 'direct-ee2-mk3', 'direct-ee2-cover')]
    [string]$Mode = 'exercise',
    [switch]$Bridge,
    [switch]$WithoutOptionalMods,
    [switch]$WithAgent,
    [switch]$KeepGame,
    [string]$CandidateJar
)

$ErrorActionPreference = 'Stop'
if ($Bridge -and $Mode -notin 'exercise', 'transport') { throw 'Use -Bridge with exercise or transport.' }
if ($WithoutOptionalMods -and ($Mode.StartsWith('transport') -or $Mode.StartsWith('industrial') -or $Mode.StartsWith('direct'))) {
    throw 'This fixture requires the corresponding native mods.'
}
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
Assert-BackgroundMinecraftAvailable
$run = New-IsolatedRuntimeRun $runtime 'saboteur' $KeepGame.IsPresent
try {
    $game = Initialize-IsolatedRuntimeGame $run $runtime -WithoutOptionalMods:$WithoutOptionalMods
    $candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
    $deployedSource = if ($CandidateJar) { (Resolve-Path -LiteralPath $CandidateJar).Path } else { $candidate }
    $deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
    Copy-Item -LiteralPath $deployedSource -Destination $deployed -Force
    $hash = (Get-FileHash -LiteralPath $deployed).Hash
    $run.Result.candidateSha256 = $hash
    $launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
    $harness = Join-Path $game 'harness'
    $report = $run.Evidence
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
    $java = Join-Path (Split-Path $launch.java) 'java.exe'
    $agentOption = ''
    if ($WithAgent -or $Mode.StartsWith('direct')) {
        $agent = Install-IsolatedZtoAgent $run $harness (Join-Path $root 'dist-zto/ZTOAgent.jar')
        $agentOption = " $agent -Dzto.test.agent=true"
    }

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
        $arguments = '-Xmx1G -Dzto.dev={6} -Dzto.devBridge.enabled={6} "-Djava.io.tmpdir={2}" "-Djava.library.path={0}"{7} -cp "{1};{2}" SaboteurRuntimeProbe$Bootstrap "{3}" {4} {5}' -f (Join-Path $runtime 'natives'), $launch.classpath, $harness, $game, $save, $Phase, $enabled, $agentOption
        $process = Start-IsolatedRuntimeClient -Run $run -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
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
    if ($Mode -eq 'restart') {
        Invoke-Probe 'halt' 23
        Invoke-Probe 'reopen' 0
    } elseif ($Mode -eq 'transport-restart') {
        Invoke-Probe 'transport-halt' 23
        Invoke-Probe 'transport-reopen' 0
    } elseif ($Mode -in 'industrial-power-quit', 'industrial-redpower-quit') {
        Invoke-Probe $Mode 0
        Invoke-Probe ($Mode -replace '-quit$', '-reopen') 0
    } elseif ($Mode -in 'direct-eu-quit', 'direct-eu-restart') {
        if ($Mode -eq 'direct-eu-quit') { Invoke-Probe $Mode 0 }
        else { Invoke-Probe 'direct-eu-halt' 23 }
        Invoke-Probe 'direct-eu-reopen' 0
    } else {
        Invoke-Probe $Mode 0
    }
    if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) { throw 'The deployed archive changed during the probe.' }
    $run.Result.status = 'PASS'
} catch {
    $run.Result.error = $_.Exception.Message
    throw
} finally {
    Complete-IsolatedRuntimeRun $run
}
