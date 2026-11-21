package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityItem;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.PathEntity;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Small Enderkin available through its spawn egg. */
public final class EntitySaboteur extends TacticalHostile {
    public static final byte IDLE = 0, APPROACH = 1, WINDUP = 2, RECOVER = 3, CANCELLED = 4,
            EVADE = 5, DECOY = 6, DISMANTLE = 7, CARRY = 8;
    private static final int POSE = 20;
    private static final float SPEED = 0.34F;
    private static final int ROUTE_INTERVAL = 20;
    private static final int APPROACH_BLINK_COOLDOWN = 32;
    private static final int EVASIVE_BLINK_COOLDOWN = 38;
    private static final int BLINK_RETRY = 8;
    private static final int SCAN_INTERVAL = 24;
    private static final int WINDUP_TICKS = 25;
    private static final int DISMANTLE_TICKS = 35;
    private static final int ESCAPE_TICKS = 100;
    private static final int RECOVERY_TICKS = 18;
    private static final int ACTION_COOLDOWN = 120;
    private static final double CONTACT_SQ = 5.76D;
    private static final double[] BLINK_ANGLES = {0, Math.PI / 5, -Math.PI / 5};
    private static final double[] ESCAPE_ANGLES = {
        0, Math.PI / 3, -Math.PI / 3, Math.PI / 2, -Math.PI / 2, 0
    };
    private static final int[] SIDE_X = {1, -1, 0, 0};
    private static final int[] SIDE_Z = {0, 0, 1, -1};
    private int nextRoute, nextBlink, retryBlink, nextMelee, evadeEnd, hitEvadeEnd, closeTicks;
    private int evasiveAttempts, evasiveBlinks, failedBlinks, rejectedDestinations;
    private enum BlinkCause { NONE, APPROACH, HIT, CLOSE, INTERRUPTED, CARGO }
    private BlinkCause blinkCause = BlinkCause.NONE, pendingEvade = BlinkCause.NONE;
    private int nextScan, windupEnd, recoverEnd, nextAction, approachEnd, nextSide;
    private int stalledRoutes;
    private double progressX, progressZ;
    private int pathPlans, blinkAttempts, blinkChecks, blinks;
    private int scans, scannedBlocks, windups, cancelled, disruptions, diversions;
    private zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds assignment;
    private int entryPathChecks, siteArrival = -1;
    private TileEntity machine;

    /** Preflight at the final verified approach cell before inserting this entity outside. */
    public boolean assignSiteMachine(zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds bounds) {
        assignment = bounds;
        onGround = true;
        try {
            scan();
            return machine != null;
        } finally {
            onGround = false;
        }
    }

    @Override public boolean carriesSiteCargo() { return hasCargo(); }

    @Override protected boolean siteObjective(EntityPlayer defender) {
        if (assignment == null) return false;
        if (siteArrival < 0) siteArrival = ticksExisted;
        target = defender;
        heartbeat = worldObj.getWorldTime();
        if (windingUp()) continueWindup(false);
        else if (machine != null) {
            if (invalidTarget() != Outcome.NONE) finish(Outcome.TARGET_CHANGED, true);
            else if (ticksExisted - siteArrival > 120) finish(Outcome.OUT_OF_REACH, true);
            else approachMachine();
        } else if (outcome == Outcome.APPLIED && ticksExisted < recoverEnd && defender != null)
            blink(defender.posX, defender.posZ, BlinkCause.INTERRUPTED);
        return true;
    }

    private TileEntity connection;
    private SabotageTarget disruption;
    private TransportTheft transportTheft;
    private ItemStack stolenComponent;
    private NBTTagCompound unresolvedComponent;
    private String componentItemClass = "";
    private boolean hasStolenPipe;
    private int escapeUntil, theftX, theftY, theftZ;
    private double escapeAngle;
    private enum Recovery { NONE, CARRYING, WAITING_FOR_RELEASE, DEATH, ESCAPE }
    private Recovery recovery = Recovery.NONE;
    private String cargoFamily = "NONE";
    private String targetType = "NONE";
    private int machineX, machineY, machineZ;
    private long heartbeat = Long.MIN_VALUE;
    private enum Outcome { NONE, NO_SUPPORTED_TARGET, UNSUPPORTED_CONNECTION, OUT_OF_REACH, NO_SIGHT, TARGET_CHANGED, ALREADY_PAUSED,
        HIT, PLAYER_LOST, APPLIED, DECOY, COMPONENT_STOLEN, UNLOADED, UNSAFE_TRANSPORT, REMOVAL_FAILED }
    private Outcome outcome = Outcome.NONE;
    private SaboteurBlink.Result blinkResult = SaboteurBlink.Result.NOT_GROUNDED;

