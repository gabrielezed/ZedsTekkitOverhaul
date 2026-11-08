package zed.tekkitoverhaul.fluids;

import buildcraft.api.API;
import buildcraft.api.LiquidData;
import buildcraft.core.ILiquid;
import net.minecraft.src.Block;
import net.minecraft.src.BlockFlowing;
import net.minecraft.src.BlockStationary;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MapColor;
import net.minecraft.src.Material;
import net.minecraft.src.MaterialLiquid;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import net.minecraft.src.forge.IBucketHandler;
import net.minecraft.src.forge.MinecraftForge;

/** The first petroleum fluid pair and its BuildCraft bucket registration. */
public final class GasolineFluid {
    private static final String TEXTURE = "/zed/textures/gasoline_fluid.png";
    private static int flowingId;
    private static int stillId;
    private static int renderType;

    private GasolineFluid() { }

    public static void setRenderType(int id) { renderType = id; }

    public static void registerBlocks(int id) {
        // Vanilla flow transitions assume moving and still IDs are consecutive.
        if (id <= 0 || id >= Block.blocksList.length - 1
                || Block.blocksList[id] != null || Block.blocksList[id + 1] != null
                || Item.itemsList[id] != null || Item.itemsList[id + 1] != null) {
            throw new IllegalStateException("ZTO: Gasoline needs two free consecutive block IDs starting at " + id);
        }

        flowingId = id;
        stillId = id + 1;
        Material material = new MaterialLiquid(MapColor.sandColor);
        Block flowing = new FlowingGasoline(flowingId, material).setBlockName("ztoGasolineFlowing");
        Block still = new StillGasoline(stillId, material).setBlockName("ztoGasolineStill");
        ModLoader.registerBlock(flowing);
        ModLoader.registerBlock(still);
        ModLoader.addName(flowing, "Flowing Gasoline");
        ModLoader.addName(still, "Gasoline");
    }

    public static int flowingId() { return flowingId; }
    public static int stillId() { return stillId; }

    public static void registerBucket(final Item gasolineBucket) {
        API.liquids.add(new LiquidData(stillId, gasolineBucket.shiftedIndex));
        MinecraftForge.registerCustomBucketHandler(new IBucketHandler() {
            public ItemStack fillCustomBucket(World world, int x, int y, int z) {
                int id = world.getBlockId(x, y, z);
                if ((id != flowingId && id != stillId) || world.getBlockMetadata(x, y, z) != 0) {
                    return null;
                }
                return world.setBlockWithNotify(x, y, z, 0) ? new ItemStack(gasolineBucket) : null;
            }
        });
    }

    private static final class FlowingGasoline extends BlockFlowing implements ILiquid {
        FlowingGasoline(int id, Material material) {
            super(id, material);
            blockIndexInTexture = 0;
            setHardness(100F);
            setLightOpacity(3);
        }

        public String getTextureFile() { return TEXTURE; }
        public int getRenderType() { return renderType; }
        public int stillLiquidId() { return GasolineFluid.stillId(); }
        public int tickRate() { return 5; }
    }

    private static final class StillGasoline extends BlockStationary implements ILiquid {
        StillGasoline(int id, Material material) {
            super(id, material);
            blockIndexInTexture = 0;
            setHardness(100F);
            setLightOpacity(3);
        }

        public String getTextureFile() { return TEXTURE; }
        public int getRenderType() { return renderType; }
        public int stillLiquidId() { return GasolineFluid.stillId(); }
    }
}
