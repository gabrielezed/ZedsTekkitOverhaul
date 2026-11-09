package zed.tekkitoverhaul.hopper;

import java.lang.reflect.Method;
import net.minecraft.src.*;
import net.minecraft.src.forge.ISidedInventory;

/** Legacy inventory policy. Optional machine classes are never linked directly. */
public final class InventoryAccess {
    private InventoryAccess() { }

    public static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.itemID == b.itemID
            && a.getItemDamage() == b.getItemDamage()
            && (a.stackTagCompound == null ? b.stackTagCompound == null
                : a.stackTagCompound.equals(b.stackTagCompound));
    }

    static boolean isIronChest(Object inventory) {
        if (inventory == null) return false;
        for (Class<?> type = inventory.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("cpw.mods.ironchest.TileEntityIronChest")) return true;
        }
        return false;
    }

    private static boolean electricMachine(IInventory inventory) {
        String name = inventory.getClass().getName();
        return name.equals("ic2.common.TileEntityMacerator")
            || name.equals("ic2.common.TileEntityExtractor")
            || name.equals("ic2.common.TileEntityCompressor")
            || name.equals("ic2.common.TileEntityElecFurnace");
    }

    public static boolean accessible(IInventory inventory, int slot, int side, boolean insert, ItemStack stack) {
        if (slot < 0 || slot >= inventory.getSizeInventory()) return false;
        if (inventory.getClass().getName().equals("ic2.common.TileEntityGenerator")) {
            return insert && slot == 1 && stack.itemID != Item.bucketLava.shiftedIndex
                && TileEntityFurnace.getItemBurnTime(stack) >= 4;
        }
        if (electricMachine(inventory)) {
            // Explicit Hopper-only adaptation: bottom extracts product, never battery/upgrades.
            if (!insert) return slot == 2 && (side == 0 || side >= 2);
            if (slot != 0 || side != 1) return false;
            try {
                Method recipe = inventory.getClass().getMethod("getResultFor", ItemStack.class, Boolean.TYPE);
                return recipe.invoke(inventory, stack.copy(), Boolean.FALSE) != null;
            } catch (Exception exception) {
                return false; // Unknown ABI must not insert into a restricted machine slot.
            }
        }
        if (inventory.getClass().getName().startsWith("ic2.")) return false;
        if (inventory instanceof ISidedInventory && side >= 0) {
            ISidedInventory sided = (ISidedInventory) inventory;
            int start = sided.getStartInventorySide(side);
            int count = sided.getSizeInventorySide(side);
            if (start < 0 || count < 0 || slot < start || slot - start >= count) return false;
        }
        if (inventory instanceof TileEntityFurnace) {
            if (!insert) return slot == 2;
            return slot == 0 ? FurnaceRecipes.smelting().getSmeltingResult(stack) != null
                : slot == 1 && TileEntityFurnace.isItemFuel(stack);
        }
        // IInventory 1.2.5 has no slot acceptance method. Unknown unsided machines fail closed.
        return inventory instanceof TileHopper || inventory instanceof TileEntityChest
            || inventory instanceof InventoryLargeChest || inventory instanceof TileEntityDispenser
            || isIronChest(inventory) || inventory instanceof InventoryPlayer || inventory instanceof ISidedInventory;
    }

    /** Returns the accepted count; never changes the caller's stack. */
    public static int insert(IInventory target, ItemStack stack, int side, boolean commit) {
        if (target == null || stack == null || stack.stackSize <= 0) return 0;
        int remaining = stack.stackSize;
        for (int slot = 0; slot < target.getSizeInventory() && remaining > 0; slot++) {
            if (!accessible(target, slot, side, true, stack)) continue;
            ItemStack current = target.getStackInSlot(slot);
            if (current != null && !same(current, stack)) continue;
            int limit = Math.min(target.getInventoryStackLimit(), stack.getMaxStackSize());
            int oldCount = current == null ? 0 : current.stackSize;
            int amount = Math.min(remaining, Math.max(0, limit - oldCount));
            if (amount == 0) continue;
            if (commit) {
                ItemStack updated = current == null ? stack.copy() : current.copy();
                updated.stackSize = oldCount + amount;
                target.setInventorySlotContents(slot, updated);
                ItemStack stored = target.getStackInSlot(slot);
                amount = same(stored, stack) ? Math.min(amount, Math.max(0, stored.stackSize - oldCount)) : 0;
            }
            remaining -= amount;
        }
        int accepted = stack.stackSize - remaining;
        if (commit && accepted > 0) {
            if (target instanceof TileHopper) ((TileHopper) target).setTransferCooldown(8);
            target.onInventoryChanged();
        }
        return accepted;
    }

    public static boolean moveOne(IInventory source, int sourceSide, IInventory target, int targetSide) {
        return moveOne(source, sourceSide, target, targetSide, 0);
    }

    static boolean moveOne(IInventory source, int sourceSide, IInventory target, int targetSide, int mode) {
        if (source == null || target == null || source == target) return false;
        if (target instanceof TileEntityFurnace && mode != 0) targetSide = mode == 1 ? 1 : 0;
        if (target.getClass().getName().equals("ic2.common.TileEntityGenerator") && mode == 1) return false;
        for (int slot = 0; slot < source.getSizeInventory(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack == null || stack.stackSize <= 0 || !accessible(source, slot, sourceSide, false, stack)) continue;
            ItemStack one = stack.copy();
            one.stackSize = 1;
            if (insert(target, one, targetSide, false) != 1) continue;
            ItemStack before = stack.copy();
            ItemStack expected = before.stackSize == 1 ? null : before.copy();
            if (expected != null) expected.stackSize--;
            ItemStack extracted = source.decrStackSize(slot, 1);
            if (!ItemStack.areItemStacksEqual(one, extracted)
                    || !ItemStack.areItemStacksEqual(expected, source.getStackInSlot(slot))
                    || insert(target, extracted, targetSide, true) != 1) {
                if (!ItemStack.areItemStacksEqual(before, source.getStackInSlot(slot))) {
                    source.setInventorySlotContents(slot, before);
                    source.onInventoryChanged();
                }
                return false;
            }
            source.onInventoryChanged();
            return true;
        }
        return false;
    }

    public static IInventory at(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return null;
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (!(tile instanceof IInventory)) return null;
        IInventory inventory = (IInventory) tile;
        if (tile instanceof TileEntityChest) {
            // Match vanilla chest ordering, without loading an adjacent chunk.
            int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int i = 0; i < offsets.length; i++) {
                int nx = x + offsets[i][0], nz = z + offsets[i][1];
                if (!world.blockExists(nx, y, nz)) continue;
                if (world.getBlockId(nx, y, nz) != Block.chest.blockID) continue;
                TileEntity other = world.getBlockTileEntity(nx, y, nz);
                if (other instanceof TileEntityChest) {
                    return i == 0 || i == 2
                        ? new InventoryLargeChest("Large chest", (IInventory) other, inventory)
                        : new InventoryLargeChest("Large chest", inventory, (IInventory) other);
                }
            }
        }
        return inventory;
    }
}
