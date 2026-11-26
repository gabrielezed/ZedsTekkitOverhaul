param(
    [Parameter(Mandatory)][string]$CandidateDirectory,
    [Parameter(Mandatory)][string]$ArtifactDirectory
)

$ErrorActionPreference = 'Stop'
$names = @('ZedsTekkitOverhaul.jar', 'ZTOAgent.jar', 'ZedsTekkitOverhaul.jar.build.json',
    'ZTOAgent.jar.build.json', 'zto-build.properties')
$destination = (New-Item -ItemType Directory -Path $ArtifactDirectory -Force).FullName
$id = [guid]::NewGuid().ToString('N')
$backups = Join-Path $destination "artifact-backups/$id"
$committed = @()
foreach ($name in $names) {
    if (!(Test-Path -LiteralPath (Join-Path $CandidateDirectory $name) -PathType Leaf)) {
        throw "Incomplete ZTO build: $name"
    }
}
try {
    foreach ($name in $names) {
        $target = Join-Path $destination $name
        Copy-Item -LiteralPath (Join-Path $CandidateDirectory $name) -Destination "$target.$id.tmp"
    }
    foreach ($name in $names) {
        $target = Join-Path $destination $name
        if (Test-Path -LiteralPath $target -PathType Leaf) {
            New-Item -ItemType Directory -Path $backups -Force | Out-Null
            [IO.File]::Replace("$target.$id.tmp", $target, (Join-Path $backups $name))
        } else {
            [IO.File]::Move("$target.$id.tmp", $target)
        }
        $committed += $name
    }
} catch {
    foreach ($name in $committed) {
        $target = Join-Path $destination $name
        $backup = Join-Path $backups $name
        if (Test-Path -LiteralPath $backup) {
            Copy-Item -LiteralPath $backup -Destination $target -Force
        } else {
            Remove-Item -LiteralPath $target
        }
    }
    throw
} finally {
    foreach ($name in $names) {
        $pending = Join-Path $destination "$name.$id.tmp"
        if (Test-Path -LiteralPath $pending) { Remove-Item -LiteralPath $pending }
    }
}
