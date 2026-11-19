package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityFlying;
import net.minecraft.src.EntityChicken;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.Material;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.Shoreline;
import zed.tekkitoverhaul.survival.weather.JetpackWeather;

/** A single predator with server-owned steering, commitment and contact. */
public class EntityAerialPredator extends EntityFlying implements IMob {
    public enum Flight {
        CRUISE, GLIDE, CIRCLE, PREPARE, DIVE, RECOVER, DISENGAGE, FALL
    }

    public static final String TEXTURE = "/zed/textures/aerial_predator.png";
    private static final Flight[] FLIGHTS = Flight.values();
    private static final int FLIGHT_WATCHER = 20;
    private static final int PREPARE_TICKS = 18;
    private static final int RECOVER_TICKS = 50;
    private static final int DISENGAGE_TICKS = 120;
    private static final double AIRBORNE_HEIGHT = 8;
    private static final int[] AVOID_ANGLES = {0, -10, 10, -20, 20};
    private static final int[] ALTERNATE_LANES = {60, -60, 115, -115, 180};
    private static final int MAX_RESTAGES = 4;

    private Flight flight = Flight.CRUISE;
    private AerialEncounter encounter;
    private int lane, wildTicks;
    private int ambientDelay = 80 + rand.nextInt(61);
    private boolean natural, approaching;
    private double arrivalX, arrivalY, arrivalZ;
    private EntityLiving target;
    private static boolean chickenHunting = true;
    private boolean preyOpportunitySpent;
    private int stateTicks, huntTicks, attempts, attemptLimit, scanDelay = 30, blockedTicks, hiddenTicks;
    private int cruiseTicks, rejectedWarnings, commitments, restageUntil;
    private int restages, nextLaneSearch;
    private boolean needsLane;
    private LaunchStage launchStage;
    private String diveStatus = "No target", lastDiveRejection = "none";
    private boolean aerialHunt;
    private float cruiseYaw;
    private double destinationX, destinationY, destinationZ;
    private double diveX, diveY, diveZ, diveRemaining, diveSpeed;
    private boolean contacted;
    private String lastFailure = "none", lastExit = "none";

    public EntityAerialPredator(World world) {
        super(world);
        setSize(0.95F, 0.62F);
        texture = TEXTURE;
        experienceValue = 4;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(FLIGHT_WATCHER, Byte.valueOf((byte) Flight.CRUISE.ordinal()));
    }

    @Override
    public int getMaxHealth() { return 16; }

    public String speciesName() { return "Fugl"; }
    boolean slashingPass() { return false; }
    private int prepareTicks() { return slashingPass() ? 24 : PREPARE_TICKS; }
    private int recoverTicks() { return slashingPass() ? 62 : RECOVER_TICKS; }
    private String sound(String cue) { return slashingPass() ? "zto.skelebird." + cue : "zto.aerial." + cue; }

    @Override
    protected String getHurtSound() { return sound("hurt"); }

    @Override
    protected String getDeathSound() { return sound("death"); }

    @Override
    protected float getSoundVolume() { return 0.55F; }

    @Override
    public boolean getCanSpawnHere() {
        return worldObj.worldProvider.worldType == 0 && worldObj.difficultySetting > 0
                && AerialTerrain.clear(this, boundingBox)
                && worldObj.checkIfAABBIsClear(boundingBox);
    }

    public Flight flight() {
        return getHealth() <= 0 ? Flight.FALL : FLIGHTS[dataWatcher.getWatchableObjectByte(FLIGHT_WATCHER)];
    }

    void joinEncounter(AerialEncounter group, int slot) {
        encounter = group;
        lane = slot;
        natural = approaching = true;
        wildTicks = 1000;
        scanDelay = group.ordinary && group.hunting ? 30 + slot * 7 : 65 + slot * 8;
        // Set a single arrival waypoint ahead of a traveling pilot before the first target scan.
        double leadX = group.airborne ? clamp(group.player.motionX * 48, -10, 10) : 0;
        double leadZ = group.airborne ? clamp(group.player.motionZ * 48, -10, 10) : 0;
        arrivalX = group.approachX + (posX - group.approachX) * 0.24 + leadX;
        arrivalY = centerY() + 1;
        arrivalZ = group.approachZ + (posZ - group.approachZ) * 0.24 + leadZ;
    }

    boolean belongsTo(AerialEncounter group) { return encounter == group; }

