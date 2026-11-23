package zed.tekkitoverhaul.distillation;

import net.minecraft.src.TileEntity;

/**
 * Auxiliary apparatus block with the linked facing used by the IC2 wrench.
 */
public class TileEntityDistillationPart extends TileEntityDistillationComponent {
    private DistillationLayout.Facing visualFacing;
    private long lastVisualCheck = Long.MIN_VALUE;

    @Override
    public void updateEntity() {
        // Older worlds used this stateless tile for both liquid port blocks.
        if (getClass() != TileEntityDistillationPart.class || worldObj == null) return;
        int id = worldObj.getBlockId(xCoord, yCoord, zCoord);
        TileEntity upgraded = null;
        if (id == DistillationRegistry.blockId(DistillationPartType.RECEIVER)) {
            upgraded = new TileEntityDistillationReceiver();
        } else if (id == DistillationRegistry.blockId(DistillationPartType.CONDENSER)) {
            upgraded = new TileEntityDistillationCondenser();
        }
        if (upgraded != null) {
            worldObj.setBlockTileEntity(xCoord, yCoord, zCoord, upgraded);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, id);
        }
    }

    public DistillationLayout.Facing getVisualFacing() {
        if (worldObj == null) return getPreferredFacing();
        long tick = worldObj.getWorldTime();
        if (lastVisualCheck == Long.MIN_VALUE || tick < lastVisualCheck ||
                tick - lastVisualCheck >= 20L) {
            lastVisualCheck = tick;
            DistillationPartType part = DistillationRegistry.typeForBlockId(
                    worldObj.getBlockId(xCoord, yCoord, zCoord));
            DistillationLayout.Match linked = DistillationStructure.resolve(
                    worldObj, xCoord, yCoord, zCoord, part);
            visualFacing = linked == null ? null : linked.facing;
            if (linked != null && (part == DistillationPartType.HEATER || part == DistillationPartType.COLUMN)) {
                TileEntity boiler = worldObj.getBlockTileEntity(linked.boilerX, linked.boilerY, linked.boilerZ);
                if (boiler instanceof TileEntityDistillationBoiler)
                    visualFacing = ((TileEntityDistillationBoiler) boiler).getVisualFacing();
            }
        }
        return visualFacing == null ? getPreferredFacing() : visualFacing;
    }

    @Override
    protected boolean canTurnTo(DistillationLayout.Facing facing) {
        DistillationPartType part = DistillationRegistry.typeForBlockId(worldObj.getBlockId(xCoord, yCoord, zCoord));
        return DistillationStructure.resolve(worldObj, xCoord, yCoord, zCoord, part) == null;
    }
}
