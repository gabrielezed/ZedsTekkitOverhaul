param(
    [Parameter(Mandatory=$true)][string]$Candidate,
    [string]$Installed,
    [ValidateSet('local-dev', 'public')][string]$Profile = 'local-dev'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'file-sha256.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path $PSScriptRoot
$dist = Join-Path $root 'dist-zto'
$paths = @($Installed, (Join-Path $dist 'ZedsTekkitOverhaul.jar'),
    (Join-Path $dist 'ZedsTekkitOverhaul.previous.jar'), (Join-Path $dist 'ZedsTekkitOverhaul.replaced.jar')) |
    Where-Object { $_ } | Select-Object -Unique
$hopperConfigured = $false
if ($Installed -and $Profile -eq 'local-dev') {
    $config = Join-Path (Split-Path (Split-Path $Installed -Parent) -Parent) 'config/ZedsTekkitOverhaul.cfg'
    $hopperConfigured = (Test-Path -LiteralPath $config -PathType Leaf) -and
        (Select-String -LiteralPath $config -Pattern '^\s*(?:block\.)?hopper\s*[=:\s]' -Quiet)
}
$groups = @(
    @{ Name = 'Hopper'; Root = $env:ZTO_HOPPER_ASSETS; Required = $hopperConfigured
       Entries = @('gui', 'outer', 'top', 'inside', 'item' | ForEach-Object { "zed/hopper-1.5.2/$_.png" }) }
    @{ Name = 'Creative'; Root = $env:ZTO_CREATIVE_ASSETS; Required = $false
       Entries = @('allitems', 'list_items', 'search', 'survival_inv' | ForEach-Object { "zed/creative-1.4.7/$_.png" }) }
)

function Test-CompleteGroup($archive, $entries) {
    if ($null -eq $archive) { return $false }
    foreach ($name in $entries) {
        $entry = $archive.GetEntry($name)
        if ($null -eq $entry -or $entry.Length -eq 0) { return $false }
    }
    return $true
}

$archives = @{}
$tempRoot = (Resolve-Path -LiteralPath (Join-Path $root 'temp')).Path
$stage = Join-Path $tempRoot ("zto-package-assets-" + [guid]::NewGuid())
$stagedEntries = @()
try {
    New-Item -ItemType Directory -Path $stage | Out-Null
    if ($Profile -eq 'local-dev') {
        foreach ($path in $paths) {
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                $archives[$path] = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($path))
            }
        }
        $profileManifest = Join-Path $root 'artwork/manifests/local-dev.json'
        $spec = Get-Content -Raw -LiteralPath $profileManifest | ConvertFrom-Json
        $profileHash = Get-ZtoFileSha256 $profileManifest
        $overlay = Join-Path $root "artwork/local-dev/$profileHash"
        $arguments = @('uf', [IO.Path]::GetFullPath($Candidate))
        foreach ($asset in $spec.assets) {
            $arguments += @('-C', $overlay, $asset.runtime)
        }
        & jar.exe @arguments
        if ($LASTEXITCODE) { throw 'JDK jar could not package approved local-dev machine artwork.' }
        Write-Host "[ZTO] Packaged $(@($spec.assets).Count) approved local-dev machine overrides."
        foreach ($group in $groups) {
            $source = $null
            if ($group.Root) {
                foreach ($name in $group.Entries) {
                    $asset = Get-Item -LiteralPath (Join-Path $group.Root $name)
                    if ($asset.PSIsContainer -or $asset.Length -eq 0) { throw "Invalid local asset: $($asset.FullName)" }
                }
            } else {
                foreach ($path in $paths) {
                    if (Test-CompleteGroup $archives[$path] $group.Entries) {
                        $source = $archives[$path]
                        break
                    }
                }
                if ($null -eq $source) {
                    if ($group.Required) {
                        throw 'This instance has a configured Hopper block. Restore its assets or set ZTO_HOPPER_ASSETS before deploying.'
                    }
                    continue
                }
            }
            foreach ($name in $group.Entries) {
                $destination = Join-Path $stage $name
                New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
                if ($group.Root) {
                    Copy-Item -LiteralPath (Join-Path $group.Root $name) -Destination $destination
                } else {
                    [IO.Compression.ZipFileExtensions]::ExtractToFile($source.GetEntry($name), $destination)
                }
                $stagedEntries += $name
            }
            Write-Host "[ZTO] Packaged $($group.Name) assets."
        }
    }

    if ($stagedEntries.Count -gt 0) {
        $jarArguments = @('uf', [IO.Path]::GetFullPath($Candidate))
        foreach ($name in $stagedEntries) { $jarArguments += @('-C', $stage, $name) }
        & jar.exe @jarArguments
        if ($LASTEXITCODE) { throw 'JDK jar could not package local assets.' }
    }
    & javac.exe -source 8 -target 8 -d $stage (Join-Path $PSScriptRoot 'VerifyModArchive.java')
    if ($LASTEXITCODE) { throw 'Could not compile the legacy archive validator.' }
    & java.exe -cp $stage VerifyModArchive $Candidate
    if ($LASTEXITCODE) { throw 'Candidate JAR is incompatible with the legacy streaming ZIP reader.' }
} finally {
    foreach ($archive in $archives.Values) { $archive.Dispose() }
    $owned = Get-Item -LiteralPath $stage -ErrorAction SilentlyContinue
    if ($owned -and $owned.Parent.FullName -eq $tempRoot -and
        !$owned.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $owned.FullName -Recurse -Force
    }
}
