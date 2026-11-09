package zed.tekkitoverhaul.hopper;

import net.minecraft.src.*;

/** Legacy atlas rendering using the measured 1.5.2 bounds and projected face UVs. */
public final class RenderHopper {
    private RenderHopper() { }
    public static boolean render(RenderBlocks renderer,IBlockAccess world,int x,int y,int z,Block block) {
        boolean all=renderer.renderAllFaces;
        int override=renderer.overrideBlockTexture;
        renderer.renderAllFaces=true;
        try {
            box(renderer,block,x,y,z,0,.625,0,1,1,1);
            Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world,x,y,z));
            Tessellator.instance.setColorOpaque_F(1,1,1);
            renderer.renderSouthFace(block,x-.875,y,z,HopperRegistry.outer);
            renderer.renderNorthFace(block,x+.875,y,z,HopperRegistry.outer);
            renderer.renderWestFace(block,x,y,z-.875,HopperRegistry.outer);
            renderer.renderEastFace(block,x,y,z+.875,HopperRegistry.outer);
            renderer.renderTopFace(block,x,y-.375,z,HopperRegistry.inside);
            renderer.overrideBlockTexture=HopperRegistry.outer;
            box(renderer,block,x,y,z,.25,.25,.25,.75,.623,.75);
            int face=world.getBlockMetadata(x,y,z)&7;
            switch(face) {
                case 2: box(renderer,block,x,y,z,.375,.25,0,.625,.5,.25); break;
                case 3: box(renderer,block,x,y,z,.375,.25,.75,.625,.5,1); break;
                case 4: box(renderer,block,x,y,z,0,.25,.375,.25,.5,.625); break;
                case 5: box(renderer,block,x,y,z,.75,.25,.375,1,.5,.625); break;
                default:
                    boolean chestBelow = world.getBlockId(x,y-1,z) == Block.chest.blockID
                        || InventoryAccess.isIronChest(world.getBlockTileEntity(x,y-1,z));
                    box(renderer,block,x,y,z,.375,chestBelow ? -.0625 : 0,.375,.625,.25,.625);
            }
        } finally {
            block.setBlockBounds(0,0,0,1,1,1);
            renderer.overrideBlockTexture=override; renderer.renderAllFaces=all;
        }
        return true;
    }
    private static void box(RenderBlocks r,Block b,int x,int y,int z,double a,double c,double d,double e,double f,double g) {
        b.setBlockBounds((float)a,(float)c,(float)d,(float)e,(float)f,(float)g);
        r.renderStandardBlock(b,x,y,z);
    }
}
