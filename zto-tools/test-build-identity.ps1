param([string]$CandidateJar)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$work = New-Item -ItemType Directory -Path (Join-Path $root "temp/build-identity-$([guid]::NewGuid().ToString('N'))")
try {
    $out = (New-Item -ItemType Directory -Path (Join-Path $work.FullName 'classes')).FullName
    $identity = Join-Path $root 'src/minecraft/zed/tekkitoverhaul/BuildIdentity.java'
    $version = Join-Path $root 'src/minecraft/zed/tekkitoverhaul/BuildVersion.java'
    $probe = Join-Path $PSScriptRoot 'build-tests/BuildIdentityProbe.java'
    & javac -encoding UTF-8 -source 6 -target 6 -Xlint:-options -d $out $identity $version $probe
    if ($LASTEXITCODE) { throw 'Build identity probe compilation failed.' }

    $versionText = (Get-Content -Raw $version) -replace '(?s).*VERSION = "([^"]+)";.*', '$1'
    $base = "ZTO $versionText"
    $separator = ' ' + [char]0x00b7 + ' '
    $run = {
        param($path, $expected, [switch]$Title)
        $arguments = @($expected)
        if ($Title) { $arguments += 'title' }
        & java -cp "${path};$out" BuildIdentityProbe @arguments
        if ($LASTEXITCODE) { throw 'Build identity assertion failed.' }
    }
    & $run $out $base
    $metadata = Join-Path $out 'zto-build.properties'
    'commit=d1d0be84c2612798690b14c7e115e844d449603b', 'sourceDirty=true' |
        Set-Content -Encoding ASCII -LiteralPath $metadata
    & $run $out "$base${separator}gd1d0be84 *"

    foreach ($i in 1, 2) {
        "buildTimestampUtc=synthetic-$i" | Add-Content -Encoding ASCII -LiteralPath $metadata
        $fakeJar = Join-Path $work.FullName "same-version-$i.jar"
        & jar cf $fakeJar -C $out zed -C $out zto-build.properties
        if ($LASTEXITCODE) { throw 'Fixture JAR packaging failed.' }
        $prefix = (Get-FileHash -LiteralPath $fakeJar).Hash.Substring(0, 8).ToLowerInvariant()
        & $run $fakeJar "$base${separator}b$prefix *"
    }
    if ($CandidateJar) {
        $real = (Resolve-Path -LiteralPath $CandidateJar).Path
        $sidecar = Get-Content -Raw -LiteralPath "$real.build.json" | ConvertFrom-Json
        $hash = (Get-FileHash -LiteralPath $real).Hash.ToLowerInvariant()
        if ($hash -cne $sidecar.jarSha256) { throw 'Candidate sidecar hash mismatch.' }
        $suffix = if ($sidecar.sourceDirty -eq 'true') { ' *' } else { '' }
        $overlay = Join-Path $root 'dist-zto/survival-hud-runtime/Tekkit Dev/minecraft/bin/minecraft.jar'
        & $run "$real;$overlay" "$base${separator}b$($hash.Substring(0, 8))$suffix" -Title
    }
} finally {
    $parent = (Resolve-Path -LiteralPath (Join-Path $root 'temp')).Path
    if ($work.Parent.FullName -eq $parent) { Remove-Item -LiteralPath $work.FullName -Recurse -Force }
}
