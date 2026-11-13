[CmdletBinding()]
param(
    [ValidateSet('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled')]
    [string[]]$Modes = @('first', 'gibbous', 'full', 'waves', 'quiet', 'disabled')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
$game = Join-Path $runtime 'Tekkit Dev/minecraft'
$harness = Join-Path $runtime 'harness'
$report = Join-Path $root 'dist-zto/test-reports'
$candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
$deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
$hash = (Get-FileHash -LiteralPath $candidate).Hash
if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
    throw 'Deploy the candidate into the isolated Tekkit Dev copy first.'
}
$launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
$null = New-Item -ItemType Directory -Path $harness, $report -Force
& javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/Alpha6RuntimeProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile against the installed pack ABI.' }

$cfg = Join-Path $game 'config/mod_ZedsTekkitOverhaul.cfg'
$original = [IO.File]::ReadAllBytes($cfg)
$config = [Text.Encoding]::ASCII.GetString($original) -replace '(?m)^(lunarOutdoorLightPressure|assaultSpawnBudget|lunarDebugKey|lunarVerboseLogging|survivalDebugHud)=.*\r?\n', ''
$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$phase = @{ first = 'FIRST_QUARTER'; gibbous = 'WAXING_GIBBOUS'; full = 'FULL_MOON'; waves = 'FULL_MOON'; quiet = 'NEW_MOON'; disabled = 'FIRST_QUARTER' }
try {
    foreach ($mode in $Modes) {
        $id = [Guid]::NewGuid().ToString('N')
        $save = "ZTO-Test-$id"
        & (Join-Path $PSScriptRoot 'new-assault-playtest.ps1') -Name "Test-$id" -LunarPhase $phase[$mode] -SavesDirectory (Join-Path $game 'saves')
        if ($LASTEXITCODE -ne 0) { throw 'Could not generate a disposable save.' }
        [IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"),
            "$save -7184827518270584220`n", [Text.Encoding]::ASCII)
        $enabled = ($mode -ne 'disabled').ToString().ToLowerInvariant()
        [IO.File]::WriteAllText($cfg, "$config`r`nlunarOutdoorLightPressure=$enabled`r`nassaultSpawnBudget=15`r`nlunarDebugKey=0`r`nlunarVerboseLogging=false`r`nsurvivalDebugHud=false`r`n", [Text.Encoding]::ASCII)
        $arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2};{3}" Alpha6RuntimeProbe "{4}" {5} {6}' -f (Join-Path $runtime 'natives'), $agents, $launch.classpath, $harness, $game, $save, $mode
        $stdout = Join-Path $report "alpha6-$mode-$save.txt"
        $stderr = Join-Path $report "alpha6-$mode-$save.stderr.txt"
        $process = Start-Process -FilePath $launch.java -ArgumentList $arguments -WorkingDirectory $game `
            -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $null = $process.Handle
        $timedOut = !$process.WaitForExit(115000)
        if ($timedOut) {
            $process.Kill()
            $process.WaitForExit()
        }
        Write-Output "ALPHA6 $mode $save SHA-256 $hash"
        Get-Content -LiteralPath $stdout | Select-String '^ALPHA6|MinecraftForge' | ForEach-Object Line
        Select-String -LiteralPath $stderr -Pattern 'Forge Mod Loader version|Forge Mod Loader load complete|\[ZTO\] PATROL' |
            ForEach-Object Line
        if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
            throw 'The deployed archive changed during the probe.'
        }
        if ($timedOut -or $process.ExitCode -ne 0 -or !(Select-String -LiteralPath $stdout -Pattern "^ALPHA6 PASS $mode$" -Quiet)) {
            Get-Content -LiteralPath $stderr -Tail 30
            throw "Runtime probe failed in $mode."
        }
    }
} finally {
    [IO.File]::WriteAllBytes($cfg, $original)
}
