package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.BlockDoor;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.PathEntity;
import net.minecraft.src.PathPoint;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

public final class EntityBreacher extends TacticalHostile {
    public static final int PRESSURE_CONTACT_TICKS = 4;
    private static final float SPEED = 0.21F;
    private static final int ROUTE_INTERVAL = 24;
    private static final int GIVE_UP_TICKS = 192;
    private static final int MAX_IMPACTS = 3;
    public static final int FORCE_TICKS = 36;
    private static final int FORCE_RETRY = 60;
    private static final int FORCE_RECOVERY = 12;
    private static final double ENTRANCE_REACH_SQ = 3.24D;
    private static final double CONTACT_SQ = 4.0D;

    private int nextRoute;
    private int nextMelee;
    private int nextBlink;
    private int lastProgress;
    private double progressX;
    private double progressZ;
    private int impacts;
    private boolean pressureSwing;
    private int pressureSoundTick = -1;
    private double impactX, impactY, impactZ;
    private Entrance entrance;
    private int forceAt, retryAt, recoveryUntil;

    public EntityBreacher(World world) {
        // A centered legacy path must clear the open door's three-sixteenths panel.
        super(world, 0.60F, 1.9F, "breacher");
        attackStrength = 3;
    }

    @Override
    public int getMaxHealth() { return 22; }

