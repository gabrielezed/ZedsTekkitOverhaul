package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Vec3D;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.Attack;
import static zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.*;

final class CustodianCombat {
    private final EntityAbyssalCustodian boss;
    private final int[] cooldown = new int[Attack.values().length];
    private EntityPlayer target;
    private Attack attack, previous;
    private int tick, introduction, introductionLength, side = -1;
    private float yaw, pitch;
    private boolean struck;
    private Vec3D destination;
    private double swimX, swimY, swimZ;
    private boolean expansionCue;

    CustodianCombat(EntityAbyssalCustodian boss) { this.boss = boss; }

    void tick() {
        for (int i = 0; i < cooldown.length; i++) if (cooldown[i] > 0) cooldown[i]--;
        EntityPlayer nearby = boss.worldObj.getClosestVulnerablePlayerToEntity(boss, DETECTION);
        if (!valid(nearby) || !boss.isInWater()) {
            disengage();
            moveHome();
            return;
        }
        if (target == null) startIntroduction(EntityAbyssalCustodian.AWAKEN, AWAKEN, "awaken");
        target = nearby;
        boss.lookAt(target);
        if (attack != null) {
            advance();
            return;
        }
        if (introductionLength > 0) {
            boss.halt();
            face();
            boss.pose(introduction, tick++);
            if (tick >= introductionLength) introductionLength = 0;
            return;
        }
        if (!boss.secondPhase() && boss.getHealth() <= SECOND_PHASE) {
            boss.enterSecondPhase();
            startIntroduction(EntityAbyssalCustodian.TRANSITION, TRANSITION, "phase");
            return;
        }
        select();
    }

    private boolean valid(EntityPlayer player) {
        if (!vulnerable(player)) return false;
        AxisAlignedBB territory = boss.arena().bounds.expand(4, 3, 4);
        return territory.intersectsWith(player.boundingBox);
    }

    private boolean vulnerable(EntityPlayer player) {
        return player != null && player.isEntityAlive() && player.isInWater()
                && !player.capabilities.disableDamage && !player.capabilities.isCreativeMode;
    }

    private void startIntroduction(int pose, int length, String cue) {
        introduction = pose;
        introductionLength = length;
        tick = 0;
        sound(cue);
    }

    boolean compressing() { return attack == Attack.COMPRESSION; }

    int pressureOn(EntityPlayer diver) {
        if (target != diver) return 0;
        return attack == Attack.STRIKE || attack == Attack.PRESSURE ? 2 : 1;
    }

    void disengage() {
        if (attack != null) cooldown[attack.ordinal()] = Math.max(30, cooldown[attack.ordinal()]);
        attack = null;
        destination = null;
        introductionLength = 0;
        target = null;
        boss.lookAt(null);
        boss.pose(EntityAbyssalCustodian.DORMANT, 0);
    }

    private void moveHome() {
        CustodianArena arena = boss.arena();
        double offset = Math.sin(boss.ticksExisted * 0.027);
        boss.steer(arena.x + offset * 1.7, arena.y + offset * 0.65, arena.z, PATROL);
    }

    private void face() {
        Vec3D center = CustodianGeometry.center(target);
        SwimRoute.face(boss, center.xCoord - boss.posX, center.yCoord - boss.boundingBox.minY - SHOULDER_Y,
                center.zCoord - boss.posZ);
    }

