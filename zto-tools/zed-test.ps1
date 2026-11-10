[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('list', 'run')]
    [string]$Action,
    [Parameter(Position = 1)]
    [ValidateSet('hopper.basic_transfer@1')]
    [string]$Scenario,
    [string]$Discovery = $env:ZTO_DEV_BRIDGE_DISCOVERY,
    [string]$ExpectedJarSha256,
    [switch]$DisposableWorld
)

$ErrorActionPreference = 'Stop'
$scenarioId = 'hopper.basic_transfer@1'
if ($Action -eq 'list') {
    @{ schema = 'zto.test-list@1'; scenarios = @($scenarioId) } | ConvertTo-Json -Compress
    exit 0
}

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
    if ($Op) { $raw = & $cli $Verb $Op -Discovery $Discovery -Json @Arguments }
    else { $raw = & $cli $Verb -Discovery $Discovery -Json }
    $reply = $raw | ConvertFrom-Json
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

function Read-Position([string]$Op, [int]$Y) {
    $position = @{ WorldId = $ctx.worldId; Dimension = $ctx.dimension; X = $ctx.x; Y = $Y; Z = $ctx.z }
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

try {
    if (!$Scenario -or !$DisposableWorld -or $ExpectedJarSha256 -notmatch '^[0-9a-fA-F]{64}$') {
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
    $result.environment = @{ mode = 'local_singleplayer'; worldId = $ctx.worldId; sessionId = $ctx.sessionId }
    $result.evidenceLevel = 'live-singleplayer'
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

    $inputId = [Guid]::NewGuid().ToString('N')
    $seeded = Invoke-Fixture test.input $created $inputId 4
    $replay = Invoke-Fixture test.input $created $inputId 4
    Assert-State 'retry_revision' $replay.revision $seeded.revision

    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        Start-Sleep -Milliseconds 125
        $postChest = Read-Position inventory.container $ctx.chestY
        $elapsed = $postChest.simulationTicks - $seeded.simulationTicks
        $moved = Count-Items $postChest
    } while ($moved -lt 4 -and $elapsed -lt 80 -and $watch.Elapsed.TotalSeconds -lt 10)
    $postHopper = Read-Position machine.inspect $ctx.y
    $result.after = @{ hopper = $postHopper; chest = $postChest; elapsedSimulationTicks = $elapsed }
    if ($elapsed -le 0 -or ($moved -lt 4 -and $elapsed -lt 80)) {
        throw 'NOT_READY: The world did not simulate enough ticks before the wall-clock deadline'
    }
    Assert-State 'within_tick_budget' ($elapsed -le 80) $true
    Assert-State 'chest_received' $moved 4
    Assert-State 'hopper_drained' (Count-Items $postHopper) 0
    Assert-State 'conservation' ($moved + (Count-Items $postHopper)) 4
    Assert-State 'no_drops' (Get-Context).drops 0

    $null = Invoke-Fixture fixture.reset (Get-Context) ([Guid]::NewGuid().ToString('N'))
    Assert-State 'reset_hopper' (Count-Items (Read-Position machine.inspect $ctx.y)) 0
    Assert-State 'reset_chest' (Count-Items (Read-Position inventory.container $ctx.chestY)) 0
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
$result | ConvertTo-Json -Depth 10 -Compress
exit $exitCode
