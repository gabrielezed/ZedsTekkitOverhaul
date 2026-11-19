package zed.tekkitoverhaul.survival.tactical;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Optional, pinned transport adapters. A target owns one block or one native multipart index. */
final class TransportTheft {
    private static final String CABLE_BLOCK = "ic2.common.BlockCable";
    private static final String MICRO_BLOCK = "eloraam.base.BlockMicro";
    private static final int CENTER = 29;

    enum State { READY, UNLOADED, TARGET_CHANGED, UNSAFE_TRANSPORT }

    final String family;
    private final int part;
    private final TileEntity tile;
    private final World world;
    private final int x, y, z;
    private final Block block;
    private final int metadata;
    private final ItemStack stack;
    private final BuildCraftPipeTheft buildcraft;

    private TransportTheft(TileEntity tile, Block block, int metadata, int part,
            String family, ItemStack stack, BuildCraftPipeTheft buildcraft) {
        this.tile = tile;
        this.world = tile.worldObj;
        this.x = tile.xCoord;
        this.y = tile.yCoord;
        this.z = tile.zCoord;
        this.block = block;
        this.metadata = metadata;
        this.part = part;
        this.family = family;
        this.stack = stack;
        this.buildcraft = buildcraft;
    }

    private static String family(TileEntity tile) throws ReflectiveOperationException {
        String name = tile.getClass().getName();
        if ("ic2.common.TileEntityCable".equals(name)) return "IC2_CABLE";
        if ("ic2.common.TileEntityCableDetector".equals(name)) return "IC2_DETECTOR";
        if ("ic2.common.TileEntityCableSplitter".equals(name)) return "IC2_SPLITTER";
        if ("eloraam.machine.TilePipe".equals(name)) return "REDPOWER_FLUID";
        if ("eloraam.machine.TileTube".equals(name)) return "REDPOWER_PNEUMATIC";
        if ("eloraam.machine.TileRedstoneTube".equals(name)) return "REDPOWER_REDSTONE_TUBE";
        if ("eloraam.machine.TileRestrictTube".equals(name)) return "REDPOWER_RESTRICTION";
        if ("eloraam.machine.TileMagTube".equals(name)) return "REDPOWER_MAGNETIC";
        if ("eloraam.wiring.TileBluewire".equals(name)) return "REDPOWER_BLUEPOWER";
        if ("eloraam.wiring.TileRedwire".equals(name)) return "REDPOWER_RED_ALLOY";
        if ("eloraam.wiring.TileInsulatedWire".equals(name)) return "REDPOWER_INSULATED";
        if ("eloraam.wiring.TileCable".equals(name)) return "REDPOWER_BUNDLED";
        if ("eloraam.control.TileRibbon".equals(name)) return "REDPOWER_RIBBON";
        return BuildCraftPipeTheft.family(tile);
    }

    /** Spawn context checks shapes and identity without inspecting payloads, energy or inventories. */
    static boolean exposedSignal(TileEntity tile, Vec3D eye) {
        return find(tile, eye, false) != null;
    }

    static TransportTheft find(TileEntity tile, Vec3D eye) {
        return find(tile, eye, true);
    }

    private static TransportTheft find(TileEntity tile, Vec3D eye, boolean detailed) {
        if (tile == null || !ThiefTrail.local(tile.worldObj)) return null;
        try {
            String family = family(tile);
            if (family == null) return null;
            World world = tile.worldObj;
            int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
            boolean multipart = family.startsWith("REDPOWER_");
            if (!loaded(tile, multipart ? 3 : 1)) return null;
            Block block = Block.blocksList[world.getBlockId(x, y, z)];
            int metadata = world.getBlockMetadata(x, y, z);
            if (block == null) return null;
            String blockType = block.getClass().getName();
            if (!multipart) {
                TransportTheft target;
                if (family.startsWith("IC2_")) {
                    if (!CABLE_BLOCK.equals(blockType) || !bareCable(tile, family, metadata)) return null;
                    ItemStack stack = detailed ? cableDrop(block, tile, metadata) : null;
                    if (detailed && stack == null) return null;
                    target = new TransportTheft(tile, block, metadata, -1, family, stack, null);
                } else {
                    if (!"buildcraft.transport.BlockGenericPipe".equals(blockType)) return null;
                    BuildCraftPipeTheft pipe = detailed ? BuildCraftPipeTheft.find(tile) : null;
                    if (detailed && pipe == null) return null;
                    target = new TransportTheft(tile, block, metadata, -1, family, null, pipe);
                }
                return target.visiblePoint(eye) == null ? null : target;
            }
            if (!MICRO_BLOCK.equals(blockType)) return null;
            int extendedId = number(call(tile, "getExtendedID"));
            if (metadata != extendedId || !preservesRoutingColor(tile, extendedId)
                    || detailed && !redPowerQuiescent(tile, extendedId)) return null;
            int parts = transportParts(tile, extendedId);
            while (parts != 0) {
                int part = Integer.numberOfTrailingZeros(parts);
                parts &= ~(1 << part);
                ItemStack stack = detailed ? microDrop(tile, block.blockID, extendedId, part) : null;
                if (detailed && stack == null) continue;
                TransportTheft target = new TransportTheft(tile, block, metadata, part, family, stack, null);
                if (target.visiblePoint(eye) != null) return target;
            }
            return null;
        } catch (Exception incompatible) {
            return null;
        } catch (LinkageError incompatible) {
            return null;
        }
    }

