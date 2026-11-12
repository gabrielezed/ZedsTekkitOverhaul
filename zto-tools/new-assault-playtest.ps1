[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9](?:[A-Za-z0-9 -]{0,62}[A-Za-z0-9])?$')]
    [string]$Name = 'Lunar Assault - Playtest',
    [string]$SavesDirectory = (Join-Path $env:APPDATA 'PrismLauncher/instances/Tekkit Dev/minecraft/saves'),
    [ValidateSet('NEW_MOON', 'FIRST_QUARTER', 'WAXING_GIBBOUS', 'FULL_MOON')]
    [string]$LunarPhase = 'FULL_MOON'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$classes = Join-Path $root 'bin/minecraft'
$output = Join-Path $root 'dist-zto/playtest-tools'
$source = Join-Path $PSScriptRoot 'playtest/AssaultPlaytestSave.java'
if (!(Test-Path -LiteralPath (Join-Path $classes 'net/minecraft/src/NBTTagCompound.class') -PathType Leaf)) {
    throw 'Compile the MCP client first with recompile.bat or build-and-deploy.bat.'
}
$saves = Get-Item -LiteralPath $SavesDirectory
if (!$saves.PSIsContainer -or ($saves.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
    throw 'Use an existing, regular saves directory in Tekkit Dev or a scratch directory.'
}
$folder = 'ZTO-' + ($Name -replace '[ -]+', '-')
$target = Join-Path $saves.FullName $folder
if (Test-Path -LiteralPath $target) { throw "Save already exists: $target. Choose another -Name." }

New-Item -ItemType Directory -Force -Path $output | Out-Null
& javac -source 8 -target 8 -cp $classes -d $output $source
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$phase = @{ NEW_MOON = 0; FIRST_QUARTER = 2; WAXING_GIBBOUS = 3; FULL_MOON = 4 }[$LunarPhase]
& java -cp "$output;$classes" AssaultPlaytestSave $target $Name $phase
exit $LASTEXITCODE
