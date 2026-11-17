[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^ZTO-Test-[a-f0-9]{32}$')]
    [string]$TemplateSave,
    [switch]$KeepGame
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$runtime = Join-Path $root 'dist-zto/survival-hud-runtime'
. (Join-Path $PSScriptRoot 'runtime-tests/IsolatedRuntime.ps1')
$sourceGame = Join-Path $runtime 'Tekkit Dev/minecraft'
Assert-BackgroundMinecraftAvailable
$run = New-IsolatedRuntimeRun $runtime 'alpha5' $KeepGame.IsPresent
try {
    $game = Initialize-IsolatedRuntimeGame $run $runtime
    $harness = Join-Path $game 'harness'
    $report = $run.Evidence
    $candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
    $deployed = Join-Path $game 'mods/ZedsTekkitOverhaul.jar'
    $hash = (Get-FileHash -LiteralPath $candidate).Hash
    $run.Result.candidateSha256 = $hash
    if ((Get-FileHash -LiteralPath $deployed).Hash -ne $hash) {
        throw 'Deploy the candidate into the isolated Tekkit Dev copy first.'
    }
    $launch = Get-Content (Join-Path $runtime 'launch.json') -Raw | ConvertFrom-Json
    $template = Join-Path $sourceGame "saves/$TemplateSave"
    Get-RuntimeTree (Assert-RegularPath $template) | Out-Null
    $marker = Join-Path $template '.zto-disposable-test'
    if (!(Test-Path -LiteralPath $marker -PathType Leaf) -or
        [IO.File]::ReadAllText($marker) -cnotmatch ('\A' + $TemplateSave + ' (-?[0-9]{1,19})\r?\n\z')) {
        throw 'Use a marked disposable template save in the runtime copy.'
    }
    $seed = $Matches[1]
    $null = New-Item -ItemType Directory -Path $harness, $report -Force
    & javac -cp "$($launch.classpath);$candidate" -d $harness (Join-Path $PSScriptRoot 'runtime-tests/Alpha5RuntimeProbe.java') (Join-Path $PSScriptRoot 'runtime-tests/SilentRuntimeAudio.java')
    if ($LASTEXITCODE -ne 0) { throw 'Could not compile the runtime probe against the installed classes.' }

    $agents = @($launch.agents | ForEach-Object { '"' + $_ + '"' }) -join ' '
    $classpath = $launch.classpath + ';' + $harness
    $failed = @()
    foreach ($mode in 'anchor', 'vanilla') {
        $save = 'ZTO-Test-' + [Guid]::NewGuid().ToString('N')
        Copy-Item -LiteralPath $template -Destination (Join-Path $game "saves/$save") -Recurse
        [IO.File]::WriteAllText((Join-Path $game "saves/$save/.zto-disposable-test"), "$save $seed`n", [Text.Encoding]::ASCII)
        $setting = ($mode -eq 'anchor').ToString().ToLowerInvariant()
        Set-RuntimeConfig $game ([ordered]@{ disableNightSkipping = $setting })
        $arguments = '-Xmx1G -Dzto.dev=false -Dzto.devBridge.enabled=false "-Djava.library.path={0}" {1} -cp "{2}" Alpha5RuntimeProbe "{3}" {4} {5}' -f (Join-Path $runtime 'natives'), $agents, $classpath, $game, $save, $mode
        $stdout = Join-Path $report "alpha5-$mode-$save.txt"
        $stderr = Join-Path $report "alpha5-$mode-$save.stderr.txt"
        $process = Start-IsolatedRuntimeClient -Run $run -FilePath $launch.java -ArgumentList $arguments -WorkingDirectory $game `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $timedOut = !(Wait-RuntimeClient $process 65000)
        Write-Output "ALPHA5 $mode $save SHA-256 $hash"
        Get-Content -LiteralPath $stdout | Select-String '^ALPHA5|MinecraftForge|Forge Mod Loader' | ForEach-Object Line
        if ($timedOut -or $process.ExitCode -ne 0 -or !(Select-String -LiteralPath $stdout -Pattern '^ALPHA5 PASS$' -Quiet)) {
            Get-Content -LiteralPath $stderr -Tail 30
            $failed += $mode
        }
    }
    if ($failed.Count) { throw "Runtime probe failed in $($failed -join ', ')." }
    $run.Result.status = 'PASS'
} catch {
    $run.Result.error = $_.Exception.Message
    throw
} finally {
    Complete-IsolatedRuntimeRun $run
}
