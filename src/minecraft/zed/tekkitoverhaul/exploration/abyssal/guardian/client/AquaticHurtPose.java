package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;

/** A client pose layered over the native ten-tick damage reaction. */
final class AquaticHurtPose {
    private AquaticHurtPose() {}

    static float recoil(EntityLiving actor, float partial, float delay) {
        if (actor.hurtTime <= 0 || actor.deathTime > 0) return 0;
        int duration = Math.max(10, actor.maxHurtTime);
        float elapsed = duration - actor.hurtTime + partial - delay;
        if (elapsed < 0) return 0;
        float progress = Math.min(1, elapsed / (duration - delay));
        float snap = 0.45F + 0.55F * Math.min(1, elapsed * 0.6F);
        return snap * (1 - progress * progress * (3 - 2 * progress));
    }

    static float lateral(EntityLiving actor) { return MathHelper.sin(actor.attackedAtYaw * 0.017453292F); }
    static float backward(EntityLiving actor) { return MathHelper.cos(actor.attackedAtYaw * 0.017453292F); }
}
