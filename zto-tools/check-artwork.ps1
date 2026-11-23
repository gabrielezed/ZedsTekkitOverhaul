param([string]$Archive)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$manifest = Get-Content -Raw -LiteralPath (Join-Path $root 'artwork/manifests/assets.json') | ConvertFrom-Json
if (!$manifest) { throw 'Artwork asset manifest is empty.' }

function Get-ArtworkHash($Path) {
    $stream = [IO.File]::OpenRead($Path)
    try { [Convert]::ToBase64String($sha.ComputeHash($stream)) }
    finally { $stream.Dispose() }
}

$jar = $null
if ($Archive) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Archive).Path)
}

$sha = [Security.Cryptography.SHA256]::Create()
try {
    $paths = @{}
    foreach ($asset in $manifest) {
        if (!$asset.jar -or $asset.runtime -cnotmatch '^zed/(textures|gui)/[a-z0-9_/]+\.png$' -or
            $asset.source -cnotmatch '^source/[a-z0-9_/]+\.png$' -or $paths.ContainsKey($asset.runtime)) {
            throw "Invalid artwork asset mapping: $($asset.id)"
        }
        $paths[$asset.runtime] = $true
        $source = Join-Path $root "artwork/$($asset.source)"
        $runtime = Join-Path $root "src/minecraft/$($asset.runtime)"
        if (!(Test-Path -LiteralPath $runtime -PathType Leaf) -or !(Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Missing artwork $($asset.runtime). Run python artwork/tools/textures.py export."
        }
        $expected = Get-ArtworkHash $source
        if ($expected -cne (Get-ArtworkHash $runtime)) {
            throw "Stale artwork $($asset.runtime). Run python artwork/tools/textures.py export."
        }
        if ($jar) {
            $entry = $jar.GetEntry($asset.runtime)
            if (!$entry) { throw "Missing packaged artwork: $($asset.runtime)" }
            $stream = $entry.Open()
            try {
                if ([Convert]::ToBase64String($sha.ComputeHash($stream)) -cne $expected) {
                    throw "Stale packaged artwork: $($asset.runtime)"
                }
            } finally { $stream.Dispose() }
        }
    }

    if ($jar) {
        $unwanted = $jar.Entries | Where-Object {
            $_.FullName -match '^(?:(artwork|armor|ic2|buildcraft|gui|net/minecraft)/|terrain\.png$)' -or
            $_.FullName -match '^zed/(models/distillation_.*\.obj|textures/distillation_(atlas|items)\.png|tekkitoverhaul/distillation/client/RenderDistillationComponent.*\.class)$'
        }
        if ($unwanted) { throw "Unexpected reference or obsolete asset: $($unwanted.FullName -join ', ')" }
    }
    Write-Output "[ZTO] Verified $($paths.Count) manifest assets."
} finally {
    $sha.Dispose()
    if ($jar) { $jar.Dispose() }
}
