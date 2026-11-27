package zed.tekkitoverhaul.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationPartType;

/** Validated startup settings shared by the local client features. */
public final class ZtoConfig {
    public final boolean experimentalSiteSieges, siteSiegeStructuralBreaching;
    public final boolean playerAssault, lunarPatrols, lunarOutdoorLightPressure;
    public final boolean experimentalEnderkinEncounters, experimentalTerritorialThreat, disableNightSkipping;
    public final int assaultSpawnBudget, assaultWaveInterval, assaultActiveCap, assaultOuterRadius;
    public final int territorialHostileCapBonusPercent;
    public final boolean naturalEnderkinSpawning, naturalAerialEncounters;
    public final boolean forestMagpies, fuglChickenHunting;
    public final int aerialEncounterCooldownSeconds;
    public final boolean electricJetpackWeather, lunarAerialEncounters;
    public final boolean creativeInventoryBackport, partialRecipeFill;
    public final int craftingUxShortcut;
    public final int lunarDebugKey, survivalDebugHudKey, survivalDebugLogCapacity;
    public final boolean lunarVerboseLogging, survivalDebugHud;
    public final int advancedCraftingTable, foundry, alchemicalConverter, skirmisherEgg, breacherEgg, saboteurEgg;
    public final int tankEgg, thiefEgg, aerialPredatorEgg, skelebirdEgg, magpieEgg;
    public final Integer hopper;
    public final int airCompressor, underwaterMask, oxygenTank, flippers;
    private final Map<String, Integer> abyssalIds = new LinkedHashMap<String, Integer>();

    Iterable<String> abyssalRegistryKeys() {
        return abyssalIds.keySet();
    }

    public int abyssalId(String key) {
        Integer id = abyssalIds.get(key);
        if (id == null) throw new IllegalArgumentException("Unknown abyssal registry key " + key);
        return id;
    }

    public final int outboardMotor, motorboat, motorboatEntity;
    public final int buildcraftOilBucketItemId;
    public final String emcProfile;
    private final int[] parts = new int[DistillationPartType.values().length];
    private final int[] flowing = new int[DistillateType.values().length];
    private final int[] buckets = new int[flowing.length];

