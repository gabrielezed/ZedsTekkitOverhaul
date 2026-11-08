package zed.tekkitoverhaul.fluids;

import java.util.Locale;
import java.util.Random;
import java.util.WeakHashMap;
import buildcraft.api.API;
import buildcraft.api.LiquidData;
import buildcraft.core.ILiquid;
import net.minecraft.src.Block;
import net.minecraft.src.BlockFlowing;
import net.minecraft.src.BlockStationary;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MapColor;
import net.minecraft.src.Material;
import net.minecraft.src.MaterialLiquid;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Potion;
import net.minecraft.src.PotionEffect;
import net.minecraft.src.World;
import net.minecraft.src.forge.IBucketHandler;
import net.minecraft.src.forge.MinecraftForge;
import zed.tekkitoverhaul.distillation.DistillateType;

/** Registers independent petroleum block pairs and their BuildCraft liquid identities. */
public final class PetroleumFluids {
    public static final int MAX_SPREAD = 4;
    private static final int FALLING = 8;
    private static final int[][] SIDES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[] FLOWING_IDS = new int[DistillateType.values().length];
    private static final WeakHashMap<EntityPlayer, Drag> PLAYER_DRAG = new WeakHashMap<EntityPlayer, Drag>();
    private static int renderType;

    private PetroleumFluids() { }

    public static void setRenderType(int id) { renderType = id; }

    public static void registerBlocks(DistillateType type, int id) {
        if (id <= 0 || id >= Block.blocksList.length - 1
                || Block.blocksList[id] != null || Block.blocksList[id + 1] != null
                || Item.itemsList[id] != null || Item.itemsList[id + 1] != null) {
            throw new IllegalStateException("ZTO: " + type.displayName
                    + " needs two free consecutive block IDs starting at " + id);
        }
        FLOWING_IDS[type.id] = id;
        Material material = new MaterialLiquid(MapColor.sandColor);
        String name = "zto" + type.displayName.replace(" ", "");
        Block flowing = new Flowing(id, material, type).setBlockName(name + "Flowing");
        Block still = new Still(id + 1, material, type).setBlockName(name + "Still");
        ModLoader.registerBlock(flowing);
        ModLoader.registerBlock(still);
        ModLoader.addName(flowing, "Flowing " + type.displayName);
        ModLoader.addName(still, type.displayName);
    }

    public static int flowingId(DistillateType type) { return FLOWING_IDS[type.id]; }

    public static int stillId(DistillateType type) { return flowingId(type) + 1; }

    private static String texture(DistillateType type) {
        // Block's constructor asks for the texture before the subclass stores its type.
        if (type == null) type = DistillateType.GASOLINE;
        return "/zed/textures/" + type.name().toLowerCase(Locale.ROOT) + "_fluid.png";
    }

    public static void registerBucket(final DistillateType type, final Item bucket) {
        API.liquids.add(new LiquidData(stillId(type), bucket.shiftedIndex));
        MinecraftForge.registerCustomBucketHandler(new IBucketHandler() {
            public ItemStack fillCustomBucket(World world, int x, int y, int z) {
                int id = world.getBlockId(x, y, z);
                if ((id != flowingId(type) && id != stillId(type)) || world.getBlockMetadata(x, y, z) != 0) {
                    return null;
                }
                return world.setBlockWithNotify(x, y, z, 0) ? new ItemStack(bucket) : null;
            }
        });
    }

    private static final class Drag {
        int tick;
        double factor = 1D;
    }

