package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.PathEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

public final class EntityBreacher extends TacticalHostile {
    private static final float SPEED = 0.21F;
    private static final int ROUTE_INTERVAL = 24;
    private static final int GIVE_UP_TICKS = 192;
    private static final int MAX_IMPACTS = 3;
    private static final double CONTACT_SQ = 4.0D;

    private int nextRoute;
    private int nextMelee;
    private int lastProgress;
    private double progressX;
    private double progressZ;
    private int impacts;

    public EntityBreacher(World world) {
        super(world, 0.85F, 1.9F, "breacher");
        attackStrength = 3;
    }

    @Override
    public int getMaxHealth() { return 22; }

    @Override
    protected int unseenLimit() { return GIVE_UP_TICKS + ROUTE_INTERVAL; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        if (visible && distanceSq < CONTACT_SQ && target.boundingBox.maxY > boundingBox.minY
                && target.boundingBox.minY < boundingBox.maxY) {
            getNavigator().clearPathEntity();
            lastProgress = ticksExisted;
            if (ticksExisted >= nextMelee) {
                nextMelee = ticksExisted + 30;
                swing();
                attackEntityAsMob(target);
            }
            return;
        }
        if (ticksExisted < nextRoute) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;

        double dx = posX - progressX;
        double dz = posZ - progressZ;
        if (lastProgress == 0 || dx * dx + dz * dz >= 0.25D) {
            lastProgress = ticksExisted;
            impacts = 0;
            progressX = posX;
            progressZ = posZ;
        }
        if (ticksExisted - lastProgress >= GIVE_UP_TICKS) {
            disengage();
            return;
        }
        double y = target.boundingBox.minY;
        PathEntity path = plan(target.posX, y, target.posZ);
        // A path may start inside a door's block cell while its panel still blocks collision.
        boolean route = reaches(path, target.posX, y, target.posZ);
        boolean stalled = ticksExisted - lastProgress >= 2 * ROUTE_INTERVAL;
        if ((route && !stalled) || !pressure()) getNavigator().setPath(path, SPEED);
        else getNavigator().clearPathEntity();
    }

    private boolean pressure() {
        double height = boundingBox.minY + 0.8D;
        Vec3D start = Vec3D.createVector(posX, height, posZ);
        Vec3D end = Vec3D.createVector(target.posX, target.boundingBox.minY + 0.8D, target.posZ);
        MovingObjectPosition hit = worldObj.rayTraceBlocks(start, end);
        // The legacy ray tracer advances its start vector in place.
        if (hit == null || hit.hitVec.squareDistanceTo(posX, height, posZ) > 2.25D) return false;

        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        Block block = Block.blocksList[worldObj.getBlockId(x, y, z)];
        if (block == null || block.hasTileEntity(worldObj.getBlockMetadata(x, y, z))) return false;
        boolean access = block == Block.doorWood || block == Block.doorSteel || block == Block.fenceGate;
        if (access ? block.getBlocksMovement(worldObj, x, y, z)
                : !block.isOpaqueCube() && block != Block.fence && block != Block.fenceIron) return false;

        getLookHelper().setLookPosition(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 30.0F, 30.0F);
        if (impacts < MAX_IMPACTS) {
            impacts++;
            swing();
            worldObj.playSoundEffect(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord,
                    "step.stone", 0.75F, 0.6F);
        }
        return true;
    }

    @Override
    protected void resetTactics() {
        lastProgress = 0;
        impacts = 0;
    }
}
