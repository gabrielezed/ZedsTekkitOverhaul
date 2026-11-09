package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;

/** Minecraft 1.2.5 singleplayer: opens a GUI with its own Container. */
public final class BlockAdvancedCraftingTable extends Block {
    public BlockAdvancedCraftingTable(int id) {
        super(id, Block.workbench.blockIndexInTexture, Material.wood);
        setHardness(2.5F);
        setResistance(5.0F);
        setStepSound(soundWoodFootstep);
        setBlockName("ztoAdvancedCraftingTable");
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return Block.workbench.getBlockTextureFromSide(side);
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (!world.isRemote) {
            ModLoader.openGUI(player, new GuiAdvancedCraftingTable(player.inventory, world, x, y, z));
        }
        return true;
    }
}
