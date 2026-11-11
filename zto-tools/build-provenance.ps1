param(
    [Parameter(Mandatory)][string]$Metadata,
    [string]$Archive
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
if (!$Archive) {
    $source = Get-Content -Raw (Join-Path $root 'src/minecraft/zed/tekkitoverhaul/BuildVersion.java')
    if ($source -cnotmatch 'VERSION = "([0-9]+\.[0-9]+\.[0-9]+(?:-[a-z0-9.]+)?)";') {
        throw 'Invalid canonical product version.'
    }
    $version = $Matches[1]
    $mod = Get-Content -Raw (Join-Path $root 'src/minecraft/mcmod.info') | ConvertFrom-Json
    if ($mod.version -cne $version) { throw 'mcmod.info and BuildVersion.VERSION disagree.' }
    $commit = git -C $root rev-parse HEAD
    if ($LASTEXITCODE -ne 0 -or $commit -cnotmatch '^[a-f0-9]{40}$') { throw 'Git commit unavailable.' }
    $changes = @(git -C $root status --porcelain)
    if ($LASTEXITCODE -ne 0) { throw 'Git status unavailable.' }
    @(
        "productVersion=$version"
        "commit=$commit"
        "sourceDirty=$(([bool]$changes.Count).ToString().ToLowerInvariant())"
        "buildTimestampUtc=$([DateTime]::UtcNow.ToString('o'))"
        'compileForge=3.4.9.171'
        'compileMcp=6.2'
        'minecraftVersion=1.2.5'
    ) | Set-Content -LiteralPath $Metadata -Encoding ASCII
    Write-Output "[ZTO] $version | Git $commit | dirty=$([bool]$changes.Count)"
} else {
    $hash = [Security.Cryptography.SHA256]::Create()
    try {
        $stream = [IO.File]::OpenRead((Resolve-Path -LiteralPath $Archive).Path)
        try { $sha = [BitConverter]::ToString($hash.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() }
        finally { $stream.Dispose() }
    } finally { $hash.Dispose() }
    $values = Get-Content -Raw -LiteralPath $Metadata | ConvertFrom-StringData
    $values['jarSha256'] = $sha
    $values | ConvertTo-Json | Set-Content -LiteralPath "$Archive.build.json" -Encoding ASCII
    Write-Output "[ZTO] Archive SHA-256: $sha"
}
