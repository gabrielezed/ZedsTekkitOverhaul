. (Join-Path $PSScriptRoot 'BackgroundMinecraft.ps1')
. (Join-Path $PSScriptRoot 'SilentMinecraft.ps1')

# Each invocation owns its game directory, config, saves and harness, including after a killed runner.
function New-IsolatedRuntimeGame([string]$Runtime, [switch]$WithoutOptionalMods) {
    $source = Join-Path $Runtime 'Tekkit Dev/minecraft'
    $game = Join-Path $Runtime ("runs/{0}/survival-hud-runtime/Tekkit Dev/minecraft" -f [Guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Path $game
    foreach ($name in 'config', 'coremods', 'mods', 'resources', 'texturepacks', 'matmos_expansions_r7',
            'matmos_internal', 'options.txt', 'optionsof.txt', 'matmos_options.cfg', 'mod_EE.props') {
        if ($WithoutOptionalMods -and $name -eq 'mods') { continue }
        $path = Join-Path $source $name
        if (Test-Path -LiteralPath $path) {
            Copy-Item -LiteralPath $path -Destination (Join-Path $game $name) -Recurse
        }
    }
    if ($WithoutOptionalMods) {
        $mods = New-Item -ItemType Directory -Path (Join-Path $game 'mods')
        # Existing distillation tiles link these APIs during registration.
        foreach ($required in 'buildcraft-client-A-core-2.2.14.zip', 'industrialcraft-2-client_1.97.jar') {
            Copy-Item -LiteralPath (Join-Path $source "mods/$required") -Destination $mods.FullName
        }
    }
    $null = New-Item -ItemType Directory -Path (Join-Path $game 'saves')
    Set-SilentMinecraftOptions $game
    return $game
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
    $null = $Process.Handle
    try {
        return $Process.WaitForExit($TimeoutMs)
    } finally {
        if (!$Process.HasExited) {
            $Process.Kill()
            $Process.WaitForExit()
        }
    }
}
