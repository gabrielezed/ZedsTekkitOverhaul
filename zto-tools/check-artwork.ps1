param(
    [ValidateSet('local-dev', 'public')][string]$Profile = 'local-dev',
    [string]$Archive,
    [string]$Fingerprints,
    [switch]$PrepareLocal
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'file-sha256.ps1')
$root = Split-Path $PSScriptRoot
$manifest = Get-Content -Raw -LiteralPath (Join-Path $root 'artwork/manifests/assets.json') | ConvertFrom-Json
if (!$manifest) { throw 'Artwork asset manifest is empty.' }

$overrides = @{}
$profileManifest = Join-Path $root 'artwork/manifests/local-dev.json'
$spec = Get-Content -Raw -LiteralPath $profileManifest | ConvertFrom-Json
if ($Profile -eq 'local-dev') {
    $profileHash = Get-ZtoFileSha256 $profileManifest
    $overlay = Join-Path $root "artwork/local-dev/$profileHash"
    $derivations = Get-Content -Raw -LiteralPath (Join-Path $root "artwork/$($spec.derivation)") | ConvertFrom-Json
    $missing = @()
    foreach ($override in $spec.assets) {
        $asset = @($manifest | Where-Object id -ceq $override.id)
        if ($asset.Count -ne 1 -or $overrides.ContainsKey($override.id) -or
            $override.runtime -cnotmatch '^zed/(textures|gui)/[a-z0-9_/]+\.png$' -or
            $asset[0].runtime -cne $override.runtime -or
            ($asset[0].size -join 'x') -cne ($override.size -join 'x') -or !$override.faceMap -or
            !$derivations.outputs.PSObject.Properties[$asset[0].source.Substring(7)] -or
            $override.pngSha256 -cnotmatch '^[a-f0-9]{64}$' -or $override.pixelSha256 -cnotmatch '^[a-f0-9]{64}$') {
            throw "Invalid local-dev artwork mapping for $($override.id). Check the face map and derivation manifest."
        }
        $faces = @($derivations.outputs.PSObject.Properties[$asset[0].source.Substring(7)].Value.PSObject.Properties.Name)
        $expectedFaces = @('image')
        if ($asset[0].opaqueCells) { $expectedFaces = @($asset[0].opaqueCells | ForEach-Object { $_ -join ',' }) }
        if (Compare-Object -ReferenceObject $expectedFaces -DifferenceObject $faces) {
            throw "The current face map differs from the approved local-dev derivation for $($override.id)."
        }
        $overrides[$override.id] = $override
        $path = Join-Path $overlay $override.runtime
        if (!(Test-Path -LiteralPath $path -PathType Leaf) -or
            (Get-ZtoFileSha256 $path) -cne $override.pngSha256) {
            $missing += $override.id
        }
    }
    if (!$overrides.Count -or $overrides.Count -ne @($derivations.outputs.PSObject.Properties).Count) {
        throw 'The local-dev profile must cover all recorded machine derivatives.'
    }
    if ($missing.Count -and $PrepareLocal) {
        $python = $env:ZTO_ART_PYTHON
        $pythonArgs = @()
        if (!$python) { $python = Join-Path $root 'artwork/.venv/Scripts/python.exe' }
        if (!$env:ZTO_ART_PYTHON -and !(Test-Path -LiteralPath $python -PathType Leaf)) {
            if (Get-Command py.exe -ErrorAction SilentlyContinue) {
                $python = 'py.exe'
                $pythonArgs = @('-3')
            } else {
                $python = 'python.exe'
            }
        }
        $mods = $env:ZTO_ART_REFERENCE_MODS
        if (!$mods) { $mods = Join-Path $env:APPDATA 'PrismLauncher/instances/Tekkit Dev/minecraft/mods' }
        $pythonArgs += @((Join-Path $root "artwork/$($spec.generator)"), 'build', '--mods-dir', $mods)
        if (!(Get-Command $python -ErrorAction SilentlyContinue)) {
            throw 'local-dev requires Python and artwork/requirements.txt to regenerate missing overrides. Set ZTO_ART_PYTHON to the prepared Python executable.'
        }
        & $python @pythonArgs
        if ($LASTEXITCODE) {
            throw 'Could not regenerate approved local-dev artwork. Install artwork/requirements.txt and check the pinned Tekkit archives or set ZTO_ART_REFERENCE_MODS. See artwork/README.md.'
        }
    }
}

