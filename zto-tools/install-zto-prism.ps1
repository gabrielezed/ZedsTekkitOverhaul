param(
    [Parameter(Mandatory)][string]$Instance,
    [string]$ArtifactDirectory = (Join-Path (Split-Path $PSScriptRoot) 'dist-zto'),
    [switch]$Staging
)

$ErrorActionPreference = 'Stop'
$instanceDir = Get-Item -LiteralPath $Instance
$instancePath = $instanceDir.FullName
if ($Staging) {
    $stageRoot = (Resolve-Path -LiteralPath (Join-Path (Split-Path $PSScriptRoot) 'dist-zto')).Path
    if ($instanceDir.Parent.FullName -ne $stageRoot -or
        $instanceDir.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        throw 'A staged instance must be a regular direct child of dist-zto.'
    }
} elseif (Get-Process -Name prismlauncher -ErrorAction SilentlyContinue) {
    throw 'Close Prism before installing its component.'
}
$profile = Join-Path $instancePath 'mmc-pack.json'
$config = Join-Path $instancePath 'instance.cfg'
$patch = Join-Path $instancePath 'patches/org.zeds.tekkitoverhaul.agent.json'
$agentFile = Join-Path $instancePath 'libraries/ZTOAgent.jar'
$gamePath = Join-Path $instancePath 'minecraft'
if (!(Test-Path -LiteralPath $gamePath)) { $gamePath = Join-Path $instancePath '.minecraft' }
$mods = (Resolve-Path -LiteralPath (Join-Path $gamePath 'mods')).Path
$modFile = Join-Path $mods 'ZedsTekkitOverhaul.jar'
$pack = Get-Content -LiteralPath $profile -Raw | ConvertFrom-Json
$minecraft = @($pack.components | Where-Object uid -eq 'net.minecraft')
if ($pack.formatVersion -ne 1 -or $minecraft.Count -ne 1 -or $minecraft[0].version -ne '1.2.5') {
    throw 'Expected a Prism Minecraft 1.2.5 instance.'
}
if (Get-ChildItem -LiteralPath $mods -File -Filter 'ZedsTekkitOverhaul*.jar' | Where-Object Name -ne 'ZedsTekkitOverhaul.jar') {
    throw 'Remove competing ZTO mod archives from this instance before installing.'
}

$uid = 'org.zeds.tekkitoverhaul.agent'
$patchDirectory = Split-Path $patch
$legacyName = 'ZTO(?:InventoryDragAgent|LunarAgent|IndustrialAgent|Agent)\.jar'
if (Test-Path -LiteralPath $patchDirectory) {
    foreach ($other in Get-ChildItem -LiteralPath $patchDirectory -File -Filter '*.json') {
        if ($other.FullName -ne $patch -and (Get-Content -LiteralPath $other.FullName -Raw) -match $legacyName) {
            throw "Remove the previous ZTO agent component in Prism: $($other.Name)"
        }
    }
}
$configText = [IO.File]::ReadAllText($config)
$legacyArgument = '(?i)(?<=^|[\s"=])(?:\\")?-javaagent:(?:\\")?(?:(?!\s+-[^\s"])[^"\r\n])*?' +
    $legacyName + '(?:\\")?(?=[\s"]|$)'
$configText = [regex]::Replace($configText, '(?m)^JvmArgs=[^\r\n]*', {
    param($line)
    $clean = [regex]::Replace($line.Value, $legacyArgument, '')
    if ($clean -match $legacyName) { throw 'Remove the previous ZTO agent entry in this instance before installing.' }
    $clean
})
$pack.components = @($pack.components | Where-Object uid -ne $uid) + @([ordered]@{
    uid = $uid; version = '0.2.0-alpha.5'; cachedName = 'ZTO Agent'
})
$patchSpec = [ordered]@{
    formatVersion = 1; uid = $uid; name = 'ZTO Agent'; version = '0.2.0-alpha.5'
    '+agents' = @([ordered]@{
        name = 'org.zeds.tekkitoverhaul:agent:0.2.0-alpha.5'
        'MMC-hint' = 'local'
        'MMC-filename' = 'ZTOAgent.jar'
    })
}

$agentSource = (Resolve-Path -LiteralPath (Join-Path $ArtifactDirectory 'ZTOAgent.jar')).Path
$modSource = (Resolve-Path -LiteralPath (Join-Path $ArtifactDirectory 'ZedsTekkitOverhaul.jar')).Path
$agentBuild = Get-Content -LiteralPath "$agentSource.build.json" -Raw | ConvertFrom-Json
$modBuild = Get-Content -LiteralPath "$modSource.build.json" -Raw | ConvertFrom-Json
if ($agentBuild.productVersion -ne $patchSpec.version -or $modBuild.productVersion -ne $patchSpec.version -or
    $agentBuild.commit -ne $modBuild.commit -or $agentBuild.buildTimestampUtc -ne $modBuild.buildTimestampUtc) {
    throw 'Expected the mod and agent from the same verified build.'
}
function Assert-Archive([string]$Path, [string]$Expected) {
    $hash = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($Path)
    try {
        if ([BitConverter]::ToString($hash.ComputeHash($stream)).Replace('-', '') -ne $Expected) {
            throw "Archive SHA-256 does not match its build: $Path"
        }
    } finally { $stream.Dispose(); $hash.Dispose() }
}
Assert-Archive $agentSource $agentBuild.jarSha256
Assert-Archive $modSource $modBuild.jarSha256
$id = [guid]::NewGuid().ToString('N')
$destinations = [ordered]@{
    $agentFile = $agentSource
    $modFile = $modSource
    $patch = ($patchSpec | ConvertTo-Json -Depth 8)
    $config = $configText
    $profile = ($pack | ConvertTo-Json -Depth 32)
}
$backups = [ordered]@{}
try {
    foreach ($destination in $destinations.Keys) {
        New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
        $pending = "$destination.$id.tmp"
        if ($destination -in $agentFile, $modFile) {
            Copy-Item -LiteralPath $destinations[$destination] -Destination $pending
        } else {
            [IO.File]::WriteAllText($pending, $destinations[$destination])
        }
    }
    foreach ($destination in $destinations.Keys) {
        $pending = "$destination.$id.tmp"
        if (Test-Path -LiteralPath $destination) {
            $backup = "$destination.$id.bak"
            [IO.File]::Replace($pending, $destination, $backup)
            $backups[$destination] = $backup
        } else {
            [IO.File]::Move($pending, $destination)
            $backups[$destination] = $null
        }
    }
    Assert-Archive $agentFile $agentBuild.jarSha256
    Assert-Archive $modFile $modBuild.jarSha256
} catch {
    foreach ($destination in $backups.Keys) {
        if ($backups[$destination]) { [IO.File]::Replace($backups[$destination], $destination, [NullString]::Value) }
        else { Remove-Item -LiteralPath $destination }
    }
    throw
} finally {
    foreach ($destination in $destinations.Keys) {
        $pending = "$destination.$id.tmp"
        if (Test-Path -LiteralPath $pending) { Remove-Item -LiteralPath $pending }
    }
}
Write-Output "Installed the ZTO mod and one Prism-managed ZTO agent in $instancePath"
