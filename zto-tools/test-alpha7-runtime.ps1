[CmdletBinding()]
param([ValidateSet('combat', 'pulse', 'stress', 'visual', 'creative', 'creative-native')][string]$Mode = 'combat')

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
$game = Join-Path $runtime 'Tekkit Dev/minecraft'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
$hash = (Get-FileHash -LiteralPath $candidate).Hash
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) { throw 'Deploy the candidate to the isolated copy first.' }
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$harness = Join-Path $runtime 'harness'
$report = Join-Path $root 'dist-zto/test-reports'
$null = New-Item -ItemType Directory -Path $harness, $report -Force
& javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/Alpha7RuntimeProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile against the installed pack ABI.' }

$id = [Guid]::NewGuid().ToString('N')
$save = "ZTO-Test-$id"
& (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$id" -LunarPhase NEW_MOON -SavesDirectory (Join-Path $game 'saves')
if ($LASTEXITCODE -ne 0) { throw 'Could not generate a disposable save.' }
[IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"),
    "$save -7184827518270584220`n", [Text.Encoding]::ASCII)
$secondSave = ''
if ($Mode -eq 'pulse') {
    $secondId = [Guid]::NewGuid().ToString('N')
    $secondSave = "ZTO-Test-$secondId"
    & (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$secondId" -LunarPhase NEW_MOON -SavesDirectory (Join-Path $game 'saves')
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate the world-switch fixture.' }
    [IO.File]::WriteAllText((Join-Path $game "saves/$secondSave/.zto-disposable-test"),
        "$secondSave -7184827518270584220`n", [Text.Encoding]::ASCII)
}
$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2};{3}" Alpha7RuntimeProbe$Bootstrap "{4}" {5} {6}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $save, $Mode
if ($secondSave) { $arguments += " $secondSave" }
$stdout = Join-Path $report "alpha7-$Mode-$save.txt"
$stderr = Join-Path $report "alpha7-$Mode-$save.stderr.txt"
$java = Join-Path (Split-Path $launch.java) 'java.exe'
$config = Join-Path $game 'config/mod_ZedsTekkitOverhaul.cfg'
$original = if ($Mode.StartsWith('creative')) { [IO.File]::ReadAllBytes($config) }
try {
    if ($original) {
        $enabled = ($Mode -eq 'creative').ToString().ToLowerInvariant()
        $content = [Text.Encoding]::ASCII.GetString($original) -replace '(?m)^creativeInventoryBackport=.*\r?\n', ''
        [IO.File]::WriteAllText($config, "$content`r`ncreativeInventoryBackport=$enabled`r`n", [Text.Encoding]::ASCII)
    }
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $game `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $null = $process.Handle
    if (!$process.WaitForExit(120000)) {
        $process.Kill()
        $process.WaitForExit()
        Get-Content -LiteralPath $stderr -Tail 30
        throw "Tactical probe timed out. See $stdout and $stderr"
    }
} finally {
    if ($original) { [IO.File]::WriteAllBytes($config, $original) }
}
Write-Output "ALPHA7 $Mode $save SHA-256 $hash"
Get-Content -LiteralPath $stdout | Select-String '^ALPHA7|MinecraftForge' | ForEach-Object Line
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) { throw 'The deployed archive changed during the probe.' }
if ($process.ExitCode -ne 0 -or !(Select-String -LiteralPath $stdout -Pattern "^ALPHA7 PASS $Mode$" -Quiet)) {
    Get-Content -LiteralPath $stderr -Tail 35
    throw "Tactical probe failed. See $stdout and $stderr"
}
