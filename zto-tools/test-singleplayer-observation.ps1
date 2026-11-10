[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('bed-sleep', 'crafting-5x5')]
    [string]$Scenario,
    [Parameter(Mandatory)]
    [string]$Evidence
)

$ErrorActionPreference = 'Stop'
$report = Get-Content -LiteralPath $Evidence -Raw | ConvertFrom-Json
$before = $report.before
$after = $report.after
if (!$before -or !$after -or $report.buildSha256 -notmatch '^[0-9a-fA-F]{64}$') {
    throw 'Evidence requires before and after snapshots and a build hash'
}

function Count-Stack($Inventory, [int]$ItemId) {
    [int](($Inventory.slots | Where-Object {
        $_ -and $_.itemId -eq $ItemId -and $_.damage -eq 0 -and !$_.tagged
    } | Measure-Object -Property count -Sum).Sum)
}

$checks = [ordered]@{
    sameWorld = $before.worldId -gt 0 -and $before.worldId -eq $after.worldId
}

if ($Scenario -eq 'bed-sleep') {
    $events = @($report.events.events | Where-Object { $null -ne $_ })
    $event = if ($events.Count -eq 1) { $events[0] }
    $checks.sameCalendar = $before.sessionId -gt 0 -and $before.sessionId -eq $after.sessionId
    $checks.nextDay = $before.night -eq $true -and $after.night -eq $false -and $after.logicalDay -eq ($before.logicalDay + 1)
    $checks.nextPhase = $after.phaseId -eq (($before.phaseId + 1) % 8)
    $checks.singleResync = $report.events.worldId -eq $before.worldId -and $report.events.dropped -eq 0 -and
        $event.kind -eq 'RESYNC' -and $event.cause -eq 'TIME_SKIP' -and $event.sequence -eq ($before.latestSequence + 1)
    $checks.vanillaDawn = $event.before.rawTime -ge $before.rawTime -and $event.after.rawTime -le $after.rawTime -and
        $event.after.rawTime -gt ($event.before.rawTime + 1) -and ($event.after.rawTime % 24000) -le 1 -and
        $event.dayDelta -eq 1 -and $event.after.phaseId -eq $after.phaseId
} else {
    $checks.fullInventories = $before.slots.Count -eq 40 -and $after.slots.Count -eq 40
    $checks.stoneConsumed = (Count-Stack $before 1) - (Count-Stack $after 1) -eq 16
    $checks.foundryCollected = (Count-Stack $after 3001) - (Count-Stack $before 3001) -eq 1
    $checks.cursorEmpty = $null -eq $before.cursor -and $null -eq $after.cursor
}

$passed = $false -notin $checks.Values
[ordered]@{
    scenario = $Scenario
    status = if ($passed) { 'PASS' } else { 'FAIL' }
    buildSha256 = $report.buildSha256
    assertions = $checks
} | ConvertTo-Json -Compress
if (!$passed) { exit 1 }
