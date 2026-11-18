package zed.tekkitoverhaul.survival.aerial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.World;

/** Local lifetime and turn-taking for one admitted group, including solitary wildlife. */
final class AerialEncounter {
    static final int HUNT_LIMIT = 660;
    private static final int ATTACK_GAP = 64;
    final EntityPlayer player;
    final boolean airborne, hunting;
    final double approachX, approachZ;
    private final List<EntityAerialPredator> members = new ArrayList<EntityAerialPredator>(5);
    private EntityAerialPredator attacker;
    private int gap, age;
    private boolean ended;

    AerialEncounter(EntityPlayer player, boolean airborne, boolean hunting, double approachX, double approachZ) {
        this.player = player;
        this.airborne = airborne;
        this.hunting = hunting;
        this.approachX = approachX;
        this.approachZ = approachZ;
    }

    void add(EntityAerialPredator mob, int lane) {
        members.add(mob);
        mob.joinEncounter(this, lane);
    }

    boolean reserveDive(EntityAerialPredator mob) {
        if (attacker != null || gap > 0 || ended) return false;
        attacker = mob;
        return true;
    }

    void releaseDive(EntityAerialPredator mob) {
        if (attacker != mob) return;
        attacker = null;
        gap = ATTACK_GAP;
    }

    void tick() {
        ++age;
        if (gap > 0) --gap;
    }

    String sample(World world, EntityPlayer current, AerialExposure exposure) {
        if (current != player || !current.isEntityAlive() || current.capabilities.disableDamage
                || current.capabilities.isCreativeMode || current.isPlayerSleeping()) return "Player unavailable";
        if (exposure.band == AerialExposure.Band.COVERED) return exposure.restriction;
        if (airborne && exposure.height < 5) return "Player descended";
        if (age >= (hunting ? HUNT_LIMIT : 700)) return "Encounter time limit";
        for (Iterator<EntityAerialPredator> it = members.iterator(); it.hasNext();) {
            EntityAerialPredator mob = it.next();
            if (mob.belongsTo(this) && mob.isEntityAlive() && world.loadedEntityList.contains(mob)
                    && player.getDistanceSqToEntity(mob) <= 56 * 56) continue;
            releaseDive(mob);
            if (mob.belongsTo(this)) mob.leaveEncounter("Group member lost or out of range");
            it.remove();
        }
        return members.isEmpty() ? "Encounter finished" : "";
    }

    void finish(String reason) {
        ended = true;
        for (EntityAerialPredator member : members) member.leaveEncounter(reason);
        members.clear();
        attacker = null;
    }

    int size() { return members.size(); }
    int attackerId() { return attacker == null ? -1 : attacker.entityId; }
    int gapTicks() { return gap; }

    String membership() {
        StringBuilder result = new StringBuilder();
        for (EntityAerialPredator mob : members) {
            if (result.length() > 0) result.append(", ");
            result.append('#').append(mob.entityId);
        }
        return result.toString();
    }
}