    void leaveEncounter(String reason) {
        if (encounter == null) return;
        if (isEntityAlive()) {
            // Passive members also move away from the player when the encounter ends.
            if (target == null && flight != Flight.DISENGAGE) target = encounter.player;
            if (flight != Flight.DISENGAGE) disengage(reason);
        }
        clearEncounter();
    }

    private void clearEncounter() {
        if (encounter == null) return;
        encounter.releaseDive(this);
        encounter = null;
        approaching = false;
        wildTicks = Math.min(wildTicks, 240);
    }

    @Override
    protected void updateEntityActionState() {
        // A nearby submerged bird stays present for native drowning, including after leaving its encounter.
        if (worldObj.difficultySetting == 0 || (natural && !isInWater() && --wildTicks <= 0)) {
            setDead();
            return;
        }
        ++entityAge;
        despawnEntity();
        if (isDead) return;
        if (isInWater()) {
            if (target != null || encounter != null) disengage("entered water");
            return;
        }
        ++stateTicks;

        if (target != null) {
            ++huntTicks;
            String exit = invalidTarget();
            if (exit != null) disengage(exit);
        }

        switch (flight) {
            case CRUISE:
            case GLIDE:
                if (--scanDelay <= 0) {
                    scanDelay = 35 + rand.nextInt(21);
                    detect();
                }
                if (target == null) {
                    cruise();
                    if (--ambientDelay <= 0) {
                        worldObj.playSoundAtEntity(this, sound("ambient"), 0.48F, 0.94F + rand.nextFloat() * 0.12F);
                        ambientDelay = 280 + rand.nextInt(141);
                    }
                } else circle();
                break;
            case CIRCLE:
                circle();
                break;
            case PREPARE:
                prepare();
                break;
            case DIVE:
                advanceDive();
                break;
            case RECOVER:
                climb();
                if (stateTicks >= recoverTicks()) {
                    if (attempts >= attemptLimit || rejectedWarnings >= (ordinaryHunt() ? 3 : 2))
                        disengage(attempts >= attemptLimit ? "attack budget" : "repeated blocked launch");
                    else enter(Flight.CIRCLE);
                }
                break;
            case DISENGAGE:
                steer(destinationX, destinationY, destinationZ, 0.30);
                if (stateTicks >= DISENGAGE_TICKS) {
                    cruiseTicks = 0;
                    scanDelay = 60;
                    enter(Flight.CRUISE);
                }
                break;
            default:
                break;
        }
    }

    public static void configureChickenHunting(boolean enabled) { chickenHunting = enabled; }

    private void detect() {
        if (slashingPass() && !EntitySkelebird.isNight(worldObj)) return;
        if (natural) {
            if (encounter == null) return;
            if (!encounter.hunting) {
                detectPrey();
                return;
            }
            EntityPlayer player = encounter.player;
            int range = encounter.ordinary ? 39 : 36;
            if (eligible(player) && getDistanceSqToEntity(player) <= range * range && targetClearance(player) >= 0
                    && (encounter.ordinary || visible(player))) {
                startHunt(player, encounter.airborne, 1);
                if (encounter.ordinary && !visible(player)) needsLane = true;
            }
            return;
        }
        if (worldObj.worldProvider.worldType != 0) return;
        EntityPlayer best = null;
        boolean bestAirborne = false;
        double bestDistance = 36 * 36;
        for (Object entry : worldObj.playerEntities) {
            EntityPlayer player = (EntityPlayer) entry;
            double distance = getDistanceSqToEntity(player);
            if (distance > 36 * 36 || !eligible(player)) continue;
            double altitude = targetClearance(player);
            if (altitude < 0 || Math.abs(centerY() - targetCenter(player)) > 30) continue;
            boolean airborne = !player.onGround && altitude >= AIRBORNE_HEIGHT;
            if (slashingPass() && !airborne && worldObj.difficultySetting < 3) continue;
            if (!airborne && (distance > 24 * 24 || bestAirborne)) continue;
            if (airborne == bestAirborne && distance >= bestDistance) continue;
            if (!visible(player)) continue;
            best = player;
            bestAirborne = airborne;
            bestDistance = distance;
        }
        int groundRarity = slashingPass() ? 24 : worldObj.isDaytime() ? 42 : 9;
        if (best == null || (!bestAirborne && rand.nextInt(groundRarity) != 0)) return;
        startHunt(best, bestAirborne, bestAirborne ? 2 : 1);
    }

