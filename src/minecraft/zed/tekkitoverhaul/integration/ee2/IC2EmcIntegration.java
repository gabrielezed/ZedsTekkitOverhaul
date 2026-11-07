package zed.tekkitoverhaul.integration.ee2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.ItemStack;

/**
 * EE2 v1.4.6.5 / IC2 v1.97: conservative, recipe-derived EMC registrations.
 * No IC2 or EE2 jar is required at compilation time.
 * Only missing EE2 mappings are added: existing Tekkit EMC values win.
 * Run only after EEBase.externalModsInitialized on the first game tick.
 *
 * This is an experimental first coverage pass. See data/ic2_emc_audit.csv.
 */
public final class IC2EmcIntegration {
    private final List<Recipe> recipes = new ArrayList<Recipe>();
    private boolean finished;

    private static final class Part {
        final String key;
        final int count;
        Part(String key, int count) { this.key = key; this.count = count; }
    }
    private static final class Recipe {
        final String item;
        final int output;
        final Part[] parts;
        Recipe(String item, int output, Part[] parts) {
            this.item = item; this.output = output; this.parts = parts;
        }
    }

    public IC2EmcIntegration() {
        buildRules();
    }

    private void add(String key, int output, Object... parts) {
        Part[] p = new Part[parts.length / 2];
        for (int i = 0; i < parts.length; i += 2)
            p[i / 2] = new Part((String)parts[i], ((Integer)parts[i + 1]).intValue());
        recipes.add(new Recipe(key, output, p));
    }

