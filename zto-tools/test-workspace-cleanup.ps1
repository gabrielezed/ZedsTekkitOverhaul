$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')

$workspace = (Assert-RegularPath (Split-Path $PSScriptRoot)).FullName
$name = 'cleanup-check-' + [guid]::NewGuid().ToString('N')
$fixture = Join-Path $workspace $name
$null = New-Item -ItemType Directory -Path $fixture
$runtime = Join-Path $fixture 'dist-zto/survival-hud-runtime'
$fixtureTools = Join-Path $fixture 'zto-tools'
$fixtureHelpers = Join-Path $fixtureTools 'runtime-tests'
$cleaner = Join-Path $fixtureTools 'clean-workspace.ps1'
$junction = $null
$lock = $null

function Assert-Test([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
}

function New-SyntheticRun([string]$State, [bool]$Keep, [string]$Status) {
    $run = New-IsolatedRuntimeRun $runtime 'synthetic' $Keep
    $run.Manifest.state = $State
    $run.Result.status = $Status
    $nested = Join-Path $run.Path 'nested'
    $null = New-Item -ItemType Directory -Path $nested, $run.Evidence
    $run.Manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $run.Path '.zto-runtime.json')
    $run.Result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $run.Evidence 'result.json')
    [IO.File]::WriteAllBytes((Join-Path $nested 'data.bin'), [byte[]]::new(8192))
    $run
}

