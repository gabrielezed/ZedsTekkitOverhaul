param(
    [string]$Catalog = 'dist-zto/ZedsTekkitOverhaul-emc-baseline.tsv',
    [string]$Output = 'dist-zto/ZedsTekkitOverhaul-zero-emc.md'
)

$ErrorActionPreference = 'Stop'

function Format-MetadataRange {
    param([int[]]$Metadata)
    $ranges = [System.Collections.Generic.List[string]]::new()
    $first = $last = $null
    foreach ($value in ($Metadata | Sort-Object -Unique)) {
        if ($null -ne $last -and $value -eq ($last + 1)) {
            $last = $value
            continue
        }
        if ($null -ne $last) {
            $ranges.Add($(if ($first -eq $last) { "$first" } else { "$first-$last" }))
        }
        $first = $last = $value
    }
    if ($null -ne $last) {
        $ranges.Add($(if ($first -eq $last) { "$first" } else { "$first-$last" }))
    }
    $ranges -join ', '
}

function Format-Cell {
    param([string]$Value)
    $Value.Replace('|', '\|')
}

$rows = @(Import-Csv -LiteralPath $Catalog -Delimiter "`t")
if ($rows.Count -eq 0 -or @($rows[0].PSObject.Properties.Name) -notcontains 'variant_source') {
    throw "Expected a nonempty ZTO EMC audit catalog: $Catalog"
}
$zero = @($rows.Where({ $_.emc -eq '0' }))
$ordinary = @($zero.Where({ $_.variant_source -notlike 'durability*' }))
$durability = @($zero.Where({ $_.variant_source -like 'durability*' }))
$positiveIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($row in $rows) {
    if ($row.emc -ne '' -and [int]$row.emc -gt 0) { [void]$positiveIds.Add($row.id) }
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Zero EMC audit')
$lines.Add('')
$lines.Add("Baseline SHA-256: $((Get-FileHash -LiteralPath $Catalog).Hash)")
$lines.Add('')
$lines.Add("- $($rows.Count) catalog rows and $($zero.Count) zero-EMC rows.")
$lines.Add("- $($ordinary.Count) base, creative or direct-key candidates across $(@($ordinary.id | Sort-Object -Unique).Count) runtime IDs.")
$lines.Add("- $($durability.Count) rows enumerated through the durability range across $(@($durability.id | Sort-Object -Unique).Count) runtime IDs.")
$lines.Add("- Candidates sharing an ID with a positive mapping: $(@($ordinary.Where({ $positiveIds.Contains($_.id) })).Count).")
$lines.Add('')
$lines.Add('The archive column identifies the implementing class. A replaced vanilla class may be listed under a mod archive. Durability enumeration can also reach hidden state metadata. Use the exact item identity and verify acquisition before proposing a rule.')
$lines.Add('')
$lines.Add('## Base, creative and direct-key candidates')
$lines.Add('')
$lines.Add('| Runtime archive | Zero-EMC candidates |')
$lines.Add('| --- | ---: |')
$groups = $ordinary | Group-Object owner_hint | Sort-Object -Property @{Expression='Count';Descending=$true},Name
foreach ($group in $groups) {
    $archive = if ($group.Name) { [uri]::UnescapeDataString($group.Name) } else { '(unknown)' }
    $lines.Add("| $(Format-Cell $archive) | $($group.Count) |")
}

foreach ($group in ($groups | Sort-Object Name)) {
    $archive = if ($group.Name) { [uri]::UnescapeDataString($group.Name) } else { '(unknown)' }
    $lines.Add('')
    $lines.Add("### $archive")
    $lines.Add('')
    $lines.Add('| ID | Metadata | Internal name | Display name | Evidence |')
    $lines.Add('| ---: | ---: | --- | --- | --- |')
    foreach ($row in ($group.Group | Sort-Object { [int]$_.id }, { [int]$_.metadata })) {
        $evidence = if ($row.id -eq '7') { 'Bedrock exception' }
            elseif ($row.value_source -eq 'unregistered-id') { 'Unregistered mapping' }
            elseif ($positiveIds.Contains($row.id)) { 'Positive variants share this ID' }
            else { $row.variant_source }
        $lines.Add("| $($row.id) | $($row.metadata) | $(Format-Cell $row.internal_name) | $(Format-Cell $row.display_name) | $(Format-Cell $evidence) |")
    }
}

$lines.Add('')
$lines.Add('## Durability and state candidates')
$lines.Add('')
$lines.Add('| ID | Zero-EMC metadata ranges | Base internal name | Base display name | Has any positive EMC |')
$lines.Add('| ---: | --- | --- | --- | --- |')
$bases = @{}
foreach ($row in $rows.Where({ $_.metadata -eq '0' })) { $bases[$row.id] = $row }
foreach ($group in ($durability | Group-Object id | Sort-Object { [int]$_.Name })) {
    $base = $bases[$group.Name]
    $ranges = Format-MetadataRange ($group.Group.metadata)
    $lines.Add("| $($group.Name) | $ranges | $(Format-Cell $base.internal_name) | $(Format-Cell $base.display_name) | $($positiveIds.Contains($group.Name)) |")
}

$directory = Split-Path -Parent $Output
if ($directory -and !(Test-Path -LiteralPath $directory)) { [void](New-Item -ItemType Directory -Path $directory) }
Set-Content -LiteralPath $Output -Value $lines -Encoding utf8
Write-Output "Wrote $Output ($($ordinary.Count) candidates and $($durability.Count) durability/state rows)."