    private void detectPrey() {
        if (!chickenHunting || slashingPass() || preyOpportunitySpent || approaching || encounter.airborne
                || encounter.size() != 1 || !worldObj.isDaytime() || worldObj.isRaining()
                || rand.nextInt(8) != 0) return;
        EntityChicken nearest = null;
        double distance = 22 * 22;
        for (Object entry : worldObj.getEntitiesWithinAABB(EntityChicken.class, boundingBox.expand(18, 18, 18))) {
            EntityChicken chicken = (EntityChicken) entry;
            double range = getDistanceSqToEntity(chicken);
            if (chicken.isEntityAlive() && range < distance && targetClearance(chicken) >= 0 && visible(chicken)) {
                distance = range;
                nearest = chicken;
            }
        }
        if (nearest != null) {
            preyOpportunitySpent = true;
            startHunt(nearest, false, 1);
        }
    }

    private void startHunt(EntityLiving subject, boolean airborne, int allowedAttempts) {
        target = subject;
        aerialHunt = airborne;
        approaching = false;
        attempts = huntTicks = hiddenTicks = rejectedWarnings = restageUntil = 0;
        restages = nextLaneSearch = 0;
        needsLane = false;
        launchStage = null;
        lastDiveRejection = "none";
        attemptLimit = allowedAttempts;
        enter(Flight.CIRCLE);
    }

    private boolean eligible(EntityPlayer player) {
        return player.worldObj == worldObj && !player.isDead && player.getHealth() > 0
                && !player.capabilities.disableDamage && !player.isPlayerSleeping();
    }

    private String invalidTarget() {
        if (target instanceof EntityPlayer ? !eligible((EntityPlayer) target)
                : !target.isEntityAlive()) return "target unavailable";
        if (huntTicks > (target instanceof EntityChicken ? 200 : encounter == null ? 400 : AerialEncounter.HUNT_LIMIT)
                || getDistanceSqToEntity(target) > 48 * 48) return "pursuit limit";
        // Native block collision protects all intervening terrain. A roof also ends pursuit.
        double altitude = targetClearance(target);
        if (altitude < 0) return "target under cover or unloaded";
        if (aerialHunt && altitude < 3 && flight != Flight.DIVE) return "target descended";
        if (huntTicks % 5 == 0) hiddenTicks = visible(target) ? 0 : hiddenTicks + 5;
        if (hiddenTicks >= (ordinaryHunt() ? 145 : 30) && flight != Flight.DIVE) return "target out of sight";
        return null;
    }

    private boolean ordinaryHunt() {
        return encounter != null && encounter.ordinary && target instanceof EntityPlayer;
    }

    private double targetClearance(EntityLiving subject) {
        double height = AerialTerrain.skyHeight(subject);
        if (height >= 0 && (subject.isInWater() || subject.isInsideOfMaterial(Material.water))) return -1;
        if ((slashingPass() || subject instanceof EntityChicken) && height >= 0 && worldObj.getPrecipitationHeight(
                MathHelper.floor_double(subject.posX), MathHelper.floor_double(subject.posZ))
                > subject.boundingBox.minY + subject.height * 0.5) return -1;
        return height;
    }

    private boolean visible(EntityLiving subject) {
        return AerialTerrain.loaded(worldObj, posX, posZ, subject.posX, subject.posZ) && canEntityBeSeen(subject);
    }

    private boolean pilotProtected() {
        return target instanceof EntityPlayer && JetpackWeather.LOCAL.protects((EntityPlayer) target);
    }

    private void cruise() {
        if (approaching) {
            approaching = horizontalRange(arrivalX, arrivalZ) >= 3;
            if (approaching) {
                steer(arrivalX, arrivalY, arrivalZ, 0.31);
                return;
            }
        }
        if (--cruiseTicks <= 0 || horizontalRange(destinationX, destinationZ) < 3) {
            cruiseTicks = 55 + rand.nextInt(41);
            cruiseYaw = rotationYaw + 20 + rand.nextInt(56);
            float angle = cruiseYaw * (float) Math.PI / 180;
            destinationX = posX - MathHelper.sin(angle) * 18;
            destinationZ = posZ + MathHelper.cos(angle) * 18;
            double desiredY = centerY() + 4;
            if (AerialTerrain.loaded(worldObj, destinationX, destinationZ, destinationX, destinationZ)) {
                int surface = worldObj.getHeightValue(MathHelper.floor_double(destinationX),
                        MathHelper.floor_double(destinationZ));
                desiredY = surface + 12 + rand.nextInt(7);
            }
            if (encounter != null && encounter.airborne) desiredY = Math.max(desiredY, arrivalY - 2);
            destinationY = limitAltitude(desiredY);
        }
        Flight pose = cruiseTicks < 29 && Math.abs(destinationY - centerY()) < 3 ? Flight.GLIDE : Flight.CRUISE;
        if (flight != pose) enter(pose);
        steer(destinationX, destinationY, destinationZ, pose == Flight.GLIDE ? 0.27 : 0.32);
    }