$jar = $null
if ($Archive) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Archive).Path)
}

$sha = [Security.Cryptography.SHA256]::Create()
try {
    $paths = @{}
    $ids = @{}
    $pins = @()
    foreach ($asset in $manifest) {
        if (!$asset.jar -or $asset.id -cnotmatch '^[a-z0-9_-]+$' -or $asset.origin -eq 'local-derivative' -or
            $asset.runtime -cnotmatch '^zed/(textures|gui)/[a-z0-9_/]+\.png$' -or
            $asset.source -cnotmatch '^source/[a-z0-9_/]+\.png$' -or $paths.ContainsKey($asset.runtime) -or
            $ids.ContainsKey($asset.id)) {
            throw "Invalid artwork asset mapping: $($asset.id)"
        }
        $paths[$asset.runtime] = $true
        $ids[$asset.id] = $true
        $source = Join-Path $root "artwork/$($asset.source)"
        $runtime = Join-Path $root "src/minecraft/$($asset.runtime)"
        if (!(Test-Path -LiteralPath $runtime -PathType Leaf) -or !(Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Missing artwork $($asset.runtime). Run python artwork/tools/textures.py export."
        }
        $expected = Get-ZtoFileSha256 $source
        if ($expected -cne (Get-ZtoFileSha256 $runtime)) {
            throw "Stale artwork $($asset.runtime). Run python artwork/tools/textures.py export."
        }
        if ($Profile -eq 'public' -and ($spec.assets | Where-Object pngSha256 -ceq $expected)) {
            throw "Approved private artwork is present in the public source or export for $($asset.runtime). Restore the public baseline."
        }
        if ($overrides.ContainsKey($asset.id)) {
            $override = $overrides[$asset.id]
            $local = Join-Path $overlay $asset.runtime
            $expected = $override.pngSha256
            if (!(Test-Path -LiteralPath $local -PathType Leaf) -or (Get-ZtoFileSha256 $local) -cne $expected) {
                throw "Missing or invalid approved local-dev artwork $($asset.runtime). Run the normal build to regenerate it from the pinned Tekkit archives."
            }
        }
        $pins += "artwork.$($asset.id)=$expected"
        if ($jar) {
            $entries = @($jar.Entries | Where-Object FullName -ceq $asset.runtime)
            if ($entries.Count -ne 1) { throw "Expected one packaged $Profile asset at $($asset.runtime)" }
            $stream = $entries[0].Open()
            try {
                $actual = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '').ToLowerInvariant()
                if ($actual -cne $expected) {
                    throw "Wrong packaged $Profile artwork for $($asset.runtime). Expected $expected, found $actual."
                }
            } finally { $stream.Dispose() }
        }
    }

    if ($jar) {
        $unwanted = $jar.Entries | Where-Object {
            $_.FullName -match '^(?:(artwork|armor|ic2|buildcraft|gui|net/minecraft)/|terrain\.png$)' -or
            $_.FullName -match '^zed/(models/distillation_.*\.obj|textures/distillation_(atlas|items)\.png|tekkitoverhaul/distillation/client/RenderDistillationComponent.*\.class)$' -or
            ($Profile -eq 'public' -and $_.FullName -match '^zed/(hopper-1\.5\.2|creative-1\.4\.7)/')
        }
        if ($unwanted) { throw "Unexpected reference or obsolete asset: $($unwanted.FullName -join ', ')" }
    }
    if ($Fingerprints) {
        $digest = $sha.ComputeHash([Text.Encoding]::ASCII.GetBytes((@($Profile) + $pins -join "`n")))
        $fingerprint = [BitConverter]::ToString($digest).Replace('-', '').ToLowerInvariant()
        $records = @("artworkProfile=$Profile", "artworkFingerprint=$fingerprint") + $pins
        if ($jar) {
            if ((Get-Content -LiteralPath $Fingerprints) -join "`n" -cne ($records -join "`n")) {
                throw 'Artwork fingerprints changed during the build.'
            }
        } else {
            $records | Set-Content -LiteralPath $Fingerprints -Encoding ASCII
        }
    }
    Write-Output "[ZTO] Verified $($paths.Count) $Profile manifest assets ($($overrides.Count) approved local overrides)."
} finally {
    $sha.Dispose()
    if ($jar) { $jar.Dispose() }
}
