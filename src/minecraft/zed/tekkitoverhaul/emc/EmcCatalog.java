package zed.tekkitoverhaul.emc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.IInventory;
import net.minecraft.src.TileEntityFurnace;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/** Exports live EE2 values alongside the direct backing entries. */
final class EmcCatalog {
    private EmcCatalog() {}

    static void writeEnergyEvidence(File file) throws IOException {
        ArrayList<String> evidence = new ArrayList<String>();
        try {
            Class<?> generator = Class.forName("ic2.common.TileEntityGenerator");
            Class<?> eeItem = Class.forName("ee.EEItem");
            Method gainFuel = generator.getMethod("gainFuel");
            Field ticks = generator.getField("itemFuelTime");
            for (String name : new String[] {"alchemicalCoal", "mobiusFuel", "aeternalisFuel"}) {
                ItemStack fuel = new ItemStack((Item) eeItem.getField(name).get(null));
                IInventory inventory = (IInventory) generator.getConstructor().newInstance();
                inventory.setInventorySlotContents(1, fuel.copy());
                boolean accepted = ((Boolean) gainFuel.invoke(inventory)).booleanValue();
                evidence.add("IC2 fuel " + name + " furnaceTicks="
                        + TileEntityFurnace.getItemBurnTime(fuel) + " generatorAccepted=" + accepted
                        + " generatorTicks=" + ticks.getInt(inventory));
            }
        } catch (Exception failure) {
            evidence.add("IC2 probe unavailable: " + failure);
        }
        try {
            Class<?> api = Class.forName("buildcraft.api.API");
            Map<?, ?> fuels = (Map<?, ?>) api.getField("ironEngineFuel").get(null);
            for (Object key : new TreeSet<Object>(fuels.keySet())) {
                Object fuel = fuels.get(key);
                Class<?> type = fuel.getClass();
                evidence.add("BuildCraft fuel ID=" + key + " powerPerCycle="
                        + type.getField("powerPerCycle").getInt(fuel) + " totalBurningTime="
                        + type.getField("totalBurningTime").getInt(fuel));
            }
            for (DistillateType type : DistillateType.values()) {
                int id = PetroleumFluids.stillId(type);
                evidence.add("BuildCraft " + type.displayName + " ID=" + id
                        + " combustionRegistered=" + fuels.containsKey(id));
            }
        } catch (Exception failure) {
            evidence.add("BuildCraft probe unavailable: " + failure);
        }
        Files.write(file.toPath(), evidence, StandardCharsets.UTF_8);
    }

    static void write(File file, Ee2EmcAdapter ee2, Map<Integer, Map<Integer, Integer>> direct)
            throws IOException, ReflectiveOperationException {
        File staging = File.createTempFile("zto-emc-", ".tsv", file.getParentFile());
        try {
            BufferedWriter out = Files.newBufferedWriter(staging.toPath(), StandardCharsets.UTF_8);
            try {
                out.write("owner_hint\tclass\tinternal_name\tdisplay_name\tid\tmetadata\temc\tdirect_emc"
                        + "\tvalue_source\tvariant_source\tfurnace_ticks\n");
                Set<Integer> ids = new TreeSet<Integer>(direct.keySet());
                for (int id = 0; id < Item.itemsList.length; id++) {
                    if (Item.itemsList[id] != null) ids.add(id);
                }
                for (int id : ids) writeItem(out, id, direct.get(id), ee2);
            } finally {
                out.close();
            }
            Files.move(staging.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staging.toPath());
        }
    }

    private static void writeItem(BufferedWriter out, int id, Map<Integer, Integer> direct, Ee2EmcAdapter ee2)
            throws IOException, ReflectiveOperationException {
        Item item = id >= 0 && id < Item.itemsList.length ? Item.itemsList[id] : null;
        Set<Integer> creative = new TreeSet<Integer>();
        String enumeration = "";
        if (item != null) {
            ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
            try {
                item.addCreativeItems(stacks);
                for (ItemStack stack : stacks) {
                    if (stack != null && stack.itemID == id) creative.add(stack.getItemDamage());
                }
            } catch (RuntimeException failure) {
                enumeration = "+creative-error:" + failure.getClass().getSimpleName();
            }
        }
        Set<Integer> variants = new TreeSet<Integer>(creative);
        variants.add(0);
        if (direct != null) variants.addAll(direct.keySet());
        if (item != null) {
            for (int damage = 1; damage <= Math.min(item.getMaxDamage(), 32767); damage++) {
                variants.add(damage);
            }
        }
        for (int meta : variants) {
            Integer registered = direct == null ? null : direct.get(meta);
            String variant = (meta == 0 ? "base" : creative.contains(meta) ? "creative"
                    : registered != null ? "direct-key" : "durability") + enumeration;
            if (item == null) {
                row(out, "", "", "", "", id, meta, registered, registered, "unregistered-id", variant, "");
                continue;
            }
            ItemStack stack = new ItemStack(id, 1, meta);
            String internal;
            String display;
            try {
                internal = item.getItemNameIS(stack);
                display = item.getItemDisplayName(stack);
            } catch (RuntimeException failure) {
                internal = item.getItemName();
                display = "";
                variant += "+name-error:" + failure.getClass().getSimpleName();
            }
            int effective = ee2.stackValue(stack);
            Class<?> owner = item.getClass();
            if (id < Block.blocksList.length && Block.blocksList[id] != null) {
                owner = Block.blocksList[id].getClass();
            }
            String className = owner.getName();
            String ownerHint = owner.getPackage() == null ? "" : owner.getPackage().getName();
            if (owner.getProtectionDomain().getCodeSource() != null) {
                String archive = owner.getProtectionDomain().getCodeSource().getLocation().getPath();
                ownerHint = archive.substring(archive.lastIndexOf('/') + 1);
            }
            String ticks = "";
            if (meta == 0 || creative.contains(meta) || registered != null) {
                try {
                    ticks = Integer.toString(TileEntityFurnace.getItemBurnTime(stack));
                } catch (RuntimeException failure) {
                    ticks = "error:" + failure.getClass().getSimpleName();
                }
            }
            String source = registered != null && registered != 0 ? "direct"
                    : effective == 0 ? "none" : stack.isItemStackDamageable() ? "durability" : "metadata-0";
            row(out, ownerHint, className, internal, display, id, meta, effective, registered, source, variant, ticks);
        }
    }

    private static void row(BufferedWriter out, Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i != 0) out.write('\t');
            if (cells[i] != null) {
                out.write(cells[i].toString().replace("\\", "\\\\").replace("\t", "\\t")
                        .replace("\r", "\\r").replace("\n", "\\n"));
            }
        }
        out.newLine();
    }
}