    private void circle() {
        if (stateTicks >= (encounter == null ? 200 : 490)) {
            disengage("no dive approach");
            return;
        }
        boolean ordinary = ordinaryHunt();
        boolean turnAvailable = !pilotProtected() && (encounter == null || encounter.diveAvailable());
        if (ordinary && (turnAvailable || hiddenTicks > 0)
                && (needsLane || hiddenTicks > 0 || launchStage != null) && followAlternateLane()) return;
        if (hiddenTicks > 0 && !ordinary) {
            diveStatus = "Target obscured";
            steer(destinationX, destinationY, destinationZ, 0.26);
            return;
        }
        int ring = encounter == null ? 0 : lane % 3;
        boolean pass = slashingPass();
        double altitude = limitAltitude(targetCenter(target) + (pass ? 5.5 + ring : 8 + ring * 1.5));
        double offset = Math.atan2(posZ - target.posZ, posX - target.posX);
        double range = horizontalRange(target.posX, target.posZ);
        double minimumRange = pass ? (ordinary ? 13 : 11) : 6;
        if (stateTicks < (pass ? 36 : 26) || !turnAvailable || stateTicks < restageUntil || range < minimumRange) {
            double direction = pass && lane % 2 != 0 ? -1 : 1;
            double angle = offset + direction * (range < minimumRange ? 0.22 : 0.82);
            double radius = (pass ? 17 : 10) + (encounter == null ? 0 : lane * 1.1);
            diveStatus = turnAvailable ? "Repositioning"
                    : pilotProtected() ? "Pilot recovering from jetpack fault" : "Waiting for flock turn";
            steer(target.posX + Math.cos(angle) * radius, altitude, target.posZ + Math.sin(angle) * radius, 0.37);
            return;
        }

        double endX = leadX(), endZ = leadZ();
        double height = centerY() - targetCenter(target);
        double maximumRange = pass ? 21 : 15;
        if (range > maximumRange || height < (pass ? 3.5 : 4) || height > (pass ? 8.5 : 14)) {
            // Close the range from above without running straight into the pilot.
            double radius = pass ? 16 : range > 15 ? 8 : Math.max(7, Math.min(13, range));
            diveStatus = "Seeking launch distance and height";
            steer(endX + Math.cos(offset) * radius, altitude, endZ + Math.sin(offset) * radius, 0.39);
            return;
        }

        aim(endX, altitude, endZ);
        int windupReady = ordinary ? (pass ? 44 : 32) : (pass ? 52 : 42);
        if (stateTicks < windupReady || stateTicks % 4 != 0) return;
        double endY = leadY();
        String rejection = diveRejection(endX, endY, endZ);
        diveStatus = rejection == null ? "Ready" : rejection;
        if (rejection != null) {
            lastDiveRejection = rejection;
            if ("Blocked dive corridor".equals(rejection) || "Unloaded dive corridor".equals(rejection)) {
                if (ordinary) needsLane = true;
                else restageUntil = stateTicks + 20;
            }
            return;
        }
        if (encounter != null && !encounter.reserveDive(this)) return;
        if (pass) {
            destinationX = endX;
            destinationY = endY;
            destinationZ = endZ;
        }
        enter(Flight.PREPARE);
        diveStatus = "Telegraphing";
        worldObj.playSoundAtEntity(this, sound("warning"), 0.76F, 1.0F);
    }

