[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('list', 'run', 'suite')]
    [string]$Action,
    [Parameter(Position = 1)]
    [ValidateSet('hopper.basic_transfer@1', 'hopper.destination_capacity@1', 'hopper.redstone_lock@1',
        'distillation.gasoline_batch@1', 'lunar.natural_boundary@1', 'singleplayer-core')]
    [string]$Scenario,
    [string]$Discovery = $env:ZTO_DEV_BRIDGE_DISCOVERY,
    [string]$ExpectedJarSha256,
    [ValidateRange(0, 720)]
    [int]$LunarWaitSeconds = 0,
    [switch]$DisposableWorld
)

$ErrorActionPreference = 'Stop'
$scenarios = @('lunar.natural_boundary@1', 'distillation.gasoline_batch@1', 'hopper.basic_transfer@1',
    'hopper.destination_capacity@1', 'hopper.redstone_lock@1')
if ($Action -eq 'list') {
    @{ schema = 'zto.test-list@1'; scenarios = $scenarios; suites = @{ 'singleplayer-core' = $scenarios } } |
        ConvertTo-Json -Depth 4 -Compress
    exit 0
}

if ($Action -eq 'suite') {
    if ($Scenario -ne 'singleplayer-core') { throw 'Specify suite singleplayer-core' }
    $results = @()
    $suiteExit = 0
    foreach ($scenarioId in $scenarios) {
        $raw = & $PSCommandPath run $scenarioId -Discovery $Discovery `
            -ExpectedJarSha256 $ExpectedJarSha256 -LunarWaitSeconds $LunarWaitSeconds -DisposableWorld:$DisposableWorld
        $scenarioExit = $LASTEXITCODE
        $results += ($raw | ConvertFrom-Json)
        if ($scenarioExit -ne 0 -and ($suiteExit -eq 0 -or $scenarioExit -lt $suiteExit)) {
            $suiteExit = $scenarioExit
        }
    }
    @{ schema = 'zto.test-suite@1'; suite = $Scenario; exitCode = $suiteExit; results = $results } |
        ConvertTo-Json -Depth 12 -Compress
    exit $suiteExit
}

$scenarioId = $Scenario
$runId = Get-Random -Minimum 1 -Maximum ([long]1e15)
$result = [ordered]@{
    schema = 'zto.test-result@1'
    scenario = $scenarioId
    runId = $runId
    status = 'ERROR'
    evidenceLevel = 'none'
    build = $null
    environment = $null
    assertions = @()
    cleanup = @{ status = 'not_started' }
}
$reportDir = Join-Path (Split-Path $PSScriptRoot) 'dist-zto/test-reports'
$reportPath = Join-Path $reportDir "$runId.json"
$ctx = $null
$attemptedCreate = $false
$exitCode = 2

function Invoke-Bridge([string]$Verb, [string]$Op, [hashtable]$Arguments = @{}) {
    $cli = Join-Path $PSScriptRoot 'zed-dev.ps1'
    for ($attempt = 0; $attempt -lt 4; $attempt++) {
        if ($Op) { $raw = & $cli $Verb $Op -Discovery $Discovery -Json @Arguments }
        else { $raw = & $cli $Verb -Discovery $Discovery -Json }
        $reply = $raw | ConvertFrom-Json
        if ($reply.ok -or $Verb -ne 'query' -or $reply.error.code -notin 'NOT_READY', 'BUSY', 'TIMEOUT') { break }
        if ($reply.error.code -eq 'NOT_READY') {
            $state = (& $cli status -Discovery $Discovery -Json | ConvertFrom-Json).data.state
            if ($state -notin 'stalled', 'ready') { break }
        }
        if ($attempt -lt 3) { Start-Sleep -Milliseconds 750 }
    }
    if (!$reply.ok) { throw ('{0}: {1}' -f $reply.error.code, $reply.error.message) }
    $reply.data
}

function Assert-State([string]$Id, $Actual, $Expected) {
    $passed = $null -ne $Actual -and $Actual -eq $Expected
    $result.assertions += @{ id = $Id; actual = $Actual; expected = $Expected; pass = $passed }
    if (!$passed) { throw "ASSERTION_FAILED: $Id" }
}

function Get-Context {
    $current = Invoke-Bridge query test.context
    if ($current.sessionId -ne $ctx.sessionId -or $current.worldId -ne $ctx.worldId) {
        throw 'STALE_WORLD: The test world or client changed'
    }
    if (!$current.allowed -or $current.dirty) { throw 'TEST_WORLD_REQUIRED: Test context is no longer usable' }
    $current
}

function Invoke-Fixture([string]$Op, $Context, [string]$RequestId, [int]$Count = 0) {
    $argsForCommand = @{
        WorldId = $Context.worldId; SessionId = $Context.sessionId
        RunId = $runId; Revision = $Context.revision; RequestId = $RequestId
    }
    if ($Op -eq 'test.input') { $argsForCommand.Count = $Count }
    Invoke-Bridge command $Op $argsForCommand
}

function Read-Position([string]$Op, [int]$Y, [int]$X = $ctx.x) {
    $position = @{ WorldId = $ctx.worldId; Dimension = $ctx.dimension; X = $X; Y = $Y; Z = $ctx.z }
    if ($Op -eq 'world.block') { $position.Remove('WorldId') }
    Invoke-Bridge query $Op $position
}

function Count-Items($Inventory) {
    $count = 0
    foreach ($stack in $Inventory.slots) {
        if ($null -eq $stack) { continue }
        if ($stack.itemId -ne $ctx.inputItemId -or $stack.damage -ne 0 -or $stack.tagged -ne $false) {
            Assert-State 'item_identity' $stack 'untagged fixture stone'
        }
        $count += $stack.count
    }
    $count
}

function Test-LunarBoundary {
    $before = Invoke-Bridge query lunar.snapshot
    $result.before = $before
    Assert-State 'lunar_world' $before.worldId $ctx.worldId
    Assert-State 'overworld_schedule' $before.schedulingEligible $true
    $boundary = if ($before.timeOfDay -lt 13000) { 13000 }
        elseif ($before.timeOfDay -lt 23000) { 23000 } else { 24000 }
    if ($boundary - $before.timeOfDay -gt 160 -and $LunarWaitSeconds -eq 0) {
        throw 'PREREQUISITES: Start within 160 ticks of a boundary or set -LunarWaitSeconds'
    }
    $target = $before.rawTime - $before.timeOfDay + $boundary
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $deadline = [Math]::Max(12, $LunarWaitSeconds)
    $after = $before
    $cursor = @{ WorldId = $before.worldId; AfterSequence = $before.latestSequence }
    do {
        $interval = if ($target - $after.rawTime -gt 160) { 1000 } else { 125 }
        Start-Sleep -Milliseconds $interval
        $after = Invoke-Bridge query lunar.snapshot
        if ($after.worldId -ne $before.worldId -or $after.sessionId -ne $before.sessionId) {
            throw 'STALE_WORLD: The lunar calendar changed ownership'
        }
        $result.after = $after
    } while ($after.rawTime -lt $target -and $watch.Elapsed.TotalSeconds -lt $deadline)
    if ($after.rawTime -lt $target) { throw 'NOT_READY: The clock did not reach the boundary' }

    # Observe several subsequent ticks before counting events to catch duplicate callbacks.
    $windowEnd = $after.rawTime + 5
    $watch.Restart()
    do {
        Start-Sleep -Milliseconds 125
        $after = Invoke-Bridge query lunar.snapshot
        if ($after.worldId -ne $before.worldId -or $after.sessionId -ne $before.sessionId) {
            throw 'STALE_WORLD: The lunar calendar changed ownership'
        }
    } while ($after.rawTime -lt $windowEnd -and $watch.Elapsed.TotalSeconds -lt 2)
    if ($after.rawTime -lt $windowEnd) { throw 'NOT_READY: The clock stalled after the boundary' }
    $events = Invoke-Bridge query lunar.events $cursor
    $end = Get-Context
    $result.after = @{ snapshot = $after; poll = $events; elapsedSimulationTicks = $end.simulationTicks - $ctx.simulationTicks }
    Assert-State 'no_cursor_loss' $events.dropped 0
    Assert-State 'after_boundary' ($after.rawTime -ge $target -and $after.rawTime -lt ($target + 1000)) $true
    $expectedKinds = switch ($boundary) {
        13000 { 'NIGHT_STARTED' }
        23000 { 'NIGHT_ENDED' }
        24000 {
            'DAY_STARTED'
            'PHASE_CHANGED'
            if ($before.phaseId -eq 7) { 'CYCLE_COMPLETED' }
        }
    }
    Assert-State 'event_order_and_count' ($events.events.kind -join ',') ($expectedKinds -join ',')
    foreach ($event in $events.events) {
        Assert-State "$($event.kind)_cause" $event.cause 'NATURAL'
        Assert-State "$($event.kind)_boundary" $event.after.rawTime $target
    }
    $phase = if ($boundary -eq 24000) { ($before.phaseId + 1) % 8 } else { $before.phaseId }
    Assert-State 'phase' $after.phaseId $phase
    Assert-State 'night' $after.night ($boundary -eq 13000)
}

function Test-DistillationBatch {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        $boiler = Read-Position machine.inspect $ctx.y
        $heater = Read-Position machine.inspect ($ctx.y - 1)
        $result.before = @{ boiler = $boiler; heater = $heater }
        if ($boiler.formed -and $heater.energyNetJoined) { break }
        Start-Sleep -Milliseconds 200
    } while ($watch.Elapsed.TotalSeconds -lt 4)
    Assert-State 'structure_formed' $boiler.formed $true
    Assert-State 'heater_online' $heater.energyNetJoined $true
    Assert-State 'initial_tanks' ($boiler.crudeMB + $boiler.coolantMB + $boiler.distillateMB) 0
    Assert-State 'initial_energy' $heater.storedEU 0
    Assert-State 'gasoline_selected' $boiler.fractionId 0

    # Independent expectations from the V1 gasoline specification.
    $started = Invoke-Fixture distillation.start (Get-Context) ([Guid]::NewGuid().ToString('N'))
    $result.before.input = $started
    Assert-State 'oil_accepted_mb' $started.crudeMB 250
    Assert-State 'water_accepted_mb' $started.coolantMB 40
    Assert-State 'energy_accepted_eu' $started.storedEU 1280
    $watch.Restart()
    do {
        Start-Sleep -Milliseconds 200
        $boiler = Read-Position machine.inspect $ctx.y
        $elapsed = $boiler.simulationTicks - $started.simulationTicks
    } while ($boiler.distillateMB -eq 0 -and $elapsed -lt 200 -and $watch.Elapsed.TotalSeconds -lt 14)
    $heater = Read-Position machine.inspect ($ctx.y - 1)
    $result.after = @{ boiler = $boiler; heater = $heater; elapsedSimulationTicks = $elapsed }
    Assert-State 'batch_duration_window' ($elapsed -ge 160 -and $elapsed -le 200) $true
    Assert-State 'oil_remaining_mb' $boiler.crudeMB 0
    Assert-State 'water_remaining_mb' $boiler.coolantMB 0
    Assert-State 'energy_remaining_eu' $heater.storedEU 0
    Assert-State 'product_mb' $boiler.distillateMB 250
    Assert-State 'product_fraction' $boiler.outputTypeId 0
    Assert-State 'progress_reset' $boiler.progressTicks 0
    $collected = Invoke-Fixture distillation.collect (Get-Context) ([Guid]::NewGuid().ToString('N'))
    $result.after.collection = $collected
    Assert-State 'registered_gasoline' ($collected.gasolineId -gt 0) $true
    Assert-State 'receiver_liquid' $collected.liquidId $collected.gasolineId
    Assert-State 'receiver_drained_mb' $collected.drainedMB 250
    Assert-State 'receiver_remaining_mb' $collected.remainingMB 0
}

try {
    if ($Scenario -notin $scenarios -or !$DisposableWorld -or $ExpectedJarSha256 -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'PREREQUISITES: Specify the scenario, -DisposableWorld and the candidate SHA-256'
    }
    $ctx = Invoke-Bridge query test.context
    if (!$ctx.allowed -or $ctx.dirty -or $ctx.active -or $null -eq $ctx.drops) {
        throw 'PREREQUISITES: Open a marked, idle disposable overworld with the arena chunk loaded'
    }
    if (!$ctx.modJarSha256 -or $ctx.modJarSha256 -ne $ExpectedJarSha256) {
        throw 'BUILD_MISMATCH: Running mod source hash differs from the expected candidate or is unavailable'
    }
    $result.build = @{ modJarSha256 = $ctx.modJarSha256 }
    $result.environment = Invoke-Bridge status
    $result.environment | Add-Member -NotePropertyName sessionId -NotePropertyValue $ctx.sessionId
    $result.reproduction = ".\zto-tools\zed-test.ps1 run $scenarioId -DisposableWorld -ExpectedJarSha256 $ExpectedJarSha256"
    $result.evidenceLevel = 'live-singleplayer'
    if ($scenarioId -eq 'lunar.natural_boundary@1') {
        $result.cleanup.status = 'not_applicable'
        if ($LunarWaitSeconds -gt 0) { $result.reproduction += " -LunarWaitSeconds $LunarWaitSeconds" }
        Test-LunarBoundary
    } elseif ($scenarioId -eq 'distillation.gasoline_batch@1') {
        Assert-State 'initial_drops' $ctx.drops 0
        $attemptedCreate = $true
        $null = Invoke-Fixture distillation.create $ctx ([Guid]::NewGuid().ToString('N'))
        Test-DistillationBatch
    } else {
    Assert-State 'initial_drops' $ctx.drops 0
    $attemptedCreate = $true
    $created = Invoke-Fixture fixture.create $ctx ([Guid]::NewGuid().ToString('N'))
    $preHopper = Read-Position machine.inspect $ctx.y
    $preChest = Read-Position inventory.container $ctx.chestY
    $result.before = @{ hopper = $preHopper; chest = $preChest }
    Assert-State 'hopper_type' $preHopper.type 'hopper'
    Assert-State 'chest_type' $preChest.type 'chest'
    Assert-State 'output_down' $preHopper.facing 0
    Assert-State 'unpowered' $preHopper.locked $false
    Assert-State 'hopper_empty' (Count-Items $preHopper) 0
    Assert-State 'chest_empty' (Count-Items $preChest) 0

    $capacity = $scenarioId -eq 'hopper.destination_capacity@1'
    $redstone = $scenarioId -eq 'hopper.redstone_lock@1'
    $initialChest = 0
    $expectedMoved = 4
    if ($capacity) {
        $created = Invoke-Fixture hopper.prepare_capacity (Get-Context) ([Guid]::NewGuid().ToString('N'))
        $preparedChest = Read-Position inventory.container $ctx.chestY
        $result.before.preparedChest = $preparedChest
        $initialChest = Count-Items $preparedChest
        Assert-State 'partial_capacity' $initialChest ($preparedChest.slots.Count * $preparedChest.stackLimit - 2)
        $expectedMoved = 2
    }
    if ($redstone) {
        $created = Invoke-Fixture hopper.power_on (Get-Context) ([Guid]::NewGuid().ToString('N'))
        Assert-State 'torch_locks_hopper' (Read-Position machine.inspect $ctx.y).locked $true
    }
    $inputId = [Guid]::NewGuid().ToString('N')
    $seeded = Invoke-Fixture test.input $created $inputId 4
    $replay = Invoke-Fixture test.input $created $inputId 4
    Assert-State 'retry_revision' $replay.revision $seeded.revision
    $transferStart = $seeded.simulationTicks
    if ($redstone) {
        $lockWatch = [Diagnostics.Stopwatch]::StartNew()
        do {
            Start-Sleep -Milliseconds 125
            $lockedChest = Read-Position inventory.container $ctx.chestY
            $lockedTicks = $lockedChest.simulationTicks - $seeded.simulationTicks
        } while ($lockedTicks -lt 40 -and $lockWatch.Elapsed.TotalSeconds -lt 8)
        $lockedHopper = Read-Position machine.inspect $ctx.y
        $result.lockedState = @{ hopper = $lockedHopper; chest = $lockedChest; elapsedSimulationTicks = $lockedTicks }
        if ($lockedTicks -lt 40) { throw 'NOT_READY: The lock observation did not reach 40 simulation ticks' }
        Assert-State 'still_locked' $lockedHopper.locked $true
        Assert-State 'powered_chest_empty' (Count-Items $lockedChest) 0
        Assert-State 'powered_source_unchanged' (Count-Items $lockedHopper) 4
        Assert-State 'no_powered_drops' (Get-Context).drops 0
        $unlocked = Invoke-Fixture hopper.power_off (Get-Context) ([Guid]::NewGuid().ToString('N'))
        Assert-State 'power_removed' (Read-Position machine.inspect $ctx.y).locked $false
        $transferStart = $unlocked.simulationTicks
    }

    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        Start-Sleep -Milliseconds 125
        $postChest = Read-Position inventory.container $ctx.chestY
        $elapsed = $postChest.simulationTicks - $transferStart
        $moved = (Count-Items $postChest) - $initialChest
    } while (($capacity -or $moved -lt 4) -and $elapsed -lt 80 -and $watch.Elapsed.TotalSeconds -lt 10)
    $postHopper = Read-Position machine.inspect $ctx.y
    $result.after = @{ hopper = $postHopper; chest = $postChest; elapsedSimulationTicks = $elapsed }
    if ($elapsed -le 0 -or (($capacity -or $moved -lt 4) -and $elapsed -lt 80)) {
        throw 'NOT_READY: The world did not simulate enough ticks before the wall-clock deadline'
    }
    if (!$capacity) { Assert-State 'within_tick_budget' ($elapsed -le 80) $true }
    Assert-State 'chest_received' $moved $expectedMoved
    Assert-State 'hopper_remaining' (Count-Items $postHopper) (4 - $expectedMoved)
    Assert-State 'conservation' ($moved + (Count-Items $postHopper)) 4
    Assert-State 'no_drops' (Get-Context).drops 0

    $null = Invoke-Fixture fixture.reset (Get-Context) ([Guid]::NewGuid().ToString('N'))
    Assert-State 'reset_hopper' (Count-Items (Read-Position machine.inspect $ctx.y)) 0
    Assert-State 'reset_chest' (Count-Items (Read-Position inventory.container $ctx.chestY)) 0
    }
    $result.status = 'PASS'
    $exitCode = 0
} catch {
    $result.error = $_.Exception.Message
    if ($result.error -like 'ASSERTION_FAILED:*') { $result.status = 'FAIL'; $exitCode = 1 }
    elseif ($result.error -like 'STALE_WORLD:*') { $result.status = 'ABORTED' }
    elseif ($result.error -like 'PREREQUISITES:*') { $result.status = 'SKIP'; $exitCode = 3 }
} finally {
    if ($attemptedCreate) {
        try {
            $current = Get-Context
            if ($current.active -and $current.runId -eq $runId) {
                $null = Invoke-Fixture fixture.destroy $current ([Guid]::NewGuid().ToString('N'))
                $afterCleanup = Get-Context
                $hopperBlock = Read-Position world.block $ctx.y
                $chestBlock = Read-Position world.block $ctx.chestY
                $null = Get-Context
                if ($afterCleanup.active -or $afterCleanup.drops -ne 0 -or
                    $hopperBlock.blockId -ne 0 -or $chestBlock.blockId -ne 0 -or
                    $hopperBlock.tilePresent -or $chestBlock.tilePresent) { throw 'Arena cleanup could not be verified' }
                if ($scenarioId -eq 'hopper.redstone_lock@1') {
                    foreach ($y in $ctx.y, $ctx.chestY) {
                        $powerCell = Read-Position world.block $y $ctx.powerX
                        if ($powerCell.blockId -ne 0 -or $powerCell.tilePresent) { throw 'Power source cleanup could not be verified' }
                    }
                    $null = Get-Context
                }
                $result.cleanup.status = 'complete'
            } elseif ($result.status -eq 'PASS') {
                throw 'Fixture ownership was lost before cleanup'
            } else {
                $result.cleanup.status = 'no_owned_fixture'
            }
        } catch {
            $result.cleanup = @{ status = 'incomplete'; error = $_.Exception.Message }
            if ($result.status -eq 'PASS') { $result.status = 'ERROR'; $exitCode = 2 }
        }
    }
}

try {
    $null = New-Item -ItemType Directory -Path $reportDir -Force
    $result.artifacts = @{ report = $reportPath }
    $result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $reportPath -Encoding UTF8
} catch {
    $result.status = 'ERROR'
    $result.reportError = 'Could not write the result artifact'
    $result.Remove('artifacts')
    $exitCode = 2
}
$result.Remove('before')
$result.Remove('after')
$result.Remove('lockedState')
$result | ConvertTo-Json -Depth 10 -Compress
exit $exitCode