    ZtoConfig(Properties primary, Properties legacy, boolean hopperAssets, Logger log,
              Map<String, String> entries) {
        Values values = new Values(primary, legacy, log, entries);
        values.section("Survival");
        playerAssault = values.flag("playerAssault", true);
        experimentalSiteSieges = values.flag("experimentalSiteSieges", true);
        siteSiegeStructuralBreaching = values.flag("siteSiegeStructuralBreaching", false);
        lunarPatrols = values.flag("lunarPatrols", true);
        lunarOutdoorLightPressure = values.flag("lunarOutdoorLightPressure", true);
        experimentalEnderkinEncounters = values.flag("experimentalEnderkinEncounters", false);
        experimentalTerritorialThreat = values.flag("experimentalTerritorialThreat", false);
        territorialHostileCapBonusPercent = values.bounded("territorialHostileCapBonusPercent", 70, 0, 100);
        forestMagpies = values.flag("forestMagpies", true);
        fuglChickenHunting = values.flag("fuglChickenHunting", true);
        naturalEnderkinSpawning = values.flag("naturalEnderkinSpawning", true);
        naturalAerialEncounters = values.flag("naturalAerialEncounters", true);
        electricJetpackWeather = values.flag("electricJetpackWeather", true);
        lunarAerialEncounters = values.flag("lunarAerialEncounters", true);
        aerialEncounterCooldownSeconds = values.bounded("aerialEncounterCooldownSeconds", 180, 90, 600);
        disableNightSkipping = values.flag("disableNightSkipping", true);
        assaultSpawnBudget = values.bounded("assaultSpawnBudget", 15, 1, 24);
        assaultWaveInterval = values.bounded("assaultWaveInterval", 900, 400, 1200);
        assaultActiveCap = values.bounded("assaultActiveCap", 8, 2, 12);
        assaultOuterRadius = values.bounded("assaultOuterRadius", 16, 14, 28);

        values.section("Interface (LWJGL shortcuts use 0 to disable)");
        creativeInventoryBackport = values.flag("creativeInventoryBackport", true);
        partialRecipeFill = values.flag("partialRecipeFill", false);
        craftingUxShortcut = values.key("craftingUxShortcut", 0);

        values.section("Debug (LWJGL shortcuts use 0 to disable)");
        lunarDebugKey = values.key("lunarDebugKey", 0);
        lunarVerboseLogging = values.flag("lunarVerboseLogging", false);
        survivalDebugHud = values.flag("survivalDebugHud", false);
        survivalDebugHudKey = values.key("survivalDebugHudKey", 68);
        survivalDebugLogCapacity = values.bounded("survivalDebugLogCapacity", 250, 20, 300);

        values.section("Registry IDs. Keep them stable for existing worlds. Item stack IDs are constructor IDs + 256.");
        advancedCraftingTable = values.block("advancedCraftingTable", 3000);
        foundry = values.block("foundry", 3001);
        if (hopperAssets || primary.containsKey("block.hopper") || legacy.containsKey("block.hopper")) {
            hopper = values.block("hopper", 3030);
        } else {
            hopper = null;
        }
        alchemicalConverter = values.block("alchemicalConverter", 3024);
        skirmisherEgg = values.item("skirmisherEgg", 22004);
        breacherEgg = values.item("breacherEgg", 22005);
        saboteurEgg = values.item("saboteurEgg", 22006);
        tankEgg = values.item("tankEgg", 22007);
        thiefEgg = values.item("thiefEgg", 22008);
        aerialPredatorEgg = values.item("aerialPredatorEgg", 22009);
        skelebirdEgg = values.item("skelebirdEgg", 22010);
        magpieEgg = values.item("magpieEgg", 22011);
        airCompressor = values.block("airCompressor", 3031);
        underwaterMask = values.item("underwaterMask", 22012);
        oxygenTank = values.item("oxygenTank", 22013);
        flippers = values.item("flippers", 22014);
        outboardMotor = values.item("outboardMotor", 22015);
        motorboat = values.item("motorboat", 22016);
        abyssalIds.put("biome.abyssalTrench", values.biome("abyssalTrench", 40));
        abyssalIds.put("block.abyssalStone", values.block("abyssalStone", 3032));
        abyssalIds.put("block.abyssalSilt", values.block("abyssalSilt", 3033));
        abyssalIds.put("block.shellstone", values.block("shellstone", 3034));
        abyssalIds.put("block.petrifiedCoral", values.block("petrifiedCoral", 3035));
        abyssalIds.put("block.abyssalBricks", values.block("abyssalBricks", 3036));
        abyssalIds.put("block.erodedAbyssalBricks", values.block("erodedAbyssalBricks", 3037));
        abyssalIds.put("block.abyssalLamp", values.block("abyssalLamp", 3038));
        abyssalIds.put("block.abyssalKelp", values.block("abyssalKelp", 3039));
        abyssalIds.put("block.ribbonAlgae", values.block("ribbonAlgae", 3040));
        abyssalIds.put("block.deepwaterFanCoral", values.block("deepwaterFanCoral", 3041));
        abyssalIds.put("block.tubeSponge", values.block("tubeSponge", 3042));
        abyssalIds.put("block.ghostAnemone", values.block("ghostAnemone", 3043));
        abyssalIds.put("item.ribbonAlgaeCutting", values.item("ribbonAlgaeCutting", 22018));
        abyssalIds.put("item.fanCoralFragment", values.item("fanCoralFragment", 22019));
        abyssalIds.put("item.spongeCutting", values.item("spongeCutting", 22020));
        abyssalIds.put("item.luminousPolyp", values.item("luminousPolyp", 22021));
        abyssalIds.put("item.kelpFrond", values.item("kelpFrond", 22017));
        abyssalIds.put("item.abyssalShell", values.item("abyssalShell", 22022));
        abyssalIds.put("block.abyssalStairs", values.block("abyssalStairs", 3044));
        abyssalIds.put("block.erodedAbyssalStairs", values.block("erodedAbyssalStairs", 3045));
        abyssalIds.put("block.abyssalSlab", values.block("abyssalSlab", 3046));
        abyssalIds.put("block.abyssalPillar", values.block("abyssalPillar", 3047));
        abyssalIds.put("block.abyssalCarved", values.block("abyssalCarved", 3048));
        abyssalIds.put("block.abyssalFrieze", values.block("abyssalFrieze", 3049));
        abyssalIds.put("block.abyssalGrate", values.block("abyssalGrate", 3050));
        abyssalIds.put("block.abyssalPedestal", values.block("abyssalPedestal", 3051));
        abyssalIds.put("item.creatureEgg", values.item("creatureEgg", 22023));
        motorboatEntity = values.integer("entity.motorboat", 240, 1, 254, false);
        for (DistillationPartType type : DistillationPartType.values()) {
            parts[type.index] = values.block(type.modelName, 3010 + type.index);
        }
        for (DistillateType type : DistillateType.values()) {
            String name = type.name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
            flowing[type.id] = values.integer("block." + name + "Flowing", 3016 + type.id * 2, 1, 4094, false);
            buckets[type.id] = values.item(name + "Bucket", 22000 + type.id);
        }

        values.section("Integration");
        buildcraftOilBucketItemId = values.integer("integration.buildcraftOilBucketItemId", -1, -1, 31999, false);
        values.section("EMC profile. Leave empty for compatibility values. Separate profile lines with \\n.");
        emcProfile = primary.getProperty("emc.profile", legacy.getProperty("emc.profile", ""));
        entries.put("emc.profile", emcProfile);
    }

