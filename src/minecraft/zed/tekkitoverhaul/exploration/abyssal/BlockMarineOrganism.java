package zed.tekkitoverhaul.exploration.abyssal;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.World;

final class BlockMarineOrganism extends Block {
    // Native fluid height treats metadata 8 as full-depth. Buckets require metadata 0.
    private static final int OCCUPIED_WATER = 8;
    private static final int MAX_HEIGHT = 4;
    final MarineType type;
    private final int dropId;
    private final int model;

    BlockMarineOrganism(int id, int dropId, int model, MarineType type) {
        super(id, type.tile, Material.water);
        this.type = type;
        this.dropId = dropId;
        this.model = model;
        setBlockName("zto." + type.key);
        setHardness(0.0F);
        setLightOpacity(3);
        setRequiresSelfNotify();
        setLightValue(type == MarineType.ANEMONE ? 3 / 15.0F : 0);
        setBlockBounds(0.25F, 0, 0.25F, 0.75F, type.height / 16.0F, 0.75F);
    }

    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public int getRenderType() { return model; }
    @Override public String getTextureFile() { return BlockAbyssalMaterial.TEXTURE; }
    @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) { return null; }
    @Override public boolean isBlockReplaceable(World world, int x, int y, int z) { return false; }
    @Override public int idDropped(int metadata, Random random, int fortune) { return dropId; }

    @Override
    @SuppressWarnings("unchecked")
    public void addCreativeItems(ArrayList items) {
        items.add(new ItemStack(this));
    }

    boolean place(World world, int x, int y, int z, int orientation) {
        if (!world.blockExists(x, y, z) || !waterAt(world, x, y, z)
                || world.getBlockMetadata(x, y, z) != 0 || !submerged(world, x, y, z)
                || !supported(world, x, y, z)) return false;
        int metadata = OCCUPIED_WATER + (type == MarineType.FAN_CORAL ? orientation & 1 : 0);
        return world.isRemote || world.setBlockAndMetadataWithNotify(x, y, z, blockID, metadata);
    }

    private boolean submerged(World world, int x, int y, int z) {
        if (!world.blockExists(x, y + 1, z)) return false;
        Block above = Block.blocksList[world.getBlockId(x, y + 1, z)];
        return above == Block.waterStill || above == Block.waterMoving || above instanceof BlockMarineOrganism;
    }

    private boolean supported(World world, int x, int y, int z) {
        if (type != MarineType.KELP) {
            return world.blockExists(x, y - 1, z) && world.isBlockSolidOnSide(x, y - 1, z, 1);
        }
        for (int below = y - 1; below >= y - MAX_HEIGHT && world.blockExists(x, below, z); below--) {
            if (world.getBlockId(x, below, z) != blockID) {
                return world.isBlockSolidOnSide(x, below, z, 1);
            }
        }
        return false;
    }

    @Override
    public boolean removeBlockByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return world.getBlockId(x, y, z) == blockID && remove(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        if (!world.isRemote && world.getBlockId(x, y, z) == blockID
                && (!submerged(world, x, y, z) || !supported(world, x, y, z))
                && remove(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, OCCUPIED_WATER, 0);
        }
    }

    private boolean remove(World world, int x, int y, int z) {
        boolean hasSource = sourceAt(world, x, y + 1, z) || sourceAt(world, x - 1, y, z)
                || sourceAt(world, x + 1, y, z) || sourceAt(world, x, y, z - 1)
                || sourceAt(world, x, y, z + 1);
        return world.setBlockWithNotify(x, y, z, hasSource ? Block.waterStill.blockID : 0);
    }

    private static boolean sourceAt(World world, int x, int y, int z) {
        return waterAt(world, x, y, z) && world.getBlockMetadata(x, y, z) == 0;
    }

    private static boolean waterAt(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return false;
        int id = world.getBlockId(x, y, z);
        return id == Block.waterStill.blockID || id == Block.waterMoving.blockID;
    }
}
