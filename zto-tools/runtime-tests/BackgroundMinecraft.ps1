function Start-BackgroundMinecraft {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string]$ArgumentList,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [string]$RedirectStandardOutput,
        [string]$RedirectStandardError
    )
    if ([bool]$RedirectStandardOutput -ne [bool]$RedirectStandardError) {
        throw 'Specify both runtime log paths or neither.'
    }
    if (!('BackgroundMinecraftNative' -as [type])) {
        Add-Type -Path (Join-Path $PSScriptRoot 'BackgroundMinecraft.cs')
    }
    $executable = (Get-Command $FilePath -CommandType Application -ErrorAction Stop).Source
    $desktop = 'ZTO-' + [Guid]::NewGuid().ToString('N')
    $process = [BackgroundMinecraftNative]::Start($desktop, $executable, $ArgumentList, $WorkingDirectory,
        $RedirectStandardOutput, $RedirectStandardError)
    $process | Add-Member -NotePropertyName BackgroundDesktop -NotePropertyValue $desktop -PassThru
}
