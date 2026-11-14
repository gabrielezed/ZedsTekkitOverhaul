$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
$game = New-IsolatedRuntimeGame $runtime
$config = Join-Path $game 'config'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
$hash = (Get-FileHash -LiteralPath $candidate).Hash
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) { throw 'Deploy the candidate to the isolated copy first.' }
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$harness = Join-Path $game 'harness'
$null = New-Item -ItemType Directory -Path $harness
& javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/ConfigRuntimeProbe.java') (Join-Path $PSScriptRoot 'runtime-tests/SilentRuntimeAudio.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile the startup probe against the installed pack ABI.' }

$canonical = Join-Path $config 'ZedsTekkitOverhaul.cfg'
$mlProp = Join-Path $config 'mod_ZedsTekkitOverhaul.cfg'
$forgeBackup = "$canonical.legacy"
$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$java = Join-Path (Split-Path $launch.java) 'java.exe'

function Test-Launch([string]$Mode) {
    $arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2};{3}" ConfigRuntimeProbe "{4}" {5}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $Mode
    $stdout = Join-Path $game "config-$Mode.log"
    $stderr = Join-Path $game "config-$Mode.stderr.log"
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    if (!(Wait-RuntimeClient $process 90000) -or $process.ExitCode -ne 0 -or
            !(Select-String -LiteralPath $stdout -Pattern "^CONFIG PASS $Mode$" -Quiet)) {
        Get-Content -LiteralPath $stderr -Tail 50
        throw "Startup probe failed. See $stdout and $stderr"
    }
    Write-Output "CONFIG PASS $Mode SHA-256 $hash"
    Select-String -LiteralPath $stdout -Pattern '^CONFIG AUDIO ' | ForEach-Object Line
    if (Test-Path -LiteralPath $mlProp) { throw 'FML left or regenerated an active MLProp file.' }
}

Set-RuntimeConfig $game ([ordered]@{})
Remove-Item -LiteralPath $canonical
Test-Launch fresh
$files = @(Get-ChildItem -LiteralPath $config -File | Where-Object Name -Match '^(mod_)?ZedsTekkitOverhaul.*\.cfg')
if ($files.Count -ne 1 -or $files[0].Name -ne 'ZedsTekkitOverhaul.cfg') {
    throw 'Expected one ZTO settings file in a fresh profile.'
}

# Establish the stock pack's post-startup state before comparing unrelated configs.
$unrelated = @(Get-ChildItem -LiteralPath $config -Recurse -File |
    Where-Object Name -NotMatch '^(mod_)?ZedsTekkitOverhaul')
$contents = @{}
foreach ($file in $unrelated) { $contents[$file.FullName] = [IO.File]::ReadAllBytes($file.FullName) }
$legacyText = @'
playerAssault=false
lunarPatrols=false
lunarOutdoorLightPressure=false
experimentalEnderkinEncounters= TRUE
disableNightSkipping=false
assaultSpawnBudget=50
assaultWaveInterval=150
assaultActiveCap=5
assaultOuterRadius=21
creativeInventoryBackport=false
partialRecipeFill=true
craftingUxShortcut=34
lunarDebugKey=67
lunarVerboseLogging=true
survivalDebugHud=true
survivalDebugHudKey=64
survivalDebugLogCapacity=4
'@
[IO.File]::WriteAllText($mlProp, $legacyText, [Text.Encoding]::ASCII)
[IO.File]::WriteAllText($canonical, "# old Forge file`nblock {`nfoundry=3080`n}`n", [Text.Encoding]::ASCII)
[IO.File]::WriteAllText((Join-Path $config 'ZedsTekkitOverhaul-emc.txt'), "zto-emc 1 AUDIT`n", [Text.Encoding]::ASCII)
$oldHash = (Get-FileHash -LiteralPath $canonical).Hash
Test-Launch legacy
if ((Get-FileHash -LiteralPath $forgeBackup).Hash -ne $oldHash) { throw 'Forge legacy backup changed.' }
if ([IO.File]::ReadAllText("$mlProp.bak") -ne $legacyText) { throw 'FML backup changed.' }
$migratedHash = (Get-FileHash -LiteralPath $canonical).Hash
Test-Launch restart
if ((Get-FileHash -LiteralPath $canonical).Hash -ne $migratedHash) { throw 'Canonical settings were rewritten.' }

# The last assignment wins under Java Properties. Earlier comments and entries remain intact.
[IO.File]::AppendAllText($canonical, @"

experimentalEnderkinEncounters=false
disableNightSkipping=true
partialRecipeFill=false
craftingUxShortcut=0
creativeInventoryBackport=true
lunarDebugKey=0
lunarVerboseLogging=false
survivalDebugHud=false
survivalDebugHudKey=68
assaultSpawnBudget=invalid
assaultWaveInterval=900
assaultActiveCap=8
assaultOuterRadius=16
playerAssault=TrUe
lunarPatrols=true
lunarOutdoorLightPressure=true

"@, [Text.Encoding]::ASCII)
$editedHash = (Get-FileHash -LiteralPath $canonical).Hash
Test-Launch canonical
if ((Get-FileHash -LiteralPath $canonical).Hash -ne $editedHash) { throw 'Canonical user edits were rewritten.' }
if (!(Select-String -LiteralPath (Join-Path $game 'config-canonical.stderr.log') -Pattern 'Invalid assaultSpawnBudget' -Quiet)) {
    throw 'Expected an invalid-value warning in the FML log.'
}
$encoding = [Text.Encoding]::GetEncoding(28591)
$unversioned = [IO.File]::ReadAllText($canonical, $encoding) -creplace '(?m)^configVersion=1\r?\n', ''
[IO.File]::WriteAllText($canonical, $unversioned, $encoding)
$writeBlockedHash = (Get-FileHash -LiteralPath $canonical).Hash
# Deny replacement on Windows while allowing the Java client to read the fixture.
$lock = [IO.File]::Open($canonical, 'Open', 'Read', 'Read')
try {
    Test-Launch 'write-blocked'
    if ((Get-FileHash -LiteralPath $canonical).Hash -ne $writeBlockedHash -or
            !(Select-String -LiteralPath (Join-Path $game 'config-write-blocked.stderr.log') -Pattern 'Could not save' -Quiet)) {
        throw 'Expected a logged write failure and unchanged settings file.'
    }
} finally {
    $lock.Dispose()
}
$changed = @(foreach ($path in $contents.Keys) {
    $name = Split-Path $path -Leaf
    if (!(Test-Path -LiteralPath $path)) { throw "Missing third-party configuration: $name" }
    $old = $contents[$path]
    $current = [IO.File]::ReadAllBytes($path)
    if ([Convert]::ToBase64String($old) -eq [Convert]::ToBase64String($current)) { continue }
    $timestamp = '(?m)^#(?:(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun) | Generated on )[^\r\n]*\r?\n'
    $oldLines = @($encoding.GetString($old) -creplace $timestamp, '' -split '\r?\n' | Sort-Object -CaseSensitive)
    $newLines = @($encoding.GetString($current) -creplace $timestamp, '' -split '\r?\n' | Sort-Object -CaseSensitive)
    if (($oldLines -join "`n") -cne ($newLines -join "`n")) {
        throw "Third-party settings changed during the migration probe: $name"
    }
    $name
})
Write-Output "Third-party settings preserved. Pack-generated timestamp or ordering changes: $($changed -join ', ')"
Write-Output "Reports: $game"