    private void select() {
        CustodianArena arena = boss.arena();
        if (!arena.contains(boss.posX, boss.boundingBox.minY, boss.posZ, HEIGHT)) {
            moveHome();
            return;
        }
        Vec3D center = CustodianGeometry.center(target);
        double distance = boss.getDistance(center.xCoord, center.yCoord - SHOULDER_Y, center.zCoord);
        boolean visible = CustodianGeometry.visible(boss.worldObj, center(), center);
        int[] weights = new int[Attack.values().length];
        if (visible && distance < STRIKE_REACH + 0.35) {
            aim(-side);
            if (CustodianGeometry.clearReach(boss.worldObj, CustodianGeometry.shoulder(boss, -side, yaw), yaw, pitch)
                    >= Math.max(0.15, distance - 0.55)) weights[Attack.STRIKE.ordinal()] = distance < 4 ? 9 : 6;
        }
        if (visible && distance < PRESSURE_RADIUS && cooldown[Attack.PRESSURE.ordinal()] == 0)
            weights[Attack.PRESSURE.ordinal()] = distance < 3.2 ? 7 : 2;
        if (cooldown[Attack.BLINK.ordinal()] == 0 && (distance > 4.2 || boss.secondPhase())) {
            destination = landing();
            if (destination != null) weights[Attack.BLINK.ordinal()] = boss.secondPhase() ? 7 : 3;
        }
        if (visible && distance > 3 && cooldown[Attack.RESONANCE.ordinal()] == 0
                && GuardianEncounters.casters(target, true) == 0 && !responders().isEmpty())
            weights[Attack.RESONANCE.ordinal()] = 3;
        if (visible && cooldown[Attack.COMPRESSION.ordinal()] == 0 && distance > 1.3 && distance < 7)
            weights[Attack.COMPRESSION.ordinal()] = boss.secondPhase() ? 6 : distance < 4 ? 4 : 2;
        int total = 0;
        for (Attack candidate : Attack.values()) {
            if (candidate == previous) weights[candidate.ordinal()] /= 3;
            if (cooldown[candidate.ordinal()] > 0) weights[candidate.ordinal()] = 0;
            total += weights[candidate.ordinal()];
        }
        if (total > 0) {
            int draw = boss.worldObj.rand.nextInt(total);
            for (Attack candidate : Attack.values()) {
                draw -= weights[candidate.ordinal()];
                if (draw < 0) {
                    begin(candidate);
                    return;
                }
            }
        }
        boss.pose(EntityAbyssalCustodian.SWIM, 0);
        double x = target.posX, z = target.posZ;
        if (!visible || distance < 2.4) {
            double orbit = boss.ticksExisted * (boss.secondPhase() ? 0.074 : 0.048);
            x += Math.cos(orbit) * 3.2;
            z += Math.sin(orbit) * 3.2;
        }
        boss.steer(x, target.boundingBox.minY - 0.25, z, boss.secondPhase() ? REPOSITION : CRUISE);
    }

    private void begin(Attack selected) {
        attack = selected;
        previous = selected;
        tick = 0;
        struck = false;
        if (selected == Attack.COMPRESSION) {
            double direction = Math.toRadians(boss.rotationYaw + (side < 0 ? 72 : -72));
            swimX = boss.posX - Math.sin(direction) * 7;
            swimZ = boss.posZ + Math.cos(direction) * 7;
            swimY = boss.boundingBox.minY + (boss.worldObj.rand.nextBoolean() ? 2 : -2);
            expansionCue = false;
            side = -side;
        }
        if (selected == Attack.STRIKE) {
            side = -side;
            aim(side);
        }
        boss.pose(EntityAbyssalCustodian.ATTACK + selected.ordinal(), 0);
        sound(cue(selected) + "_charge");
    }

    private void advance() {
        if (attack == Attack.COMPRESSION) {
            compression();
            return;
        }
        boss.halt();
        if (attack != Attack.STRIKE) face();
        boss.pose(EntityAbyssalCustodian.ATTACK + attack.ordinal(), tick);
        if (attack == Attack.BLINK && destination != null && tick >= attack.windup - 6
                && tick < attack.windup && tick % 3 == 0)
            boss.riftParticles(destination.xCoord, destination.yCoord, destination.zCoord, 3);
        if (attack == Attack.STRIKE) {
            if (tick < attack.windup - 6) aim(side);
            boss.rotationYaw = yaw;
            boss.rotationPitch = pitch;
            strike();
        }
        if (tick == attack.windup) {
            sound(cue(attack) + "_release");
            if (attack == Attack.PRESSURE) pressure();
            if (attack == Attack.BLINK) teleport();
            if (attack == Attack.RESONANCE) resonance();
        }
        if (++tick >= attack.end()) finish();
    }