try {
    $null = New-Item -ItemType Directory -Path $fixtureHelpers
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'clean-workspace.ps1') -Destination $cleaner
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'runtime-tests/RuntimeArtifacts.ps1') -Destination $fixtureHelpers
    $passed = New-SyntheticRun complete $false PASS
    $failed = New-SyntheticRun complete $false FAIL
    $active = New-SyntheticRun active $false PASS
    $retained = New-SyntheticRun complete $true PASS
    $locked = New-SyntheticRun complete $false PASS
    $linked = New-SyntheticRun complete $false PASS
    $unknown = Join-Path $active.Runs ('0' * 32)
    $unknownGame = Join-Path $unknown 'survival-hud-runtime/Tekkit Dev/minecraft'
    $unmarkedName = 'ZTO-Test-' + ('e' * 32)
    $unmarkedSave = Join-Path $unknownGame "saves/$unmarkedName"
    $unmarkedFile = Join-Path $unmarkedSave 'level.dat'
    $personal = Join-Path $fixture 'normal-profile/saves/Personal'
    $null = New-Item -ItemType Directory -Path $unmarkedSave, (Join-Path $unknownGame 'harness'), $personal
    'unmarked data' | Set-Content -LiteralPath $unmarkedFile
    $legacyReports = Join-Path $fixture 'dist-zto/test-reports'
    'SABOTEUR PASS exercise' | Set-Content -LiteralPath (Join-Path $legacyReports "saboteur-exercise-$unmarkedName.txt")
    'sample output' | Set-Content -LiteralPath (Join-Path $legacyReports "saboteur-exercise-$unmarkedName.stderr.txt")
    $personalFile = Join-Path $personal 'level.dat'
    'protected' | Set-Content -LiteralPath $personalFile
    $junction = Join-Path $linked.Path 'nested/linked'
    $null = New-Item -ItemType Junction -Path $junction -Target $personal
    $lock = [IO.File]::Open((Join-Path $locked.Path 'nested/data.bin'), 'Open', 'Read', 'None')

    $audit = & $cleaner | ConvertFrom-Json
    Assert-Test ($audit.netBytes -eq 0 -and (Test-Path -LiteralPath $passed.Path)) 'Audit changed the fixture.'
    $cleaned = & $cleaner -Clean | ConvertFrom-Json
    Assert-Test (!(Test-Path -LiteralPath $passed.Path) -and !(Test-Path -LiteralPath $failed.Path)) 'A completed fixture survived.'
    Assert-Test ($cleaned.netBytes -gt 16384 -and $cleaned.netBytes -eq $cleaned.removedBytes) 'Incorrect byte accounting.'
    foreach ($preserved in $active.Path, $retained.Path, $locked.Path, $linked.Path, $unmarkedFile, $personalFile,
        (Join-Path $failed.Evidence 'result.json')) {
        Assert-Test (Test-Path -LiteralPath $preserved) "Deleted a protected fixture: $preserved"
    }
    Assert-Test ([bool]($cleaned.runs | Where-Object path -eq $locked.Path).reason) 'Locked fixture lacked a diagnosis.'
    $repeat = & $cleaner -Clean | ConvertFrom-Json
    Assert-Test ($repeat.netBytes -eq 0) 'Repeated cleanup changed the fixture.'
    $lock.Dispose()
    $lock = $null

    # The source is initially missing, so setup fails after writing the manifest.
    $partial = New-IsolatedRuntimeRun $runtime 'setup-check' $false
    try {
        $null = Initialize-IsolatedRuntimeGame $partial $runtime
        throw 'Unexpected fixture setup success.'
    } catch {
        Assert-Test ($partial.OwnsPath -and $_.Exception.Message -match 'Tekkit Dev') 'Setup failed at the wrong step.'
        $partial.Result.error = $_.Exception.Message
    } finally {
        Complete-IsolatedRuntimeRun $partial
    }
    Assert-Test (!(Test-Path -LiteralPath $partial.Path)) 'Setup failure left a game copy.'
    Assert-Test (Test-Path -LiteralPath (Join-Path $partial.Evidence 'result.json')) 'Setup failure lost its evidence.'

    $config = Join-Path $runtime 'Tekkit Dev/minecraft/config'
    $null = New-Item -ItemType Directory -Path $config
    'generated' | Set-Content -LiteralPath (Join-Path $config 'ZedsTekkitOverhaul-emc-baseline.tsv')
    'runtime=enabled' | Set-Content -LiteralPath (Join-Path $config 'mod.cfg')
    $compile = New-IsolatedRuntimeRun $runtime 'compile-check' $false
    try {
        $game = Initialize-IsolatedRuntimeGame $compile $runtime
        Assert-Test (!(Test-Path -LiteralPath (Join-Path $game 'config/ZedsTekkitOverhaul-emc-baseline.tsv'))) 'Copied a generated export.'
        Assert-Test (Test-Path -LiteralPath (Join-Path $game 'config/mod.cfg')) 'Dropped a runtime config.'
        throw 'Simulated compilation failure.'
    } catch {
        Assert-Test ($_.Exception.Message -eq 'Simulated compilation failure.') $_.Exception.Message
        $compile.Result.error = $_.Exception.Message
    } finally {
        Complete-IsolatedRuntimeRun $compile
    }
    Assert-Test (!(Test-Path -LiteralPath $compile.Path)) 'Compilation failure left a game copy.'

    $successes = foreach ($iteration in 1..3) {
        $run = New-IsolatedRuntimeRun $runtime 'retention-check' $false
        try {
            $null = Initialize-IsolatedRuntimeGame $run $runtime
            $log = Join-Path $run.Evidence 'probe.txt'
            [IO.File]::WriteAllText($log, ('o' * 80000) + "`nCONFIG PASS synthetic`n")
            $run.Result.status = 'PASS'
        } finally {
            Complete-IsolatedRuntimeRun $run | Out-Null
        }
        Assert-Test (!(Test-Path -LiteralPath $run.Path)) 'Success left a game copy.'
        $run
    }
    Assert-Test (!(Test-Path -LiteralPath (Join-Path $successes[0].Evidence 'probe.txt'))) 'Old success console was retained.'
    foreach ($run in $successes[1..2]) {
        Assert-Test ((Get-Item -LiteralPath (Join-Path $run.Evidence 'probe.txt')).Length -eq 65536) 'Console tail is not bounded.'
    }
    $executable = (Get-Process -Id $PID).Path
    $start = @{
        FilePath = $executable
        ArgumentList = '-NoProfile -NonInteractive -Command Start-Sleep -Seconds 30'
        WindowStyle = 'Hidden'
        PassThru = $true
    }
    $child = Start-Process @start
    try {
        Assert-Test (!(Wait-RuntimeClient $child 100) -and $child.HasExited) 'Timeout left the owned child running.'
    } finally {
        if (!$child.HasExited) {
            $child.Kill()
            $null = $child.WaitForExit(10000)
        }
        $child.Dispose()
    }
    Write-Output 'PASS workspace cleanup, fail-safe paths, setup failure, byte accounting and evidence retention.'
} finally {
    if ($lock) { $lock.Dispose() }
    if ($junction -and (Test-Path -LiteralPath $junction)) { [IO.Directory]::Delete($junction) }
    $resolved = Assert-RegularPath $fixture
    if ($resolved.Parent.FullName -ne $workspace -or $resolved.Name -cnotmatch '^cleanup-check-[0-9a-f]{32}$') {
        throw "Unexpected synthetic directory: $fixture"
    }
    Get-RuntimeTree $resolved | Out-Null
    Remove-Item -LiteralPath $resolved.FullName -Recurse -Force
}