    /** Called from the ModLoader game tick hook, after EE2's own integration. */
    public void onGameTick() {
        if (finished) return;
        try {
            Class<?> eeBase = Class.forName("ee.EEBase");
            if (!eeBase.getField("externalModsInitialized").getBoolean(null)) return;
            Class<?> maps = Class.forName("ee.EEMaps");
            Class<?> ic2 = Class.forName("ic2.common.Ic2Items");
            Method get = maps.getMethod("getEMC", Integer.TYPE, Integer.TYPE);
            Method put = maps.getMethod("addEMC", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            // Evaluate all alternatives to each output before writing any new value.
            // Iterate because inputs such as circuits depend on earlier outputs.
            int assigned = 0;
            for (int pass = 0; pass < 15; pass++) {
                int updated = 0;
                for (int j = 0; j < recipes.size(); j++) {
                    Recipe rule = recipes.get(j);
                    if (!firstRuleFor(rule.item, j)) continue;
                    ItemStack out = findIC2(ic2, rule.item);
                    if (out == null) continue;
                    int present = emc(get, out.itemID, out.getItemDamage());
                    if (present > 0) continue;
                    int best = 0;
                    for (int k = 0; k < recipes.size(); k++) {
                        Recipe alternative = recipes.get(k);
                        if (!rule.item.equals(alternative.item)) continue;
                        long total = 0;
                        boolean valid = true;
                        for (int l = 0; l < alternative.parts.length; l++) {
                            Part part = alternative.parts[l];
                            int value = getValue(part.key, ic2, get);
                            if (value <= 0) { valid = false; break; }
                            total += (long)value * part.count;
                        }
                        if (valid) {
                            int candidate = (int)Math.max(1L, total / alternative.output);
                            if (candidate > 0 && candidate < Integer.MAX_VALUE &&
                                    (best == 0 || candidate < best)) best = candidate;
                        }
                    }
                    if (best > 0) {
                        put.invoke(null, new Object[]{ Integer.valueOf(out.itemID),
                                Integer.valueOf(out.getItemDamage()), Integer.valueOf(best) });
                        updated++;
                        assigned++;
                        System.out.println("[ZedsTekkitOverhaul] IC2 EMC " + rule.item + " = " + best);
                    }
                }
                if (updated == 0) break;
            }
            System.out.println("[ZedsTekkitOverhaul] IC2 EMC completed: " + assigned + " missing mappings registered");
            finished = true;
        } catch (ClassNotFoundException missingMod) {
            // Plain MCP workspace without EE2/IC2: wait, no changes.
        } catch (Exception err) {
            System.err.println("[ZedsTekkitOverhaul] IC2 EMC registration failed: " + err);
            err.printStackTrace();
            finished = true; // avoid retry-spamming and non-atomic partial updates
        } catch (LinkageError err) {
            System.err.println("[ZedsTekkitOverhaul] IC2 EMC linkage failed: " + err);
            finished = true;
        }
    }

    private boolean firstRuleFor(String item, int index) {
        for (int i = 0; i < index; i++)
            if (recipes.get(i).item.equals(item)) return false;
        return true;
    }

    private static ItemStack findIC2(Class<?> ic2, String name) throws Exception {
        Object value = ic2.getField(name).get(null);
        return value instanceof ItemStack ? (ItemStack)value : null;
    }

    private static int emc(Method getter, int id, int meta) throws Exception {
        return ((Integer)getter.invoke(null, new Object[]{ Integer.valueOf(id),
            Integer.valueOf(meta) })).intValue();
    }

    private static int getValue(String key, Class<?> ic2, Method get) throws Exception {
        if (key.startsWith("i:")) {
            ItemStack stack = findIC2(ic2, key.substring(2));
            return stack == null ? 0 : emc(get, stack.itemID, stack.getItemDamage());
        }
        if (key.startsWith("v:")) {
            String[] bits = key.substring(2).split(":");
            return emc(get, Integer.parseInt(bits[0]),
                bits.length == 1 ? 0 : Integer.parseInt(bits[1]));
        }
        return 0;
    }

    private void buildRules() {
/* START_GENERATED_RULES */
        add("rubber", 3, "i:resin", 1);
        add("refinedIronIngot", 1, "v:265", 1);
        add("ironFence", 12, "i:refinedIronIngot", 6);
        add("machine", 1, "i:refinedIronIngot", 8);
        add("ironFurnace", 1, "v:265", 8);
        add("ironFurnace", 1, "v:265", 5, "v:61", 1);
        add("copperCableItem", 6, "i:copperIngot", 3);
        add("insulatedCopperCableItem", 6, "i:copperIngot", 3, "i:rubber", 6);
        add("insulatedCopperCableItem", 1, "i:copperCableItem", 1, "i:rubber", 1);
        add("goldCableItem", 12, "v:266", 3);
        add("insulatedGoldCableItem", 4, "v:266", 1, "i:rubber", 4);
        add("insulatedGoldCableItem", 1, "i:goldCableItem", 1, "i:rubber", 1);
        add("doubleInsulatedGoldCableItem", 1, "i:goldCableItem", 1, "i:rubber", 2);
        add("doubleInsulatedGoldCableItem", 1, "i:insulatedGoldCableItem", 1, "i:rubber", 1);
        add("ironCableItem", 12, "i:refinedIronIngot", 3);
        add("insulatedIronCableItem", 4, "i:refinedIronIngot", 1, "i:rubber", 4);
        add("insulatedIronCableItem", 1, "i:ironCableItem", 1, "i:rubber", 1);
        add("doubleInsulatedIronCableItem", 1, "i:ironCableItem", 1, "i:rubber", 2);
        add("doubleInsulatedIronCableItem", 1, "i:insulatedIronCableItem", 1, "i:rubber", 1);
        add("trippleInsulatedIronCableItem", 1, "i:ironCableItem", 1, "i:rubber", 3);
        add("trippleInsulatedIronCableItem", 1, "i:doubleInsulatedIronCableItem", 1, "i:rubber", 1);
        add("tinCableItem", 9, "i:tinIngot", 3);
        add("glassFiberCableItem", 4, "v:20", 6, "v:331", 2, "v:264", 1);
        add("glassFiberCableItem", 6, "v:20", 6, "i:silverDust", 2, "v:264", 1);
        add("detectorCableItem", 1, "v:331", 3, "i:trippleInsulatedIronCableItem", 1, "i:electronicCircuit", 1);
        add("electronicCircuit", 1, "i:refinedIronIngot", 1, "v:331", 2, "i:insulatedCopperCableItem", 6);
        add("advancedCircuit", 1, "i:electronicCircuit", 1, "v:331", 4, "v:348", 2, "v:351:4", 2);
        add("treetap", 1, "v:5", 5);
        add("wrench", 1, "i:bronzeIngot", 6);
        add("cutter", 1, "i:refinedIronIngot", 3, "v:265", 2);
        add("toolbox", 1, "i:refinedIronIngot", 5, "v:54", 1);
        add("bronzePickaxe", 1, "i:bronzeIngot", 3, "v:280", 2);
        add("bronzeAxe", 1, "i:bronzeIngot", 3, "v:280", 2);
        add("bronzeSword", 1, "i:bronzeIngot", 2, "v:280", 1);
        add("bronzeShovel", 1, "i:bronzeIngot", 1, "v:280", 2);
        add("bronzeHoe", 1, "i:bronzeIngot", 2, "v:280", 2);
        add("bronzeHelmet", 1, "i:bronzeIngot", 5);
        add("bronzeChestplate", 1, "i:bronzeIngot", 8);
        add("bronzeLeggings", 1, "i:bronzeIngot", 7);
        add("bronzeBoots", 1, "i:bronzeIngot", 4);
        add("electroFurnace", 1, "i:electronicCircuit", 1, "v:331", 2, "i:ironFurnace", 1);
        add("macerator", 1, "v:318", 3, "v:4", 2, "i:machine", 1, "i:electronicCircuit", 1);
        add("extractor", 1, "i:treetap", 4, "i:machine", 1, "i:electronicCircuit", 1);
        add("compressor", 1, "v:1", 6, "i:machine", 1, "i:electronicCircuit", 1);
        add("electrolyzer", 1, "i:cell", 2, "i:insulatedCopperCableItem", 4, "i:machine", 1, "i:electronicCircuit", 1);
        add("advancedMachine", 1, "i:advancedAlloy", 2, "i:carbonPlate", 2, "i:machine", 1);
        add("personalSafe", 1, "i:electronicCircuit", 1, "v:54", 1, "i:machine", 1);
        add("generator", 1, "i:reBattery", 1, "i:refinedIronIngot", 3, "i:ironFurnace", 1);
        add("generator", 1, "i:reBattery", 1, "i:machine", 1, "v:61", 1);
        add("solarPanel", 1, "i:generator", 1, "i:coalDust", 3, "v:20", 3, "i:electronicCircuit", 2);
        add("waterMill", 2, "i:generator", 1, "v:5", 4, "v:280", 4);
        add("windMill", 1, "i:generator", 1, "v:265", 4);
        add("geothermalGenerator", 1, "i:generator", 1, "i:cell", 2, "v:20", 4, "i:refinedIronIngot", 2);
        add("batBox", 1, "v:5", 5, "i:insulatedCopperCableItem", 1, "i:reBattery", 3);
        add("mfeUnit", 1, "i:machine", 1, "i:doubleInsulatedGoldCableItem", 4, "i:energyCrystal", 4);
        add("mfsUnit", 1, "i:mfeUnit", 1, "i:advancedMachine", 1, "i:advancedCircuit", 1, "i:lapotronCrystal", 6);
        add("lvTransformer", 1, "v:5", 4, "i:insulatedCopperCableItem", 1, "i:copperIngot", 3);
        add("mvTransformer", 1, "i:machine", 1, "i:doubleInsulatedGoldCableItem", 2);
        add("hvTransformer", 1, "i:mvTransformer", 1, "i:trippleInsulatedIronCableItem", 2, "i:energyCrystal", 1, "i:electronicCircuit", 1);
        add("scaffold", 16, "v:5", 3, "v:280", 3);
        add("ironScaffold", 16, "i:refinedIronIngot", 3, "i:ironFence", 3);
        add("reinforcedStone", 8, "v:1", 8, "i:advancedAlloy", 1);
        add("reinforcedGlass", 7, "v:20", 7, "i:advancedAlloy", 2);
        add("reinforcedDoor", 1, "i:reinforcedStone", 6);
        add("rubberTrampoline", 3, "i:rubber", 6);
        add("frequencyTransmitter", 1, "i:electronicCircuit", 1, "i:insulatedCopperCableItem", 1);
        add("overclockerUpgrade", 1, "i:coolingCell", 3, "i:insulatedCopperCableItem", 2, "i:electronicCircuit", 1);
        add("transformerUpgrade", 1, "v:20", 5, "i:doubleInsulatedGoldCableItem", 2, "i:mvTransformer", 1, "i:electronicCircuit", 1);
        add("energyStorageUpgrade", 1, "v:5", 4, "i:insulatedCopperCableItem", 2, "i:reBattery", 1, "i:electronicCircuit", 1);
        add("carbonFiber", 1, "i:coalDust", 4);
        add("carbonMesh", 1, "i:carbonFiber", 2);
        add("carbonPlate", 1, "i:carbonMesh", 1);
        add("coalBall", 1, "i:coalDust", 8, "v:318", 1);
        add("hydratedCoalDust", 8, "i:coalDust", 8);
        add("compressedCoalBall", 1, "i:coalBall", 1);
        add("coalChunk", 1, "i:compressedCoalBall", 8, "v:49", 1);
        add("industrialDiamond", 1, "i:coalChunk", 1);
        add("advancedAlloy", 1, "i:mixedMetalIngot", 1);
        add("iridiumPlate", 1, "i:iridiumOre", 4, "i:advancedAlloy", 4, "v:264", 1);
        add("plantBall", 1, "v:338", 8);
        add("compressedPlantBall", 1, "i:plantBall", 1);
        add("bronzeBlock", 1, "i:bronzeIngot", 9);
        add("copperBlock", 1, "i:copperIngot", 9);
        add("tinBlock", 1, "i:tinIngot", 9);
        add("uraniumBlock", 1, "i:uraniumIngot", 9);
        add("insulatedCopperCableBlock", 1, "i:insulatedCopperCableItem", 1);
        add("coalDust", 1, "v:263", 1);
        add("treetap", 1, "v:5", 5);
        add("rubberBoots", 1, "i:rubber", 6, "v:35", 1);
/* END_GENERATED_RULES */
    }
}