    public EntitySaboteur(World world) {
        super(world, 0.45F, 1.1F, "saboteur");
        attackStrength = 1;
        nextScan = 1 + rand.nextInt(SCAN_INTERVAL);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE, Byte.valueOf(IDLE));
    }

    public byte pose() { return dataWatcher.getWatchableObjectByte(POSE); }

    private void pose(byte value) { dataWatcher.updateObject(POSE, Byte.valueOf(value)); }

    public int blinkCooldown() { return Math.max(0, nextBlink - ticksExisted); }
    public int actionCooldown() { return Math.max(0, nextAction - ticksExisted); }
    public int blinkBackoff() { return Math.max(0, retryBlink - ticksExisted); }
    public int windupRemaining() { return windingUp() ? Math.max(0, windupEnd - ticksExisted) : 0; }
    public TileEntity disruptionTarget() { return machine; }
    public TileEntity disruptionConnection() { return connection; }
    public String targetType() { return targetType; }
    public String lastOutcome() { return outcome.name(); }
    public String recoveryOutcome() { return recovery.name(); }
    public String cargoFamily() { return cargoFamily; }
    public String lastBlinkResult() { return blinkResult.name(); }
    public String lastBlinkCause() { return blinkCause.name(); }
    public int evasiveAttempts() { return evasiveAttempts; }
    public int evasiveBlinks() { return evasiveBlinks; }
    public int failedBlinks() { return failedBlinks; }
    public int rejectedDestinations() { return rejectedDestinations; }
    public int scans() { return scans; }
    public int scannedBlocks() { return scannedBlocks; }
    public int pathPlans() { return pathPlans; }
    public int blinkAttempts() { return blinkAttempts; }
    public int blinkChecks() { return blinkChecks; }
    public int blinks() { return blinks; }
    public int windups() { return windups; }
    public int cancelled() { return cancelled; }
    public int disruptions() { return disruptions; }
    public int diversions() { return diversions; }

    @Override
    public int getMaxHealth() { return 12; }

    @Override
    protected int unseenLimit() { return 60; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        heartbeat = worldObj.getWorldTime();
        if (windingUp()) {
            continueWindup(visible);
            return;
        }
        if (ticksExisted < evadeEnd) return;
        closeTicks = visible && distanceSq <= 6.25D ? Math.min(8, closeTicks + 1) : 0;
        BlinkCause escape = ticksExisted < hitEvadeEnd ? pendingEvade
                : closeTicks >= 8 && ticksExisted >= recoverEnd ? BlinkCause.CLOSE : BlinkCause.NONE;
        if (escape != BlinkCause.NONE && (escape != BlinkCause.INTERRUPTED || ticksExisted >= recoverEnd - 12)
                && blink(target.posX, target.posZ, escape)) {
            hitEvadeEnd = closeTicks = 0;
            evadeEnd = ticksExisted + 6;
            pose(EVADE);
            return;
        }
        if (ticksExisted < recoverEnd) return;
        pose(APPROACH);

        if (ticksExisted >= nextAction) {
            Outcome rejection = approachRejection();
            if (rejection != Outcome.NONE) {
                outcome = rejection;
                clearTarget();
                nextScan = Math.max(nextScan, ticksExisted + SCAN_INTERVAL);
                getNavigator().clearPathEntity();
            }
            if (machine == null && ticksExisted >= nextScan) scan();
            if (machine != null) {
                approachMachine();
                return;
            }
            if (scans > 0 && visible && distanceSq > 2.25D && distanceSq <= 12.25D && onGround) {
                startWindup();
                return;
            }
        }

        if (defend(distanceSq, visible)) return;
        pursue(target.posX, target.boundingBox.minY, target.posZ, visible);
    }

    private Outcome approachRejection() {
        if (machine == null) return Outcome.NONE;
        Outcome invalid = invalidTarget();
        if (invalid != Outcome.NONE) return invalid;
        if (disruption != null && disruption.isDisrupted()) return Outcome.ALREADY_PAUSED;
        return ticksExisted >= approachEnd ? Outcome.OUT_OF_REACH : Outcome.NONE;
    }

    private void scan() {
        if (!holdingSiteSiege() && addedToChunk) assignment = null;
        nextScan = ticksExisted + SCAN_INTERVAL;
        scans++;
        outcome = Outcome.NO_SUPPORTED_TARGET;
        targetType = "NONE";
        int baseX = MathHelper.floor_double(posX);
        int baseY = MathHelper.floor_double(boundingBox.minY);
        int baseZ = MathHelper.floor_double(posZ);
        int bestPriority = -1;
        double bestDistance = 64.0D;
        boolean seekTransport = !hasStolenPipe && worldObj.difficultySetting >= 2 && (assignment != null || rand.nextInt(3) == 0);
        for (int x = baseX - 8; x <= baseX + 8; x++) {
            double dx = x + 0.5D - posX;
            for (int z = baseZ - 8; z <= baseZ + 8; z++) {
                double dz = z + 0.5D - posZ;
                double horizontal = dx * dx + dz * dz;
                if (horizontal > 64.0D || !worldObj.blockExists(x, baseY, z)) continue;
                for (int y = Math.max(0, baseY - 2); y <= Math.min(worldObj.getHeight() - 1, baseY + 2); y++) {
                    if (assignment != null && !assignment.contains(x, y, z)) continue;
                    scannedBlocks++;
                    double distance = horizontal + (y + 0.5D - posY) * (y + 0.5D - posY);
                    if (distance > 64.0D) continue;
                    Block block = Block.blocksList[worldObj.getBlockId(x, y, z)];
                    if (block == null || !block.hasTileEntity(worldObj.getBlockMetadata(x, y, z))) continue;
                    TileEntity candidate = LoadedTile.at(worldObj, x, y, z);
                    TileEntity pipe = null;
                    SabotageTarget effect;
                    TransportTheft theft = seekTransport ? TransportTheft.find(candidate, eyes()) : null;
                    String type;
                    int priority;
                    if (theft != null) {
                        effect = null;
                        type = theft.family;
                        priority = 4;
                    } else if (candidate instanceof TileHopper) {
                        TileHopper hopper = (TileHopper) candidate;
                        if (assignment != null && !hopper.hasStock() && hopper.activeExtractionPeer() == null) continue;
                        effect = hopper;
                        pipe = hopper.activeExtractionPeer();
                        type = pipe == null ? "HOPPER" : hopper.activeIndustrialOperation();
                        priority = pipe == null ? 0 : 2;
                    } else if (candidate instanceof TileEntityDistillationReceiver) {
                        TileEntityDistillationReceiver receiver = (TileEntityDistillationReceiver) candidate;
                        if (receiver.isDisrupted()) {
                            outcome = Outcome.ALREADY_PAUSED;
                            continue;
                        }
                        pipe = receiver.activeExtractionPipe();
                        if (pipe == null) {
                            outcome = Outcome.UNSUPPORTED_CONNECTION;
                            continue;
                        }
                        effect = receiver;
                        type = "BUILDCRAFT_RECEIVER";
                        priority = 2;
                    } else if (candidate instanceof SabotageTarget) {
                        effect = (SabotageTarget) candidate;
                        type = effect.activeIndustrialOperation();
                        if (type == null) continue;
                        priority = 2;
                    } else {
                        IndustrialSabotage direct = IndustrialSabotage.find(candidate);
                        if (direct == null) continue;
                        effect = direct;
                        type = direct.activeIndustrialOperation();
                        priority = direct.operation.priority;
                    }
                    if (priority < bestPriority || priority == bestPriority && distance >= bestDistance) continue;
                    if (effect != null && effect.isDisrupted()) {
                        outcome = Outcome.ALREADY_PAUSED;
                    } else if (theft == null && !seesMachine(x, y, z)) {
                        outcome = Outcome.NO_SIGHT;
                    } else {
                        if (assignment != null && !reachableFromEntry(x, y, z)) continue;
                        bestPriority = priority;
                        bestDistance = distance;
                        machine = candidate;
                        connection = pipe;
                        disruption = effect;
                        transportTheft = theft;
                        targetType = type;
                        machineX = x;
                        machineY = y;
                        machineZ = z;
                    }
                }
            }
        }
        if (machine == null) return;
        outcome = Outcome.NONE;
        approachEnd = ticksExisted + 100;
        double dx = posX - machineX - 0.5D;
        double dz = posZ - machineZ - 0.5D;
        nextSide = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 0 : 1) : (dz > 0 ? 2 : 3);
        getNavigator().clearPathEntity();
    }

    private boolean reachableFromEntry(int x, int y, int z) {
        // A short unobstructed interaction from the verified entry cell needs no further navigation.
        if (Math.abs(boundingBox.minY - y) <= 1.25D && getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= CONTACT_SQ)
            return true;
        for (int side = 0; side < SIDE_X.length && entryPathChecks < 4; side++) {
            int sx = x + SIDE_X[side], sz = z + SIDE_Z[side];
            entryPathChecks++;
            if (reaches(plan(sx, y, sz), sx, y, sz)) return true;
        }
        return false;
    }

    private void approachMachine() {
        Vec3D point = visibleTargetPoint();
        lookAtMachine(point);
        if (point != null && inContact(point)) {
            startWindup();
            return;
        }
        if (assignment == null && blink(machineX + 0.5D, machineZ + 0.5D, BlinkCause.APPROACH)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double x = machineX + 0.5D + SIDE_X[nextSide];
        double z = machineZ + 0.5D + SIDE_Z[nextSide];
        nextSide = (nextSide + 1) % SIDE_X.length;
        pathPlans++;
        PathEntity path = plan(x, machineY, z);
        if (reaches(path, x, machineY, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean inContact(Vec3D point) {
        double x = point == null ? machineX + 0.5D : point.xCoord;
        double y = point == null || transportTheft == null ? machineY + 0.5D : point.yCoord;
        double z = point == null ? machineZ + 0.5D : point.zCoord;
        return onGround && Math.abs(boundingBox.minY - machineY) <= 1.25D
                && getDistanceSq(x, y, z) <= CONTACT_SQ;
    }

    private boolean sameMachine() {
        if (machine == null || machine.xCoord != machineX || machine.yCoord != machineY || machine.zCoord != machineZ)
            return false;
        if (LoadedTile.at(worldObj, machineX, machineY, machineZ) != machine) return false;
        if (disruption instanceof IndustrialSabotage) return ((IndustrialSabotage) disruption).stillBound();
        if (connection == null) return true;
        if (machine instanceof TileHopper) return ((TileHopper) machine).activeExtractionPeer() == connection;
        return ((TileEntityDistillationReceiver) machine).activeExtractionPipe() == connection;
    }

    private Outcome invalidTarget() {
        if (transportTheft == null) return sameMachine() ? Outcome.NONE : Outcome.TARGET_CHANGED;
        if (worldObj.difficultySetting < 2) return Outcome.UNSAFE_TRANSPORT;
        switch (transportTheft.state()) {
            case READY: return Outcome.NONE;
            case UNLOADED: return Outcome.UNLOADED;
            case TARGET_CHANGED: return Outcome.TARGET_CHANGED;
            default: return Outcome.UNSAFE_TRANSPORT;
        }
    }

    private Vec3D visibleTargetPoint() {
        if (transportTheft != null) return transportTheft.visiblePoint(eyes());
        return seesMachine(machineX, machineY, machineZ)
                ? Vec3D.createVector(machineX + 0.5D, machineY + 0.65D, machineZ + 0.5D) : null;
    }

    private boolean seesMachine(int x, int y, int z) {
        if (!worldObj.checkChunksExist(MathHelper.floor_double(Math.min(posX, x)) - 1, 0,
                MathHelper.floor_double(Math.min(posZ, z)) - 1,
                MathHelper.floor_double(Math.max(posX, x)) + 1, 0,
                MathHelper.floor_double(Math.max(posZ, z)) + 1)) return false;
        MovingObjectPosition hit = worldObj.rayTraceBlocks(eyes(), Vec3D.createVector(x + 0.5D, y + 0.65D, z + 0.5D));
        return hit == null || hit.blockX == x && hit.blockY == y && hit.blockZ == z;
    }

    private void lookAtMachine(Vec3D point) {
        getLookHelper().setLookPosition(point == null ? machineX + 0.5D : point.xCoord,
                point == null ? machineY + 0.65D : point.yCoord, point == null ? machineZ + 0.5D : point.zCoord,
                30.0F, 30.0F);
    }

    private void startWindup() {
        pose(machine == null ? DECOY : transportTheft != null ? DISMANTLE : WINDUP);
        windupEnd = ticksExisted + (transportTheft == null ? WINDUP_TICKS : DISMANTLE_TICKS);
        windups++;
        getNavigator().clearPathEntity();
        motionX = motionZ = 0;
        link();
    }

    private boolean windingUp() { return pose() == WINDUP || pose() == DECOY || pose() == DISMANTLE; }

    private void continueWindup(boolean visible) {
        if (machine == null) {
            if (!visible) finish(Outcome.NO_SIGHT, true);
            else if (ticksExisted >= windupEnd) {
                diversions++;
                finish(Outcome.DECOY, false);
            } else if ((windupEnd - ticksExisted) % 5 == 0) link();
            return;
        }

        Outcome invalid = invalidTarget();
        if (invalid != Outcome.NONE) {
            finish(invalid, true);
            return;
        }
        Vec3D point = visibleTargetPoint();
        if (!inContact(point)) finish(Outcome.OUT_OF_REACH, true);
        else if (point == null) finish(Outcome.NO_SIGHT, true);
        else if (disruption != null && disruption.isDisrupted()) finish(Outcome.ALREADY_PAUSED, true);
        else {
            lookAtMachine(point);
            if (ticksExisted >= windupEnd) {
                if (transportTheft != null) {
                    ItemStack removed = transportTheft.remove(eyes());
                    if (removed != null) carry(removed);
                    else finish(Outcome.REMOVAL_FAILED, true);
                } else if (disruption.beginDisruption(this)) {
                    disruptions++;
                    if (connection == null) burst(machineX + 0.5D, machineY + 0.85D, machineZ + 0.5D, "portal");
                    finish(Outcome.APPLIED, false);
                } else finish(Outcome.TARGET_CHANGED, true);
            } else if ((windupEnd - ticksExisted) % 5 == 0) link();
        }
    }

    private void carry(ItemStack component) {
        stolenComponent = component;
        componentItemClass = component.getItem().getClass().getName();
        cargoFamily = targetType;
        recovery = Recovery.CARRYING;
        hasStolenPipe = true;
        theftX = machineX;
        theftY = machineY;
        theftZ = machineZ;
        escapeAngle = Math.atan2(posZ - target.posZ, posX - target.posX);
        escapeUntil = ticksExisted + ESCAPE_TICKS;
        nextRoute = ticksExisted;
        nextBlink = ticksExisted + 6;
        retryBlink = ticksExisted;
        outcome = Outcome.COMPONENT_STOLEN;
        pose(CARRY);
        burst(theftX + 0.5D, theftY + 0.55D, theftZ + 0.5D, "portal");
        worldObj.playSoundAtEntity(this, "step.stone", 0.8F, 0.65F);
        disengage();
    }

    @Override
    protected boolean independentTactics() {
        if (!hasCargo()) return false;
        pose(CARRY);
        if (ticksExisted >= escapeUntil && ticksExisted % 10 == 0 && releaseComponent(Recovery.ESCAPE)) {
            nextAction = ticksExisted + ACTION_COOLDOWN;
            recoverEnd = ticksExisted + RECOVERY_TICKS;
            getNavigator().clearPathEntity();
            pose(RECOVER);
            return true;
        }
        EntityPlayer pursuer = worldObj.getClosestVulnerablePlayerToEntity(this, 28.0D);
        if (pursuer != null) {
            escapeAngle = Math.atan2(posZ - pursuer.posZ, posX - pursuer.posX);
            if (getDistanceSqToEntity(pursuer) < 100 && blink(pursuer.posX, pursuer.posZ, BlinkCause.CARGO))
                return true;
        }
        if (onGround && ticksExisted >= nextRoute) {
            nextRoute = ticksExisted + 14;
            double side = ((ticksExisted / 14 + entityId) & 1) == 0 ? 0.45D : -0.45D;
            for (double offset : new double[] {side, -side, side * 2.6D}) {
                double heading = escapeAngle + offset;
                double x = posX + Math.cos(heading) * 7, z = posZ + Math.sin(heading) * 7;
                int y = MathHelper.floor_double(boundingBox.minY + 0.01D);
                for (int step : new int[] {0, 1, -1}) {
                    PathEntity path = plan(x, y + step, z);
                    if (reaches(path, x, y + step, z) && getNavigator().setPath(path, 0.39F)) return true;
                }
            }
        }
        return true;
    }

    private void link() {
        double x = posX;
        double y = boundingBox.minY + 0.7D;
        double z = posZ;
        Vec3D part = transportTheft == null ? null : transportTheft.visiblePoint(eyes());
        if (part != null) worldObj.spawnParticle("crit", part.xCoord, part.yCoord, part.zCoord, 0, 0.12D, 0);
        double endX = part == null ? machineX + 0.5D : part.xCoord;
        double endY = part == null ? machineY + 0.7D : part.yCoord;
        double endZ = part == null ? machineZ + 0.5D : part.zCoord;
        double red = part != null ? 1.0D : machine == null ? 0.44D : 0.82D;
        for (int i = 1; i <= 3; i++) {
            double t = i / 3.0D;
            worldObj.spawnParticle("reddust",
                    machine == null ? x : x + (endX - x) * t,
                    machine == null ? y + t * 0.3D : y + (endY - y) * t,
                    machine == null ? z : z + (endZ - z) * t,
                    red,
                    machine == null ? 0.12D : 0.42D, part != null ? 0.15D : 0.9D);
        }
    }

    private void finish(Outcome result, boolean interrupted) {
        clearTarget();
        outcome = result;
        nextAction = ticksExisted + ACTION_COOLDOWN;
        recoverEnd = ticksExisted + RECOVERY_TICKS;
        getNavigator().clearPathEntity();
        pose(interrupted ? CANCELLED : RECOVER);
        if (interrupted) cancelled++;
        if (interrupted || result == Outcome.DECOY) burst(posX, boundingBox.minY + 0.7D, posZ, "smoke");
    }

    private void clearTarget() {
        machine = connection = null;
        disruption = null;
        transportTheft = null;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        boolean dismantling = pose() == DISMANTLE;
        if (hit && damage > 0 && !worldObj.isRemote && dismantling) finish(Outcome.HIT, true);
        if (hit && damage > 0 && !worldObj.isRemote && source.getEntity() instanceof EntityPlayer) {
            boolean interrupted = dismantling || windingUp();
            if (!dismantling && interrupted) finish(Outcome.HIT, true);
            if (isEntityAlive() && (interrupted || damage >= 2 && rand.nextInt(3) != 0)) {
                pendingEvade = interrupted ? BlinkCause.INTERRUPTED : BlinkCause.HIT;
                hitEvadeEnd = ticksExisted + 22;
            }
        }
        return hit;
    }

    /** The lease expires if either world object stops owning or ticking this encounter. */
    public boolean canMaintainDisruption(TileEntity leasedTarget) {
        if (leasedTarget == null || leasedTarget.worldObj != worldObj || worldObj.isRemote
                || worldObj.difficultySetting == 0 || !addedToChunk || !isEntityAlive()) return false;
        if (holdingSiteSiege()) {
            int x = leasedTarget.xCoord, y = leasedTarget.yCoord, z = leasedTarget.zCoord;
            if (getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > CONTACT_SQ
                    || LoadedTile.at(worldObj, x, y, z) != leasedTarget) return false;
        } else if (target == null || target.worldObj != worldObj || !target.isEntityAlive()
                || target.capabilities.disableDamage || getDistanceSqToEntity(target) > 256) return false;
        long age = worldObj.getWorldTime() - heartbeat;
        return age >= 0 && age <= 2 && worldObj.getChunkProvider().chunkExists(chunkCoordX, chunkCoordZ);
    }

    public boolean canStartDisruption(TileEntity tile) {
        return pose() == WINDUP && machine == tile && ticksExisted >= windupEnd;
    }

    private boolean defend(double distanceSq, boolean visible) {
        if (!visible || distanceSq > 1.69D || target.boundingBox.minY >= boundingBox.maxY
                || target.boundingBox.maxY <= boundingBox.minY) return false;
        getNavigator().clearPathEntity();
        stalledRoutes = 0;
        if (ticksExisted >= nextMelee) {
            nextMelee = ticksExisted + 34;
            swing();
            attackEntityAsMob(target);
        }
        return true;
    }

    private void pursue(double x, double y, double z, boolean allowBlink) {
        if (allowBlink && blink(x, z, BlinkCause.APPROACH)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double dx = posX - progressX;
        double dz = posZ - progressZ;
        progressX = posX;
        progressZ = posZ;
        stalledRoutes = dx * dx + dz * dz < 0.16D ? stalledRoutes + 1 : 0;
        if (stalledRoutes >= 5) {
            disengage();
            return;
        }
        pathPlans++;
        PathEntity path = plan(x, y, z);
        if (reaches(path, x, y, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean blink(double x, double z, BlinkCause cause) {
        double dx = x - posX;
        double dz = z - posZ;
        double distanceSq = dx * dx + dz * dz;
        boolean evasive = cause != BlinkCause.APPROACH;
        if (ticksExisted < nextBlink || ticksExisted < retryBlink || !onGround
                || !evasive && (hurtTime > 0 || distanceSq < 12.25D || distanceSq > 64))
            return false;
        blinkCause = cause;
        blinkAttempts++;
        if (evasive) evasiveAttempts++;
        double direction = Math.atan2(dz, dx) + (evasive ? Math.PI : 0);
        double[] angles = evasive ? ESCAPE_ANGLES : BLINK_ANGLES;
        int flank = (entityId & 1) == 0 ? 1 : -1;
        for (int i = 0; i < angles.length; i++) {
            double angle = direction + flank * angles[i];
            double range = !evasive ? 2.75D : i == angles.length - 1 ? 2.125D : 3.25D;
            double shiftX = Math.cos(angle) * range;
            double shiftZ = Math.sin(angle) * range;
            double separation = (dx - shiftX) * (dx - shiftX) + (dz - shiftZ) * (dz - shiftZ);
            if (evasive && (separation < 9 || separation <= distanceSq)) continue;
            blinkChecks++;
            blinkResult = SaboteurBlink.check(this, shiftX, shiftZ);
            if (blinkResult != SaboteurBlink.Result.CLEAR) {
                rejectedDestinations++;
                if (blinkResult == SaboteurBlink.Result.NOT_GROUNDED) break;
                continue;
            }
            burst(posX, posY + 0.45D, posZ, "portal");
            setPosition(posX + shiftX, posY, posZ + shiftZ);
            prevPosX = lastTickPosX = posX;
            prevPosZ = lastTickPosZ = posZ;
            motionX = motionZ = 0;
            getNavigator().clearPathEntity();
            burst(posX, posY + 0.45D, posZ, "portal");
            blinks++;
            if (evasive) evasiveBlinks++;
            nextBlink = ticksExisted + (evasive ? EVASIVE_BLINK_COOLDOWN : APPROACH_BLINK_COOLDOWN);
            nextRoute = ticksExisted + (evasive ? 10 : 2);
            stalledRoutes = 0;
            return true;
        }
        failedBlinks++;
        retryBlink = ticksExisted + BLINK_RETRY;
        return false;
    }

    private void burst(double x, double y, double z, String particle) {
        for (int i = 0; i < 4; i++) {
            worldObj.spawnParticle(particle, x, y, z,
                    (rand.nextFloat() - 0.5F) * 0.6D, rand.nextFloat() * 0.3D,
                    (rand.nextFloat() - 0.5F) * 0.6D);
        }
    }

    @Override
    protected void resetTactics() {
        stalledRoutes = 0;
        closeTicks = hitEvadeEnd = evadeEnd = 0;
        clearTarget();
        if (windingUp()) finish(Outcome.PLAYER_LOST, true);
        else if (!hasCargo() && ticksExisted >= recoverEnd) pose(IDLE);
    }

    @Override
    protected void updateAttackPose() {
        if (!hasCargo() && target == null && ticksExisted >= recoverEnd && pose() != IDLE) pose(IDLE);
    }

    private boolean hasCargo() { return stolenComponent != null || unresolvedComponent != null; }

    @Override protected boolean canDespawn() { return super.canDespawn() && !hasCargo(); }

    @Override
    public void onDeath(DamageSource source) {
        if (dead) return;
        super.onDeath(source);
        if (dead) releaseComponent(Recovery.DEATH);
    }

    @Override
    public void setDead() {
        releaseComponent(Recovery.DEATH);
        if (!hasCargo() || worldObj.isRemote) super.setDead();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (getHealth() <= 0 && hasCargo() && ticksExisted % 20 == 0) setDead();
    }

    private boolean releaseComponent(Recovery cause) {
        if (!hasCargo() || worldObj.isRemote) return false;
        boolean dropped = stolenComponent != null && dropComponent(MathHelper.floor_double(posX),
                MathHelper.floor_double(boundingBox.minY + 0.01D), MathHelper.floor_double(posZ));
        recovery = dropped ? cause : Recovery.WAITING_FOR_RELEASE;
        return dropped;
    }

    private boolean dropComponent(int bx, int by, int bz) {
        for (int dy : new int[] {0, 1, -1, 2, -2}) {
            for (int x = bx - 1; x <= bx + 1; x++) {
                for (int z = bz - 1; z <= bz + 1; z++) {
                    int y = by + dy;
                    if (y < 1 || y + 1 >= worldObj.getHeight() || !worldObj.blockExists(x, y, z)
                            || worldObj.getBlockId(x, y, z) != 0 || worldObj.getBlockId(x, y + 1, z) != 0
                            || !worldObj.isBlockNormalCube(x, y - 1, z)
                            || !worldObj.checkChunksExist(Math.min(bx, x), 0, Math.min(bz, z),
                                    Math.max(bx, x), 0, Math.max(bz, z))
                            || worldObj.rayTraceBlocks(eyes(), Vec3D.createVector(x + 0.5D, y + 0.1D, z + 0.5D)) != null)
                        continue;
                    EntityItem drop = new EntityItem(worldObj, x + 0.5D, y + 0.1D, z + 0.5D, stolenComponent);
                    drop.motionX = drop.motionY = drop.motionZ = 0;
                    drop.delayBeforeCanPickup = 10;
                    if (!worldObj.spawnEntityInWorld(drop)) continue;
                    stolenComponent = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (hasStolenPipe) tag.setBoolean("ZtoPipeStolen", true);
        if (hasCargo()) {
            tag.setCompoundTag("ZtoPipeCargo", stolenComponent == null ? unresolvedComponent
                    : stolenComponent.writeToNBT(new NBTTagCompound()));
            tag.setString("ZtoCargoItemClass", componentItemClass);
            tag.setInteger("ZtoPipeX", theftX);
            tag.setInteger("ZtoPipeY", theftY);
            tag.setInteger("ZtoPipeZ", theftZ);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        stolenComponent = null;
        unresolvedComponent = null;
        hasStolenPipe = tag.getBoolean("ZtoPipeStolen");
        if (tag.hasKey("ZtoPipeCargo")) {
            NBTTagCompound cargo = tag.getCompoundTag("ZtoPipeCargo");
            int id = cargo.getShort("id");
            if (id > 0 && id < Item.itemsList.length && cargo.getByte("Count") == 1) {
                componentItemClass = tag.hasKey("ZtoCargoItemClass")
                        ? tag.getString("ZtoCargoItemClass") : "buildcraft.transport.ItemPipe";
                if (Item.itemsList[id] != null && componentItemClass.equals(Item.itemsList[id].getClass().getName()))
                    stolenComponent = ItemStack.loadItemStackFromNBT(cargo);
                if (stolenComponent == null) unresolvedComponent = cargo;
                hasStolenPipe = true;
                cargoFamily = "SAVED_COMPONENT";
                recovery = Recovery.CARRYING;
                escapeUntil = ticksExisted + ESCAPE_TICKS;
            }
            theftX = tag.getInteger("ZtoPipeX");
            theftY = tag.getInteger("ZtoPipeY");
            theftZ = tag.getInteger("ZtoPipeZ");
        }
    }
}