    private boolean followAlternateLane() {
        if (launchStage == null && restages < MAX_RESTAGES && huntTicks >= nextLaneSearch) {
            ++restages;
            nextLaneSearch = huntTicks + 26;
            launchStage = findAlternateLane();
        }
        if (launchStage == null) {
            if (restages >= MAX_RESTAGES) needsLane = false;
            return false; // Existing local orbit remains available until the original hunt deadline.
        }
        LaunchStage stage = launchStage;
        double targetDx = leadX() - stage.targetX, targetDz = leadZ() - stage.targetZ;
        if (huntTicks >= stage.deadline || targetDx * targetDx + targetDz * targetDz > 7 * 7) {
            launchStage = null;
            return false;
        }
        if (stage.climbing && centerY() >= stage.loft - 0.45) stage.climbing = false;
        double range = horizontalRange(stage.x, stage.z);
        if (!stage.climbing && range < 2.3 && Math.abs(centerY() - stage.y) < 1.2) {
            launchStage = null;
            needsLane = !visible(target);
            if (!needsLane) hiddenTicks = 0;
            return false;
        }
        diveStatus = "Re-staging from a different lane";
        if (stage.climbing) steer(posX, stage.loft, posZ, 0);
        else steer(stage.x, range < 3.5 ? stage.y : stage.loft, stage.z, range < 3.5 ? 0.23 : 0.40);
        return true;
    }

    private LaunchStage findAlternateLane() {
        boolean pass = slashingPass();
        double radius = (pass ? 17 : 11) + lane % 3 * 0.85;
        double x = leadX(), y = leadY(), z = leadZ();
        double altitude = limitAltitude(y + (pass ? 5.3 : 9.0));
        if (altitude - y < (pass ? 3.5 : 4)) return null;
        double bearing = Math.atan2(posZ - z, posX - x);
        int direction = lane % 2 == 0 ? 1 : -1;
        for (int offset : ALTERNATE_LANES) {
            double angle = bearing + Math.toRadians(offset * direction);
            double dx = Math.cos(angle) * radius, dz = Math.sin(angle) * radius;
            double sx = x + dx, sz = z + dz;
            double moveX = sx - posX, moveY = altitude - centerY(), moveZ = sz - posZ;
            if (moveX * moveX + moveZ * moveZ > 39 * 39) continue;
            AxisAlignedBB stage = boundingBox.getOffsetBoundingBox(moveX, moveY, moveZ);
            if (!clearDivePath(stage, -dx, y - altitude, -dz)) continue;
            if (clearPath(boundingBox, moveX, moveY, moveZ)) {
                return new LaunchStage(sx, altitude, sz, x, z, altitude, false, huntTicks + 110);
            }
            double loft = limitAltitude(Math.max(altitude, centerY()) + 6);
            double up = loft - centerY();
            if (clearPath(boundingBox, 0, up, 0)
                    && clearPath(boundingBox.getOffsetBoundingBox(0, up, 0), moveX, 0, moveZ)
                    && clearPath(stage.getOffsetBoundingBox(0, loft - altitude, 0), 0, altitude - loft, 0)) {
                return new LaunchStage(sx, altitude, sz, x, z, loft, true, huntTicks + 135);
            }
        }
        return null;
    }

    private static final class LaunchStage {
        final double x, y, z, targetX, targetZ, loft;
        final int deadline;
        boolean climbing;

