package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Shared placement behavior for the boiler and its auxiliaries. */
abstract class BlockDistillationComponent extends BlockContainer {
    protected BlockDistillationComponent(int id) {
        super(id, Block.blockSteel.blockIndexInTexture, Material.iron);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer) {
        // Same horizontal front sides as the vanilla furnace.
        int quadrant = MathHelper.floor_double(placer.rotationYaw * 4F / 360F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, DistillationLayout.Facing.values()[quadrant].frontSide);
    }
}
