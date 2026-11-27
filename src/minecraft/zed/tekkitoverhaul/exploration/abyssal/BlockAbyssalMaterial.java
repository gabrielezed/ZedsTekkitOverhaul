package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.Material;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

final class BlockAbyssalMaterial extends Block {
    static final String TEXTURE = "/zed/textures/abyssal/materials.png";

    BlockAbyssalMaterial(int id, int texture, String key, float hardness, boolean sediment, int light) {
        super(id, texture, sediment ? Material.ground : Material.rock);
        setBlockName("zto." + key);
        setHardness(hardness);
        setResistance(sediment ? 0.5F : 10.0F);
        setStepSound(sediment ? soundGravelFootstep : soundStoneFootstep);
        setLightValue(light / 15.0F);
    }

    @Override public int getBlockTextureFromSideAndMetadata(int side, int meta) {
        if (blockIndexInTexture == 16 && side < 2) return 17;
        if (blockIndexInTexture != 6) return blockIndexInTexture;
        if (side < 2) return 24;
        return meta == 0 || side == meta ? 6 : 23;
    }

    @Override public int getBlockTextureFromSide(int side) {
        return getBlockTextureFromSideAndMetadata(side, 3);
    }

    @Override public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer) {
        if (blockIndexInTexture == 6) {
            int direction = MathHelper.floor_double(placer.rotationYaw * 4 / 360 + 0.5) & 3;
            world.setBlockMetadataWithNotify(x, y, z, new int[] {2, 5, 3, 4}[direction]);
        }
    }

    @Override
    public String getTextureFile() {
        return TEXTURE;
    }
}
