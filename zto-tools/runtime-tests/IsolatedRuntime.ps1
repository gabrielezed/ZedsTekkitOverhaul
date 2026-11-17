. (Join-Path $PSScriptRoot 'BackgroundMinecraft.ps1')
. (Join-Path $PSScriptRoot 'SilentMinecraft.ps1')
. (Join-Path $PSScriptRoot 'RuntimeArtifacts.ps1')

function Copy-RuntimeInput([string]$Path, [string]$Destination) {
    $item = Assert-RegularPath $Path
    if ($item.PSIsContainer) { Get-RuntimeTree $item | Out-Null }
    Copy-Item -LiteralPath $item.FullName -Destination $Destination -Recurse
}

function New-IsolatedRuntimeRun([string]$Runtime, [string]$Family, [bool]$KeepGame) {
    $id = [Guid]::NewGuid().ToString('N')
    $runs = Join-Path $Runtime 'runs'
    $path = Join-Path $runs $id
    $evidence = Join-Path (Split-Path $Runtime) "test-reports/runtime/$id"
    $owner = Get-Process -Id $PID
    @{
        Id = $id; Runs = $runs; Path = $path; Evidence = $evidence; Process = $null; OwnsPath = $false
        Game = Join-Path $path 'survival-hud-runtime/Tekkit Dev/minecraft'
        Manifest = [ordered]@{
            schema = 'zto.runtime@1'; id = $id; family = $Family; state = 'active'; retain = $KeepGame
            ownerPid = $PID; ownerStarted = $owner.StartTime.ToUniversalTime().ToString('o')
            game = Join-Path $path 'survival-hud-runtime/Tekkit Dev/minecraft'
            childPid = $null; childStarted = $null
        }
        Result = [ordered]@{
            id = $id; family = $Family; status = 'FAIL'; error = $null
            candidateSha256 = $null; agentSha256 = $null; sourceCommit = $null; logs = @(); observations = @()
            target = 'Tekkit Classic 3.1.2 / Minecraft 1.2.5 / Forge 3.3.8.164 / FML 2.2.106.176'
        }
    }
}

function Initialize-IsolatedRuntimeGame($Run, [string]$Runtime, [switch]$WithoutOptionalMods) {
    $null = Assert-RegularPath $Runtime
    $reports = Join-Path (Split-Path $Runtime) 'test-reports'
    foreach ($directory in $Run.Runs, $reports, (Split-Path $Run.Evidence)) {
        $null = New-Item -ItemType Directory -Path $directory -Force
        $null = Assert-RegularPath $directory
    }
    $null = New-Item -ItemType Directory -Path $Run.Evidence
    $null = New-Item -ItemType Directory -Path $Run.Path
    $Run.OwnsPath = $true
    $Run.Manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Run.Path '.zto-runtime.json') -Encoding UTF8
    $Run.Result.sourceCommit = & git -C (Split-Path (Split-Path $Runtime)) rev-parse HEAD
    $source = Join-Path $Runtime 'Tekkit Dev/minecraft'
    $null = Assert-RegularPath $source
    $game = $Run.Game
    $null = New-Item -ItemType Directory -Path $game
    $sourceConfig = Join-Path $source 'config'
    if (Test-Path -LiteralPath $sourceConfig -PathType Container) {
        $config = (Assert-RegularPath $sourceConfig).FullName
        $destination = (New-Item -ItemType Directory -Path (Join-Path $game 'config')).FullName
        $exports = 'ZedsTekkitOverhaul-emc-baseline.tsv', 'ZedsTekkitOverhaul-emc-energy.txt',
            'ZedsTekkitOverhaul-emc.pre-recipe-audit.txt', 'ZedsTekkitOverhaul-emc.pre-zto-ic2-followup.txt'
        foreach ($item in Get-ChildItem -LiteralPath $config -Force) {
            if (!$item.PSIsContainer -and $item.Name -in $exports) { continue }
            Copy-RuntimeInput $item.FullName $destination
        }
    }
    foreach ($name in 'coremods', 'mods', 'resources', 'texturepacks', 'matmos_expansions_r7',
            'matmos_internal', 'options.txt', 'optionsof.txt', 'matmos_options.cfg', 'mod_EE.props') {
        if ($WithoutOptionalMods -and $name -eq 'mods') { continue }
        $path = Join-Path $source $name
        if (Test-Path -LiteralPath $path) {
            Copy-RuntimeInput $path (Join-Path $game $name)
        }
    }
    if ($WithoutOptionalMods) {
        $mods = New-Item -ItemType Directory -Path (Join-Path $game 'mods')
        # Existing distillation tiles link these APIs during registration.
        foreach ($required in 'buildcraft-client-A-core-2.2.14.zip', 'industrialcraft-2-client_1.97.jar') {
            $file = Assert-RegularPath (Join-Path $source "mods/$required")
            Copy-Item -LiteralPath $file.FullName -Destination $mods.FullName
        }
    }
    $null = New-Item -ItemType Directory -Path (Join-Path $game 'saves')
    Set-SilentMinecraftOptions $game
    return $game
}

