# The only recursively removable runtime path is a direct GUID child of this root.
function Assert-RegularPath([string]$Path) {
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    for ($ancestor = $item; $ancestor; $ancestor = if ($ancestor -is [IO.FileInfo]) { $ancestor.Directory } else { $ancestor.Parent }) {
        if ($ancestor.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "Linked path: $($ancestor.FullName)"
        }
    }
    $item
}

function Get-RuntimeTree([IO.DirectoryInfo]$Directory) {
    foreach ($entry in $Directory.EnumerateFileSystemInfos()) {
        if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "Linked tree entry: $($entry.FullName)"
        }
        $entry
        if ($entry -is [IO.DirectoryInfo]) { Get-RuntimeTree $entry }
    }
}

function Get-OwnedRunDirectory([string]$Runs, [string]$Path) {
    $parent = Assert-RegularPath $Runs
    $run = Assert-RegularPath $Path
    if (!$run.PSIsContainer -or $run.Parent.FullName -ne $parent.FullName -or
        $run.Name -cnotmatch '^[a-f0-9]{32}$') { throw "Unexpected runtime run path: $Path" }
    $run
}

function Assert-RuntimeFilesClosed($Entries) {
    foreach ($entry in $Entries) {
        if ($entry -is [IO.FileInfo]) {
            # Refuse to start deleting a tree with an open file, even a readable one.
            [IO.File]::Open($entry.FullName, 'Open', 'Read', 'None').Dispose()
        }
    }
}

function Remove-OwnedRunDirectory([string]$Runs, [string]$Path) {
    $run = Get-OwnedRunDirectory $Runs $Path
    $entries = @(Get-RuntimeTree $run)
    Assert-RuntimeFilesClosed $entries
    Remove-Item -LiteralPath $run.FullName -Recurse -Force -ErrorAction Stop
}
