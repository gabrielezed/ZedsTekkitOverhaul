package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.BlockStairs;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry.MaterialType;
import static zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry.MaterialType.*;

final class AbyssalArchitecture {
    private AbyssalArchitecture() {}

    static final class Stairs extends BlockStairs {
        Stairs(int id, Block model, String key) {
            super(id, model);
            setBlockName("zto." + key);
        }

        @Override public String getTextureFile() { return BlockAbyssalMaterial.TEXTURE; }
        @Override public int idDropped(int metadata, java.util.Random random, int fortune) { return blockID; }
    }

    static final class Partial extends Block {
        Partial(int id, MaterialType type) {
            super(id, type.texture, Material.rock);
            setBlockName("zto." + type.key);
            setHardness(type.hardness);
            setResistance(8);
            setStepSound(soundStoneFootstep);
            setLightOpacity(0);
            if (type == GRATE) setBlockBounds(0, 0, 0, 1, 0.1875F, 1);
            else if (type == PEDESTAL) setBlockBounds(0.125F, 0, 0.125F, 0.875F, 0.75F, 0.875F);
            else setBlockBounds(0, 0, 0, 1, 0.5F, 1);
        }

        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean renderAsNormalBlock() { return false; }
        @Override public String getTextureFile() { return BlockAbyssalMaterial.TEXTURE; }
        @Override public int getBlockTextureFromSide(int side) {
            if (blockIndexInTexture == 21 && side < 2) return 22;
            return blockIndexInTexture;
        }
    }

    static void recipes() {
        stairs(STAIRS, BRICKS);
        stairs(ERODED_STAIRS, ERODED_BRICKS);
        ModLoader.addRecipe(new ItemStack(SLAB.block(), 6), "BBB", 'B', BRICKS.block());
        ModLoader.addRecipe(new ItemStack(PILLAR.block(), 3), "B", "B", "B", 'B', BRICKS.block());
        ModLoader.addShapelessRecipe(new ItemStack(CARVED.block()), BRICKS.block(), SHELLSTONE.block());
        ModLoader.addRecipe(new ItemStack(FRIEZE.block(), 3), "CCC", 'C', CARVED.block());
        ModLoader.addRecipe(new ItemStack(GRATE.block(), 3), " B ", "B B", " B ", 'B', ERODED_BRICKS.block());
        ModLoader.addRecipe(new ItemStack(PEDESTAL.block()), " S ", " P ", " S ", 'S', SLAB.block(), 'P', PILLAR.block());
    }

    private static void stairs(MaterialType type, MaterialType source) {
        ModLoader.addRecipe(new ItemStack(type.block(), 4), "B  ", "BB ", "BBB", 'B', source.block());
    }
}