    public int partId(DistillationPartType type) { return parts[type.index]; }
    public int flowingId(DistillateType type) { return flowing[type.id]; }
    public int bucketId(DistillateType type) { return buckets[type.id]; }

    private static final class Values {
        private final Properties primary, legacy;
        private final Logger log;
        private final Map<String, String> entries;

        Values(Properties primary, Properties legacy, Logger log, Map<String, String> entries) {
            this.primary = primary;
            this.legacy = legacy;
            this.log = log;
            this.entries = entries;
        }

        void section(String text) { entries.put("# " + text, null); }

        String raw(String key) {
            String value = primary.getProperty(key, legacy.getProperty(key));
            return value == null ? null : value.trim();
        }

        boolean flag(String key, boolean fallback) {
            String value = raw(key);
            boolean result = fallback;
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                result = Boolean.parseBoolean(value);
            } else if (value != null) {
                invalid(key, fallback);
            }
            entries.put(key, Boolean.toString(result));
            return result;
        }

        int integer(String key, int fallback, int min, int max, boolean clamp) {
            String value = raw(key);
            int result = fallback;
            if (value != null) {
                try {
                    result = Integer.parseInt(value);
                    if (result < min || result > max) {
                        result = clamp ? Math.max(min, Math.min(max, result)) : fallback;
                        invalid(key, result);
                    }
                } catch (NumberFormatException invalid) {
                    invalid(key, fallback);
                }
            }
            entries.put(key, Integer.toString(result));
            return result;
        }

        int bounded(String key, int fallback, int min, int max) {
            return integer(key, fallback, min, max, true);
        }

        int key(String key, int fallback) {
            return integer(key, fallback, 0, 255, false);
        }

        int biome(String name, int fallback) {
            String key = "biome." + name;
            String value = raw(key);
            int id = fallback;
            if (value != null) {
                try {
                    id = Integer.parseInt(value);
                } catch (NumberFormatException invalid) {
                    throw new IllegalArgumentException(key + " must be a biome ID between 0 and 254.", invalid);
                }
                if (id < 0 || id >= 255)
                    throw new IllegalArgumentException(key + " must be a biome ID between 0 and 254.");
            }
            entries.put(key, Integer.toString(id));
            return id;
        }

        int block(String name, int fallback) { return integer("block." + name, fallback, 1, 4095, false); }
        int item(String name, int fallback) { return integer("item." + name, fallback, 0, 31743, false); }

        void invalid(String key, Object fallback) {
            log.warning("[ZTO] Invalid " + key + " in " + (primary.containsKey(key) ?
                    ZtoConfigLoader.FILE_NAME : "legacy configuration") + ". Using " + fallback + ".");
        }
    }
}
