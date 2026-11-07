package zed.tekkitoverhaul.distillation;

import net.minecraft.src.TileEntity;

/**
 * Visual anchor for an auxiliary apparatus block.
 * Resolve and cache the nearest matching boiler orientation for rendering.
 */
public class TileEntityDistillationPart extends TileEntity {
    private DistillationLayout.Facing visualFacing = DistillationLayout.Facing.EAST;
    private boolean upperColumn;
    private long lastVisualCheck = Long.MIN_VALUE;

    public DistillationLayout.Facing getVisualFacing() {
        if (worldObj == null) return visualFacing;
        long tick = worldObj.getWorldTime();
        if (lastVisualCheck == Long.MIN_VALUE || tick < lastVisualCheck ||
                tick - lastVisualCheck >= 20L) {
            lastVisualCheck = tick;
            DistillationPartType part = DistillationRegistry.typeForBlockId(
                    worldObj.getBlockId(xCoord, yCoord, zCoord));
            DistillationLayout.Match linked = DistillationStructure.resolve(
                    worldObj, xCoord, yCoord, zCoord, part);
            visualFacing = linked == null ?
                    DistillationLayout.Facing.EAST : linked.facing;
            upperColumn = part == DistillationPartType.COLUMN &&
                    linked != null && yCoord == linked.boilerY + 2;
        }
        return visualFacing;
    }

    public boolean isUpperColumn() {
        getVisualFacing();
        return upperColumn;
    }
}
