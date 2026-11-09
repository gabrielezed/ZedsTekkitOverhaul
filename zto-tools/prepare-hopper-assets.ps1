param(
    [string]$ResearchDirectory = (Join-Path $env:TEMP 'zto-hopper-1.5.2')
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
$version = $manifest.versions | Where-Object id -eq '1.5.2'
$metadataFile = Join-Path $root '1.5.2.json'
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
$destination = Join-Path $stage 'zed\hopper-1.5.2'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $resources = @{
        'gui/hopper.png' = 'gui.png'
        'textures/items/hopper.png' = 'item.png'
        'textures/blocks/hopper.png' = 'outer.png'
        'textures/blocks/hopper_top.png' = 'top.png'
        'textures/blocks/hopper_inside.png' = 'inside.png'
    }
    foreach ($name in $resources.Keys) {
        $entry = $archive.GetEntry($name)
        if ($null -eq $entry) { throw "Missing verified client resource: $name" }
        $target = Join-Path $destination $resources[$name]
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
    }
} finally {
    $archive.Dispose()
}

Write-Host "Verified Minecraft 1.5.2 client SHA-1: $($client.sha1)"
Write-Host "Set ZTO_HOPPER_ASSETS to this staging directory for the local prototype:"
Write-Output $stage
