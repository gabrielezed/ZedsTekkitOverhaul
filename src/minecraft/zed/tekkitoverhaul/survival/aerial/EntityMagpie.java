package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.Shoreline;

/** Small forest wildlife with short flights and no combat target. */
public final class EntityMagpie extends EntityLiving {
    public static final String TEXTURE = "/zed/textures/magpie.png";
    private static final int POSE_WATCHER = 20;
    private static final int MAX_AGE = 6000;
    private static final int HABITAT_CHECK_INTERVAL = 20;
    private static final int HABITAT_GRACE = 120;
    private static final int SHELTER_DEPTH = 6;
    private static final int THEFT_WINDOW = 60;
    private static final int THEFT_WARNING = 12;
    private static final double THEFT_REACH = 0.95;
    private static final Pose[] POSES = Pose.values();
    private static final double WALK_SPEED = 0.105;
    private static final int[] RELEASE_DIRECTIONS = {0, 60, -60, 120, -120, 180};
    public enum Pose { WATCH, WALK, HOP, FLY, PECK, PERCH, CLIMB, LAND }
    private int decisionDelay, flightTicks, lifeTicks, stepTick;
    private int habitatTicks;
    private boolean naturallyAdmitted;
    private EntityPlayer theftTarget;
    private ItemStack theftStack;
    private int theftSlot, theftDeadline, theftPecks;
    private boolean theftConsidered;
    private String stolenFrom = "";
    private EntityItem curiosity;
    private EntityItem lastReleased;
    private ItemStack carried;
    private int peckTicks, carryPause, retryReleaseAt;
    private boolean wildGold, escapedWithGold, releasePlanned, walkingToRelease, releasing;
    private double releaseX, releaseZ;
    private boolean startled;
    private Step step;
    private float gait, lastGait, stride, lastStride;
    private float headYaw, lastHeadYaw, headPitch, lastHeadPitch;

    public EntityMagpie(World world) {
        super(world);
        texture = TEXTURE;
        setSize(0.38F, 0.42F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE_WATCHER, Byte.valueOf((byte) Pose.WATCH.ordinal()));
    }

    @Override
    public int getMaxHealth() { return 4; }

    @Override
    public boolean getCanSpawnHere() {
        return worldObj.worldProvider.worldType == 0 && AerialTerrain.clear(this, boundingBox)
                && worldObj.checkIfAABBIsClear(boundingBox);
    }

    /** The beak renderer reads the actual one-item stack on the local client. */
    public ItemStack carriedItem() { return carried; }

    public String ecologyStatus() {
        String origin = naturallyAdmitted ? (habitatTicks > 0 ? "Seeking surface" : "Natural") : "Manual or legacy";
        if (carried != null) {
            String source = wildGold ? "wild" : stolenFrom.isEmpty() ? "scavenged" : "stolen";
            return origin + " | " + source + " " + carried.getItem().getItemDisplayName(carried);
        }
        if (theftTarget != null) return origin + (theftPecks > 0 ? " | Warning peck" : " | Approaching player");
        return origin + (theftConsidered ? " | Theft opportunity spent" : " | " + pose());
    }

    void markNaturallyAdmitted() { naturallyAdmitted = true; }

    void takeNaturalGold() {
        if (carried != null) return;
        carried = new ItemStack(Item.ingotGold);
        wildGold = true;
    }

    public Pose pose() { return POSES[dataWatcher.getWatchableObjectByte(POSE_WATCHER)]; }

    private void pose(Pose pose) { dataWatcher.updateObject(POSE_WATCHER, Byte.valueOf((byte) pose.ordinal())); }

    public float gait(float partial) { return lastGait + (gait - lastGait) * partial; }
    public float stride(float partial) { return lastStride + (stride - lastStride) * partial; }
    public float headYaw(float partial) { return lastHeadYaw + (headYaw - lastHeadYaw) * partial; }
    public float headPitch(float partial) { return lastHeadPitch + (headPitch - lastHeadPitch) * partial; }

