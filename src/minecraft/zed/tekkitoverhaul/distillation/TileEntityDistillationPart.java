package zed.tekkitoverhaul.distillation;

import net.minecraft.src.TileEntity;

/**
 * Visual anchor for an auxiliary apparatus block.
 * Resolve and cache the nearest matching boiler orientation for rendering.
 */
public class TileEntityDistillationPart extends TileEntityDistillationComponent {
    private DistillationLayout.Facing visualFacing;
    private boolean upperColumn;
    private long lastVisualCheck = Long.MIN_VALUE;

    @Override
    public void updateEntity() {
        // Older worlds used this stateless tile for receivers. Upgrade it in place.
        if (getClass() != TileEntityDistillationPart.class || worldObj == null) return;
        int id = worldObj.getBlockId(xCoord, yCoord, zCoord);
        if (id == DistillationRegistry.blockId(DistillationPartType.RECEIVER)) {
            worldObj.setBlockTileEntity(xCoord, yCoord, zCoord, new TileEntityDistillationReceiver());
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
            upperColumn = part == DistillationPartType.COLUMN &&
                    linked != null && yCoord == linked.boilerY + 2;
        }
        return visualFacing == null ? getPreferredFacing() : visualFacing;
    }

    @Override
    protected boolean canTurnTo(DistillationLayout.Facing facing) {
        DistillationPartType part = DistillationRegistry.typeForBlockId(worldObj.getBlockId(xCoord, yCoord, zCoord));
        return DistillationStructure.resolve(worldObj, xCoord, yCoord, zCoord, part) == null;
    }

    public boolean isUpperColumn() {
        getVisualFacing();
        return upperColumn;
    }
}
