package zed.tekkitoverhaul.hopper;

import java.io.File;
import net.minecraft.src.*;
import net.minecraft.src.forge.Configuration;

public final class HopperRegistry {
    public static Block block;
    public static int model, outer, top, inside, itemIcon;
    private HopperRegistry() { }
    public static void register(BaseMod mod) {
        Configuration config=new Configuration(new File("config/ZedsTekkitOverhaul.cfg"));
        config.load();
        String[] assets={"gui","outer","top","inside","item"};
        for(int i=0;i<assets.length;i++) {
            if (HopperRegistry.class.getResource("/zed/hopper-1.5.2/"+assets[i]+".png") == null) {
                if(config.categories.get(Configuration.CATEGORY_BLOCK).containsKey("hopper"))
                    throw new IllegalStateException("ZTO Hopper assets missing from a configured installation. "
                        +"Restore the local assets before loading Hopper worlds.");
                System.out.println("[ZTO] Hopper disabled: prepare local Hopper assets before building.");
                return;
            }
        }
        int id=config.getOrCreateIntProperty("hopper",Configuration.CATEGORY_BLOCK,3030).getInt();
        if(id<1 || id>=Block.blocksList.length || Block.blocksList[id]!=null || Item.itemsList[id]!=null)
            throw new IllegalStateException("ZTO Hopper block ID collision: "+id);
        config.save();
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
