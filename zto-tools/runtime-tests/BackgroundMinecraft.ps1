function Assert-BackgroundMinecraftAvailable {
    throw 'INPUT_ISOLATION_UNVERIFIED: Automated Windows clients are disabled. Run portable checks, or use a separate VM for graphical testing.'
}

function Start-BackgroundMinecraft {
    param([string]$FilePath, [string]$ArgumentList, [string]$WorkingDirectory,
        [string]$RedirectStandardOutput, [string]$RedirectStandardError)
    Assert-BackgroundMinecraftAvailable
}
