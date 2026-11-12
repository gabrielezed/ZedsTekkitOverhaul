[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^ZTO-Test-[a-f0-9]{32}$')]
    [string]$TemplateSave
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
$template = Join-Path $game "saves/$TemplateSave"
if (!(Test-Path -LiteralPath (Join-Path $template '.zto-disposable-test') -PathType Leaf)) {
    throw 'Use a marked disposable template save in the runtime copy.'
}
$null = New-Item -ItemType Directory -Path $harness, $report -Force
& javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/Alpha5RuntimeProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile the runtime probe against the installed classes.' }

$cfg = Join-Path $game 'config/mod_ZedsTekkitOverhaul.cfg'
$original = [IO.File]::ReadAllBytes($cfg)
$agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
$classpath = $launch.classpath + ';' + $harness
$failed = @()
try {
    foreach ($mode in 'anchor', 'vanilla') {
        $save = 'ZTO-Test-' + [Guid]::NewGuid().ToString('N')
        Copy-Item -LiteralPath $template -Destination (Join-Path $game "saves/$save") -Recurse
        $setting = ($mode -eq 'anchor').ToString().ToLowerInvariant()
        $config = [Text.Encoding]::ASCII.GetString($original) -replace '(?m)^disableNightSkipping=.*\r?\n', ''
        $config = $config -replace '(?m)^survivalDebugHud=true\b', 'survivalDebugHud=false'
        [IO.File]::WriteAllText($cfg, "$config`r`ndisableNightSkipping=$setting`r`n", [Text.Encoding]::ASCII)
        $arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2}" Alpha5RuntimeProbe "{3}" {4} {5}' -f (Join-Path $runtime 'natives'), $agents, $classpath, $game, $save, $mode
        $stdout = Join-Path $report "alpha5-$mode-$save.txt"
        $stderr = Join-Path $report "alpha5-$mode-$save.stderr.txt"
        $process = Start-Process -FilePath $launch.java -ArgumentList $arguments -WorkingDirectory $game `
            -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $timedOut = !$process.WaitForExit(65000)
        if ($timedOut) {
            $process.Kill()
            $process.WaitForExit()
        }
        Write-Output "ALPHA5 $mode $save SHA-256 $hash"
        Get-Content -LiteralPath $stdout | Select-String '^ALPHA5|MinecraftForge|Forge Mod Loader' | ForEach-Object Line
        if ($timedOut -or $process.ExitCode -ne 0 -or !(Select-String -LiteralPath $stdout -Pattern '^ALPHA5 PASS$' -Quiet)) {
            Get-Content -LiteralPath $stderr -Tail 30
            $failed += $mode
        }
    }
} finally {
    [IO.File]::WriteAllBytes($cfg, $original)
}
if ($failed.Count) { throw "Runtime probe failed in $($failed -join ', ')." }
