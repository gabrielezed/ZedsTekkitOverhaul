package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

public final class EntityThief extends TacticalHostile {
    private static final int WINDUP = 16;
    private static final int HOTBAR = 9;
    private static final float APPROACH_SPEED = 0.32F;
    private final ThiefEscape escape = new ThiefEscape(this);
    private String carrier = "";
    private ThiefTrail custody;
    private int nextTheft = 30, nextRoute, nextBlink;
    private int theftStart = -1, contact = -10;
    private double reachX, reachZ;

    public EntityThief(World world) {
        super(world, 0.72F, 1.95F, "thief");
        attackStrength = 2;
    }

    @Override public int getMaxHealth() { return 16; }
    @Override public float getEyeHeight() { return 1.66F; }
    @Override protected int unseenLimit() { return 80; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double range = dx * dx + dz * dz;
        boolean inReach = visible && range <= 2.2 * 2.2
                && target.boundingBox.maxY > boundingBox.minY && target.boundingBox.minY < boundingBox.maxY;
        if (theftStart >= 0) {
            if (!inReach || !onGround || !eligible(target)
                    || (posX - reachX) * (posX - reachX) + (posZ - reachZ) * (posZ - reachZ) > 0.49D) {
                cancelTheft();
            } else if (ticksExisted - theftStart >= WINDUP) {
                steal();
            }
            return;
        }
        if (inReach && onGround && ticksExisted >= nextTheft && eligible(target)) {
            ThiefTrail trail = ThiefTrail.get(worldObj, true);
            if (trail != null && !trail.occupied() && hasCandidate(target)) {
                theftStart = ticksExisted;
                reachX = posX;
                reachZ = posZ;
                getNavigator().clearPathEntity();
                worldObj.playSoundAtEntity(this, "mob.endermen.idle", 0.55F, 1.55F);
                return;
            }
            nextTheft = ticksExisted + 45;
        }
        if (!holdingSiteSiege() && visible && distanceSq <= 36 && ticksExisted >= nextBlink && (hurtTime > 0 || attackTime >= 12)) {
            double away = Math.atan2(posZ - target.posZ, posX - target.posX);
            if (EnderkinBlink.shift(this, away + ((entityId & 1) == 0 ? 0.9D : -0.9D), 3.4D, 4, 1.5F)) {
                nextBlink = ticksExisted + 70;
                return;
            }
            nextBlink = ticksExisted + 12;
        }
        if (inReach && range < 1.8 * 1.8) {
            getNavigator().clearPathEntity();
            if (attackTime == 0) {
                attackTime = 24;
                swing();
                attackEntityAsMob(target);
            }
        } else if (ticksExisted >= nextRoute) {
            nextRoute = ticksExisted + 15;
            getNavigator().setPath(plan(target.posX, target.boundingBox.minY, target.posZ), APPROACH_SPEED);
        }
    }

    private boolean eligible(EntityPlayer player) {
        return ThiefTrail.local(worldObj) && worldObj.playerEntities.size() == 1
                && player.isEntityAlive() && !player.capabilities.disableDamage && !player.isPlayerSleeping()
                && player.username != null && player.craftingInventory == player.inventorySlots
                && player.inventory.getItemStack() == null;
    }

    private boolean validSlot(EntityPlayer player, int slot) {
        return !(slot == player.inventory.currentItem && player.isUsingItem())
                && ThiefTrail.validItem(player.inventory.mainInventory[slot]);
    }