    private static boolean loaded(TileEntity tile, int halo) {
        return tile.worldObj.checkChunksExist(tile.xCoord - halo, tile.yCoord, tile.zCoord - halo,
                tile.xCoord + halo, tile.yCoord, tile.zCoord + halo);
    }

    private static boolean bareCable(TileEntity tile, String family, int metadata) throws ReflectiveOperationException {
        int variant = number(field(tile, "cableType"));
        return number(field(tile, "foamed")) == 0 && number(field(tile, "color")) == 0
                && metadata == variant && (family.equals("IC2_CABLE") ? variant >= 0 && variant <= 10
                : variant == (family.equals("IC2_DETECTOR") ? 11 : 12));
    }

    private static ItemStack cableDrop(Block block, TileEntity tile, int metadata) {
        List<ItemStack> drops = block.getBlockDropped(tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord, metadata, 0);
        if (drops.size() != 1) return null;
        ItemStack item = drops.get(0);
        return item != null && item.stackSize == 1 && item.getItem() != null && item.getItemDamage() == metadata
                && "ic2.common.ItemCable".equals(item.getItem().getClass().getName()) ? item : null;
    }

    private static int transportParts(TileEntity tile, int extendedId) throws ReflectiveOperationException {
        if (extendedId >= 7 && extendedId <= 11) return 1 << CENTER;
        int sides = number(field(tile, "ConSides"));
        return sides & 63 | (sides & 64) << 23;
    }

    private static boolean redPowerQuiescent(TileEntity tile, int extendedId) throws ReflectiveOperationException {
        if (extendedId == 7) {
            Object buffer = field(tile, "pipebuf");
            return number(field(buffer, "Level")) == 0 && number(field(buffer, "Delta")) == 0;
        }
        if (extendedId >= 8 && extendedId <= 11)
            return ((Collection<?>) field(call(tile, "getTubeFlow"), "contents")).isEmpty();
        return true;
    }

    private static boolean preservesRoutingColor(TileEntity tile, int extendedId) throws ReflectiveOperationException {
        // Native tube drops carry the subtype. A painted tube's channel would be lost on placement.
        return extendedId < 8 || extendedId > 11 || number(field(tile, "paintColor")) == 0;
    }

    private static ItemStack microDrop(TileEntity tile, int blockId, int extendedId, int part)
            throws ReflectiveOperationException {
        Item item = Item.itemsList[blockId];
        if (item == null || !"eloraam.base.ItemMicro".equals(item.getClass().getName())) return null;
        int damage = extendedId << 8;
        if (extendedId < 7 || extendedId == 12) {
            if (part == CENTER) {
                int offset = extendedId == 1 ? 0 : extendedId == 3 ? 256 : extendedId == 5 ? 512 : -1;
                if (offset < 0) return null;
                damage = 16384 + offset + number(field(tile, "CenterPost"));
            } else damage += number(field(tile, "Metadata"));
        }
        return new ItemStack(blockId, 1, damage);
    }