    private void finish() {
        int delay = 12;
        if (attack == Attack.BLINK) delay = boss.secondPhase() ? BLINK_COOLDOWN_TWO : BLINK_COOLDOWN;
        if (attack == Attack.PRESSURE) delay = PRESSURE_COOLDOWN;
        if (attack == Attack.RESONANCE) delay = RESONANCE_COOLDOWN;
        if (attack == Attack.COMPRESSION) delay = boss.secondPhase() ? COMPRESSION_COOLDOWN_TWO : COMPRESSION_COOLDOWN;
        cooldown[attack.ordinal()] = delay;
        attack = null;
    }

    private void compression() {
        Attack move = Attack.COMPRESSION;
        boss.pose(EntityAbyssalCustodian.ATTACK + move.ordinal(), tick);
        if (tick < move.windup) {
            boss.halt();
            boss.resize(1 - (1 - COMPRESSED_SCALE) * ease((tick + 1F) / move.windup));
        } else if (tick < move.windup + move.active) {
            boss.steer(swimX, swimY, swimZ, 0.23);
        } else {
            float next = Math.min(1, boss.bodyScale() + (1 - COMPRESSED_SCALE) / COMPRESSION_EXPAND);
            if (boss.bodyScale() < 1 && !boss.resize(next)) {
                moveHome();
                return;
            }
            if (!expansionCue) {
                sound("compression_release");
                expansionCue = true;
            }
            boss.halt();
        }
        if (++tick >= move.end() && boss.bodyScale() >= 1) finish();
    }

    private void aim(int arm) {
        Vec3D center = CustodianGeometry.center(target);
        double x = center.xCoord - boss.posX, z = center.zCoord - boss.posZ;
        double horizontal = Math.hypot(x, z);
        yaw = (float) Math.toDegrees(Math.atan2(-x, z)
                - Math.asin(Math.max(-0.9, Math.min(0.9, arm * SHOULDER / Math.max(0.01, horizontal)))));
        Vec3D origin = CustodianGeometry.shoulder(boss, arm, yaw);
        pitch = (float) -Math.toDegrees(Math.atan2(center.yCoord - origin.yCoord,
                Math.hypot(center.xCoord - origin.xCoord, center.zCoord - origin.zCoord)));
    }

