param(
    [string[]]$Java = @('java'),
    [string]$PackJava,
    [long]$TrenchSeed = 32021,
    [string]$TrenchPreviewDirectory,
    [switch]$Pack,
    [switch]$AerialSample,
    [string]$CandidateJar,
    [string]$IndustrialProfileBaseline,
    [switch]$TerrainBaseline,
    [switch]$CustodianOwnerSave,
    [ValidateSet('furnace','mining','breacher','provenance','settlement','cabin','mutations','biome','settlement-profile',
        'compound','protection','query','industrial','industrial-profile','modded','modded-profile',
        'partial','partial-baseline','site-siege','site-siege-escalation','site-siege-industry','site-siege-terrain',
        'site-siege-completion','underwater','distillation','motorboat','abyssal','trench','observatory','custodian')][string]$Focus
)

$ErrorActionPreference = 'Stop'
if ($CustodianOwnerSave -and $Focus -ne 'custodian') {
    throw 'CustodianOwnerSave requires -Focus custodian.'
}
if ($IndustrialProfileBaseline -and $Focus -ne 'industrial-profile') {
    throw 'IndustrialProfileBaseline requires -Focus industrial-profile.'
}
if ($IndustrialProfileBaseline) {
    $IndustrialProfileBaseline = (Resolve-Path -LiteralPath $IndustrialProfileBaseline).Path
}
if ($TerrainBaseline -and ($Focus -ne 'site-siege-terrain' -or !$CandidateJar)) {
    throw 'TerrainBaseline requires -Focus site-siege-terrain and -CandidateJar.'
}
$root = Split-Path $PSScriptRoot
$agent = (Resolve-Path -LiteralPath (Join-Path $root 'dist-zto/ZTOAgent.jar')).Path
$runtimeSource = Join-Path $root 'dist-zto/survival-hud-runtime'
$game = Join-Path $runtimeSource 'Tekkit Dev/minecraft'
$overlay = (Resolve-Path -LiteralPath (Join-Path $game 'bin/minecraft.jar')).Path
$parent = (Resolve-Path -LiteralPath (Join-Path $root 'temp')).Path
$id = "zto-agent-probe-$([guid]::NewGuid().ToString('N'))"
$out = New-Item -ItemType Directory -Path (Join-Path $parent $id)
try {
    & javac -source 8 -target 8 -cp $agent -d $out.FullName (Join-Path $PSScriptRoot 'zto-agent/AgentProbe.java')
    if ($LASTEXITCODE) { throw 'Agent probe compilation failed.' }
    foreach ($runtime in $Java) {
        & $runtime '-Xverify:all' "-javaagent:$agent" -cp $out.FullName zed.tekkitoverhaul.agent.AgentProbe archive $overlay
        if ($LASTEXITCODE) { throw "Standalone agent probe failed for $runtime" }
    }
    if ($Pack -or $AerialSample -or $Focus) {
        $candidate = Join-Path $root 'dist-zto/ZedsTekkitOverhaul.jar'
        if ($CandidateJar) { $CandidateJar = (Resolve-Path -LiteralPath $CandidateJar).Path }
        if ($CandidateJar -and !$AerialSample) { $candidate = $CandidateJar }
        $headless = @($out.FullName, $overlay, $candidate, $agent, (Join-Path $root 'jars/bin/lwjgl.jar'))
        $buildCraft = @('buildcraft-client-A-core-2.2.14.zip','buildcraft-client-B-transport-2.2.14.zip') |
            ForEach-Object { Join-Path $game "mods/$_" }
        $industrialCraft = Join-Path $game 'mods/industrialcraft-2-client_1.97.jar'
        $transports = @('buildcraft-client-B-energy-2.2.14.zip','buildcraft-client-B-factory-2.2.14.zip',
            'buildcraft-client-DA-additionalpipes-2.1.3.zip','RedPowerCore-2.0pr5b2.zip',
            'RedPowerMachine-2.0pr5b2.zip','RedPowerWiring-2.0pr5b2.zip','RedPowerControl-2.0pr5b2.zip','RedPowerWorld-2.0pr5b2.zip') |
            ForEach-Object { Join-Path $game "mods/$_" }
        $industrialClasspath = ($headless + $buildCraft + $industrialCraft + $transports) -join ';'
        $furnaces = @('EE2ClientV1.4.6.5.jar','Railcraft_Client_5.3.3.zip') | ForEach-Object { Join-Path $game "mods/$_" }
        $furnaceClasspath = "$industrialClasspath;$($furnaces -join ';')"
        $moddedClasspath = "$furnaceClasspath;$(Join-Path $game 'mods/ComputerCraft');$(Join-Path $game 'mods/NetherOres_Client_1.2.1.zip')"
        $headlessSources = @('NaturalSpawnProbe.java','BiomeEcologyProbe.java','MagpieScavengeProbe.java','EnderkinBehaviorProbe.java','ThiefBehaviorProbe.java',
            'IndustrialTheftProbe.java','SurvivalIntegrationProbe.java','AerialPredatorProbe.java','AerialEcologyProbe.java',
            'JetpackWeatherProbe.java','LunarAerialProbe.java','SurvivalOverlayProbe.java','WaterBehaviorProbe.java',
            'FurnaceFuelProbe.java','NativeMiningProbe.java','ProvenanceProbe.java','SettlementProbe.java','SettlementProfile.java') |
            ForEach-Object { Join-Path $PSScriptRoot "zto-agent/$_" }
        $moddedSuite = $Focus -in 'modded','modded-profile' -or (!$CandidateJar -and ($Pack -or $Focus -eq 'settlement'))
        $partialSuite = $Focus -in 'partial','partial-baseline' -or (!$CandidateJar -and ($Pack -or $Focus -eq 'settlement'))
        $siteSiegeSuite = $Focus -in 'site-siege','site-siege-escalation','site-siege-industry','site-siege-terrain',
            'site-siege-completion' -or ($Pack -and !$CandidateJar)
        $mixedSiegeSuite = $siteSiegeSuite -and (!$CandidateJar -or $Focus -in 'site-siege-terrain','site-siege-completion')
        if ($siteSiegeSuite -or $partialSuite) { $headlessSources += Join-Path $PSScriptRoot 'zto-agent/SiteSiegeProbe.java' }
        $compileClasspath = $furnaceClasspath
        if ($Focus -in 'underwater','distillation','motorboat','abyssal','trench','observatory','custodian') {
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/UnderwaterEquipmentProbe.java'
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/DistillationVisualProbe.java'
        }
        if ($Focus -eq 'trench') { $headlessSources += Join-Path $PSScriptRoot 'zto-agent/AbyssalWorldgenProbe.java' }
        if ($Focus -in 'observatory','custodian') {
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/ObservatoryProbe.java'
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/CustodianProbe.java'
        }
        if ($Focus -eq 'abyssal') { $headlessSources += Join-Path $PSScriptRoot 'zto-agent/AbyssalDecorationProbe.java' }
        if ($Focus -eq 'motorboat') { $headlessSources += Join-Path $PSScriptRoot 'zto-agent/MotorboatProbe.java' }
        if ($moddedSuite -or $mixedSiegeSuite -or $partialSuite) {
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/ModdedSettlementProbe.java'
            $compileClasspath = $moddedClasspath
        }
        if ($mixedSiegeSuite) {
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/TacticalSiteSiegeProbe.java'
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/ModdedSiteSiegeProbe.java'
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/SiteSiegeEscalationProbe.java'
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/TerrainSiteSiegeProbe.java'
        }
        $partialMain = 'PartialSettlementProbe'
        if ($partialSuite) {
            $headlessSources += Join-Path $PSScriptRoot 'zto-agent/PartialSettlementProbe.java'
            if (!$CandidateJar -and $Focus -ne 'partial-baseline') {
                $headlessSources += Join-Path $PSScriptRoot 'zto-agent/ManualPartialSiteSiegeProbe.java'
                $partialMain = 'ManualPartialSiteSiegeProbe'
            }
        }
        & javac -source 8 -target 8 -cp $compileClasspath -d $out.FullName @headlessSources
        if ($LASTEXITCODE) { throw 'Pack gameplay probe compilation failed.' }
        if ($Focus -eq 'underwater') {
            $fogClasses = New-Item -ItemType Directory -Path (Join-Path $out.FullName 'fog-classes')
            $fogLwjgl = Join-Path $out.FullName 'lwjgl-fog-fixture.jar'
            $lwjgl = Join-Path $root 'jars/bin/lwjgl.jar'
            Copy-Item -LiteralPath $lwjgl -Destination $fogLwjgl
            $fogSources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'zto-agent/fog-probe') -Filter '*.java'
            & javac -source 8 -target 8 -cp $lwjgl -d $fogClasses.FullName @($fogSources.FullName)
            if ($LASTEXITCODE) { throw 'Fog capture compilation failed.' }
            & jar uf $fogLwjgl -C $fogClasses.FullName org/lwjgl/opengl
            if ($LASTEXITCODE) { throw 'Fog capture packaging failed.' }
            $industrialClasspath = $industrialClasspath.Replace($lwjgl, $fogLwjgl)
        }
        $launch = Get-Content -Raw (Join-Path $runtimeSource 'launch.json') | ConvertFrom-Json
        $classpath = @($out.FullName, $launch.classpath, $candidate)
        $mods = @(Get-ChildItem -LiteralPath (Join-Path $game 'mods') -File |
            Where-Object { $_.Extension -in '.jar','.zip' -and $_.Name -notlike 'ZedsTekkitOverhaul*.jar' })
        if (!$PackJava) { $PackJava = Join-Path (Split-Path $launch.java) 'java.exe' }
        Push-Location $out.FullName
        try {
            if ($Focus -in 'underwater','distillation','motorboat','abyssal','trench','observatory','custodian') {
                $fixture = New-Item -ItemType Directory -Path (Join-Path $out.FullName "$Focus-fixture")
                $config = New-Item -ItemType Directory -Path (Join-Path $fixture.FullName 'config')
                Copy-Item -LiteralPath (Join-Path $game 'config/IC2.cfg') -Destination $config.FullName
                $buildcraftConfig = New-Item -ItemType Directory -Path (Join-Path $fixture.FullName 'buildcraft/config')
                Copy-Item -LiteralPath (Join-Path $game 'buildcraft/config/buildcraft.cfg') -Destination $buildcraftConfig.FullName
                $probe = if ($Focus -eq 'motorboat') { 'MotorboatProbe' } elseif ($Focus -eq 'abyssal') { 'AbyssalDecorationProbe' } else { 'UnderwaterEquipmentProbe' }
                if ($Focus -in 'observatory','custodian') {
                    $previews = Join-Path $root 'dist-zto/observatory-beta1-previews'
                    $ownerArgs = @()
                    if ($CustodianOwnerSave) { $ownerArgs += 'owner-save' }
                    & $PackJava '-Xverify:all' '-Djava.awt.headless=true' "-javaagent:$agent" -cp $furnaceClasspath ObservatoryProbe $fixture.FullName $previews $Focus @ownerArgs
                    if ($LASTEXITCODE) { throw 'Native Observatory fixture failed.' }
                    if ($CustodianOwnerSave) {
                        Add-Type -AssemblyName System.IO.Compression.FileSystem
                        $artifact = Join-Path $root "dist-zto/abyss-beta1-owner-$([guid]::NewGuid().ToString('N')).zip"
                        [IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $fixture.FullName 'ZTO Beauty of the Abyss'),
                            $artifact, [IO.Compression.CompressionLevel]::Optimal, $true)
                        Write-Output "Owner save: $artifact"
                    }
                    return
                }
                if ($Focus -eq 'trench') {
                    Copy-Item -LiteralPath (Join-Path $game 'redpower') -Destination $fixture.FullName -Recurse
                    if (!$TrenchPreviewDirectory) {
                        $TrenchPreviewDirectory = Join-Path $root "dist-zto/trench-refinement/after/$TrenchSeed"
                    }
                    & $PackJava '-Xverify:all' '-Djava.awt.headless=true' "-javaagent:$agent" -cp $industrialClasspath AbyssalWorldgenProbe $fixture.FullName $TrenchPreviewDirectory $TrenchSeed
                    if ($LASTEXITCODE) { throw 'Native trench fixture failed.' }
                    return
                }
                & $PackJava '-Xverify:all' '-Djava.awt.headless=true' "-javaagent:$agent" -cp $industrialClasspath $probe $fixture.FullName $Focus
                if ($LASTEXITCODE) { throw "Native $Focus fixture failed." }
                return
            }
            if ($Focus -eq 'biome') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') BiomeEcologyProbe $out.FullName
                if ($LASTEXITCODE) { throw 'Biome ecology probe failed.' }
                return
            }
            if ($Focus -in 'mutations','settlement-profile','compound','protection','query','industrial-profile') {
                if ($IndustrialProfileBaseline) {
                    $before = New-Item -ItemType Directory -Path (Join-Path $out.FullName 'before')
                    Write-Output 'Industrial profile before'
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($industrialClasspath.Replace($candidate, $IndustrialProfileBaseline)) SettlementProbe $before.FullName $Focus
                    if ($LASTEXITCODE) { throw 'Baseline industrial profile failed.' }
                    Write-Output 'Industrial profile after'
                }
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SettlementProbe $out.FullName $Focus
                if ($LASTEXITCODE) { throw "Native settlement $Focus probe failed." }
                return
            }
            if ($siteSiegeSuite) {
                $siegeWorlds = New-Item -ItemType Directory -Path (Join-Path $out.FullName 'site-siege-worlds')
                if ($Focus -eq 'site-siege-completion') {
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath TacticalSiteSiegeProbe $siegeWorlds.FullName completion
                    if ($LASTEXITCODE) { throw 'Native site siege completion probe failed.' }
                    return
                }
                if ($Focus -eq 'site-siege-industry') {
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $moddedClasspath ModdedSiteSiegeProbe $siegeWorlds.FullName
                    if ($LASTEXITCODE) { throw 'Native industrial site siege probe failed.' }
                    return
                }
                if ($mixedSiegeSuite -and $Focus -ne 'site-siege-escalation') {
                    $terrainArgs = @($siegeWorlds.FullName)
                    if ($TerrainBaseline) { $terrainArgs += 'baseline' }
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $moddedClasspath TerrainSiteSiegeProbe @terrainArgs
                    if ($LASTEXITCODE) { throw 'Native site siege terrain probe failed.' }
                }
                if ($Focus -eq 'site-siege-terrain') { return }
                if ($mixedSiegeSuite) {
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SiteSiegeEscalationProbe $siegeWorlds.FullName
                    if ($LASTEXITCODE) { throw 'Native site siege escalation probe failed.' }
                }
                if ($Focus -eq 'site-siege-escalation') { return }
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SiteSiegeProbe $siegeWorlds.FullName
                if ($LASTEXITCODE) { throw 'Native site siege probe failed.' }
                if ($mixedSiegeSuite) {
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath TacticalSiteSiegeProbe $siegeWorlds.FullName
                    if ($LASTEXITCODE) { throw 'Native tactical site siege probe failed.' }
                    & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $moddedClasspath ModdedSiteSiegeProbe $siegeWorlds.FullName
                    if ($LASTEXITCODE) { throw 'Native mixed-mod site siege probe failed.' }
                }
                if ($Focus -eq 'site-siege') { return }
            }
            if ($moddedSuite) {
                $probeArgs = @($out.FullName)
                if ($Focus -eq 'modded-profile') { $probeArgs += 'profile' }
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $moddedClasspath ModdedSettlementProbe @probeArgs
                if ($LASTEXITCODE) { throw 'Native modded settlement probe failed.' }
                if ($Focus -in 'modded','modded-profile') { return }
            }
            if ($partialSuite) {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $moddedClasspath $partialMain $out.FullName $Focus
                if ($LASTEXITCODE) { throw 'Native partial settlement probe failed.' }
                if ($Focus -in 'partial','partial-baseline') { return }
            }
            if ($Focus -eq 'cabin') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SettlementProbe $out.FullName cabin
                if ($LASTEXITCODE) { throw 'Native suspended cabin probe failed.' }
                return
            }
            if ($Pack -or $Focus -eq 'settlement') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SettlementProbe $out.FullName
                if ($LASTEXITCODE) { throw 'Native settlement recognition probe failed.' }
            }
            if ($Pack -or $Focus -in 'industrial','settlement') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath SettlementProbe $out.FullName industrial
                if ($LASTEXITCODE) { throw 'Native IC2 settlement probe failed.' }
                if ($Focus) { return }
            }
            if ($Pack -or $Focus -eq 'provenance') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath ProvenanceProbe $out.FullName
                if ($LASTEXITCODE) { throw 'Native provenance probe failed.' }
                if ($Focus -eq 'provenance') { return }
            }
            if ($Pack -or $Focus -eq 'mining') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath NativeMiningProbe $out.FullName
                if ($LASTEXITCODE) { throw 'Native mining probe failed.' }
                if ($Focus -eq 'mining') { return }
            }
            if ($Focus -eq 'breacher') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') SurvivalIntegrationProbe $out.FullName --breacher
                if ($LASTEXITCODE) { throw 'Breacher door probe failed.' }
                return
            }
            if ($Pack -or $Focus -eq 'furnace') {
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $furnaceClasspath FurnaceFuelProbe $out.FullName
                if ($LASTEXITCODE) { throw 'Furnace fuel transfer probe failed.' }
                if ($Focus -eq 'furnace') { return }
            }
            if ($AerialSample) {
                $sampleClasspath = $headless.Clone()
                if ($CandidateJar) { $sampleClasspath[2] = $CandidateJar }
                & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($sampleClasspath -join ';') AerialEcologyProbe $out.FullName sample
                if ($LASTEXITCODE) { throw 'Ordinary aerial balance sample failed.' }
            }
            if (!$Pack) { return }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($classpath + $mods.FullName) -join ';') zed.tekkitoverhaul.agent.AgentProbe pack
            if ($LASTEXITCODE) { throw 'Pack ABI agent probe failed.' }
            $reduced = @($mods | Where-Object Name -in 'buildcraft-client-A-core-2.2.14.zip','industrialcraft-2-client_1.97.jar')
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($classpath + $reduced.FullName) -join ';') zed.tekkitoverhaul.agent.AgentProbe reduced
            if ($LASTEXITCODE) { throw 'Reduced pack ABI agent probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') NaturalSpawnProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Natural spawning comparison failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') BiomeEcologyProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Biome ecology probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') MagpieScavengeProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Magpie scavenging and egg probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') WaterBehaviorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Mob shoreline and drowning probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($headless + $buildCraft) -join ';') EnderkinBehaviorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Enderkin behavior probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') ThiefBehaviorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Thief behavior probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp $industrialClasspath IndustrialTheftProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Industrial theft and Ender Trail probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') SurvivalIntegrationProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Survival integration probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') AerialPredatorProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Aerial predator probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') AerialEcologyProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Aerial ecology probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp (($headless + $industrialCraft) -join ';') JetpackWeatherProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Jetpack weather probe failed.' }
            & $PackJava '-Xverify:all' "-javaagent:$agent" -cp ($headless -join ';') LunarAerialProbe $out.FullName
            if ($LASTEXITCODE) { throw 'Lunar aerial probe failed.' }
            $profile = Join-Path $out.FullName 'overlay-profile'
            & $PackJava '-Xverify:all' -cp ($headless -join ';') SurvivalOverlayProbe $profile 'false' 'toggle'
            if ($LASTEXITCODE) { throw 'Overlay default or ON toggle failed.' }
            & $PackJava '-Xverify:all' -cp ($headless -join ';') SurvivalOverlayProbe $profile 'true' 'toggle'
            if ($LASTEXITCODE) { throw 'Overlay ON persistence or OFF toggle failed.' }
            & $PackJava '-Xverify:all' -cp ($headless -join ';') SurvivalOverlayProbe $profile 'false'
            if ($LASTEXITCODE) { throw 'Overlay OFF persistence failed.' }
        } finally { Pop-Location }
    }
} finally {
    $resolved = Get-Item -LiteralPath $out.FullName
    if ($resolved.Parent.FullName -eq $parent -and $resolved.Name -eq $id -and
        !$resolved.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {
        Remove-Item -LiteralPath $resolved.FullName -Recurse -Force
    }
}