    @Override
    protected void updateEntityActionState() {
        if (lifeTicks < MAX_AGE) ++lifeTicks;
        if (lifeTicks >= MAX_AGE && !isInWater()) {
            setDead();
            return;
        }
        ++entityAge;
        despawnEntity();
        if (isDead) return;
        if (naturallyAdmitted && lifeTicks % HABITAT_CHECK_INTERVAL == 0 && !checkHabitat()) return;

        if (isInWater() || flightTicks > 0 || step != null || !onGround) {
            cancelTheft();
            lookAt(null);
            return;
        }
        if (habitatTicks != 0) {
            motionX = motionZ = 0;
            return;
        }
        EntityPlayer observer = worldObj.getClosestPlayerToEntity(this, 6.0);
        if (!visible(observer) || !observer.isEntityAlive()) observer = null;
        if (observer != null && rushing(observer)) {
            face(posX - observer.posX, posZ - observer.posZ);
            startled = true;
        }
        if (startled) {
            startled = false;
            curiosity = null;
            cancelTheft();
            if (carried != null) {
                escapedWithGold = wildGold;
                releasePlanned = true;
                walkingToRelease = false;
                carryPause = 0;
            }
            lookAt(null);
            launch();
            return;
        }
        if (carried != null && (!wildGold || escapedWithGold)) {
            lookAt(observer != null && attentiveTo(observer) ? observer : null);
            carryOnGround();
            return;
        }
        if (theftTarget != null) {
            approachPlayer();
            return;
        }
        if (--decisionDelay <= 0) {
            if (carried == null && curiosity == null && observer != null && considerTheft(observer)) return;
            chooseActivity();
        }
        if (curiosity != null) approachItem();
        lookAt(curiosity != null ? curiosity : observer != null && attentiveTo(observer) ? observer : null);
        motionX = motionZ = 0;
        if (pose() == Pose.WALK) walk();
    }

    private boolean checkHabitat() {
        int depth = coverDepth(boundingBox);
        if (depth <= SHELTER_DEPTH) {
            habitatTicks = 0;
            return true;
        }
        habitatTicks += HABITAT_CHECK_INTERVAL;
        curiosity = null;
        cancelTheft();
        if (habitatTicks >= HABITAT_GRACE) {
            setDead(); // A carried stack must successfully release before retirement.
        } else if (onGround && !isInWater() && flightTicks == 0 && step == null) {
            seekShallowerFooting(depth);
        }
        return false;
    }

