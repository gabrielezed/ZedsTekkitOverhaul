param([string]$DependencyDirectory)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$modules = Join-Path $PSScriptRoot 'zto-agent'
if (!$DependencyDirectory) { $DependencyDirectory = Join-Path $root 'temp/zto-agent-deps' }
$dependency = New-Item -ItemType Directory -Path $DependencyDirectory -Force
$version = '9.9.1'
$pins = [ordered]@{
    asm = '6F3828A215C920059A5EFA2FB55C233D6C54EC5CADCA99CE1B1BDD10077C7DDD'
    'asm-commons' = 'C2319E014CE7199F2B7F7D56D6BB991863168C3F4B6CD6C9F542A4937EF7EF88'
}
function Get-ArtifactHash([string]$Path) {
    $hash = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($Path)
    try { return [BitConverter]::ToString($hash.ComputeHash($stream)).Replace('-', '') }
    finally { $stream.Dispose(); $hash.Dispose() }
}
foreach ($name in $pins.Keys) {
    $file = Join-Path $dependency.FullName "$name-$version.jar"
    if (!(Test-Path -LiteralPath $file)) {
        $download = "$file.$([guid]::NewGuid().ToString('N')).download"
        try {
            $uri = "https://repo.maven.apache.org/maven2/org/ow2/asm/$name/$version/$name-$version.jar"
            Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $download
            if ((Get-ArtifactHash $download) -ne $pins[$name]) { throw "Invalid ASM download $name" }
            Move-Item -LiteralPath $download -Destination $file
        } finally {
            if (Test-Path -LiteralPath $download) { Remove-Item -LiteralPath $download }
        }
    }
    if ((Get-ArtifactHash $file) -ne $pins[$name]) { throw "Invalid cached ASM artifact $file" }
}
$asm = Join-Path $dependency.FullName "asm-$version.jar"
$commons = Join-Path $dependency.FullName "asm-commons-$version.jar"
$dist = New-Item -ItemType Directory -Path (Join-Path $root 'dist-zto') -Force
$buildParent = (Resolve-Path -LiteralPath (Join-Path $root 'temp')).Path
$id = "zto-agent-build-$([guid]::NewGuid().ToString('N'))"
$build = New-Item -ItemType Directory -Path (Join-Path $buildParent $id)
try {
    $classes = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'classes')
    $tool = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'tool')
    $source = @('ZTOAgent','InventoryHook','FurnaceTransferHook','LunarHook','IndustrialHooks','NaturalSpawnHook','JetpackHook','ProvenanceHook','UnderwaterHook','TitleBrandingHook') |
        ForEach-Object { Join-Path $modules "$_.java" }
    & javac -source 8 -target 8 -cp $asm -d $classes.FullName $source
    if ($LASTEXITCODE) { throw 'Agent compilation failed.' }
    & javac -source 8 -target 8 -cp "$asm;$commons" -d $tool.FullName (Join-Path $modules 'PackageAgent.java')
    if ($LASTEXITCODE) { throw 'Relocation tool compilation failed.' }
    $candidate = Join-Path $build.FullName 'ZTOAgent.jar'
    & java -cp "$($tool.FullName);$asm;$commons" PackageAgent $classes.FullName $asm $modules $candidate
    if ($LASTEXITCODE) { throw 'Agent packaging failed.' }
    Move-Item -LiteralPath $candidate -Destination (Join-Path $dist.FullName 'ZTOAgent.jar') -Force
} finally {
    $resolved = Get-Item -LiteralPath $build.FullName
    if ($resolved.Parent.FullName -eq $buildParent -and $resolved.Name -eq $id -and
        !$resolved.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $resolved.FullName -Recurse -Force
    }
}
