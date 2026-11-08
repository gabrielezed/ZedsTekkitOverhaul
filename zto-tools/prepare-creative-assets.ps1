param(
    [string]$ResearchDirectory = (Join-Path $env:TEMP 'zto-creative-1.4.7')
)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($ResearchDirectory).TrimEnd('\', '/')
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$instances = Join-Path $env:APPDATA 'PrismLauncher\instances'
foreach ($excluded in @($repo, $instances)) {
    if ($root.Equals($excluded, [StringComparison]::OrdinalIgnoreCase) -or
        $root.StartsWith($excluded + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Choose a research directory outside the repository and Prism instances.'
    }
}
New-Item -ItemType Directory -Force -Path $root | Out-Null

$manifest = Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
$version = $manifest.versions | Where-Object id -eq '1.4.7'
$metadataFile = Join-Path $root '1.4.7.json'
Invoke-WebRequest $version.url -OutFile $metadataFile
if ((Get-FileHash -LiteralPath $metadataFile -Algorithm SHA1).Hash -ne $version.sha1) {
    throw 'The launcher version metadata failed SHA-1 verification.'
}
$client = (Get-Content -LiteralPath $metadataFile -Raw | ConvertFrom-Json).downloads.client
$jar = Join-Path $root 'client.jar'
if (!(Test-Path -LiteralPath $jar) -or (Get-FileHash -LiteralPath $jar -Algorithm SHA1).Hash -ne $client.sha1) {
    Invoke-WebRequest $client.url -OutFile $jar
}
if ((Get-Item -LiteralPath $jar).Length -ne $client.size -or
    (Get-FileHash -LiteralPath $jar -Algorithm SHA1).Hash -ne $client.sha1) {
    throw 'The Minecraft client failed size or SHA-1 verification.'
}

$stage = Join-Path $root 'stage'
$destination = Join-Path $stage 'zed\creative-1.4.7'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    foreach ($name in @('gui/allitems.png', 'gui/creative_inv/list_items.png',
                       'gui/creative_inv/search.png', 'gui/creative_inv/survival_inv.png')) {
        $entry = $archive.GetEntry($name)
        if ($null -eq $entry) { throw "Missing verified client resource: $name" }
        $target = Join-Path $destination ([IO.Path]::GetFileName($name))
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
    }
} finally {
    $archive.Dispose()
}

Write-Host "Verified Minecraft 1.4.7 client SHA-1: $($client.sha1)"
Write-Host "Set ZTO_CREATIVE_ASSETS to this staging directory for the local prototype:"
Write-Output $stage
