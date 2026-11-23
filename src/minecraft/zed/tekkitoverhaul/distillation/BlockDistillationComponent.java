package zed.tekkitoverhaul.distillation;

import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Shared placement behavior for the boiler and its auxiliaries. */
abstract class BlockDistillationComponent extends BlockContainer {
    private final DistillationPartType part;

    protected BlockDistillationComponent(int id, DistillationPartType part) {
        super(id, part.index, Material.iron);
        this.part = part;
    }

    @Override
    public String getTextureFile() {
        return DistillationTextures.SHEET;
    }

    // Retain existing light and neighbor solidity behavior with native type-zero rendering.
    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getBlockTextureFromSide(int side) {
        return getBlockTextureFromSideAndMetadata(side, 0);
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
        // Item stacks use metadata zero. Show the panel on the vanilla furnace's visible face.
        return DistillationTextures.cell(part, side, metadata == 0 ? 3 : metadata, false);
    }

    @Override
    public int getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
        // The repeatable column section needs only two local block lookups.
        boolean upper = part == DistillationPartType.COLUMN && side >= 2 && y > 0
                && world.getBlockId(x, y - 1, z) == blockID
                && world.getBlockId(x, y + 1, z) != blockID;
        return DistillationTextures.cell(part, side, world.getBlockMetadata(x, y, z), upper);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer) {
        // Same horizontal front sides as the vanilla furnace.
        int quadrant = MathHelper.floor_double(placer.rotationYaw * 4F / 360F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, DistillationLayout.Facing.values()[quadrant].frontSide);
    }
}
