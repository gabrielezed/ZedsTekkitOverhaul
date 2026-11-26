package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

public final class ItemMarineOrganism extends ItemBlock {
    public ItemMarineOrganism(int id) {
        super(id);
        setTextureFile(BlockAbyssalMaterial.TEXTURE);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
        return plant((BlockMarineOrganism) Block.blocksList[getBlockID()], stack, player, world, x, y, z, side);
    }

    private static boolean plant(BlockMarineOrganism organism, ItemStack stack, EntityPlayer player,
                                 World world, int x, int y, int z, int side) {
        switch (side) {
            case 0: y--; break;
            case 1: y++; break;
            case 2: z--; break;
            case 3: z++; break;
            case 4: x--; break;
            case 5: x++; break;
            default: return false;
        }
        int orientation = MathHelper.floor_double(player.rotationYaw / 90.0F + 0.5F);
        if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z)
                || !organism.place(world, x, y, z, orientation)) return false;
        if (!world.isRemote && !player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }

    static final class Propagule extends Item {
        private final BlockMarineOrganism organism;

        Propagule(int id, BlockMarineOrganism organism) {
            super(id);
            this.organism = organism;
            setItemName("zto." + organism.type.itemKey);
            setTextureFile(AbyssalRegistry.ITEMS_TEXTURE);
            setIconIndex(organism.type.itemTile);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
            return plant(organism, stack, player, world, x, y, z, side);
        }
    }
}