    private void strike() {
        Vec3D origin = CustodianGeometry.shoulder(boss, side, yaw);
        double length = 0;
        int activeTick = tick - attack.windup;
        if (activeTick >= 0) {
            double clear = CustodianGeometry.clearReach(boss.worldObj, origin, yaw, pitch);
            if (activeTick < attack.active) length = clear * ease((activeTick + 1F) / attack.active);
            else length = clear * (1 - ease((activeTick - attack.active) / 12F));
        }
        boss.strikePose(side, length, yaw, pitch);
        if (activeTick < 0 || activeTick >= attack.active || struck || length <= 0) return;
        Vec3D end = CustodianGeometry.point(origin, yaw, pitch, length);
        EntityPlayer victim = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (Object object : boss.worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                boss.boundingBox.expand(STRIKE_REACH, STRIKE_REACH, STRIKE_REACH))) {
            EntityPlayer player = (EntityPlayer) object;
            if (!vulnerable(player)) continue;
            double hit = CustodianGeometry.hitDistance(player, origin, end);
            if (hit < nearest) {
                nearest = hit;
                victim = player;
            }
        }
        if (victim != null) {
            struck = true;
            victim.attackEntityFrom(DamageSource.causeMobDamage(boss), STRIKE_DAMAGE);
        }
    }

    private void pressure() {
        Vec3D origin = center();
        for (Object object : boss.worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                boss.boundingBox.expand(PRESSURE_RADIUS, PRESSURE_RADIUS, PRESSURE_RADIUS))) {
            EntityPlayer player = (EntityPlayer) object;
            if (!vulnerable(player)) continue;
            Vec3D at = CustodianGeometry.center(player);
            double distance = origin.distanceTo(at);
            if (distance >= PRESSURE_RADIUS || !CustodianGeometry.visible(boss.worldObj, origin, at)) continue;
            double vx = player.motionX, vy = player.motionY, vz = player.motionZ;
            player.attackEntityFrom(DamageSource.causeMobDamage(boss), PRESSURE_DAMAGE);
            if (!player.isEntityAlive()) continue;
            double force = PRESSURE_FORCE * (1 - distance / PRESSURE_RADIUS);
            double dx = at.xCoord - origin.xCoord, dz = at.zCoord - origin.zCoord;
            double horizontal = Math.max(0.2, Math.hypot(dx, dz));
            player.motionX = Math.max(-0.8, Math.min(0.8, vx + dx / horizontal * force));
            player.motionY = Math.max(-0.4, Math.min(0.4, vy + 0.22 * force));
            player.motionZ = Math.max(-0.8, Math.min(0.8, vz + dz / horizontal * force));
            player.velocityChanged = true;
        }
    }

    private Vec3D landing() {
        CustodianArena arena = boss.arena();
        for (int i = 0; i < BLINK_CANDIDATES; i++) {
            double angle = boss.worldObj.rand.nextDouble() * Math.PI * 2;
            double radius = BLINK_MIN + boss.worldObj.rand.nextDouble() * (BLINK_MAX - BLINK_MIN);
            double x = boss.posX + Math.cos(angle) * radius, z = boss.posZ + Math.sin(angle) * radius;
            double y = arena.yWithin(boss.boundingBox.minY + boss.worldObj.rand.nextInt(7) - 3, HEIGHT + 0.3);
            double span = boss.getDistance(x, y, z);
            if (span < BLINK_MIN || span > BLINK_MAX || target.getDistanceSq(x, y, z) < 3 * 3
                    || !arena.landing(boss, x, y, z)) continue;
            // This destination survives the wind-up and the legacy per-frame vector pool reset.
            return Vec3D.createVectorHelper(x, y, z);
        }
        return null;
    }

    private void teleport() {
        if (destination == null || !boss.arena().landing(boss, destination.xCoord, destination.yCoord, destination.zCoord))
            return;
        boss.blink(destination);
        destination = null;
        sound("arrival");
    }

    private List<EntityAbyssalEnderkin> responders() {
        List<EntityAbyssalEnderkin> result = new ArrayList<EntityAbyssalEnderkin>(RESONANCE_LIMIT);
        if (!boss.siteOwned() && !AquaticDevelopment.manual(boss)) return result;
        for (Object object : boss.worldObj.getEntitiesWithinAABB(EntityAbyssalEnderkin.class,
                boss.boundingBox.expand(22, 13, 22))) {
            EntityAbyssalEnderkin minion = (EntityAbyssalEnderkin) object;
            if (!minion.canResonate(boss.siteIdentity(), target, !boss.siteOwned())) continue;
            result.add(minion);
            if (result.size() >= RESONANCE_LIMIT) break;
        }
        return result;
    }

    private void resonance() {
        for (EntityAbyssalEnderkin minion : responders()) minion.resonate(target);
    }

    private Vec3D center() {
        return Vec3D.createVector(boss.posX, (boss.boundingBox.minY + boss.boundingBox.maxY) * 0.5, boss.posZ);
    }

    private void sound(String cue) { boss.worldObj.playSoundAtEntity(boss, EntityAbyssalCustodian.SOUND + cue, 0.82F, 1); }

    private static String cue(Attack value) {
        switch (value) {
            case STRIKE: return "strike";
            case PRESSURE: return "pressure";
            case BLINK: return "blink";
            case RESONANCE: return "resonance";
            case COMPRESSION: return "compression";
            default: throw new AssertionError(value);
        }
    }
}
