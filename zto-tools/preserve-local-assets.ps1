param(
    [Parameter(Mandatory=$true)][string]$Candidate,
    [Parameter(Mandatory=$true)][string]$Installed
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$previous = 'dist-zto/ZedsTekkitOverhaul.previous.jar'
$published = 'dist-zto/ZedsTekkitOverhaul.jar'
$config = Join-Path (Split-Path (Split-Path $Installed -Parent) -Parent) 'config/ZedsTekkitOverhaul.cfg'
$hopperConfigured = (Test-Path -LiteralPath $config -PathType Leaf) -and
    (Select-String -LiteralPath $config -Pattern '^\s*(?:block\.)?hopper\s*[=:\s]' -Quiet)
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
$stage = Join-Path (Get-Location) ("temp/zto-package-assets-" + [guid]::NewGuid())
$stagedEntries = @()
$keepPrevious = $false
try {
    foreach ($path in @($Installed, $published, $previous)) {
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            $archives[$path] = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($path))
        }
    }
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    foreach ($group in $groups) {
        $source = $null
        if ($group.Root) {
            foreach ($name in $group.Entries) {
                $asset = Get-Item -LiteralPath (Join-Path $group.Root $name)
                if ($asset.PSIsContainer -or $asset.Length -eq 0) { throw "Invalid local asset: $($asset.FullName)" }
            }
        } else {
            foreach ($path in @($Installed, $published, $previous)) {
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
        if (!(Test-CompleteGroup $archives[$Installed] $group.Entries) -and
            (Test-CompleteGroup $archives[$previous] $group.Entries)) { $keepPrevious = $true }
        Write-Host "[ZTO] Packaged $($group.Name) assets."
    }
} finally {
    foreach ($archive in $archives.Values) { $archive.Dispose() }
}

if ($stagedEntries.Count -gt 0) {
    $jarArguments = @('uf', [IO.Path]::GetFullPath($Candidate))
    foreach ($name in $stagedEntries) { $jarArguments += @('-C', $stage, $name) }
    & jar.exe @jarArguments
    if ($LASTEXITCODE) { throw 'JDK jar could not package local assets.' }
}
& javac.exe -source 8 -target 8 -d $stage 'zto-tools/VerifyModArchive.java'
if ($LASTEXITCODE) { throw 'Could not compile the legacy archive validator.' }
& java.exe -cp $stage VerifyModArchive $Candidate
if ($LASTEXITCODE) { throw 'Candidate JAR is incompatible with the legacy streaming ZIP reader.' }

if (Test-Path -LiteralPath $Installed -PathType Leaf) {
    & java.exe -cp $stage VerifyModArchive $Installed quiet | Out-Null
    if ($LASTEXITCODE -and (Test-Path -LiteralPath $previous -PathType Leaf)) { $keepPrevious = $true }
    $backup = if ($keepPrevious) { 'dist-zto/ZedsTekkitOverhaul.replaced.jar' } else { $previous }
    Copy-Item -LiteralPath $Installed -Destination $backup -Force
}
