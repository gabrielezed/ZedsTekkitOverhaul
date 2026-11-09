package zed.tekkitoverhaul.hopper.dev;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.*;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import zed.tekkitoverhaul.hopper.*;

/** Opt-in live-world fixture. Requires a marker file AND the exact disposable world name. */
public final class HopperAcceptance {
    private static boolean started, finished;
    private static int ticks, x, z;
    private static World world;
    private static PrintWriter log;
    private static IInventory machine;
    private static TileHopper machineOutput, pipeSource, pipeTarget;
    private static Object woodenPipe;
    private static final int Y=100;
    private HopperAcceptance() { }
    public static void tick(Minecraft game) {
        if (finished || game.theWorld==null || game.theWorld.isRemote || HopperRegistry.block==null
            || !"ZTO Hopper Acceptance".equals(game.theWorld.getWorldInfo().getWorldName())
            || !new File(game.mcDataDir,"zto-hopper-acceptance.enable").isFile()) return;
        try {
            if (!started) {
                started=true; world=game.theWorld;
                log=new PrintWriter(new FileWriter(new File(game.mcDataDir,"zto-hopper-acceptance.log"),true),true);
                x=((int)game.thePlayer.posX/16)*16; z=((int)game.thePlayer.posZ/16)*16;
                if(world.getBlockId(x,Y-2,z)==Block.blockDiamond.blockID) {
                    say("RELOAD: fixture marker found; preserving saved inventories."); finished=true; log.close(); return;
                }
                for(int a=-2;a<=18;a++) for(int b=-2;b<=12;b++) {
                    world.setBlockWithNotify(x+a,Y-1,z+b,Block.stone.blockID);
                    for(int c=0;c<5;c++) world.setBlockWithNotify(x+a,Y+c,z+b,0);
                }
                world.setBlockWithNotify(x,Y-2,z,Block.blockDiamond.blockID);
                game.thePlayer.setPositionAndRotation(x+7.5,Y+1,z+10.5,180,25);
                game.thePlayer.inventory.setInventorySlotContents(0,new ItemStack(HopperRegistry.block,16));
                vanilla(); optional(game);
                say("WAIT: real world ticks for chest chains, IC2 processing and BuildCraft delivery.");
            }
            ticks++;
            if(machine!=null && ticks<900) machine.getClass().getField("energy").setInt(machine,10000);
            if(woodenPipe!=null && ticks==40) {
                Object provider=woodenPipe.getClass().getMethod("getPowerProvider").invoke(woodenPipe);
                provider.getClass().getField("energyStored").setInt(provider,8);
                woodenPipe.getClass().getMethod("doWork").invoke(woodenPipe);
                say("BC: wooden item extraction powered with 8 API energy units.");
            }
            if(ticks==1000) {
                check(count((IInventory)world.getBlockTileEntity(x,Y,z))==16,"chest -> Hopper -> chest: 16/16");
                check(count((IInventory)world.getBlockTileEntity(x+3,Y,z))==11,"two Hopper chain: 11/11");
                check(count((IInventory)world.getBlockTileEntity(x+6,Y,z))==9,"double chest second half: 9/9");
                check(count((IInventory)world.getBlockTileEntity(x+9,Y,z))==7,"loose wool pickup: 7/7");
                if(machine!=null) check(count(machineOutput)==2 && machine.getStackInSlot(0)==null
                    && machine.getStackInSlot(2)==null,"IC2: one iron ore processed; two dust extracted from bottom");
                if(pipeTarget!=null) check(count(pipeTarget)==8 && count(pipeSource)==0,"BC wooden -> cobblestone -> Hopper: 8/8");
                say("COMPLETE: automated live-world assertions passed. Manual GUI, visual and reload checks remain.");
                finished=true; log.close();
            }
        } catch(Throwable failure) {
            finished=true; if(log!=null) { failure.printStackTrace(log); log.close(); }
            failure.printStackTrace();
        }
    }
    private static void vanilla() {
        chest(0,0,0); hopper(0,1,0,0); chest(0,2,0).setInventorySlotContents(0,new ItemStack(Block.cobblestone,16));
        chest(3,0,0); hopper(3,1,0,0); hopper(3,2,0,0).setInventorySlotContents(0,new ItemStack(Block.dirt,11));
        chest(6,0,0); hopper(6,1,0,0); chest(6,2,0); chest(7,2,0).setInventorySlotContents(26,new ItemStack(Block.sand,9));
        chest(9,0,0); hopper(9,1,0,0);
        world.spawnEntityInWorld(new EntityItem(world,x+9.5,Y+2.2,z+.5,new ItemStack(Block.cloth,7,3)));
        TileHopper test=hopper(0,0,4,5); TileEntityChest dest=chest(1,0,4);
        for(int i=0;i<27;i++) dest.setInventorySlotContents(i,new ItemStack(Block.stone,64));
        test.setInventorySlotContents(0,new ItemStack(Block.cloth,5,3));
        check(!InventoryAccess.moveOne(test,-1,dest,4) && count(test)==5,"full destination preserves source");
        dest.setInventorySlotContents(0,new ItemStack(Block.cloth,63,3));
        check(InventoryAccess.moveOne(test,-1,dest,4) && count(test)==4 && dest.getStackInSlot(0).stackSize==64,
            "partial stack accepts exactly one matching metadata item");
        dest.setInventorySlotContents(0,new ItemStack(Block.cloth,63,2));
        check(!InventoryAccess.moveOne(test,-1,dest,4),"different metadata does not merge");
        ItemStack tagged=new ItemStack(Item.pickaxeDiamond,1,17);
        tagged.addEnchantment(Enchantment.efficiency,2);
        test.setInventorySlotContents(1,tagged);
        NBTTagCompound tag=new NBTTagCompound(); test.writeToNBT(tag);
        TileHopper copy=new TileHopper(); copy.readFromNBT(tag);
        check(InventoryAccess.same(tagged,copy.getStackInSlot(1)),"NBT round trip preserves damage and enchantment");
        test.setInventorySlotContents(0,null);
        dest.setInventorySlotContents(1,null);
        check(InventoryAccess.moveOne(test,-1,dest,4) && InventoryAccess.same(tagged,dest.getStackInSlot(1)),
            "NBT-bearing tool transfers intact");
        TileHopper locked=hopper(4,0,4,5); chest(5,0,4);
        locked.setInventorySlotContents(0,new ItemStack(Block.dirt,3));
        world.setBlockWithNotify(x+4,Y,z+5,Block.torchRedstoneActive.blockID);
        check(locked.isLocked(),"redstone torch locks Hopper");
        locked.updateEntity(); check(count(locked)==3,"locked Hopper does not push");
        world.setBlockWithNotify(x+4,Y,z+5,0);
        locked.updateEntity(); check(count(locked)==2,"unlock resumes transfer");
        TileHopper broken=hopper(8,0,4,0); broken.setInventorySlotContents(0,tagged.copy());
        world.setBlockWithNotify(x+8,Y,z+4,0);
        java.util.List drops=world.getEntitiesWithinAABB(EntityItem.class,AxisAlignedBB.getBoundingBoxFromPool(x+8,Y,z+4,x+9,Y+1,z+5));
        check(drops.size()==1 && InventoryAccess.same(((EntityItem)drops.get(0)).item,tagged),"breaking loaded Hopper drops one intact tool");
        world.setBlockWithNotify(x+8,Y,z+4,0);
        drops=world.getEntitiesWithinAABB(EntityItem.class,AxisAlignedBB.getBoundingBoxFromPool(x+8,Y,z+4,x+9,Y+1,z+5));
        check(drops.size()==1,"removed block has no repeated drops");
        world.setBlockWithNotify(x+11,Y,z+4,Block.stoneOvenIdle.blockID);
        IInventory furnace=(IInventory)world.getBlockTileEntity(x+11,Y,z+4);
        check(InventoryAccess.insert(furnace,new ItemStack(Block.oreIron),1,true)==1,"furnace top accepts smeltable input");
        check(InventoryAccess.insert(furnace,new ItemStack(Block.dirt),1,true)==0,"furnace rejects unsmeltable input");
        check(InventoryAccess.insert(furnace,new ItemStack(Item.coal),0,true)==1,"legacy furnace bottom accepts fuel");
        check(InventoryAccess.insert(furnace,new ItemStack(Block.oreIron),2,true)==0,"furnace side output rejects insertion");
        furnace.setInventorySlotContents(2,new ItemStack(Item.ingotIron));
        check(InventoryAccess.moveOne(furnace,2,new TileHopper(),-1),"legacy furnace side output extraction contract");
        for(int side=2;side<=5;side++) {
            TileHopper visual=hopper(side*2,0,8,side);
            HopperRegistry.block.onBlockPlaced(world,visual.xCoord,Y,visual.zCoord,side^1);
            check(visual.facing()==side,"placement opposite clicked face "+side);
        }
        TileHopper display=hopper(7,0,7,0); display.setInventorySlotContents(0,new ItemStack(Block.cloth,13,3));
        display.setInventorySlotContents(1,tagged.copy());
    }
    private static void optional(Minecraft game) throws Exception {
        try {
            ItemStack macerator=((ItemStack)Class.forName("ic2.common.Ic2Items").getField("macerator").get(null)).copy();
            world.setBlockAndMetadataWithNotify(x+14,Y+1,z,macerator.itemID,macerator.getItemDamage());
            machine=(IInventory)world.getBlockTileEntity(x+14,Y+1,z);
            check(machine!=null && machine.getClass().getName().equals("ic2.common.TileEntityMacerator"),"actual IC2 Macerator created");
            machineOutput=hopper(14,0,0,5);
            hopper(14,2,0,0).setInventorySlotContents(0,new ItemStack(Block.oreIron));
            check(InventoryAccess.insert(machine,new ItemStack(Block.dirt),1,false)==0,"IC2 rejects invalid recipe");
            check(InventoryAccess.insert(machine,new ItemStack(Block.oreIron),0,false)==0,"IC2 battery face rejects ore");
            check(InventoryAccess.insert(machine,new ItemStack(Block.oreIron),2,false)==0,"IC2 output face rejects ore");
            machine.setInventorySlotContents(1,new ItemStack(Item.redstone,4));
            check(!InventoryAccess.moveOne(machine,0,new TileHopper(),-1),"IC2 battery cannot be extracted by Hopper");
            machine.setInventorySlotContents(1,null);
        } catch(ClassNotFoundException absent) { say("SKIP IC2: not installed"); }
        try {
            Class registry=Class.forName("BuildCraftTransport");
            pipeSource=hopper(12,0,6,0); pipeSource.setInventorySlotContents(0,new ItemStack(Block.cloth,8,5));
            pipeTarget=hopper(15,0,6,0);
            placePipe(game,registry,"pipeItemsWood",13,0,6);
            placePipe(game,registry,"pipeItemsCobblestone",14,0,6);
            woodenPipe=world.getBlockTileEntity(x+13,Y,z+6);
            check(woodenPipe!=null,"BuildCraft wooden and cobblestone item pipes placed using ItemPipe");
            check(!InventoryAccess.moveOne(pipeTarget,-1,(IInventory)woodenPipe,5),"no unsupported active pipe injection");
        } catch(ClassNotFoundException absent) { say("SKIP BuildCraft: not installed"); }
    }
    private static void placePipe(Minecraft game,Class registry,String field,int a,int b,int c) throws Exception {
        Item item=(Item)registry.getField(field).get(null);
        check(item.onItemUse(new ItemStack(item),game.thePlayer,world,x+a,Y+b-1,z+c,1),"placed "+field);
    }
    private static TileHopper hopper(int a,int b,int c,int face) {
        world.setBlockAndMetadataWithNotify(x+a,Y+b,z+c,HopperRegistry.block.blockID,face);
        return (TileHopper)world.getBlockTileEntity(x+a,Y+b,z+c);
    }
    private static TileEntityChest chest(int a,int b,int c) {
        world.setBlockWithNotify(x+a,Y+b,z+c,Block.chest.blockID);
        return (TileEntityChest)world.getBlockTileEntity(x+a,Y+b,z+c);
    }
    private static int count(IInventory inventory) {
        int count=0; for(int i=0;i<inventory.getSizeInventory();i++) if(inventory.getStackInSlot(i)!=null) count+=inventory.getStackInSlot(i).stackSize;
        return count;
    }
    private static void check(boolean condition,String name) { if(!condition) throw new AssertionError(name); say("PASS: "+name); }
    private static void say(String message) { log.println(message); System.out.println("[ZTO Hopper test] "+message); }
}
