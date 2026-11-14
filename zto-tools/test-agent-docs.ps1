$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$docs = @('AGENTS.md', 'docs/agents/README.md', 'docs/agents/current-state.md', 'docs/agents/testing.md',
    'docs/agents/configuration.md', 'docs/agents/survival-alpha8.md', 'docs/agents/alpha8-maintenance.md')
foreach ($doc in $docs) {
    $file = Join-Path $root $doc
    $content = [IO.File]::ReadAllText($file)
    foreach ($link in [regex]::Matches($content, '\]\(([^)]+)\)')) {
        $target = $link.Groups[1].Value
        if ($target -match '^(?:https?://|#)') { continue }
        $relative = $target.Split('#')[0]
        $path = Join-Path (Split-Path $file) ([Uri]::UnescapeDataString($relative))
        if (!(Test-Path -LiteralPath $path)) { throw "$doc refers to missing path $relative" }
    }
    foreach ($command in [regex]::Matches($content, 'zto-tools[/\\][\w.-]+\.(?:bat|ps1)\b')) {
        if (!(Test-Path -LiteralPath (Join-Path $root $command.Value) -PathType Leaf)) {
            throw "$doc refers to missing command $($command.Value)"
        }
    }
}
$current = [IO.File]::ReadAllText((Join-Path $root 'docs/agents/current-state.md'))
$versionSource = [IO.File]::ReadAllText((Join-Path $root 'src/minecraft/zed/tekkitoverhaul/BuildVersion.java'))
$version = [regex]::Match($versionSource, 'VERSION = "([^"]+)"').Groups[1].Value
if (!$version -or !$current.Contains($version)) { throw 'Update the release in current-state.md.' }
if (!$current.Contains('minecraft/config/ZedsTekkitOverhaul.cfg') -or $current.Contains('mod_ZedsTekkitOverhaul.cfg')) {
    throw 'Update the canonical config path in current-state.md.'
}
Write-Output 'PASS: current documentation paths, commands, release and config filename'