function Install-IsolatedZtoAgent($Run, [string]$Harness, [string]$Archive) {
    $source = (Assert-RegularPath $Archive).FullName
    $isolated = Join-Path $Harness 'ZTOAgent.jar'
    Copy-Item -LiteralPath $source -Destination $isolated
    $Run.Result.agentSha256 = (Get-FileHash -LiteralPath $isolated).Hash
    return '"-javaagent:' + $isolated + '"'
}

function Start-IsolatedRuntimeClient {
    param($Run, [string]$FilePath, [string]$ArgumentList, [string]$WorkingDirectory,
        [string]$RedirectStandardOutput, [string]$RedirectStandardError)
    if ($Run.Process -and !$Run.Process.HasExited) { throw 'The previous runtime client is still active.' }
    $null = $PSBoundParameters.Remove('Run')
    $Run.Process = Start-BackgroundMinecraft @PSBoundParameters
    $Run.Manifest.childPid = $Run.Process.Id
    $Run.Manifest.childStarted = $Run.Process.StartTime.ToUniversalTime().ToString('o')
    $Run.Manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Run.Path '.zto-runtime.json') -Encoding UTF8
    $Run.Process
}

function Limit-SuccessfulRuntimeLogs($Run) {
    $reportRoot = Assert-RegularPath (Split-Path $Run.Evidence)
    $completed = foreach ($entry in $reportRoot.EnumerateDirectories()) {
        if ($entry.Name -cnotmatch '^[a-f0-9]{32}$' -or
            ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) { continue }
        $resultPath = Join-Path $entry.FullName 'result.json'
        if (!(Test-Path -LiteralPath $resultPath -PathType Leaf)) { continue }
        try {
            $resultFile = Assert-RegularPath $resultPath
            $result = Get-Content -LiteralPath $resultFile.FullName -Raw | ConvertFrom-Json
        } catch { continue }
        if ($result.id -ceq $entry.Name -and $result.family -ceq $Run.Result.family -and
            $result.status -ceq 'PASS' -and !$result.retainedGame) { $result }
    }
    foreach ($result in ($completed | Sort-Object completedUtc -Descending | Select-Object -Skip 2)) {
        foreach ($name in $result.logs) {
            if ($name -cnotmatch '^[a-zA-Z0-9-]+(?:\.stderr)?\.(?:txt|log)$') { continue }
            $path = Join-Path $reportRoot.FullName "$($result.id)/$name"
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                $log = Assert-RegularPath $path
                Remove-Item -LiteralPath $log.FullName -ErrorAction Stop
            }
        }
    }
}