    private boolean considerTheft(EntityPlayer player) {
        if (theftConsidered || !eligiblePlayer(player) || horizontalDistanceSq(player) > 9
                || !attentiveTo(player) || coverDepth(boundingBox) > SHELTER_DEPTH) return false;
        InventoryPlayer inventory = player.inventory;
        for (int slot = 9; slot < inventory.mainInventory.length; ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!theftItem(stack)) continue;
            theftConsidered = true;
            if (rand.nextInt(8) != 0) return false;
            theftTarget = player;
            theftSlot = slot;
            theftStack = stack;
            theftDeadline = ticksExisted + THEFT_WINDOW;
            approachPlayer();
            return true;
        }
        return false;
    }

    private boolean eligiblePlayer(EntityPlayer player) {
        return !worldObj.isRemote && worldObj.worldProvider.worldType == 0 && worldObj.playerEntities.size() == 1
                && worldObj.playerEntities.get(0) == player && player.worldObj == worldObj && player.isEntityAlive()
                && !player.capabilities.disableDamage && !player.capabilities.isCreativeMode && !player.isPlayerSleeping()
                && player.username != null && !player.username.isEmpty()
                && player.craftingInventory == player.inventorySlots && player.inventory.getItemStack() == null
                && Math.abs(player.boundingBox.minY - boundingBox.minY) <= 0.5;
    }

    private static boolean theftItem(ItemStack stack) {
        return isScavengedItem(stack) && stack.getItemDamage() == 0 && !stack.hasTagCompound();
    }

    private double horizontalDistanceSq(EntityPlayer player) {
        double dx = player.posX - posX, dz = player.posZ - posZ;
        return dx * dx + dz * dz;
    }

    private void approachPlayer() {
        EntityPlayer player = theftTarget;
        motionX = motionZ = 0;
        double range = horizontalDistanceSq(player);
        boolean close = range <= THEFT_REACH * THEFT_REACH;
        if (ticksExisted >= theftDeadline || range > 9 || (theftPecks > 0 && !close)
                || !eligiblePlayer(player) || !visible(player) || !theftItem(theftStack)
                || player.inventory.getStackInSlot(theftSlot) != theftStack) {
            cancelTheft();
            rest();
            return;
        }
        face(player.posX - posX, player.posZ - posZ);
        lookAt(player);
        if (close) {
            if (worldObj.rayTraceBlocks(Vec3D.createVector(posX, boundingBox.minY + 0.25, posZ),
                    Vec3D.createVector(player.posX, player.boundingBox.minY + 0.25, player.posZ)) != null) {
                cancelTheft();
                rest();
                return;
            }
            pose(Pose.PECK);
            if (++theftPecks >= THEFT_WARNING) {
                carried = player.inventory.decrStackSize(theftSlot, 1);
                stolenFrom = player.username;
                player.inventory.onInventoryChanged();
                cancelTheft();
                releasePlanned = walkingToRelease = false;
                face(posX - player.posX, posZ - player.posZ);
                worldObj.playSoundAtEntity(this, "random.pop", 0.50F, 1.55F);
                launch();
            }
        } else if (clearAhead(rotationYaw, 0.37)) {
            pose(Pose.WALK);
            double distance = Math.sqrt(range);
            motionX = (player.posX - posX) * WALK_SPEED / distance;
            motionZ = (player.posZ - posZ) * WALK_SPEED / distance;
        } else {
            cancelTheft();
            rest();
        }
    }

    private void cancelTheft() {
        theftTarget = null;
        theftStack = null;
        theftPecks = 0;
    }

    private int coverDepth(AxisAlignedBB box) {
        int x = MathHelper.floor_double((box.minX + box.maxX) * 0.5);
        int y = MathHelper.floor_double(box.minY + 0.05);
        int z = MathHelper.floor_double((box.minZ + box.maxZ) * 0.5);
        return worldObj.blockExists(x, y, z) ? worldObj.getHeightValue(x, z) - y : Integer.MAX_VALUE;
    }

    private boolean remainsNearSurface(AxisAlignedBB next, double dx, double dy, double dz) {
        double feet = boundingBox.minY + 0.05;
        if (MathHelper.floor_double(posX) == MathHelper.floor_double(posX + dx)
                && MathHelper.floor_double(posZ) == MathHelper.floor_double(posZ + dz)
                && MathHelper.floor_double(feet) == MathHelper.floor_double(feet + dy)) return true;
        return coverDepth(next) <= Math.max(SHELTER_DEPTH, coverDepth(boundingBox));
    }

    private void seekShallowerFooting(int depth) {
        int feet = MathHelper.floor_double(boundingBox.minY + 0.05);
        for (int bend : RELEASE_DIRECTIONS) {
            double angle = Math.toRadians(rotationYaw + bend);
            int x = MathHelper.floor_double(posX - Math.sin(angle) * 1.5);
            int z = MathHelper.floor_double(posZ + Math.cos(angle) * 1.5);
            if (!worldObj.blockExists(x, feet + 2, z)) continue;
            for (int rise = 0; rise <= 2; ++rise) {
                if (!worldObj.isBlockNormalCube(x, feet + rise - 1, z)
                        && worldObj.getBlockId(x, feet + rise - 1, z) != Block.leaves.blockID) continue;
                Step exit = new Step(x + 0.5 - posX, feet + rise - boundingBox.minY, z + 0.5 - posZ, 22);
                if (coverDepth(boundingBox.getOffsetBoundingBox(exit.dx, exit.dy, exit.dz)) < depth && exit.clear()) {
                    startStep(exit, rise);
                    return;
                }
            }
        }
        motionX = motionZ = 0;
        pose(Pose.WATCH);
    }

    private void chooseActivity() {
        curiosity = null;
        if (pose() == Pose.LAND || pose() == Pose.WALK || pose() == Pose.PECK) {
            rest();
            return;
        }
        int roll = rand.nextInt(10);
        curiosity = carried == null && roll < 4 ? nearbyItem() : null;
        peckTicks = 0;
        if (curiosity != null) {
            decisionDelay = 28;
            return;
        }
        rotationYaw += rand.nextInt(111) - 55;
        if (roll == 9) launch();
        else if (roll < 3) rest();
        else {
            pose(Pose.WALK);
            decisionDelay = 14 + rand.nextInt(14);
        }
    }

    private void rest() {
        Pose resting = restingPose();
        pose(resting);
        decisionDelay = resting == Pose.PERCH ? 60 : 8 + rand.nextInt(11);
    }

    private boolean visible(Entity target) {
        return target != null && AerialTerrain.loaded(worldObj, posX, posZ, target.posX, target.posZ)
                && canEntityBeSeen(target);
    }

    private boolean rushing(EntityPlayer player) {
        if (player.isSneaking() || player.capabilities.isCreativeMode || getDistanceSqToEntity(player) >= 16) return false;
        double dx = player.posX - player.prevPosX, dz = player.posZ - player.prevPosZ;
        return dx * dx + dz * dz > 0.09 * 0.09
                && dx * (posX - player.posX) + dz * (posZ - player.posZ) > 0.04;
    }

    private static boolean attentiveTo(EntityPlayer player) {
        double dx = player.posX - player.prevPosX, dz = player.posZ - player.prevPosZ;
        return player.isSneaking() || dx * dx + dz * dz < 0.045 * 0.045;
    }

    private void lookAt(Entity target) {
        lastHeadYaw = headYaw;
        lastHeadPitch = headPitch;
        float yaw = 0, pitch = 0;
        if (target != null) {
            double dx = target.posX - posX, dz = target.posZ - posZ;
            double dy = target.posY + target.getEyeHeight() - posY - getEyeHeight();
            float angle = heading(dx, dz) - rotationYaw;
            yaw = angle - 360 * MathHelper.floor_float((angle + 180) / 360);
            pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
        headYaw += limit(limit(yaw, 82) - headYaw, 13);
        headPitch += limit(limit(pitch, 40) - headPitch, 8);
    }

    private static float limit(float value, float maximum) {
        return Math.max(-maximum, Math.min(maximum, value));
    }

    private Pose restingPose() {
        int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(boundingBox.minY - 0.05);
        int z = MathHelper.floor_double(posZ);
        return worldObj.blockExists(x, y, z) && worldObj.getBlockId(x, y, z) == Block.leaves.blockID
                ? Pose.PERCH : Pose.WATCH;
    }

    private EntityItem nearbyItem() {
        for (Object entry : worldObj.getEntitiesWithinAABB(EntityItem.class, boundingBox.expand(4, 1, 4))) {
            EntityItem item = (EntityItem) entry;
            if (scavengeable(item) && visible(item)) return item;
        }
        return null;
    }

    private void approachItem() {
        if (!scavengeable(curiosity) || getDistanceSqToEntity(curiosity) > 25 || !visible(curiosity)) {
            curiosity = null;
            rest();
        } else {
            boolean close = getDistanceSqToEntity(curiosity) < 0.56;
            if (!close) face(curiosity.posX - posX, curiosity.posZ - posZ);
            pose(close ? Pose.PECK : Pose.WALK);
            if (close && ++peckTicks == 6 && rand.nextInt(5) == 0
                    && worldObj.loadedEntityList.contains(curiosity)) takeOne();
        }
    }

    private boolean scavengeable(EntityItem item) {
        return item != lastReleased && !item.isDead && isScavengedItem(item.item);
    }

    private static boolean isScavengedItem(ItemStack stack) {
        return stack != null && stack.stackSize > 0
                && (stack.itemID == Item.seeds.shiftedIndex || stack.itemID == Item.feather.shiftedIndex);
    }

    private void takeOne() {
        if (carried != null || !scavengeable(curiosity)) return;
        carried = curiosity.item.splitStack(1);
        if (curiosity.item.stackSize == 0) curiosity.setDead();
        curiosity = null;
        releasePlanned = walkingToRelease = false;
        carryPause = 8;
    }

    private void carryOnGround() {
        motionX = motionZ = 0;
        if (pose() == Pose.LAND && --decisionDelay > 0) return;
        if (carryPause > 0) {
            --carryPause;
            pose(Pose.WATCH);
            return;
        }
        if (!releasePlanned) {
            releasePlanned = true;
            planRelease();
            if (step != null) return;
        }
        if (walkingToRelease) {
            double dx = releaseX - posX, dz = releaseZ - posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.20 && supportedPath(dx, dz)) {
                face(dx, dz);
                pose(Pose.WALK);
                motionX = dx * WALK_SPEED / distance;
                motionZ = dz * WALK_SPEED / distance;
                return;
            }
            walkingToRelease = false;
        }
        if (releaseCarry()) rest();
        else pose(restingPose());
    }

    private void planRelease() {
        int feet = MathHelper.floor_double(boundingBox.minY + 0.05);
        for (int bend : RELEASE_DIRECTIONS) {
            double angle = Math.toRadians(rotationYaw + bend);
            int x = MathHelper.floor_double(posX - Math.sin(angle) * 2.4);
            int z = MathHelper.floor_double(posZ + Math.cos(angle) * 2.4);
            if (!worldObj.blockExists(x, feet + 2, z)) continue;
            for (int rise = 1; rise <= 2; ++rise) {
                if (worldObj.getBlockId(x, feet + rise - 1, z) != Block.leaves.blockID) continue;
                Step candidate = new Step(x + 0.5 - posX, feet + rise - boundingBox.minY, z + 0.5 - posZ,
                        rise == 1 ? 24 : 28);
                if (candidate.clear()) {
                    startStep(candidate, rise);
                    return;
                }
            }
        }
        for (int bend : RELEASE_DIRECTIONS) {
            double angle = Math.toRadians(rotationYaw + bend);
            double dx = -Math.sin(angle) * 2.4, dz = Math.cos(angle) * 2.4;
            if (!supportedPath(dx, dz)) continue;
            releaseX = posX + dx;
            releaseZ = posZ + dz;
            walkingToRelease = true;
            return;
        }
    }

    private boolean supportedPath(double dx, double dz) {
        if (!AerialTerrain.clear(this, boundingBox.addCoord(dx, 0, dz))) return false;
        int y = MathHelper.floor_double(boundingBox.minY - 0.05);
        for (int i = 1; i <= 5; ++i) {
            int x = MathHelper.floor_double(posX + dx * i / 5);
            int z = MathHelper.floor_double(posZ + dz * i / 5);
            if (!worldObj.isBlockNormalCube(x, y, z) && worldObj.getBlockId(x, y, z) != Block.leaves.blockID) return false;
        }
        return true;
    }

    private boolean releaseCarry() {
        if (carried == null) return true;
        if (worldObj.isRemote || releasing || ticksExisted < retryReleaseAt) return false;
        retryReleaseAt = ticksExisted + 10;
        EntityItem drop = new EntityItem(worldObj, posX, boundingBox.minY + 0.34, posZ, carried.copy());
        AxisAlignedBB room = drop.boundingBox;
        // A submerged retirement or death can hand the stack back to native item physics.
        if (!AerialTerrain.clear(this, room) && !(isInWater() && room.minY >= 0 && room.maxY < worldObj.getHeight()
                && AerialTerrain.loaded(worldObj, room) && worldObj.getCollidingBoundingBoxes(this, room).isEmpty()
                && !worldObj.isMaterialInBB(room, Material.lava))) return false;
        drop.motionX = drop.motionZ = 0;
        drop.motionY = 0.03;
        drop.delayBeforeCanPickup = 10;
        releasing = true;
        try {
            if (!worldObj.spawnEntityInWorld(drop)) return false;
            carried = null;
            stolenFrom = "";
            lastReleased = drop;
            wildGold = escapedWithGold = false;
            return true;
        } finally {
            releasing = false;
        }
    }

    @Override
    public void setDead() {
        if (!worldObj.isRemote && carried != null) {
            if (wildGold && !escapedWithGold && getHealth() > 0) carried = null;
            else if (!releaseCarry()) return;
        }
        super.setDead();
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (getHealth() <= 0) releaseCarry();
    }

    @Override
    protected void onDeathUpdate() {
        super.onDeathUpdate();
        if (deathTime > 20 && !isDead) setDead();
    }

    private static float heading(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }

    private void face(double x, double z) { rotationYaw = heading(x, z); }

    private void walk() {
        if (clearAhead(rotationYaw, 0.37)) {
            motionX = -Math.sin(Math.toRadians(rotationYaw)) * WALK_SPEED;
            motionZ = Math.cos(Math.toRadians(rotationYaw)) * WALK_SPEED;
        } else if (!findStep()) {
            redirect();
        }
    }

    private boolean clearAhead(float heading, double distance) {
        double angle = Math.toRadians(heading);
        double dx = -Math.sin(angle) * distance, dz = Math.cos(angle) * distance;
        return AerialTerrain.clear(this, boundingBox.addCoord(dx, 0, dz)) && Shoreline.mayStep(this, dx, dz)
                && (!naturallyAdmitted || shallowStep(boundingBox.getOffsetBoundingBox(dx, 0, dz)));
    }

    private boolean shallowStep(AxisAlignedBB next) {
        int x = MathHelper.floor_double((next.minX + next.maxX) * 0.5);
        int z = MathHelper.floor_double((next.minZ + next.maxZ) * 0.5);
        int feet = MathHelper.floor_double(next.minY + 0.05);
        for (int y = feet - 1; y >= Math.max(0, feet - 2); --y) {
            if (worldObj.isBlockNormalCube(x, y, z) || worldObj.getBlockId(x, y, z) == Block.leaves.blockID) {
                int currentDepth = coverDepth(boundingBox);
                return worldObj.getHeightValue(x, z) - (y + 1) <= Math.max(SHELTER_DEPTH, currentDepth);
            }
        }
        return false;
    }

    private void redirect() {
        curiosity = null;
        motionX = motionZ = 0;
        int turn = rand.nextBoolean() ? 1 : -1;
        for (int bend : new int[] {65 * turn, -65 * turn, 120 * turn, -120 * turn, 180}) {
            if (!clearAhead(rotationYaw + bend, 0.55)) continue;
            rotationYaw += bend;
            decisionDelay = 15;
            pose(Pose.WALK);
            return;
        }
        rotationYaw += 110 * turn;
        rest();
    }

    private boolean findStep() {
        double angle = Math.toRadians(rotationYaw);
        int x = MathHelper.floor_double(posX - Math.sin(angle) * 0.75);
        int z = MathHelper.floor_double(posZ + Math.cos(angle) * 0.75);
        int feet = MathHelper.floor_double(boundingBox.minY + 0.05);
        if (!worldObj.blockExists(x, feet + 2, z)) return false;
        for (int rise = 1; rise <= 2; ++rise) {
            // Require a full support block and clearance for the entire takeoff and landing.
            if (!worldObj.isBlockNormalCube(x, feet + rise - 1, z)) continue;
            Step candidate = new Step(x + 0.5 - posX, feet + rise - boundingBox.minY, z + 0.5 - posZ,
                    rise == 1 ? 11 : 16);
            if (!candidate.clear()) continue;
            startStep(candidate, rise);
            return true;
        }
        return false;
    }

    private void startStep(Step candidate, int rise) {
        step = candidate;
        stepTick = 0;
        curiosity = null;
        face(step.dx, step.dz);
        pose(rise <= 1 ? Pose.HOP : Pose.CLIMB);
    }

    private final class Step {
        final double dx, dy, dz;
        final int ticks;

        Step(double dx, double dy, double dz, int ticks) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.ticks = ticks;
        }

        double forward(int tick) {
            // Clear the face before crossing, then slow over the center of the landing block.
            double progress = Math.max(0, Math.min(1, (tick / (double) ticks - 0.36) / 0.45));
            return progress * progress * (3 - 2 * progress);
        }

        double height(int tick) {
            // Finish just above support so native gravity makes the final contact.
            double t = tick / (double) ticks;
            double up = Math.min(1, t / 0.36);
            double down = Math.max(0, (t - 0.81) / 0.19);
            return (dy + 0.18) * Math.sin(up * Math.PI / 2) - down * 0.15;
        }

        boolean clear() {
            double along = 0, up = 0;
            for (int tick = 1; tick <= ticks; ++tick) {
                double next = forward(tick), above = height(tick);
                AxisAlignedBB start = boundingBox.getOffsetBoundingBox(dx * along, up, dz * along);
                if (!AerialTerrain.clear(EntityMagpie.this,
                        start.addCoord(dx * (next - along), above - up, dz * (next - along)))) return false;
                along = next;
                up = above;
            }
            return true;
        }
    }

    private void launch() {
        flightTicks = 24 + rand.nextInt(12);
        decisionDelay = 24;
        motionX = motionZ = 0;
        motionY = 0.25;
        pose(Pose.FLY);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        if (hit && getHealth() > 0 && source != DamageSource.drown) {
            cancelTheft();
            if (wildGold) escapedWithGold = true;
            if (source.getEntity() != null) face(posX - source.getEntity().posX, posZ - source.getEntity().posZ);
            startled = true;
        }
        return hit;
    }

    @Override
    public boolean handleWaterMovement() { return Shoreline.birdWaterContact(this); }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        lastGait = gait;
        lastStride = stride;
        stride = 0;
        if (isInWater()) {
            step = null;
            flightTicks = 0;
            pose(Pose.WATCH);
        }
        if (step != null) {
            double along = step.forward(stepTick + 1) - step.forward(stepTick);
            double dy = step.height(stepTick + 1) - step.height(stepTick);
            double dx = step.dx * along, dz = step.dz * along;
            if (AerialTerrain.clear(this, boundingBox.addCoord(dx, dy, dz))) {
                moveEntity(dx, dy, dz);
                motionX = motionZ = 0;
                motionY = dy;
                if (++stepTick == step.ticks) step = null;
                return;
            }
            step = null;
            motionX = motionY = motionZ = 0;
        }
        if (flightTicks > 0) {
            --flightTicks;
            double angle = rotationYaw * Math.PI / 180;
            double dx = -Math.sin(angle) * 0.23, dz = Math.cos(angle) * 0.23;
            double dy = flightTicks > 22 ? 0.18 : flightTicks > 12 ? 0.025 : -0.15;
            AxisAlignedBB path = boundingBox.addCoord(dx, dy, dz);
            AxisAlignedBB next = boundingBox.getOffsetBoundingBox(dx, dy, dz);
            if (AerialTerrain.clear(this, path)
                    && Shoreline.mayLand(worldObj, next)
                    && (!naturallyAdmitted || remainsNearSurface(next, dx, dy, dz))) {
                moveEntity(dx, dy, dz);
                fallDistance = 0;
                motionX = dx;
                motionY = dy;
                motionZ = dz;
                return;
            }
            flightTicks = 0;
            motionX = motionY = motionZ = 0;
        }
        if (!AerialTerrain.loaded(worldObj, boundingBox.addCoord(motionX, motionY, motionZ))) {
            motionX = motionY = motionZ = 0;
            return;
        }
        double x = posX, z = posZ;
        boolean walking = onGround && pose() == Pose.WALK;
        super.moveEntityWithHeading(0, 0);
        if (!onGround) return;
        if (pose() == Pose.FLY || pose() == Pose.HOP || pose() == Pose.CLIMB) {
            pose(Pose.LAND);
            decisionDelay = 5;
        } else if (walking) {
            double dx = posX - x, dz = posZ - z;
            double travel = Math.sqrt(dx * dx + dz * dz);
            gait += (float) (travel * 11);
            stride = (float) Math.min(1, travel / WALK_SPEED);
            if (isCollidedHorizontally) redirect();
        }
    }

    @Override
    protected void fall(float distance) {}

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ZtoMagpieAge", lifeTicks);
        if (naturallyAdmitted) tag.setBoolean("ZtoMagpieNatural", true);
        if (theftConsidered) tag.setBoolean("ZtoMagpieTheftConsidered", true);
        if (carried != null) {
            tag.setCompoundTag("ZtoMagpieBeak", carried.writeToNBT(new NBTTagCompound()));
            if (!stolenFrom.isEmpty()) tag.setString("ZtoMagpieStolenFrom", stolenFrom);
            if (wildGold) {
                tag.setBoolean("ZtoMagpieWildGold", true);
                tag.setBoolean("ZtoMagpieGoldEscaped", escapedWithGold);
            }
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        lifeTicks = MathHelper.clamp_int(tag.getInteger("ZtoMagpieAge"), 0, MAX_AGE);
        naturallyAdmitted = tag.getBoolean("ZtoMagpieNatural");
        theftConsidered = tag.getBoolean("ZtoMagpieTheftConsidered");
        cancelTheft();
        ItemStack saved = tag.hasKey("ZtoMagpieBeak")
                ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("ZtoMagpieBeak")) : null;
        wildGold = saved != null && saved.stackSize > 0 && saved.itemID == Item.ingotGold.shiftedIndex
                && tag.getBoolean("ZtoMagpieWildGold");
        escapedWithGold = wildGold && tag.getBoolean("ZtoMagpieGoldEscaped");
        carried = wildGold || isScavengedItem(saved) ? saved : null;
        stolenFrom = theftItem(carried) ? tag.getString("ZtoMagpieStolenFrom") : "";
        if (!stolenFrom.isEmpty()) theftConsidered = true;
        if (carried != null) carried.stackSize = 1;
        releasePlanned = walkingToRelease = false;
    }
}
