package zed.tekkitoverhaul.hopper;

import net.minecraft.src.*;
import zed.tekkitoverhaul.config.ZtoConfig;

public final class HopperRegistry {
    public static Block block;
    public static int model, outer, top, inside, itemIcon;
    private HopperRegistry() { }
    public static boolean hasAssets() {
        for (String asset : new String[] {"gui", "outer", "top", "inside", "item"}) {
            if (HopperRegistry.class.getResource("/zed/hopper-1.5.2/" + asset + ".png") == null)
                return false;
        }
        return true;
    }

    public static void register(BaseMod mod, ZtoConfig config) {
        if (!hasAssets()) {
            if (config.hopper != null)
                throw new IllegalStateException("ZTO Hopper assets missing from a configured installation. "
                        + "Restore the local assets before loading Hopper worlds.");
            System.out.println("[ZTO] Hopper disabled: prepare local Hopper assets before building.");
            return;
        }
        int id = config.hopper;
        if (id < 1 || id >= Block.blocksList.length || Block.blocksList[id] != null || Item.itemsList[id] != null)
            throw new IllegalStateException("ZTO Hopper block ID collision: " + id);
        model=ModLoader.getUniqueBlockModelID(mod,false);
        outer=ModLoader.addOverride("/terrain.png","/zed/hopper-1.5.2/outer.png");
        top=ModLoader.addOverride("/terrain.png","/zed/hopper-1.5.2/top.png");
        inside=ModLoader.addOverride("/terrain.png","/zed/hopper-1.5.2/inside.png");
        itemIcon=ModLoader.addOverride("/gui/items.png","/zed/hopper-1.5.2/item.png");
        block=new BlockHopper(id);
        ModLoader.registerBlock(block,ItemHopper.class);
        ModLoader.addName(block,"Hopper");
        ModLoader.registerTileEntity(TileHopper.class,"ZTOHopper");
        ModLoader.addRecipe(new ItemStack(block),new Object[]{"I I","ICI"," I ",
            Character.valueOf('I'),Item.ingotIron,Character.valueOf('C'),Block.chest});
        System.out.println("[ZTO] Hopper ID="+id+", single-player prototype, cooldown=8 ticks");
    }
}
