import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationPartType;

public final class ZtoConfigSelfTest {
    private static final Logger LOG = Logger.getLogger("ZTO configuration tests");

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Paths.get(args[0]), "config-fixtures-");
        Path fresh = root.resolve("fresh");
        Path canonical = fresh.resolve(ZtoConfigLoader.FILE_NAME);
        ZtoConfig defaults = ZtoConfigLoader.load(fresh, false, LOG);
        check(defaults.naturalAerialEncounters && defaults.electricJetpackWeather && defaults.lunarAerialEncounters
                && defaults.aerialEncounterCooldownSeconds == 180,
                "natural aerial ecology defaults and cooldown");
        check(defaults.playerAssault && defaults.experimentalSiteSieges && !defaults.siteSiegeStructuralBreaching
                && defaults.lunarPatrols && defaults.lunarOutdoorLightPressure
                && defaults.disableNightSkipping && !defaults.experimentalEnderkinEncounters
                && !defaults.experimentalTerritorialThreat, "survival flags");
        check(defaults.assaultSpawnBudget == 15 && defaults.assaultWaveInterval == 900
                && defaults.assaultActiveCap == 8 && defaults.assaultOuterRadius == 16, "assault defaults");
        check(defaults.territorialHostileCapBonusPercent == 70, "natural hostile cap bonus default");
        check(defaults.naturalEnderkinSpawning, "natural Enderkin ecology defaults on");
        check(defaults.creativeInventoryBackport && !defaults.partialRecipeFill && defaults.craftingUxShortcut == 0,
                "interface defaults");
        check(!defaults.survivalDebugHud && !defaults.lunarVerboseLogging && defaults.lunarDebugKey == 0
                && defaults.survivalDebugHudKey == 68 && defaults.survivalDebugLogCapacity == 250, "debug defaults");
        check(defaults.hopper == null && defaults.foundry == 3001 && defaults.advancedCraftingTable == 3000
                && defaults.alchemicalConverter == 3024 && defaults.breacherEgg == 22005
                && defaults.skirmisherEgg == 22004 && defaults.saboteurEgg == 22006
                && defaults.tankEgg == 22007 && defaults.thiefEgg == 22008
                && defaults.aerialPredatorEgg == 22009 && defaults.skelebirdEgg == 22010
                && defaults.magpieEgg == 22011 && defaults.emcProfile.isEmpty(),
                "other defaults");
        byte[] generated = Files.readAllBytes(canonical);
        try (java.util.stream.Stream<Path> files = Files.list(fresh)) {
            check(files.count() == 1, "exactly one file");
        }
        write(fresh, "mod_ZedsTekkitOverhaul.cfg.bak", "experimentalEnderkinEncounters=true\n");
        check(!ZtoConfigLoader.load(fresh, false, LOG).experimentalEnderkinEncounters, "ignore inactive legacy");
        check(Arrays.equals(generated, Files.readAllBytes(canonical)), "no rewrite");
        check(ZtoConfigLoader.load(fresh, true, LOG).hopper == 3030, "adding Hopper assets");
        check(ZtoConfigLoader.load(fresh, false, LOG).hopper == 3030, "retain Hopper ID without assets");

        Path underwater = Files.createDirectory(root.resolve("underwater-ids"));
        String previous = "# retain the installed IDs\r\nconfigVersion=1\r\nblock.hopper=3030\r\nitem.oxygenTank=23223\r\nentity.motorboat=242\r\n";
        write(underwater, ZtoConfigLoader.FILE_NAME, previous);
        ZtoConfig diving = ZtoConfigLoader.load(underwater, true, LOG);
        check(diving.airCompressor == 3031 && diving.underwaterMask == 22012 && diving.oxygenTank == 23223
                && diving.flippers == 22014 && diving.hopper == 3030, "append diving IDs and preserve existing assignments");
        check(diving.outboardMotor == 22015 && diving.motorboat == 22016 && diving.motorboatEntity == 242,
                "append new item defaults and preserve the Motorboat entity override");
        String upgraded = read(underwater, ZtoConfigLoader.FILE_NAME);
        check(upgraded.startsWith(previous) && upgraded.contains("block.airCompressor=3031")
                && upgraded.contains("item.underwaterMask=22012") && upgraded.contains("item.flippers=22014")
                && upgraded.contains("item.outboardMotor=22015") && upgraded.contains("item.motorboat=22016"),
                "append only missing exploration IDs");
        ZtoConfigLoader.load(underwater, true, LOG);
        check(upgraded.equals(read(underwater, ZtoConfigLoader.FILE_NAME)), "underwater migration is idempotent");

        Path breachTest = Files.createDirectory(root.resolve("structural-breach-opt-in"));
        write(breachTest, ZtoConfigLoader.FILE_NAME, "siteSiegeStructuralBreaching=true\nexperimentalSiteSieges=false\n");
        ZtoConfig optedIn = ZtoConfigLoader.load(breachTest, false, LOG);
        check(optedIn.siteSiegeStructuralBreaching && !optedIn.experimentalSiteSieges,
                "structural opt-in and explicit natural opt-out are independent");

        Path older = Files.createDirectory(root.resolve("legacy"));
        String forge = "# Original comment\nblock {\nfoundry=3074\nunknown=17\n}\n";
        String profile = "# accent: café\r\nzto-emc 1 APPLY\r\nSET name:item.ingotIron 0 12\r\n";
        write(older, ZtoConfigLoader.FILE_NAME, forge);
        write(older, "mod_ZedsTekkitOverhaul.cfg", "experimentalEnderkinEncounters= TrUE  \nplayerAssault=false\nexperimentalSiteSieges=false\n");
        write(older, "mod_ZedsTekkitOverhaul.cfg.bak", "playerAssault=true\nlunarPatrols=false\n");
        write(older, "ZedsTekkitOverhaul-emc.txt", profile);
        write(older, "ZedsTekkitOverhaul-AlchemicalPower.cfg", "block {\nalchemicalConverter=3075\n}\n");
        write(older, "ZedsTekkitOverhaul-Tactical.cfg", "item {\nbreacherEgg=23222\n}\n");
        write(older, "ZedsTekkitOverhaul-Distillation.cfg",
                "block {\ncolumn=3080\n}\nitem {\nheavy_gas_oilBucket=23000\n}\n"
                + "integration {\nbuildcraftOilBucketItemId=12345\n}\n");
        write(older, "IC2.cfg", "# unrelated\n");
        ZtoConfig migrated = ZtoConfigLoader.load(older, false, LOG);
        check(migrated.experimentalEnderkinEncounters && !migrated.playerAssault && !migrated.lunarPatrols
                && !migrated.experimentalSiteSieges, "MLProp precedence and site siege opt-out migration");
        check(migrated.foundry == 3074 && migrated.alchemicalConverter == 3075 && migrated.breacherEgg == 23222
                && migrated.saboteurEgg == 22006,
                "registry migrations");
        check(migrated.partId(DistillationPartType.COLUMN) == 3080
                && migrated.bucketId(DistillateType.HEAVY_GAS_OIL) == 23000
                && migrated.buildcraftOilBucketItemId == 12345, "distillation migration");
        check(migrated.emcProfile.equals(profile) && ZtoConfigLoader.load(older, false, LOG).emcProfile.equals(profile),
                "profile encoding and escapes");
        check(read(older, ZtoConfigLoader.FILE_NAME + ".legacy").equals(forge), "Forge bytes kept");
        check(read(older, "IC2.cfg").equals("# unrelated\n"), "third-party bytes kept");

        Path partial = Files.createDirectory(root.resolve("partial"));
        String edits = "# Personal comments\nunknown=kept\nplayerAssault = TRUE\nassaultOuterRadius=banana\n"
                + "survivalDebugLogCapacity=999\nassaultActiveCap=-2\nitem.magpieEgg=23223\n";
        write(partial, ZtoConfigLoader.FILE_NAME, edits);
        write(partial, "mod_ZedsTekkitOverhaul.cfg.bak", "playerAssault=false\nlunarPatrols=invalid\n"
                + "assaultOuterRadius=22\nassaultSpawnBudget=99\nassaultWaveInterval=1\n");
        ZtoConfig mixed = ZtoConfigLoader.load(partial, false, LOG);
        check(mixed.playerAssault && mixed.lunarPatrols && mixed.assaultOuterRadius == 16, "precedence and invalid fallback");
        check(mixed.magpieEgg == 23223 && mixed.skelebirdEgg == 22010, "independent magpie egg ID override");
        check(mixed.survivalDebugLogCapacity == 300 && mixed.assaultActiveCap == 2
                && mixed.assaultSpawnBudget == 24 && mixed.assaultWaveInterval == 400, "legacy clamps");
        check(read(partial, ZtoConfigLoader.FILE_NAME).startsWith(edits), "comments and unknown keys preserved");

        Path empty = Files.createDirectory(root.resolve("empty"));
        write(empty, ZtoConfigLoader.FILE_NAME, "");
        write(empty, "mod_ZedsTekkitOverhaul.cfg.bak", "partialRecipeFill=true\n");
        check(ZtoConfigLoader.load(empty, false, LOG).partialRecipeFill, "empty upgrades");
        Path interrupted = Files.createDirectory(root.resolve("interrupted"));
        write(interrupted, ZtoConfigLoader.FILE_NAME, forge);
        write(interrupted, ZtoConfigLoader.FILE_NAME + ".legacy", forge);
        write(interrupted, "zto-settings-interrupted.tmp", "truncated");
        check(ZtoConfigLoader.load(interrupted, false, LOG).foundry == 3074, "restart after interrupted migration");

        Path blocked = Files.createDirectory(root.resolve("blocked"));
        write(blocked, ZtoConfigLoader.FILE_NAME, forge);
        write(blocked, ZtoConfigLoader.FILE_NAME + ".legacy", "# conflict\n");
        check(ZtoConfigLoader.load(blocked, false, LOG).foundry == 3074
                && read(blocked, ZtoConfigLoader.FILE_NAME).equals(forge), "write failure preserves inputs");
        System.out.println("ZTO configuration self-test passed.");
    }

    private static void check(boolean condition, String test) {
        if (!condition) throw new AssertionError(test);
    }

    private static void write(Path directory, String name, String content) throws IOException {
        Files.write(directory.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path directory, String name) throws IOException {
        return new String(Files.readAllBytes(directory.resolve(name)), StandardCharsets.UTF_8);
    }
}