    private boolean hasCandidate(EntityPlayer player) {
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++)
            if (validSlot(player, slot)) return true;
        return false;
    }

    private int randomSlot(EntityPlayer player, int start, int end) {
        int chosen = -1, count = 0;
        for (int slot = start; slot < end; slot++)
            if (validSlot(player, slot) && rand.nextInt(++count) == 0) chosen = slot;
        return chosen;
    }

    private void steal() {
        EntityPlayer victim = target;
        ThiefTrail trail = ThiefTrail.get(worldObj, true);
        cancelTheft();
        if (trail == null || trail.occupied()) return;
        boolean hotbarFirst = rand.nextInt(3) == 0;
        InventoryPlayer inventory = victim.inventory;
        int slot = randomSlot(victim, hotbarFirst ? 0 : HOTBAR, hotbarFirst ? HOTBAR : inventory.mainInventory.length);
        if (slot < 0) slot = randomSlot(victim, hotbarFirst ? HOTBAR : 0, hotbarFirst ? inventory.mainInventory.length : HOTBAR);
        if (slot < 0) return;
        carrier = trail.claim(this, victim, slot);
        if (carrier.length() == 0) return;
        custody = trail;
        contact = ticksExisted;
        disengage();
        escape.start(victim);
        inventory.onInventoryChanged();
        trailParticles(8);
        boolean hotbar = slot < HOTBAR;
        worldObj.playSoundAtEntity(this, hotbar ? "mob.endermen.portal" : "random.pop",
                hotbar ? 0.95F : 0.75F, hotbar ? 1.20F : 0.72F);
        victim.addChatMessage("A Thief stole " + trail.description() + (hotbar ? " from your hotbar." : ".")
                + " Follow it, or sneak with a compass to check its trail.");
    }

    void trailParticles(int count) {
        for (int i = 0; i < count; i++) {
            worldObj.spawnParticle("portal", posX + (rand.nextDouble() - 0.5) * width,
                    boundingBox.minY + 0.7 + rand.nextDouble() * 0.7, posZ + (rand.nextDouble() - 0.5) * width,
                    (rand.nextDouble() - 0.5) * 0.4D, 0.10D, (rand.nextDouble() - 0.5) * 0.4D);
        }
    }

    @Override
    protected boolean independentTactics() {
        if (trail() == null) return false;
        escape.tick();
        return true;
    }

    @Override
    protected void idle() {
        if (ticksExisted < nextRoute || !getNavigator().noPath()) return;
        nextRoute = ticksExisted + 45 + rand.nextInt(40);
        getNavigator().setPath(plan(posX + rand.nextInt(9) - 4, boundingBox.minY, posZ + rand.nextInt(9) - 4),
                APPROACH_SPEED);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        ThiefTrail trail = trail();
        if (trail != null) trail.locate(this);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        if (hit && theftStart >= 0) cancelTheft();
        return hit;
    }

    @Override protected void resetTactics() { if (theftStart >= 0) cancelTheft(); }

    private void cancelTheft() {
        theftStart = -1;
        nextTheft = ticksExisted + 90;
    }

    private ThiefTrail trail() {
        if (carrier.length() == 0 || !ThiefTrail.local(worldObj)) return null;
        if (custody == null) custody = ThiefTrail.get(worldObj, false);
        return custody != null && custody.carries(carrier) ? custody : null;
    }

    boolean carries(String token) { return !isDead && carrier.equals(token); }

    public boolean carryingLoot() { return custody != null && custody.carries(carrier); }

    public float theftPose(float partialTick) {
        if (theftStart >= 0) return Math.min(1, (ticksExisted - theftStart + partialTick) / WINDUP);
        return Math.max(0, 1 - (ticksExisted - contact + partialTick) / 7);
    }

    @Override protected boolean canDespawn() { return super.canDespawn() && trail() == null; }

    @Override
    public void onDeath(DamageSource source) {
        if (dead) return;
        super.onDeath(source);
        if (dead) releaseLoot();
    }

    @Override
    public void setDead() {
        releaseLoot(); // Peaceful removal also passes through here.
        super.setDead();
    }

    private void releaseLoot() {
        ThiefTrail trail = trail();
        if (trail != null) {
            trail.release(this, carrier);
            carrier = "";
            custody = null;
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (carrier.length() == 36) tag.setString("ZtoLootToken", carrier);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        String saved = tag.getString("ZtoLootToken");
        carrier = saved.length() == 36 ? saved : "";
        custody = null;
    }
}