    Vec3D visiblePoint(Vec3D eye) {
        int eyeX = MathHelper.floor_double(eye.xCoord), eyeZ = MathHelper.floor_double(eye.zCoord);
        if (!world.checkChunksExist(Math.min(x, eyeX) - 1, 0, Math.min(z, eyeZ) - 1,
                Math.max(x, eyeX) + 1, 0, Math.max(z, eyeZ) + 1)) return null;
        double px = x + 0.5D, py = y + 0.5D, pz = z + 0.5D;
        try {
            if (part >= 0 && part < 6) {
                double inset = ((Number) call(tile, "getWireHeight")).doubleValue() / 2.0D;
                double offset = (part & 1) == 0 ? inset - 0.5D : 0.5D - inset;
                if (part < 2) py += offset;
                else if (part < 4) pz += offset;
                else px += offset;
            }
            Vec3D point = Vec3D.createVector(px, py, pz);
            MovingObjectPosition hit = world.rayTraceBlocks(Vec3D.createVector(eye.xCoord, eye.yCoord, eye.zCoord), point);
            if (hit == null) hit = block.collisionRayTrace(world, x, y, z, eye, point);
            return hit != null && hit.blockX == x && hit.blockY == y && hit.blockZ == z
                    && (part < 0 || hit.subHit == part) ? point : null;
        } catch (Exception incompatible) {
            return null;
        } catch (LinkageError incompatible) {
            return null;
        }
    }

    State state() {
        if (tile.worldObj != world || tile.xCoord != x || tile.yCoord != y || tile.zCoord != z)
            return State.TARGET_CHANGED;
        if (!loaded(tile, part >= 0 ? 3 : 1)) return State.UNLOADED;
        if (LoadedTile.at(world, x, y, z) != tile || world.getBlockId(x, y, z) != block.blockID
                || world.getBlockMetadata(x, y, z) != metadata) return State.TARGET_CHANGED;
        try {
            if (buildcraft != null) return buildcraft.stillEligible() ? State.READY : State.UNSAFE_TRANSPORT;
            if (part < 0) return bareCable(tile, family, metadata) && sameItem(stack, cableDrop(block, tile, metadata))
                    ? State.READY : State.UNSAFE_TRANSPORT;
            if ((transportParts(tile, metadata) & 1 << part) == 0
                    || !sameItem(stack, microDrop(tile, block.blockID, metadata, part))) return State.TARGET_CHANGED;
            return preservesRoutingColor(tile, metadata) && redPowerQuiescent(tile, metadata)
                    ? State.READY : State.UNSAFE_TRANSPORT;
        } catch (Exception incompatible) {
            return State.UNSAFE_TRANSPORT;
        } catch (LinkageError incompatible) {
            return State.UNSAFE_TRANSPORT;
        }
    }

    ItemStack remove(Vec3D eye) {
        if (state() != State.READY || visiblePoint(eye) == null) return null;
        if (buildcraft != null) return buildcraft.remove();
        if (part < 0) {
            if (!world.setBlockWithNotify(x, y, z, 0) || world.getBlockId(x, y, z) != 0
                    || LoadedTile.at(world, x, y, z) != null || !tile.isInvalid()) return null;
            try {
                return Boolean.FALSE.equals(field(tile, "addedToEnergyNet")) ? stack : null;
            } catch (Exception incompatible) {
                return null;
            } catch (LinkageError incompatible) {
                return null;
            }
        }
        return harvestPart();
    }

    private ItemStack harvestPart() {
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        List<?> previous = world.getEntitiesWithinAABB(EntityItem.class, area);
        try {
            Method harvest = tile.getClass().getMethod("onHarvestPart", EntityPlayer.class, Integer.TYPE);
            // Pinned overloads use the player only for cover parts, which are never selected.
            harvest.invoke(tile, null, part);
            if (LoadedTile.at(world, x, y, z) == tile && (transportParts(tile, metadata) & 1 << part) != 0)
                return null;
        } catch (Exception incompatible) {
            return null;
        } catch (LinkageError incompatible) {
            return null;
        }
        EntityItem component = null;
        for (Object value : world.getEntitiesWithinAABB(EntityItem.class, area)) {
            EntityItem drop = (EntityItem) value;
            if (previous.contains(drop) || drop.isDead || !sameItem(stack, drop.item)) continue;
            if (drop.posX < x || drop.posX >= x + 1 || drop.posY < y || drop.posY >= y + 1
                    || drop.posZ < z || drop.posZ >= z + 1) continue;
            if (component != null) return null;
            component = drop;
        }
        if (component == null) return null;
        component.setDead();
        return component.item;
    }

    private static boolean sameItem(ItemStack expected, ItemStack actual) {
        return expected != null && actual != null && expected.itemID == actual.itemID && actual.stackSize == 1
                && expected.getItemDamage() == actual.getItemDamage();
    }

    private static Object call(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getMethod(name).invoke(object);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getField(name).get(object);
    }

    private static int number(Object value) {
        return ((Number) value).intValue();
    }
}