function Complete-IsolatedRuntimeRun($Run) {
    if (!$Run.OwnsPath) { return }
    if ($Run.Process -and !$Run.Process.HasExited) {
        try { $Run.Process.Kill() } catch { if (!$Run.Process.HasExited) { throw } }
        if (!$Run.Process.WaitForExit(10000)) { throw "Runtime client is still active: $($Run.Path)" }
    }
    $null = Assert-RegularPath $Run.Evidence
    $Run.Result.completedUtc = [DateTime]::UtcNow.ToString('o')
    $Run.Result.retainedGame = if ($Run.Manifest.retain) { $Run.Game } else { $null }
    try {
        if ($Run.Result.status -eq 'PASS') {
            foreach ($log in Get-ChildItem -LiteralPath $Run.Evidence -File) {
                if ($log.Extension -notin '.txt', '.log') { continue }
                $log = Assert-RegularPath $log.FullName
                $stream = [IO.File]::OpenRead($log.FullName)
                try {
                    $tail = [byte[]]::new([int][Math]::Min(65536, $stream.Length))
                    $null = $stream.Seek(-$tail.Length, [IO.SeekOrigin]::End)
                    $reader = [IO.BinaryReader]::new($stream)
                    $tail = $reader.ReadBytes($tail.Length)
                } finally { $stream.Dispose() }
                [IO.File]::WriteAllBytes($log.FullName, $tail)
                $Run.Result.logs += $log.Name
                $Run.Result.observations += @(
                    [Text.Encoding]::UTF8.GetString($tail) -split '\r?\n' |
                        Where-Object { $_ -match '^(SABOTEUR|ALPHA[567]|CONFIG|MinecraftForge)|Forge Mod Loader version' } |
                        Select-Object -Last 10 |
                        ForEach-Object { $_.Substring(0, [Math]::Min(512, $_.Length)) }
                )
            }
        } else {
            foreach ($name in 'ForgeModLoader-0.log', 'crash-reports') {
                $path = Join-Path $Run.Game $name
                if (!(Test-Path -LiteralPath $path)) { continue }
                $item = Assert-RegularPath $path
                if ($item.PSIsContainer) { Get-RuntimeTree $item | Out-Null }
                Copy-Item -LiteralPath $path -Destination $Run.Evidence -Recurse
            }
        }
        $frames = Join-Path $Run.Game 'screenshots'
        if (Test-Path -LiteralPath $frames -PathType Container) {
            Get-RuntimeTree (Assert-RegularPath $frames) | Out-Null
            Copy-Item -LiteralPath $frames -Destination $Run.Evidence -Recurse
        }
        $Run.Result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Run.Evidence 'result.json') -Encoding UTF8
        $Run.Manifest.state = 'complete'
        $Run.Manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Run.Path '.zto-runtime.json') -Encoding UTF8
    } finally {
        if (!$Run.Manifest.retain) {
            Remove-OwnedRunDirectory $Run.Runs $Run.Path
        }
    }
    if ($Run.Result.status -eq 'PASS') {
        try { Limit-SuccessfulRuntimeLogs $Run } catch { Write-Warning "Could not prune old console logs: $($_.Exception.Message)" }
    }
    Write-Output "Runtime evidence: $($Run.Evidence)"
    if ($Run.Manifest.retain) { Write-Output "Retained runtime: $($Run.Game)" }
}

function Set-RuntimeConfig([string]$Game, [System.Collections.IDictionary]$Settings) {
    $config = Join-Path $Game 'config'
    $file = Join-Path $config 'ZedsTekkitOverhaul.cfg'
    $original = Join-Path $Game 'original-zto-config'
    if (!(Test-Path -LiteralPath $original)) {
        $null = New-Item -ItemType Directory -Path $original
        foreach ($name in 'ZedsTekkitOverhaul.cfg', 'ZedsTekkitOverhaul.cfg.legacy',
                'mod_ZedsTekkitOverhaul.cfg', 'mod_ZedsTekkitOverhaul.cfg.bak',
                'ZedsTekkitOverhaul-Tactical.cfg', 'ZedsTekkitOverhaul-Distillation.cfg',
                'ZedsTekkitOverhaul-AlchemicalPower.cfg', 'ZedsTekkitOverhaul-emc.txt') {
            $path = Join-Path $config $name
            if (Test-Path -LiteralPath $path) { Move-Item -LiteralPath $path -Destination $original }
        }
    }
    $lines = @('configVersion=1')
    foreach ($key in $Settings.Keys) { $lines += "$key=$($Settings[$key])" }
    [IO.File]::WriteAllLines($file, [string[]]$lines, [Text.Encoding]::ASCII)
}

function Wait-RuntimeClient([Diagnostics.Process]$Process, [int]$TimeoutMs) {
    try {
        $null = $Process.Handle
        return $Process.WaitForExit($TimeoutMs)
    } finally {
        if (!$Process.HasExited) {
            try { $Process.Kill() } catch { if (!$Process.HasExited) { throw } }
            if (!$Process.WaitForExit(10000)) { throw 'The test client did not exit after termination.' }
        }
    }
}