        LaunchStage(double x, double y, double z, double targetX, double targetZ, double loft,
                    boolean climbing, int deadline) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.loft = loft;
            this.climbing = climbing;
            this.deadline = deadline;
        }
    }

    private void aim(double x, double y, double z) {
        destinationX = x;
        destinationY = y;
        destinationZ = z;
        float bearing = yaw(x - posX, z - posZ);
        rotationYaw += (float) clamp(wrap(bearing - rotationYaw), -10, 10);
        double dy = clamp((y - centerY()) * 0.10, -0.14, 0.19);
        double radians = Math.toRadians(rotationYaw);
        double vx = -Math.sin(radians) * 0.085, vz = Math.cos(radians) * 0.085;
        if (clearance(vx, dy, vz, 5)) blend(vx, dy, vz);
        else blend(0, 0, 0);
    }

    private double leadX() { return target.posX + clamp(target.motionX * 10, -3, 3); }
    private double leadZ() { return target.posZ + clamp(target.motionZ * 10, -3, 3); }
    private double leadY() {
        return targetCenter(target) + (aerialHunt ? clamp(target.motionY * 8, -2, 2) : 0);
    }

    private String diveRejection(double x, double y, double z) {
        double dx = x - posX, dy = y - centerY(), dz = z - posZ;
        double range = horizontalRange(x, z);
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dy > -2.5 || range < 3 || length > 23) return "Outside launch geometry";
        if (slashingPass() && (range < 9 || -dy > 0.50 * range)) return "Outside slashing angle";
        if (Math.abs(wrap(yaw(dx, dz) - rotationYaw)) > 25) return "Aligning heading";
        if (!AerialTerrain.loaded(worldObj, posX, posZ, x, z)) return "Unloaded dive corridor";
        if (!visible(target) || !unobstructed(posX, centerY(), posZ, x, y, z)) return "Blocked dive corridor";

        return clearDivePath(boundingBox, dx, dy, dz) ? null : "Blocked dive corridor";
    }

    private boolean clearDivePath(AxisAlignedBB origin, double dx, double dy, double dz) {
        if (slashingPass()) {
            double scale = 1 + passRunout() / Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx *= scale;
            dy *= scale;
            dz *= scale;
        }
        return clearPath(origin, dx, dy, dz);
    }

    private boolean clearPath(AxisAlignedBB origin, double dx, double dy, double dz) {
        // Check short swept boxes along the launch line instead of testing a single diagonal-sized box.
        int segments = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.5));
        for (int i = 0; i < segments; ++i) {
            double fraction = (double) i / segments;
            AxisAlignedBB section = origin.getOffsetBoundingBox(dx * fraction, dy * fraction, dz * fraction);
            if (!AerialTerrain.clear(this, section.addCoord(dx / segments, dy / segments, dz / segments)))
                return false;
        }
        return true;
    }

    private void prepare() {
        if (pilotProtected()) {
            lastDiveRejection = "Pilot recovering from jetpack fault";
            recover();
            return;
        }
        boolean pass = slashingPass();
        double endX = pass ? destinationX : leadX();
        double endY = pass ? destinationY : leadY();
        double endZ = pass ? destinationZ : leadZ();
        if (pass) blend(0, 0, 0);
        else if (hiddenTicks == 0) aim(endX, limitAltitude(Math.max(centerY(), targetCenter(target) + 5)), endZ);
        else blend(0, 0, 0);
        if (stateTicks < prepareTicks()) return;

        String rejection = diveRejection(endX, endY, endZ);
        if (rejection != null) {
            ++rejectedWarnings;
            lastDiveRejection = lastFailure = rejection;
            if (ordinaryHunt()) needsLane = true;
            recover();
            return;
        }
        ++attempts;
        ++commitments;
        contacted = false;
        diveStatus = "Committed";
        double dx = endX - posX, dy = endY - centerY(), dz = endZ - posZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        diveX = dx / length;
        diveY = dy / length;
        diveZ = dz / length;
        diveRemaining = length + (pass ? passRunout() : 1.4);
        diveSpeed = pass ? 0.82 : 0.40;
        destinationX = endX;
        destinationY = endY;
        destinationZ = endZ;
        enter(Flight.DIVE);
        worldObj.playSoundAtEntity(this, sound(pass ? "slash" : "dive"), 0.63F, 1.0F);
        advanceDive();
    }

    private double passRunout() { return aerialHunt ? 3.2 : 0.9; }

    private void advanceDive() {
        boolean pass = slashingPass();
        diveSpeed = Math.min(pass ? 1.22 : 1.05, diveSpeed + 0.085);
        double step = pass ? Math.min(diveSpeed, diveRemaining) : diveSpeed;
        motionX = diveX * step;
        motionY = diveY * step;
        motionZ = diveZ * step;
        faceMotion();
    }

    private void recover() {
        if (encounter != null) encounter.releaseDive(this);
        diveStatus = "Recovering";
        enter(Flight.RECOVER);
        destinationX = posX;
        destinationY = limitAltitude(centerY() + 8);
        destinationZ = posZ;
    }

    private void climb() {
        steer(posX - MathHelper.sin(rotationYaw * (float) Math.PI / 180) * 9,
                destinationY, posZ + MathHelper.cos(rotationYaw * (float) Math.PI / 180) * 9, 0.31);
    }

    private void disengage(String reason) {
        lastExit = reason;
        diveStatus = "No target";
        double dx = target == null ? 0 : posX - target.posX;
        double dz = target == null ? 0 : posZ - target.posZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        float angle = rotationYaw * (float) Math.PI / 180;
        destinationX = posX + (range < 1 ? -MathHelper.sin(angle) : dx / range) * 23;
        destinationZ = posZ + (range < 1 ? MathHelper.cos(angle) : dz / range) * 23;
        destinationY = limitAltitude(centerY() + 7);
        target = null;
        enter(Flight.DISENGAGE);
        clearEncounter();
    }

    private void steer(double x, double y, double z, double speed) {
        destinationX = x;
        destinationY = limitAltitude(y);
        destinationZ = z;
        double dy = clamp((destinationY - centerY()) * 0.095, -0.25, 0.31);
        float desired = horizontalRange(x, z) < 1 ? rotationYaw : yaw(x - posX, z - posZ);
        // Check the current reachable headings first. A climb or slow pivot is the local fallback.
        double turn = clamp(wrap(desired - rotationYaw), -10, 10);
        int previous = Integer.MIN_VALUE;
        for (int offset : AVOID_ANGLES) {
            int step = (int) Math.round(clamp(turn + offset, -10, 10));
            if (step == previous) continue;
            previous = step;
            float bearing = rotationYaw + step;
            float angle = bearing * (float) Math.PI / 180;
            double vx = -MathHelper.sin(angle) * speed;
            double vz = MathHelper.cos(angle) * speed;
            if (!clearance(vx, dy, vz, 7)) continue;
            rotationYaw = bearing;
            blend(vx, dy, vz);
            return;
        }
        if (clearance(0, 0.24, 0, 5)) {
            lastFailure = "obstacle: climbing";
            rotationYaw += 9;
            blend(0, 0.24, 0);
        } else {
            lastFailure = "obstacle: turning";
            rotationYaw += 11;
            // Pivot until a reachable corridor opens. Repeated contacts request a fresh cruise waypoint.
            blend(0, clearance(0, -0.14, 0, 4) ? -0.14 : 0, 0);
            if (blockedTicks >= 6 && target == null) cruiseTicks = 0;
        }
    }

    private boolean clearance(double x, double y, double z, int ticks) {
        return AerialTerrain.clear(this, boundingBox.addCoord(x * ticks, y * ticks, z * ticks));
    }

    private void blend(double x, double y, double z) {
        motionX += (x - motionX) * 0.25;
        motionY += (y - motionY) * 0.25;
        motionZ += (z - motionZ) * 0.25;
        rotationPitch += ((float) -Math.toDegrees(Math.atan2(motionY, Math.max(0.12,
                Math.sqrt(motionX * motionX + motionZ * motionZ)))) - rotationPitch) * 0.28F;
    }

    @Override
    public boolean handleWaterMovement() { return Shoreline.birdWaterContact(this); }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (worldObj.isRemote || isDead) return;
        if (isInWater() && getHealth() > 0) {
            motionY = Math.min(0, motionY) - 0.02D;
            super.moveEntityWithHeading(0, 0);
            return;
        }
        if (getHealth() <= 0) {
            motionX *= 0.91;
            motionY = Math.max(-0.85, motionY - 0.085);
            motionZ *= 0.91;
        }
        boolean diving = flight == Flight.DIVE && getHealth() > 0;
        double fromX = posX, fromY = centerY(), fromZ = posZ;
        AxisAlignedBB path = boundingBox.addCoord(motionX, motionY, motionZ);
        if (!AerialTerrain.loaded(worldObj, path)) {
            lastFailure = "unloaded boundary";
            motionX = motionY = motionZ = 0;
            rotationYaw += 11;
            cruiseTicks = 0;
            if (target != null) disengage("unloaded boundary");
            return;
        }
        // The native block collision pass does not stop a dive or drift at a liquid boundary.
        if (getHealth() > 0 && (diving || hurtTime == 0)
                && worldObj.isMaterialInBB(path.contract(0.001D, 0.001D, 0.001D), Material.water)) {
            if (diving) recover();
            lastFailure = "water in flight path";
            motionX = motionZ = 0;
            motionY = clearance(0, 0.22D, 0, 1) ? 0.22D : 0;
            return;
        }
        moveEntity(motionX, motionY, motionZ);
        if (isCollided) {
            ++blockedTicks;
            lastFailure = "terrain contact";
        } else {
            blockedTicks = 0;
        }
        if (!diving) return;
        if (!contacted) contacted = hitAlongMovement(fromX, fromY, fromZ);
        if (contacted && !slashingPass()) {
            recover();
        } else if (isCollided || (diveRemaining -= diveSpeed) <= 0) {
            if (isCollided) lastFailure = slashingPass() ? "pass blocked by terrain" : "dive blocked by terrain";
            else if (!contacted) lastFailure = slashingPass() ? "slashing pass missed" : "dive missed";
            recover();
        }
    }

    private boolean hitAlongMovement(double x, double y, double z) {
        if (target == null || (target instanceof EntityChicken && targetClearance(target) < 0)) return false;
        AxisAlignedBB contact = target.boundingBox.expand(width * 0.43, height * 0.43, width * 0.43);
        Vec3D start = Vec3D.createVector(x, y, z);
        Vec3D end = Vec3D.createVector(posX, centerY(), posZ);
        if (!contact.isVecInside(start) && !contact.isVecInside(end)
                && contact.calculateIntercept(start, end) == null) return false;
        if (!visible(target) || !unobstructed(posX, centerY(), posZ, target.posX, targetCenter(target), target.posZ)) return false;
        double vx = target.motionX, vy = target.motionY, vz = target.motionZ;
        int damage = slashingPass() ? 5 : 4 + worldObj.difficultySetting;
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) {
            target.motionX = vx + clamp(target.motionX - vx, -0.30, 0.30) * 0.35;
            target.motionY = vy;
            target.motionZ = vz + clamp(target.motionZ - vz, -0.30, 0.30) * 0.35;
            worldObj.playSoundAtEntity(this, "random.bowhit", 0.8F, 0.72F);
        }
        lastFailure = hit ? "none" : "contact damage resisted";
        return true;
    }

    private boolean unobstructed(double x, double y, double z, double endX, double endY, double endZ) {
        return worldObj.rayTraceBlocks(Vec3D.createVector(x, y, z),
                Vec3D.createVector(endX, endY, endZ)) == null;
    }

    private void enter(Flight state) {
        stateTicks = 0;
        flight = state;
        dataWatcher.updateObject(FLIGHT_WATCHER, Byte.valueOf((byte) state.ordinal()));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        int delay = target != null ? DISENGAGE_TICKS : scanDelay;
        if (flight == Flight.DISENGAGE) delay = Math.max(delay, DISENGAGE_TICKS - stateTicks + 60);
        tag.setInteger("ZtoAerialScanDelay", delay);
        if (natural) tag.setInteger("ZtoAerialWildTicks", Math.max(1, Math.min(240, wildTicks)));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        target = null;
        encounter = null;
        approaching = false;
        wildTicks = Math.max(0, Math.min(240, tag.getInteger("ZtoAerialWildTicks")));
        natural = wildTicks > 0;
        enter(Flight.CRUISE);
        cruiseTicks = 0;
        scanDelay = (int) clamp(tag.getInteger("ZtoAerialScanDelay"), 30, 180);
        motionX = clamp(motionX, -0.32, 0.32);
        motionY = clamp(motionY, -0.25, 0.31);
        motionZ = clamp(motionZ, -0.32, 0.32);
    }

    public String targetName() {
        if (target instanceof EntityPlayer) return ((EntityPlayer) target).username;
        return huntingChicken() ? "Chicken #" + target.entityId : "-";
    }

    boolean huntingChicken() { return target instanceof EntityChicken; }
    public String encounterStatus() { return encounter != null ? "Encounter member" : natural ? "Retiring wildlife" : "Manual"; }
    public double terrainClearance() { return AerialTerrain.skyHeight(this); }
    public double destinationX() { return destinationX; }
    public double destinationY() { return destinationY; }
    public double destinationZ() { return destinationZ; }
    public int phaseTicks() { return stateTicks; }
    public int remainingPhaseTicks() {
        switch (flight) {
            case PREPARE: return Math.max(0, prepareTicks() - stateTicks);
            case RECOVER: return Math.max(0, recoverTicks() - stateTicks);
            case DISENGAGE: return Math.max(0, DISENGAGE_TICKS - stateTicks);
            default: return 0;
        }
    }
    public String diveStatus() { return diveStatus; }
    public String lastDiveRejection() { return lastDiveRejection; }
    public int rejectedWarnings() { return rejectedWarnings; }
    public int diveCommitments() { return commitments; }
    public String lastFailure() { return lastFailure; }
    public String lastExit() { return lastExit; }

    private double centerY() { return boundingBox.minY + height * 0.5; }
    private static double targetCenter(EntityLiving subject) {
        return (subject.boundingBox.minY + subject.boundingBox.maxY) * 0.5;
    }
    private double horizontalRange(double x, double z) {
        return Math.sqrt((x - posX) * (x - posX) + (z - posZ) * (z - posZ));
    }
    private double limitAltitude(double y) { return clamp(y, 2, worldObj.getHeight() - 2.0); }
    private void faceMotion() {
        rotationYaw = yaw(motionX, motionZ);
        rotationPitch = (float) -Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
    }
    private static float yaw(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }
    private static double wrap(double degrees) {
        degrees %= 360;
        if (degrees >= 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
