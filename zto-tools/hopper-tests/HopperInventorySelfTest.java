import java.lang.reflect.Method;
import net.minecraft.src.*;
import zed.tekkitoverhaul.hopper.*;

/** Runs against compiled MCP classes, not inventory/NBT stubs. */
public final class HopperInventorySelfTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Method mapping=TileEntity.class.getDeclaredMethod("addMapping",Class.class,String.class);
        mapping.setAccessible(true); mapping.invoke(null,TileHopper.class,"ZTOHopperTest");
        TileHopper source=new TileHopper(), target=new TileHopper();
        ItemStack wool=new ItemStack(Block.cloth,64,3);
        source.setInventorySlotContents(0,wool.copy());
        target.setInventorySlotContents(0,new ItemStack(Block.cloth,63,3));
        check(InventoryAccess.moveOne(source,-1,target,-1),"one-item transfer");
        check(source.getStackInSlot(0).stackSize==63 && target.getStackInSlot(0).stackSize==64,"count conservation");
        for(int i=0;i<5;i++) target.setInventorySlotContents(i,new ItemStack(Block.cloth,64,3));
        check(!InventoryAccess.moveOne(source,-1,target,-1),"full destination");
        check(source.getStackInSlot(0).stackSize==63,"failed transfer retains source");
        target.setInventorySlotContents(0,new ItemStack(Block.cloth,63,2));
        check(InventoryAccess.insert(target,wool,-1,true)==0,"metadata separation");
        ItemStack a=new ItemStack(Block.cloth,63,3), b=new ItemStack(Block.cloth,4,3);
        a.stackTagCompound=new NBTTagCompound(); a.stackTagCompound.setString("owner","A");
        b.stackTagCompound=new NBTTagCompound(); b.stackTagCompound.setString("owner","B");
        target.setInventorySlotContents(0,a);
        check(InventoryAccess.insert(target,b,-1,true)==0,"different NBT cannot merge");
        b.stackTagCompound=(NBTTagCompound)a.stackTagCompound.copy();
        check(InventoryAccess.insert(target,b,-1,false)==1 && target.getStackInSlot(0).stackSize==63,"simulation has no mutation");
        check(InventoryAccess.insert(target,b,-1,true)==1 && b.stackSize==4,"partial insertion leaves caller ownership");
        source.setInventorySlotContents(1,b);
        NBTTagCompound nbt=new NBTTagCompound(); source.writeToNBT(nbt);
        TileHopper loaded=new TileHopper(); loaded.readFromNBT(nbt);
        check(InventoryAccess.same(b,loaded.getStackInSlot(1)) && loaded.getStackInSlot(1).stackSize==4,"persistent NBT");
        TileEntityFurnace furnace=new TileEntityFurnace();
        check(InventoryAccess.insert(furnace,new ItemStack(Block.oreIron),1,false)==1,"furnace top input");
        check(InventoryAccess.insert(furnace,new ItemStack(Block.dirt),1,false)==0,"invalid furnace input");
        check(InventoryAccess.insert(furnace,new ItemStack(Item.coal),0,false)==1,"legacy fuel bottom");
        check(InventoryAccess.insert(furnace,new ItemStack(Block.oreIron),2,false)==0,"output insertion denied");
        furnace.setInventorySlotContents(1,new ItemStack(Item.coal));
        check(!InventoryAccess.moveOne(furnace,0,new TileHopper(),-1),"fuel extraction denied");
        furnace.setInventorySlotContents(2,new ItemStack(Item.ingotIron));
        check(InventoryAccess.moveOne(furnace,2,new TileHopper(),-1),"legacy lateral product extraction");
        TileEntityChest left=new TileEntityChest(), right=new TileEntityChest();
        right.setInventorySlotContents(26,new ItemStack(Block.stone,2));
        check(InventoryAccess.moveOne(new InventoryLargeChest("test",left,right),0,new TileHopper(),-1),"second double chest half");
        check(right.getStackInSlot(26).stackSize==1,"double chest count");
        System.out.println("PASS: "+checks+" Hopper assertions using actual MCP inventory/NBT classes; IC2/BC absent.");
    }
    private static void check(boolean condition,String message) {
        checks++; if(!condition) throw new AssertionError(message);
    }
}
