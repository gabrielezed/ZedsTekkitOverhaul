# Minecraft 1.2.5 reads these two options before initializing SoundManager.
function Set-SilentMinecraftOptions([string]$Game) {
    $path = Join-Path $Game 'options.txt'
    $encoding = [Text.Encoding]::GetEncoding(28591)
    $content = if ([IO.File]::Exists($path)) { [IO.File]::ReadAllText($path, $encoding) } else { '' }
    $muted = $content -creplace '(?m)^(music|sound):[^\r\n]*(?:\r?\n|$)', ''
    [IO.File]::WriteAllText($path, "$muted`r`nmusic:0.0`r`nsound:0.0`r`n", $encoding)
}
