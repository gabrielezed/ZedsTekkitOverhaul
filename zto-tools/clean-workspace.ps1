[CmdletBinding()]
param([switch]$Clean)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'runtime-tests/RuntimeArtifacts.ps1')
$root = (Assert-RegularPath (Split-Path $PSScriptRoot)).FullName
$dist = Join-Path $root 'dist-zto'
$runs = Join-Path $dist 'survival-hud-runtime/runs'
$reports = Join-Path $dist 'test-reports'

function Get-WorkspaceFiles([IO.DirectoryInfo]$Directory) {
    foreach ($entry in $Directory.EnumerateFileSystemInfos()) {
        if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
        if ($entry -is [IO.DirectoryInfo]) { Get-WorkspaceFiles $entry }
        else { $entry }
    }
}

function Get-WorkspaceInventory {
    $files = @(Get-WorkspaceFiles ([IO.DirectoryInfo]$root))
    $groups = $files | Group-Object {
        $parts = $_.FullName.Substring($root.Length + 1).Split([IO.Path]::DirectorySeparatorChar)
        if ($parts.Count -gt 3 -and $parts[1] -eq 'survival-hud-runtime' -and $parts[0] -eq 'dist-zto') {
            $parts[0..2] -join '/'
        } elseif ($parts.Count -gt 2 -and $parts[0] -eq 'dist-zto') { $parts[0..1] -join '/' }
        else { $parts[0] }
    }
    @{
        bytes = [long]($files | Measure-Object Length -Sum).Sum
        paths = @($groups | ForEach-Object {
            @{ path = $_.Name; bytes = [long]($_.Group | Measure-Object Length -Sum).Sum }
        } | Sort-Object { $_.bytes } -Descending)
        largestFiles = @($files | Sort-Object Length -Descending | Select-Object -First 15 |
            ForEach-Object { @{ path = $_.FullName.Substring($root.Length + 1); bytes = $_.Length } })
    }
}

function Test-LegacyRunPassed($Run, $ReportsBySave) {
    # Old runs predate run manifests. Require the exact runner layout, fixture
    # markers and external success evidence for each save.
    $game = Join-Path $Run.FullName 'survival-hud-runtime/Tekkit Dev/minecraft'
    $saves = Join-Path $game 'saves'
    $expectedParents = @($Run.FullName, (Split-Path (Split-Path $game)), (Split-Path $game))
    $expectedChildren = @('survival-hud-runtime', 'Tekkit Dev', 'minecraft')
    for ($i = 0; $i -lt $expectedParents.Count; $i++) {
        $child = @(Get-ChildItem -LiteralPath $expectedParents[$i] -Force)
        if ($child.Count -ne 1 -or !$child[0].PSIsContainer -or $child[0].Name -cne $expectedChildren[$i]) { return $false }
    }
    if (!(Test-Path -LiteralPath (Join-Path $game 'harness') -PathType Container) -or
        !(Test-Path -LiteralPath $saves -PathType Container)) { return $false }
    $worlds = @(Get-ChildItem -LiteralPath $saves -Directory -Force)
    if (!$worlds.Count) { return $false }
    foreach ($world in $worlds) {
        if ($world.Name -cnotmatch '^ZTO-Test-[a-f0-9]{32}$') { return $false }
        $marker = Join-Path $world.FullName '.zto-disposable-test'
        if (!(Test-Path -LiteralPath $marker -PathType Leaf) -or
            [IO.File]::ReadAllText($marker) -cnotmatch ('\A' + $world.Name + ' -?[0-9]{1,19}\r?\n\z')) { return $false }
        $logs = @($ReportsBySave[$world.Name] | Where-Object { $_ })
        if (!$logs.Count) { return $false }
        $names = $logs.Name
        foreach ($pair in @(@('halt','reopen'), @('transport-halt','transport-reopen'),
            @('industrial-power-quit','industrial-power-reopen'), @('industrial-redpower-quit','industrial-redpower-reopen'))) {
            if ($names -contains "saboteur-$($pair[0])-$($world.Name).txt" -and
                $names -notcontains "saboteur-$($pair[1])-$($world.Name).txt") { return $false }
        }
        foreach ($log in $logs) {
            $null = $log.Name -match '^(alpha5|alpha6|alpha7|saboteur)-(.+)-ZTO-Test-[a-f0-9]{32}\.txt$'
            $family = $Matches[1].ToUpperInvariant()
            $phase = [regex]::Escape($Matches[2])
            $pass = if ($family -eq 'ALPHA5') { '^ALPHA5 PASS$' } else { "^$family PASS $phase$" }
            if (!(Test-Path -LiteralPath ($log.FullName -replace '\.txt$', '.stderr.txt') -PathType Leaf) -or
                !(Select-String -LiteralPath $log.FullName -Pattern $pass -Quiet)) { return $false }
        }
    }
    return $true
}