    private static synchronized void slowPlayer(World world, int x, int y, int z,
                                                Entity entity, DistillateType type) {
        if (!(entity instanceof EntityPlayer)) return;
        int level = world.getBlockMetadata(x, y, z);
        if (entity.boundingBox.minY >= y + (8D - (level >= FALLING ? 0 : level)) / 9D
                && world.getBlockMaterial(x, y + 1, z) != world.getBlockMaterial(x, y, z)) return;
        EntityPlayer player = (EntityPlayer) entity;
        Drag drag = PLAYER_DRAG.get(player);
        if (drag == null) {
            drag = new Drag();
            PLAYER_DRAG.put(player, drag);
        }
        if (drag.tick != player.ticksExisted) {
            drag.tick = player.ticksExisted;
            drag.factor = 1D;
        }
        if (type.horizontalDrag < drag.factor) {
            double extraDrag = type.horizontalDrag / drag.factor;
            player.motionX *= extraDrag;
            player.motionZ *= extraDrag;
            drag.factor = type.horizontalDrag;
        }
        // Ground acceleration alone can exceed swimming speed even with strong drag.
        // Use the existing speed modifier briefly without replacing another effect.
        if (type == DistillateType.HEAVY_GAS_OIL && !world.isRemote
                && !player.isPotionActive(Potion.moveSlowdown)) {
            player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2, 2));
        }
    }

    private static final class Flowing extends BlockFlowing implements ILiquid {
        private final DistillateType type;

        Flowing(int id, Material material, DistillateType type) {
            super(id, material);
            this.type = type;
            blockIndexInTexture = 0;
            setHardness(100F);
            setLightOpacity(3);
            setTickRandomly(false);
        }

        public String getTextureFile() { return texture(type); }
        public int getRenderType() { return renderType; }
        public int stillLiquidId() { return stillId(type); }
        public int tickRate() { return type.flowTicks; }

        @Override
        public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
            slowPlayer(world, x, y, z, entity, type);
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (world.isRemote || world.getBlockId(x, y, z) != blockID) return;
            int current = world.getBlockMetadata(x, y, z);
            int level = current == 0 ? 0 : supportedLevel(world, x, y, z);
            if (level < 0) {
                world.setBlockWithNotify(x, y, z, 0);
                return;
            }

            // Keep the same material and metadata when changing to the resting block.
            world.setBlockAndMetadata(x, y, z, stillLiquidId(), level);
            if (current != level) world.notifyBlocksOfNeighborChange(x, y, z, stillLiquidId());

            int distance = level & 7;
            if (y > 0 && flowInto(world, x, y - 1, z, FALLING | distance)) return;
            if (distance < MAX_SPREAD) {
                for (int[] side : SIDES) {
                    flowInto(world, x + side[0], y, z + side[1], distance + 1);
                }
            }
        }

        private int supportedLevel(World world, int x, int y, int z) {
            int above = getFlowDecay(world, x, y + 1, z);
            if (above >= 0 && (above & 7) <= MAX_SPREAD) return FALLING | (above & 7);

            int distance = MAX_SPREAD;
            for (int[] side : SIDES) {
                int neighbor = getFlowDecay(world, x + side[0], y, z + side[1]);
                if (neighbor >= 0) distance = Math.min(distance, neighbor & 7);
            }
            return distance < MAX_SPREAD ? distance + 1 : -1;
        }

        private boolean flowInto(World world, int x, int y, int z, int level) {
            int id = world.getBlockId(x, y, z);
            if (id != 0) {
                if (id != blockID && id != stillLiquidId()) return false;
                int existing = world.getBlockMetadata(x, y, z);
                if (existing == 0) return false;
                if ((existing & 7) <= (level & 7)) return true;
            }
            if (!world.setBlockAndMetadataWithNotify(x, y, z, blockID, level)) return false;
            world.scheduleBlockUpdate(x, y, z, blockID, tickRate());
            return true;
        }
    }

    private static final class Still extends BlockStationary implements ILiquid {
        private final DistillateType type;

        Still(int id, Material material, DistillateType type) {
            super(id, material);
            this.type = type;
            blockIndexInTexture = 0;
            setHardness(100F);
            setLightOpacity(3);
        }

        public String getTextureFile() { return texture(type); }
        public int getRenderType() { return renderType; }
        public int stillLiquidId() { return blockID; }
        public int tickRate() { return type.flowTicks; }

        @Override
        public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
            slowPlayer(world, x, y, z, entity, type);
        }
    }
}