    @Override
    protected int unseenLimit() { return GIVE_UP_TICKS + ROUTE_INTERVAL; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        if (entrance != null) {
            continueForcing();
            return;
        }
        if (ticksExisted < recoveryUntil) return;
        if (visible && distanceSq < CONTACT_SQ && target.boundingBox.maxY > boundingBox.minY
                && target.boundingBox.minY < boundingBox.maxY) {
            getNavigator().clearPathEntity();
            lastProgress = ticksExisted;
            if (ticksExisted >= nextMelee) {
                nextMelee = ticksExisted + 30;
                pressureSwing = false;
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
        boolean route = reaches(path, target.posX, y, target.posZ);
        if (!holdingSiteSiege() && worldObj.difficultySetting >= 2) {
            Entrance barrier = entranceBetween(target);
            if (barrier != null && (!route || usesClosedEntrance(path, barrier))) {
                if (ticksExisted >= retryAt && onGround && barrier.inReach(this)) {
                    entrance = barrier;
                    forceAt = ticksExisted + FORCE_TICKS;
                    getNavigator().clearPathEntity();
                    motionX = motionZ = 0;
                    lookAt(barrier);
                    return;
                }
                PathEntity approach = approach(barrier);
                if (approach != null) getNavigator().setPath(approach, SPEED);
                else getNavigator().clearPathEntity();
                return;
            }
        }
        boolean stalled = ticksExisted - lastProgress >= 2 * ROUTE_INTERVAL;
        if (!holdingSiteSiege() && visible && (!route || stalled) && ticksExisted >= nextBlink && distanceSq > 9 && distanceSq < 81) {
            double[] landing = EnderkinBlink.elevatedLanding(this, target, 2.6D, 3);
            boolean advanced = landing != null;
            if (advanced) EnderkinBlink.move(this, landing[0], landing[1], landing[2], 6, 0.92F);
            else if (Math.abs(y - boundingBox.minY) < 0.9D) advanced = EnderkinBlink.shift(this,
                    Math.atan2(target.posZ - posZ, target.posX - posX), 2.25D, 6, 0.92F);
            nextBlink = ticksExisted + (advanced ? 125 : ROUTE_INTERVAL);
            if (advanced) {
                lastProgress = ticksExisted;
                return;
            }
        }
        if ((route && !stalled) || !pressure()) getNavigator().setPath(path, SPEED);
        else getNavigator().clearPathEntity();
    }

    @Override
    protected boolean canAcquireUnseen(EntityPlayer player) {
        if (worldObj.difficultySetting < 2 || getDistanceSqToEntity(player) > 49.0D) return false;
        Entrance barrier = entranceBetween(player);
        return barrier != null && (barrier.inReach(this) || approach(barrier) != null);
    }

    private Entrance entranceBetween(EntityPlayer player) {
        double height = boundingBox.minY + 0.8D;
        double playerHeight = player.boundingBox.minY + 0.8D;
        MovingObjectPosition hit = worldObj.rayTraceBlocks(Vec3D.createVector(posX, height, posZ),
                Vec3D.createVector(player.posX, playerHeight, player.posZ));
        if (hit == null || hit.sideHit < 2 || hit.sideHit > 5
                || !worldObj.blockExists(hit.blockX, hit.blockY, hit.blockZ)) return null;
        int id = worldObj.getBlockId(hit.blockX, hit.blockY, hit.blockZ);
        boolean door = id == Block.doorWood.blockID;
        if (!door && id != Block.fenceGate.blockID) return null;
        int bottom = hit.blockY;
        if (door && (worldObj.getBlockMetadata(hit.blockX, bottom, hit.blockZ) & 8) != 0) bottom--;
        Entrance barrier = new Entrance(worldObj, hit, bottom, door);
        if (!barrier.stillClosed(worldObj) || !barrier.onFarSide(player)) return null;

        // Start the second ray immediately beyond this cell to reject a second obstacle.
        double edge = barrier.farEdge();
        double offset = barrier.side < 4 ? player.posZ - posZ : player.posX - posX;
        double start = barrier.side < 4 ? posZ : posX;
        double fraction = (edge - start) / offset;
        if (fraction <= 0 || fraction >= 1) return null;
        Vec3D beyond = Vec3D.createVector(posX + (player.posX - posX) * fraction,
                height + (playerHeight - height) * fraction, posZ + (player.posZ - posZ) * fraction);
        return worldObj.rayTraceBlocks(beyond, Vec3D.createVector(player.posX, playerHeight, player.posZ)) == null
                ? barrier : null;
    }

    private boolean usesClosedEntrance(PathEntity path, Entrance barrier) {
        if (MathHelper.floor_double(posX) == barrier.x && MathHelper.floor_double(posZ) == barrier.z) return true;
        for (int i = path.getCurrentPathIndex(); i < path.getCurrentPathLength(); i++) {
            PathPoint point = path.getPathPointFromIndex(i);
            int x = point.xCoord, y = point.yCoord, z = point.zCoord;
            for (int dy = 0; dy <= 1; dy++) {
                Block block = Block.blocksList[worldObj.getBlockId(x, y + dy, z)];
                if ((block == Block.doorWood || block == Block.doorSteel || block == Block.fenceGate)
                        && !block.getBlocksMovement(worldObj, x, y + dy, z)) return true;
            }
        }
        return false;
    }

    private PathEntity approach(Entrance barrier) {
        double x = barrier.x + 0.5D + (barrier.side == 4 ? -1 : barrier.side == 5 ? 1 : 0);
        double z = barrier.z + 0.5D + (barrier.side == 2 ? -1 : barrier.side == 3 ? 1 : 0);
        PathEntity path = plan(x, barrier.y, z);
        return reaches(path, x, barrier.y, z) && !usesClosedEntrance(path, barrier) ? path : null;
    }

    private void continueForcing() {
        Entrance current = entrance.stillClosed(worldObj) ? entranceBetween(target) : null;
        if (worldObj.difficultySetting < 2 || !onGround || current == null || !entrance.samePart(current)
                || !current.inReach(this)) {
            stopForcing();
            return;
        }
        lookAt(current);
        if (ticksExisted < forceAt) return;
        openWoodenEntrance(worldObj, current.x, current.y, current.z, current.door, current.lower);
        stopForcing();
        signalSiteImpact();
        lastProgress = ticksExisted;
        getNavigator().setPath(plan(target.posX, target.boundingBox.minY, target.posZ), SPEED);
    }

    /** Callers revalidate the specific vanilla entrance, its state and the approach before applying. */
    public static void openWoodenEntrance(World world, int x, int y, int z, boolean door, int lower) {
        if (door) {
            ((BlockDoor) Block.doorWood).onPoweredBlockChange(world, x, y, z, true);
        } else {
            world.setBlockMetadataWithNotify(x, y, z, lower | 4);
            world.playAuxSFX(1003, x, y, z, 0);
        }
    }

    public void signalSiteImpact() {
        pressureSwing = true;
        swing();
    }

    private void stopForcing() {
        entrance = null;
        retryAt = ticksExisted + FORCE_RETRY;
        nextRoute = ticksExisted;
        recoveryUntil = ticksExisted + FORCE_RECOVERY;
    }

    private void lookAt(Entrance barrier) {
        getLookHelper().setLookPosition(barrier.hitX, barrier.hitY, barrier.hitZ, 30.0F, 30.0F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean landed = super.attackEntityFrom(source, damage);
        if (landed && damage > 0 && entrance != null) stopForcing();
        return landed;
    }

    public boolean isForcingEntrance() { return entrance != null; }

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
            pressureSwing = true;
            swing();
            pressureSoundTick = ticksExisted + PRESSURE_CONTACT_TICKS;
            impactX = hit.hitVec.xCoord;
            impactY = hit.hitVec.yCoord;
            impactZ = hit.hitVec.zCoord;
        }
        return true;
    }

    public boolean isPressureSwing() { return pressureSwing; }

    @Override
    protected void updateAttackPose() {
        if (ticksExisted == pressureSoundTick) {
            worldObj.playSoundEffect(impactX, impactY, impactZ, "step.stone", 0.75F, 0.6F);
            pressureSoundTick = -1;
        }
    }

    @Override
    protected void resetTactics() {
        lastProgress = 0;
        impacts = 0;
        if (entrance != null) stopForcing();
    }

    private static final class Entrance {
        final int x, y, z, side, lower, upper;
        final boolean door;
        final double hitX, hitY, hitZ;

        Entrance(World world, MovingObjectPosition hit, int bottom, boolean door) {
            x = hit.blockX;
            y = bottom;
            z = hit.blockZ;
            side = hit.sideHit;
            this.door = door;
            lower = world.getBlockMetadata(x, y, z);
            upper = door ? world.getBlockMetadata(x, y + 1, z) : 0;
            hitX = hit.hitVec.xCoord;
            hitY = hit.hitVec.yCoord;
            hitZ = hit.hitVec.zCoord;
        }

        boolean stillClosed(World world) {
            if (!world.checkChunksExist(x - 1, y, z - 1, x + 1, y + 1, z + 1) || (lower & 12) != 0
                    || world.getBlockMetadata(x, y, z) != lower) return false;
            return door ? world.getBlockId(x, y, z) == Block.doorWood.blockID
                    && world.getBlockId(x, y + 1, z) == Block.doorWood.blockID && (upper & 8) != 0
                    && world.getBlockMetadata(x, y + 1, z) == upper
                    : world.getBlockId(x, y, z) == Block.fenceGate.blockID;
        }

        boolean samePart(Entrance other) {
            return x == other.x && y == other.y && z == other.z && door == other.door
                    && lower == other.lower && upper == other.upper && side == other.side;
        }

        double farEdge() {
            return side == 2 ? z + 1.001D : side == 3 ? z - 0.001D
                    : side == 4 ? x + 1.001D : x - 0.001D;
        }

        boolean onFarSide(EntityPlayer player) {
            double position = side < 4 ? player.posZ : player.posX;
            return (side & 1) == 0 ? position > farEdge() : position < farEdge();
        }

        boolean inReach(EntityBreacher actor) {
            return actor.getDistanceSq(hitX, hitY, hitZ) <= ENTRANCE_REACH_SQ;
        }
    }
}
