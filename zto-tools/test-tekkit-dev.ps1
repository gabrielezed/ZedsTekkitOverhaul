[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^ZTO-Test-[a-f0-9]{32}$')]
    [string]$DisposableSave,
    [string]$PrismExe = (Join-Path $env:LOCALAPPDATA 'Programs/PrismLauncher/prismlauncher.exe'),
    [ValidateRange(15, 300)]
    [int]$StartupSeconds = 120,
    [ValidateRange(1, 720)]
    [int]$LunarWaitSeconds = 720,
    [ValidateSet('Hopper', 'Distillation', 'Assault', 'Patrol')]
    [string]$Persistence = 'Hopper',
    [switch]$PhysicalDistillation,
    [switch]$SkipCore,
    [switch]$InterruptAssault,
    [ValidateRange(0, 24)]
    [int]$AssaultMinimumSpawns = 1
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$prismData = Join-Path $env:APPDATA 'PrismLauncher'
$instance = Join-Path $prismData 'instances/Tekkit Dev'
$game = Join-Path $instance 'minecraft'
$nativeArgument = [regex]::Escape('-Djava.library.path=' + (Join-Path $instance 'natives').Replace('\', '/'))
$log = Join-Path $game 'ForgeModLoader-0.log'
$request = Join-Path $game 'zto-dev-autoload.request'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$copyName = 'ZTO-Test-' + [Guid]::NewGuid().ToString('N')
$reportDir = Join-Path $root 'dist-zto/test-reports'
$reportPath = Join-Path $reportDir "autonomy-$copyName.json"
$report = [ordered]@{
    schema = 'zto.tekkit-dev-lifecycle@1'; status = 'BLOCKED'; evidenceLevel = 'unattended'
    gitHead = (& git -C $root rev-parse HEAD); gitChanges = @(& git -C $root status --porcelain)
    buildSha256 = $null; build = $null; sourceSave = $DisposableSave; testSave = $copyName; worldSeed = $null
    launches = @(); suite = $null; crafting = $null; physicalDistillation = $null; assault = $null
    persistence = [ordered]@{ scenario = $Persistence; status = 'NOT_RUN'; checks = @() }
    error = $null; reportPath = $reportPath
}
$client = $null
$launcher = $null
$launch = $null
$payload = $null
$discovery = $null
$exitCode = 2

function Invoke-Bridge([string]$Verb, [string]$Op, [hashtable]$Arguments = @{}) {
    $cli = Join-Path $PSScriptRoot 'zed-dev.ps1'
    if ($Op) { $raw = & $cli $Verb $Op -Discovery $discovery -Json @Arguments }
    else { $raw = & $cli $Verb -Discovery $discovery -Json }
    $reply = $raw | ConvertFrom-Json
    if (!$reply.ok) { throw "$($reply.error.code): $($reply.error.message)" }
    $reply.data
}

function Get-OwnedClient {
    if (!$launch -or $client) { return }
    $match = Select-String -LiteralPath $log -ErrorAction SilentlyContinue -Pattern (
        '\[ZTO\] Autoload ' + $launch.nonce + ' PID (\d+) Discovery: (.+)$') | Select-Object -Last 1
    if (!$match) { return }
    $process = Get-Process -Id ([int]$match.Matches[0].Groups[1].Value) -ErrorAction SilentlyContinue
    if (!$process -or $process.ProcessName -notin 'java', 'javaw' -or $process.StartTime -lt $launch.started) {
        throw 'CLIENT_EXITED: The game process from the launch announcement is gone.'
    }
    $script:client = $process
    $null = $client.Handle
    $script:discovery = $match.Matches[0].Groups[2].Value.Trim()
    $launch.pid = $process.Id
    if ($process.Path) { $launch.javaVersion = (Get-Item -LiteralPath $process.Path).VersionInfo.ProductVersion }
}

function Get-TekkitDevJava {
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" | Where-Object {
        $_.CommandLine -and $_.CommandLine -match '\borg\.prismlauncher\.EntryPoint(?:\s|$)' -and
            $_.CommandLine.Replace('\', '/') -match ('(?:^|[\s"])' + $nativeArgument + '(?:[\s"]|$)')
    }
}

function Remove-OwnedRequest {
    if (!$payload -or !(Test-Path -LiteralPath $request)) { return }
    try {
        if ([IO.File]::ReadAllText($request) -ceq $payload) { Remove-Item -LiteralPath $request }
    } catch [IO.FileNotFoundException] {
        # The game consumed this request before cleanup.
    }
}

function Stop-DevClient {
    if (!$launch -or $launch.stop) { return }
    Get-OwnedClient
    if (!$client) {
        # A client with disabled flags or a failed bootstrap never announces a nonce.
        Remove-OwnedRequest
        $children = @(Get-TekkitDevJava | Where-Object { $_.CreationDate -ge $launch.started })
        if ($children.Count -eq 0) { $launch.stop = 'no_game_process'; return }
        if ($children.Count -ne 1) {
            $launch.stop = 'ambiguous'
            throw 'CLIENT_UNIDENTIFIED: Multiple new Tekkit Dev Java processes were found.'
        }
        $script:client = Get-Process -Id $children[0].ProcessId -ErrorAction SilentlyContinue
        if (!$client) { $launch.stop = 'exited_before_shutdown'; return }
        $null = $client.Handle
        $launch.pid = $client.Id
    }
    $client.Refresh()
    if ($client.HasExited) {
        $launch.stop = 'exited_before_shutdown'
        throw 'CLIENT_EXITED: The game exited before the orchestrator requested shutdown.'
    }
    $requested = $launch.worldOpened -and !(Test-Path -LiteralPath $request)
    if ($requested) {
        Write-Request "$($launch.nonce) stop`n"
        $stopped = $client.WaitForExit(30000)
    } else {
        $stopped = $client.CloseMainWindow() -and $client.WaitForExit(15000)
    }
    $launch.exitCode = if ($client.HasExited) { $client.ExitCode } else { $null }
    $acknowledged = !$requested -or (Select-String -LiteralPath $log -Quiet -Pattern (
        '\[ZTO\] Autoload ' + $launch.nonce + ' STOP_REQUESTED$'))
    if ($stopped -and $launch.exitCode -eq 0 -and $acknowledged -and
        (!$discovery -or !(Test-Path -LiteralPath $discovery))) {
        $launch.stop = 'graceful'
        # Prism's CLI launcher can still be winding down after the Java child exits.
        if ($launcher -and !$launcher.WaitForExit(3000)) { $launch.launcherStillRunning = $true }
        Start-Sleep -Seconds 2
    } else {
        if (!$client.HasExited) {
            $client.Kill()
            if (!$client.WaitForExit(10000)) { throw 'STOP_TIMEOUT: The owned game process did not exit.' }
            $launch.stop = 'forced'
        } else {
            $launch.stop = 'unverified_exit'
        }
        throw 'UNVERIFIED_SHUTDOWN: Restart requires a clean client exit. Use a fresh disposable copy.'
    }
}

function Write-Request([string]$Content) {
    $stream = [IO.File]::Open($request, 'CreateNew', 'Write', 'None')
    $script:payload = $Content
    try {
        $bytes = [Text.Encoding]::ASCII.GetBytes($Content)
        $stream.Write($bytes, 0, $bytes.Length)
    } finally { $stream.Dispose() }
}

function Start-DevClient {
    $script:client = $null
    $script:discovery = $null
    $script:launch = [ordered]@{
        nonce = [Guid]::NewGuid().ToString('N'); started = Get-Date; pid = $null; stop = $null
        sessionId = $null; worldId = $null; worldOpened = $false; runtime = @(); log = $log; diagnostics = @()
    }
    $report.launches += $launch
    Write-Request "$($launch.nonce) $copyName $($report.buildSha256)`n"
    $script:launcher = Start-Process -FilePath $PrismExe -ArgumentList "--dir `"$prismData`" --launch `"Tekkit Dev`"" `
        -WindowStyle Hidden -PassThru

    $deadline = [Diagnostics.Stopwatch]::StartNew()
    $openedPattern = '\[ZTO\] Autoload ' + $launch.nonce + ' WORLD_OPENED ' + $copyName + '$'
    while ($deadline.Elapsed.TotalSeconds -lt $StartupSeconds) {
        Get-OwnedClient
        if ($client -and $client.HasExited) { throw 'CLIENT_EXITED: Tekkit Dev exited during startup.' }
        if ($discovery -and (Test-Path -LiteralPath $discovery)) {
            $launch.runtime = @(Select-String -LiteralPath $log -Pattern 'Forge Mod Loader version|MinecraftForge v' |
                ForEach-Object { $_.Line })
            $refusal = Select-String -LiteralPath $log -Pattern (
                '\[ZTO\] Autoload ' + $launch.nonce + ' (BUILD_MISMATCH|SAVE_NOT_FOUND|INVALID_SAVE_BACKUP|TEST_WORLD_REQUIRED)$')
            if ($refusal) { throw $refusal.Line }
            if (Select-String -LiteralPath $log -Pattern $openedPattern -Quiet) {
                $launch.worldOpened = $true
                try {
                    $status = Invoke-Bridge status
                    if ($status.state -eq 'ready') {
                        $context = Invoke-Bridge query test.context
                        if (!$context.allowed -or $context.modJarSha256 -ne $report.buildSha256) {
                            throw 'TEST_CONTEXT_REJECTED: The loaded disposable world or runtime JAR failed verification.'
                        }
                        $launch.modVersion = $status.modVersion
                        if ($report.build -and $launch.modVersion -cne $report.build.productVersion) {
                            throw 'BUILD_MISMATCH: Running product version differs from the packaged version.'
                        }
                        if ($Persistence -in 'Assault', 'Patrol') { $null = Invoke-Bridge query lunar.snapshot }
                        $launch.sessionId = $context.sessionId
                        $launch.worldId = $context.worldId
                        return $context
                    }
                } catch {
                    if ($_.Exception.Message -notmatch '^(CONNECTION_FAILED|NOT_READY|BUSY|TIMEOUT):') { throw }
                }
            }
        }
        Start-Sleep -Milliseconds 500
    }
    throw 'START_TIMEOUT: Tekkit Dev did not reach a verified ready world.'
}

function Invoke-Fixture([string]$Op, $Context, [long]$RunId) {
    $arguments = @{ WorldId = $Context.worldId; SessionId = $Context.sessionId; Revision = $Context.revision; RunId = $RunId }
    Invoke-Bridge command $Op $arguments
}

function Read-ArenaMachine($Context) {
    Invoke-Bridge query machine.inspect @{ WorldId = $Context.worldId; X = $Context.x; Y = $Context.y; Z = $Context.z }
}

function Wait-Distillation($Context, [int]$Progress) {
    $deadline = [Diagnostics.Stopwatch]::StartNew()
    do {
        $snapshot = Read-ArenaMachine $Context
        if ($snapshot.formed -and $snapshot.progressTicks -ge $Progress) { return $snapshot }
        Start-Sleep -Milliseconds 200
    } while ($deadline.Elapsed.TotalSeconds -lt 20)
    return $snapshot
}

function Test-PhysicalDistillation($Context, [long]$RunId) {
    $report.physicalDistillation = [ordered]@{ status = 'BLOCKED' }
    $setup = Invoke-Fixture 'distillation.transport' $Context $RunId
    $report.physicalDistillation.setup = $setup
    $position = @{ WorldId = $Context.worldId; X = $Context.x; Y = $Context.y; Z = $Context.z }
    $deadline = [Diagnostics.Stopwatch]::StartNew()
    $maxEnergy = 0
    do {
        $position.Y = $Context.y - 1
        $heater = Invoke-Bridge query machine.inspect $position
        $maxEnergy = [Math]::Max($maxEnergy, $heater.storedEU)
        $position.X = $Context.x + 1
        $position.Y = $Context.y + 1
        $position.Z = $Context.z + 2
        $output = Invoke-Bridge query machine.inspect $position
        if ($output.liquidMB -ge $setup.oilMB) { break }
        $position.X = $Context.x
        $position.Z = $Context.z
        Start-Sleep -Milliseconds 350
    } while ($deadline.Elapsed.TotalSeconds -lt 45)
    $input = Invoke-Bridge query machine.inspect @{
        WorldId = $Context.worldId; X = ($Context.x - 2); Y = $Context.y; Z = $Context.z
    }
    $boiler = Read-ArenaMachine $Context
    $checks = [ordered]@{
        inletTankEmpty = $input.liquidMB -eq 0; boilerCrudeConsumed = $boiler.crudeMB -eq 0
        coolantConsumed = $boiler.coolantMB -eq 0; receiverEmpty = $boiler.distillateMB -eq 0
        gasolineVolume = $output.liquidMB -eq $setup.oilMB; gasolineIdentity = $output.liquidId -eq $setup.gasolineId
        externalEU = $heater.energyNetJoined -and $maxEnergy -gt 0
    }
    $report.physicalDistillation.checks = $checks
    $report.physicalDistillation.inputTank = $input
    $report.physicalDistillation.outputTank = $output
    $report.physicalDistillation.boiler = $boiler
    $report.physicalDistillation.maxObservedHeaterEU = $maxEnergy
    if ($checks.Values -contains $false) {
        $report.physicalDistillation.status = 'FAIL'
        throw 'PHYSICAL_DISTILLATION_FAILED: See the pipe, boiler and external energy assertions.'
    }
    $report.physicalDistillation.status = 'PASS'
}

function Assert-Persistence([string]$Id, $Actual, $Expected) {
    $passed = $null -ne $Actual -and $Actual -ceq $Expected
    $report.persistence.checks += @{ id = $Id; actual = $Actual; expected = $Expected; pass = $passed }
    if (!$passed) { $report.persistence.status = 'FAIL'; throw "ASSERTION_FAILED: $Id" }
}

function Test-Restart($Context, [long]$RunId) {
    $report.persistence.status = 'BLOCKED'
    if ($Persistence -eq 'Hopper') {
        $null = Invoke-Fixture 'hopper.stage_reload' $Context $RunId
        $before = Read-ArenaMachine $Context
        Assert-Persistence 'seeded_count' $before.slots[0].count 4
        Assert-Persistence 'seeded_facing' $before.facing 2
        Assert-Persistence 'seeded_mode' $before.outputMode 'Input'
    } else {
        $created = Invoke-Fixture 'distillation.create' $Context $RunId
        $formed = Wait-Distillation $Context 0
        Assert-Persistence 'formed_before_restart' $formed.formed $true
        $started = Invoke-Fixture 'distillation.start_partial' $created $RunId
        $before = Wait-Distillation $Context $started.poweredTicks
        Assert-Persistence 'half_batch' $before.progressTicks $started.poweredTicks
        Assert-Persistence 'batch_pending' $before.distillateMB 0
    }
    $report.persistence.before = $before
    Stop-DevClient
    $reopened = Start-DevClient
    $after = if ($Persistence -eq 'Hopper') { Read-ArenaMachine $reopened } else { Wait-Distillation $reopened 0 }
    $report.persistence.after = $after
    Assert-Persistence 'new_session' ($reopened.sessionId -ne $Context.sessionId) $true
    Assert-Persistence 'arena_x' $reopened.x $Context.x
    Assert-Persistence 'arena_z' $reopened.z $Context.z
    if ($Persistence -eq 'Hopper') {
        foreach ($field in 'facing', 'outputMode') { Assert-Persistence $field $after.$field $before.$field }
        Assert-Persistence 'inventory_slots' (ConvertTo-Json -InputObject $after.slots -Compress) `
            (ConvertTo-Json -InputObject $before.slots -Compress)
    } else {
        foreach ($field in 'formed', 'progressTicks', 'crudeMB', 'coolantMB', 'distillateMB', 'fractionId') {
            Assert-Persistence $field $after.$field $before.$field
        }
    }
    $report.persistence.status = 'PASS'
}

function Test-AssaultRestart($Context, [long]$Boundary) {
    $report.persistence.status = 'BLOCKED'
    $raw = & (Join-Path $PSScriptRoot 'zed-test.ps1') run 'assault.natural_full_moon@1' -Discovery $discovery `
        -DisposableWorld -ExpectedJarSha256 $report.buildSha256 -LunarWaitSeconds $LunarWaitSeconds `
        -AssaultBoundary $Boundary -InterruptAssault:$InterruptAssault -AssaultMinimumSpawns $AssaultMinimumSpawns
    $report.assault = $raw | ConvertFrom-Json
    if ($report.assault.status -ne 'PASS') { throw "ASSAULT_SCENARIO_FAILED: $($report.assault.error)" }
    $before = Invoke-Bridge query assault.snapshot
    $report.persistence.before = $before
    Assert-Persistence 'pre_shutdown_state' $before.state $(if ($InterruptAssault) { 'ACTIVE' } else { 'RESOLVED' })
    Assert-Persistence 'no_full_moon_patrol' (Select-String -LiteralPath $log -Pattern '\[ZTO\] PATROL \[' -Quiet) $false
    Stop-DevClient
    $reopened = Start-DevClient
    $after = Invoke-Bridge query assault.snapshot
    $report.persistence.after = $after
    Assert-Persistence 'new_session' ($reopened.sessionId -ne $Context.sessionId) $true
    Assert-Persistence 'consumed_boundary' $after.boundary $before.boundary
    Assert-Persistence 'same_night' (Invoke-Bridge query lunar.snapshot).night $true
    Assert-Persistence 'no_new_assault' $after.starts 0
    Assert-Persistence 'no_new_spawns' $after.spawns.Count 0
    Assert-Persistence 'reopen_state' $after.state $(if ($InterruptAssault) { 'ABORTED' } else { 'RESOLVED' })
    Assert-Persistence 'reopen_reason' $after.reason $(if ($InterruptAssault) { 'INTERRUPTED_ON_REOPEN' } else { 'NIGHT_CONSUMED' })
    $report.persistence.status = 'PASS'
}

function Wait-LunarTick([long]$Tick) {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        try {
            $moon = Invoke-Bridge query lunar.snapshot
            if ($moon.rawTime -ge $Tick) { return $moon }
        } catch {
            if ($_.Exception.Message -notmatch '^(NOT_READY|BUSY|TIMEOUT):') { throw }
        }
        Start-Sleep -Milliseconds 500
    } while ($watch.Elapsed.TotalSeconds -lt $LunarWaitSeconds)
    throw "LUNAR_TIMEOUT: Did not reach tick $Tick."
}

function Test-PatrolRestart($Context) {
    $before = Wait-LunarTick 0
    $budget = switch ($before.phaseId) { 0 { 0 }; 2 { 2 }; 3 { 3 } }
    if ($null -eq $budget -or $before.timeOfDay -ge 13000) {
        throw 'PREREQUISITES: Start before dusk on a new moon, first quarter or waxing gibbous.'
    }
    $boundary = $before.rawTime - $before.timeOfDay + 13000
    $pattern = '\[ZTO\] PATROL \[(\d+)\] RESOLVED \| \w+ \| Spawned (\d+)/(\d+) \| Attempts (\d+)'
    $night = Wait-LunarTick ($boundary + 260)
    $events = Invoke-Bridge query lunar.events @{ WorldId = $before.worldId; AfterSequence = $before.latestSequence }
    $entries = @(Select-String -LiteralPath $log -Pattern '\[ZTO\] PATROL \[')
    $report.persistence.before = $before
    $report.persistence.night = $night
    $report.persistence.lunarEvents = $events
    $report.persistence.patrol = @($entries | ForEach-Object { $_.Line })
    Assert-Persistence 'no_cursor_loss' $events.dropped 0
    Assert-Persistence 'one_night_started' ($events.events.kind -join ',') 'NIGHT_STARTED'
    Assert-Persistence 'natural_boundary' ($events.events[0].cause -eq 'NATURAL' -and
        $events.events[0].after.rawTime -eq $boundary) $true
    Assert-Persistence 'same_phase_at_night' $night.phaseId $before.phaseId
    Assert-Persistence 'no_assault' (Invoke-Bridge query assault.snapshot).starts 0
    Assert-Persistence 'patrol_outcomes' $entries.Count $(if ($budget -gt 0) { 1 } else { 0 })
    if ($budget -gt 0) {
        $match = [regex]::Match($entries[0].Line, $pattern)
        Assert-Persistence 'resolved_patrol' $match.Success $true
        Assert-Persistence 'natural_dusk_and_delay' ([long]$match.Groups[1].Value) ($boundary + 240)
        $spawned = [int]$match.Groups[2].Value
        $attempts = [int]$match.Groups[4].Value
        Assert-Persistence 'patrol_budget' ([int]$match.Groups[3].Value) $budget
        Assert-Persistence 'bounded_spawns_and_attempts' ($spawned -gt 0 -and $spawned -le $budget -and
            $attempts -ge $spawned -and $attempts -le 24) $true
        Stop-DevClient
        $reopened = Start-DevClient
        $reload = Wait-LunarTick 0
        $report.persistence.after = Wait-LunarTick ($reload.rawTime + 260)
        Assert-Persistence 'new_session' ($reopened.sessionId -ne $Context.sessionId) $true
        Assert-Persistence 'same_night' ($report.persistence.after.night -and
            $report.persistence.after.rawTime -lt ($boundary + 10000)) $true
        Assert-Persistence 'no_replayed_patrol' (Select-String -LiteralPath $log -Pattern '\[ZTO\] PATROL \[' -Quiet) $false
    }
    $report.persistence.status = 'PASS'
}

$mutex = [Threading.Mutex]::new($false, 'Local\ZTO-Tekkit-Dev-Lifecycle')
$locked = $false
try {
    if ($Persistence -eq 'Patrol' -and (!$SkipCore -or $PhysicalDistillation)) {
        throw 'PREREQUISITES: Patrol requires -SkipCore and its own disposable run.'
    }
    if ($InterruptAssault -and ($Persistence -ne 'Assault' -or !$SkipCore)) {
        throw 'PREREQUISITES: InterruptAssault requires -Persistence Assault -SkipCore.'
    }
    if ($PhysicalDistillation -and $Persistence -eq 'Assault') {
        throw 'PREREQUISITES: Use a separate disposable run for physical distillation.'
    }
    try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (!$locked) { throw 'INSTANCE_BUSY: A Tekkit Dev lifecycle is already in progress.' }
    if (Test-Path -LiteralPath $request) { throw 'REQUEST_PRESENT: Inspect the pending Tekkit Dev autoload request before retrying.' }
    if (Get-TekkitDevJava) { throw 'INSTANCE_BUSY: Close the running Tekkit Dev client before starting the lifecycle.' }
    $report.buildSha256 = (Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash.ToLowerInvariant()
    $report.build = Get-Content -LiteralPath "$candidate.build.json" -Raw | ConvertFrom-Json
    if ($report.build.jarSha256 -cne $report.buildSha256) {
        throw 'BUILD_MISMATCH: The build provenance does not match the archive.'
    }
    $installed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
    if ((Get-FileHash -LiteralPath $installed -Algorithm SHA256).Hash -ne $report.buildSha256) {
        throw 'BUILD_MISMATCH: Deploy the candidate to Tekkit Dev before running the lifecycle.'
    }
    $saves = Join-Path $game 'saves'
    $source = Join-Path $saves $DisposableSave
    $target = Join-Path $saves $copyName
    foreach ($path in @($instance, $game, $saves, $source)) {
        $directory = Get-Item -LiteralPath $path -Force
        if (!$directory.PSIsContainer -or ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'TEST_WORLD_REQUIRED: The instance and disposable save must use regular directories.'
        }
    }
    if (Get-ChildItem -LiteralPath $source -Recurse -Force -Attributes ReparsePoint | Select-Object -First 1) {
        throw 'TEST_WORLD_REQUIRED: The disposable source contains linked files.'
    }
    $marker = [IO.File]::ReadAllText((Join-Path $source '.zto-disposable-test'), [Text.Encoding]::ASCII)
    if ($marker -cnotmatch ('\A' + $DisposableSave + ' (-?[0-9]{1,19})\n\z')) {
        throw 'TEST_WORLD_REQUIRED: The source needs its exact disposable marker.'
    }
    $seed = $Matches[1]
    $report.worldSeed = $seed
    Copy-Item -LiteralPath $source -Destination $target -Recurse -Force
    [IO.File]::WriteAllText((Join-Path $target '.zto-disposable-test'), "$copyName $seed`n", [Text.Encoding]::ASCII)

    $context = Start-DevClient
    $assaultBoundary = 0
    if ($Persistence -eq 'Assault' -and !$SkipCore) {
        $before = Invoke-Bridge query lunar.snapshot
        $null = Invoke-Fixture 'assault.prepare_full_moon' $context (Get-Random -Minimum 1 -Maximum ([long]1e15))
        $assaultBoundary = $before.rawTime - $before.timeOfDay + 13000
    }
    if (!$SkipCore) {
        $raw = & (Join-Path $PSScriptRoot 'zed-test.ps1') suite singleplayer-core -Discovery $discovery `
            -DisposableWorld -ExpectedJarSha256 $report.buildSha256 -LunarWaitSeconds $LunarWaitSeconds
        $report.suite = $raw | ConvertFrom-Json
        if ($report.suite.exitCode -ne 0) { throw 'CORE_SUITE_FAILED: See the scenario results.' }
        $context = Invoke-Bridge query test.context
    }

    $run = Get-Random -Minimum 1 -Maximum ([long]1e15)
    if (!$SkipCore -or $Persistence -notin 'Assault', 'Patrol') {
        $craft = Invoke-Fixture 'crafting.transactions' $context $run
        $report.crafting = [ordered]@{}
        foreach ($field in 'craft3', 'craft5', 'hotbarLock', 'hardProtection') {
            $report.crafting[$field] = if ($null -eq $craft.$field) { 'BLOCKED' } elseif ($craft.$field) { 'PASS' } else { 'FAIL' }
        }
        $context = Invoke-Bridge query test.context
    }
    if ($PhysicalDistillation) {
        Test-PhysicalDistillation $context $run
    } elseif ($Persistence -eq 'Assault') {
        Test-AssaultRestart $context $assaultBoundary
    } elseif ($Persistence -eq 'Patrol') {
        Test-PatrolRestart $context
    } else {
        Test-Restart $context $run
    }
    Stop-DevClient
    if ($report.crafting.Values -contains 'FAIL') { throw 'CRAFTING_FAILED: See the transaction results.' }
    $report.status = 'PASS'
    $exitCode = 0
} catch {
    $report.error = $_.Exception.Message
    if ($report.persistence.status -eq 'FAIL' -or $report.physicalDistillation.status -eq 'FAIL' -or
        ($report.suite.results.status -contains 'FAIL') -or $report.assault.status -eq 'FAIL' -or
        ($report.crafting.Values -contains 'FAIL')) {
        $report.status = 'FAIL'
        $exitCode = 1
    }
} finally {
    if ($launch -and !$launch.stop) {
        try { Stop-DevClient } catch { $launch.diagnostics += $_.Exception.Message }
    }
    try { Remove-OwnedRequest } catch { $report.cleanupError = $_.Exception.Message; $report.status = 'BLOCKED'; $exitCode = 2 }
    if ($report.status -ne 'PASS' -and $launch -and (Test-Path -LiteralPath $log)) {
        $launch.diagnostics += @(Select-String -LiteralPath $log -Pattern '\[ZTO\] Disposable world autoload|\[SEVERE\]|Exception in thread' |
            Select-Object -Last 12 | ForEach-Object { $_.Line })
    }
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
    $report | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath $reportPath -Encoding UTF8
}

@{ status = $report.status; buildSha256 = $report.buildSha256; reportPath = $reportPath; error = $report.error } |
    ConvertTo-Json -Compress
exit $exitCode