function Assert-NoRuntimeProcesses {
    $busy = @(Get-Process | Where-Object ProcessName -Match '^(javaw?|javac|prismlauncher|pythonw?|pypy|gradle|eclipse)$')
    if ($busy.Count) { throw "Close build or runtime processes before cleanup: $($busy.Id -join ', ')" }
}

$before = Get-WorkspaceInventory
$results = [Collections.Generic.List[object]]::new()
if (Test-Path -LiteralPath $runs) {
    $null = Assert-RegularPath $runs
    $bySave = @{}
    if (Test-Path -LiteralPath $reports) {
        $null = Assert-RegularPath $reports
        foreach ($log in Get-ChildItem -LiteralPath $reports -File) {
            if ($log.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
            if ($log.Name -cmatch '^(alpha5|alpha6|alpha7|saboteur)-.+-(ZTO-Test-[a-f0-9]{32})\.txt$') {
                $bySave[$Matches[2]] += @($log)
            }
        }
    }
    if ($Clean) { Assert-NoRuntimeProcesses }
    foreach ($item in Get-ChildItem -LiteralPath $runs -Force) {
        $result = [ordered]@{ path = $item.FullName; classification = 'UNKNOWN'; bytes = 0; removedBytes = 0; frames = $null; reason = $null }
        $results.Add($result)
        try {
            $run = Get-OwnedRunDirectory $runs $item.FullName
            $entries = @(Get-RuntimeTree $run)
            $result.bytes = [long]($entries | Where-Object { $_ -is [IO.FileInfo] } | Measure-Object Length -Sum).Sum
            $manifest = Join-Path $run.FullName '.zto-runtime.json'
            if (Test-Path -LiteralPath $manifest -PathType Leaf) {
                $data = Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
                if ($data.schema -cne 'zto.runtime@1' -or $data.id -cne $run.Name) { throw 'Invalid run manifest.' }
                if ($data.state -ne 'complete') { $result.classification = 'ACTIVE'; continue }
                if ($data.retain) { $result.classification = 'PROTECTED'; continue }
                $evidence = Join-Path $reports "runtime/$($run.Name)/result.json"
                $null = Assert-RegularPath $evidence
                $saved = Get-Content -LiteralPath $evidence -Raw | ConvertFrom-Json
                if ($saved.id -cne $data.id -or $saved.family -cne $data.family -or
                    $saved.status -notin 'PASS', 'FAIL') { throw 'Invalid completion evidence.' }
            } elseif (!(Test-LegacyRunPassed $run $bySave)) {
                $result.reason = 'Legacy completion or ownership unverified.'
                continue
            }
            $result.classification = 'OBSOLETE_DISPOSABLE'
            if ($Clean) {
                Assert-NoRuntimeProcesses
                Assert-RuntimeFilesClosed $entries
                $frames = Join-Path $run.FullName 'survival-hud-runtime/Tekkit Dev/minecraft/screenshots'
                if (!(Test-Path -LiteralPath $manifest) -and (Test-Path -LiteralPath $frames -PathType Container)) {
                    $archive = Join-Path $reports 'legacy-runtime-frames'
                    $null = New-Item -ItemType Directory -Path $archive -Force
                    $null = Assert-RegularPath $archive
                    $destination = Join-Path $archive $run.Name
                    if (Test-Path -LiteralPath $destination) { throw "Inspect the existing frame archive: $destination" }
                    Copy-Item -LiteralPath $frames -Destination $destination -Recurse
                    $result.frames = $destination
                }
                Remove-OwnedRunDirectory $runs $run.FullName
                $result.removedBytes = $result.bytes
            }
        } catch { $result.reason = $_.Exception.Message }
    }
}
$after = if ($Clean) { Get-WorkspaceInventory } else { $before }
[ordered]@{
    workspace = $root; mode = if ($Clean) { 'cleanup' } else { 'audit' }
    protected = @('.git', 'src', 'eclipse', 'forge', 'jars', 'runtime', 'tekkit-mods-decompiles',
        'dist-zto/ZedsTekkitOverhaul.jar', 'dist-zto/test-reports', 'all paths outside the run allowlist')
    before = $before; after = $after; netBytes = $before.bytes - $after.bytes
    eligibleBytes = [long]($results | Where-Object { $_.classification -eq 'OBSOLETE_DISPOSABLE' } |
        ForEach-Object { $_.bytes } | Measure-Object -Sum).Sum
    removedBytes = [long]($results | ForEach-Object { $_.removedBytes } | Measure-Object -Sum).Sum
    runs = $results.ToArray()
} | ConvertTo-Json -Depth 8
