# Cross-check direct game-thread observations against authenticated transport in the isolated probe.
function Test-TerritoryHttp($Process, [string]$Game, [string]$Save, [string]$Mode, [string]$Stdout, [string]$Report) {
    $client = Join-Path $PSScriptRoot '../zed-dev.ps1'
    $observations = [ordered]@{}
    $pressure = $Mode.StartsWith('composition-')
    $deadline = [DateTime]::UtcNow.AddMinutes($(if ($pressure) { 6 } else { 3 }))
    $discovery = $null
    function Read-Bridge([string]$Action, [string]$Operation) {
        $raw = if ($Operation) { & $client $Action $Operation -Discovery $discovery -Json }
               else { & $client $Action -Discovery $discovery -Json }
        $reply = $raw | ConvertFrom-Json
        if (!$reply.ok) { throw "Bridge request failed with $($reply.error.code)." }
        $reply.data
    }
    try {
        while (!$Process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
            $lines = @(Get-Content -LiteralPath $Stdout -ErrorAction SilentlyContinue)
            $failure = $lines | Where-Object { $_ -match '^ALPHA6 FAIL ' } | Select-Object -First 1
            if ($failure) { throw $failure }
            foreach ($line in @($lines | Where-Object {
                $_ -match '^ALPHA6 HTTP '
            })) {
                $parts = $line.Split(' ')
                $label = $parts[2]
                if ($observations.Contains($label)) { continue }
                $discovery = (Get-ChildItem -LiteralPath $Game -Filter 'zto-dev-bridge-*.json' -File |
                    Select-Object -First 1).FullName
                if (!$discovery) { throw 'Read-only Bridge discovery was not created.' }
                $territory = Read-Bridge query territory.snapshot
                $status = Read-Bridge status
                $expected = $parts[3].Split(',')
                if (!$status.worldReady -or $territory.worldId -le 0 -or $status.worldId -ne $territory.worldId -or
                        $status.dimension -ne $territory.dimension -or $expected[0] -cne $territory.status -or
                        [int]$expected[1] -ne $territory.dimension -or $territory.clock -lt [long]$expected[2] -or
                        [int]$expected[3] -ne $territory.count -or $territory.capacity -ne 512 -or
                        [int]$expected[4] -ne $territory.presence -or [int]$expected[5] -ne $territory.returns -or
                        $territory.localActivity -ne ($territory.presence + $territory.returns)) {
                    throw "Territory HTTP counters diverged at $label."
                }
                $threat = $territory.threat
                if ($parts[4] -eq 'null') {
                    if ($null -ne $threat) { throw "Unexpected threat at $label." }
                } elseif (!$threat -or (!$pressure -and $parts[4] -ne (@($threat.regionX, $threat.regionZ, $threat.activity,
                        $threat.environmental, $threat.environment, $threat.value, $threat.observed) -join ',')) -or
                        $threat.value -ne ($threat.environmental + $threat.activity) -or
                        $threat.observed -ne ($territory.observationAge -ge 0)) {
                    throw "Territory HTTP assessment diverged at $label."
                }
                $assault = $null
                if ($parts[5] -ne 'null') {
                    $assault = Read-Bridge query assault.snapshot
                    $admissionThreat = if ($assault.admissionThreat) { $assault.admissionThreat.value }
                                       else { '-' }
                    if ($assault.worldId -ne $territory.worldId -or $assault.configuredBudget -ne 15 -or
                            $parts[5] -cne (@($assault.admissionProfile, $assault.budget, $admissionThreat,
                                $assault.activeCap, $assault.state) + @($assault.waves | ForEach-Object planned) -join ',') -or
                            $assault.peakActive -gt $assault.activeCap -or @($assault.spawns).Count -gt $assault.budget) {
                        throw "Assault HTTP admission or limits diverged at $label."
                    }
                    if ($label -eq 'pressure') {
                        $slots = foreach ($wave in $assault.waves) {
                            $types = ($wave.vanillaSlots | ForEach-Object { $_[0] }) -join ''
                            "$($wave.composition):$types"
                        }
                        $frozen = $assault.admissionThreat
                        $frozenInputs = if ($frozen) { "$($frozen.activity),$($frozen.environmental),$($frozen.environment)" }
                                        else { '-' }
                        if ($parts[6] -cne (($slots -join ',') + ',') -or
                                $parts[7] -cne ((($assault.spawns | ForEach-Object mob) -join ',') + ',') -or
                                $parts[8] -cne "$($assault.peakActive),$($assault.reason)" -or
                                $parts[9] -cne $frozenInputs) {
                            throw 'Assault HTTP composition, insertions or terminal state diverged.'
                        }
                    }
                }
                $observations[$label] = [ordered]@{ territory = $territory; assault = $assault }
                if ($label -eq 'pressure') {
                    $lunar = Read-Bridge query lunar.snapshot
                    if ($lunar.worldId -ne $assault.worldId -or $lunar.phase -ne 'FULL_MOON' -or
                            $assault.boundary -ne 205000 -or !$lunar.night) {
                        throw 'Pressure snapshot is outside the expected natural full-moon window.'
                    }
                    $observations[$label]['lunar'] = $lunar
                }
                New-Item -ItemType File -Path (Join-Path $Game "$Save-territory-http-$label.ack") | Out-Null
            }
            Start-Sleep -Milliseconds 75
        }
        if ($pressure) {
            if (!$observations.Contains('pressure') -or (Test-Path -LiteralPath $discovery)) {
                throw 'Missing pressure HTTP checkpoint or discovery was left behind.'
            }
            Write-Output "PASS authenticated composition and pressure snapshot ($Report)"
            return
        }
        foreach ($label in 'unvisited', 'sustained', 'admitted', 'resolved', 'nether', 'returned', 'reopened') {
            if (!$observations.Contains($label)) { throw "Missing territory HTTP checkpoint $label." }
        }
        if ($Mode -eq 'territory-elevated') {
            foreach ($label in 'absence', 'before-return-credit', 'return-credit') {
                if (!$observations.Contains($label)) { throw "Missing territory HTTP checkpoint $label." }
            }
        } elseif (!$observations.Contains('other-region')) {
            throw 'Missing territory HTTP checkpoint other-region.'
        }
        $initial = $observations.unvisited.territory.worldId
        if ($observations.admitted.territory.worldId -ne $initial -or
                $observations.nether.territory.worldId -le $initial -or
                $observations.returned.territory.worldId -le $observations.nether.territory.worldId -or
                $observations.reopened.territory.worldId -le $observations.returned.territory.worldId -or
                (Test-Path -LiteralPath $discovery)) {
            throw 'Bridge world identity or client-exit cleanup check failed.'
        }
        Write-Output "PASS authenticated territory and assault snapshots ($Report)"
    } finally {
        if (!$Process.HasExited) { $Process.Kill(); $Process.WaitForExit() }
        if ($discovery -and (Test-Path -LiteralPath $discovery)) { Remove-Item -LiteralPath $discovery }
        $observations | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $Report -Encoding UTF8
    }
}
