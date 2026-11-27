package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.Attack;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalCustodian;

final class CustodianModel extends ModelBase {
    private final boolean eyes;
    private final ModelRenderer head, neck, torso, hips;
    private final ModelRenderer[] upper = new ModelRenderer[2], fore = new ModelRenderer[2], hands = new ModelRenderer[2];
    private final ModelRenderer[] tentacle = new ModelRenderer[4], joint = new ModelRenderer[4],
            taper = new ModelRenderer[4], tip = new ModelRenderer[4];
    private float partial;

    CustodianModel(boolean eyes) {
        this.eyes = eyes;
        textureWidth = textureHeight = 128;
        head = box(0, 0, -8, -15, -7, 16, 13, 14);
        ModelRenderer gillLeft = box(62, 0, -10, -9, -2, 2, 7, 6);
        ModelRenderer gillRight = box(79, 0, 8, -9, -2, 2, 7, 6);
        head.addChild(gillLeft);
        head.addChild(gillRight);
        neck = box(96, 0, -3, -7, -3, 6, 8, 6);
        torso = box(0, 29, -9, 0, -5, 18, 22, 10);
        hips = box(58, 29, -6, 0, -5, 12, 7, 10);
        for (int i = 0; i < 2; i++) {
            upper[i] = box(0, 65, -2.5F, 0, -2.5F, 5, 13, 5);
            fore[i] = box(22, 65, -2, 0, -2, 4, 12, 4);
            hands[i] = box(40, 65, -1.5F, 0, -1.5F, 3, 5, 3);
        }
        for (int i = 0; i < 4; i++) {
            tentacle[i] = box(0, 90, -3, 0, -2.5F, 6, 12, 5);
            joint[i] = box(23, 90, -2, 0, -2, 4, 11, 4);
            taper[i] = box(41, 90, -1.5F, 0, -1.5F, 3, 10, 3);
            tip[i] = box(55, 90, -1, 0, -1, 2, 9, 2);
            hips.addChild(tentacle[i]);
            tentacle[i].addChild(joint[i]);
            joint[i].addChild(taper[i]);
            taper[i].addChild(tip[i]);
            tentacle[i].setRotationPoint((i % 2 == 0 ? -1 : 1) * (i < 2 ? 4.3F : 5.8F), 4, (i < 2 ? -1 : 1) * 4.4F);
            joint[i].setRotationPoint(0, 11.3F, 0);
            taper[i].setRotationPoint(0, 10.5F, 0);
            tip[i].setRotationPoint(0, 9.5F, 0);
        }
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int width, int height, int depth) {
        return new ModelRenderer(this, u, v).addBox(x, y, z, width, height, depth);
    }

    @Override public void setLivingAnimations(net.minecraft.src.EntityLiving actor, float stride, float swing, float partial) {
        this.partial = partial;
    }

    @Override public void render(Entity actor, float stride, float swing, float age, float yaw, float pitch, float scale) {
        EntityAbyssalCustodian boss = (EntityAbyssalCustodian) actor;
        int state = boss.pose();
        float tick = boss.animationTick(partial);
        float wave = (float) Math.sin(age * 0.095);
        float contraction = state == EntityAbyssalCustodian.DORMANT ? 0.24F + 0.08F * wave : 0;
        float tall = 0, spread = 0, lift = 0, lunge = 0, recovery = 0;
        float torsoTop = -30;
        Attack attack = state >= EntityAbyssalCustodian.ATTACK ? Attack.values()[state - EntityAbyssalCustodian.ATTACK] : null;
        if (state == EntityAbyssalCustodian.AWAKEN || state == EntityAbyssalCustodian.TRANSITION) {
            int length = state == EntityAbyssalCustodian.AWAKEN ? CustodianBalance.AWAKEN : CustodianBalance.TRANSITION;
            tall = CustodianBalance.ease(tick / 8) * CustodianBalance.ease((length - tick) / 8);
            spread = tall;
            lift = tall * 0.6F;
        }
        if (attack != null) {
            float windup = CustodianBalance.ease(tick / attack.windup);
            recovery = 1 - CustodianBalance.ease((tick - attack.windup - attack.active) / 15);
            if (attack == Attack.STRIKE) {
                lunge = windup * recovery;
                if (tick < attack.windup) torsoTop += 1.4F * windup;
            } else if (attack == Attack.PRESSURE) {
                float discharge = CustodianBalance.ease((tick - attack.windup + 1) / 4);
                contraction = windup * (1 - discharge);
                spread = discharge * recovery;
            } else if (attack == Attack.BLINK) {
                float arrival = CustodianBalance.ease((tick - attack.windup) / 7);
                contraction = windup * (1 - arrival);
                tall = arrival * CustodianBalance.ease((attack.end() - tick) / 9);
            } else if (attack == Attack.COMPRESSION) {
                contraction = 0.45F * (1 - boss.bodyScale()) / (1 - CustodianBalance.COMPRESSED_SCALE);
            } else {
                lift = spread = windup * recovery;
                tall = 0.35F * lift;
            }
        }
        if (boss.deathTime > 0) {
            contraction = CustodianBalance.ease(boss.deathTime / 17F);
            tall = spread = lift = lunge = 0;
        }
        float flinch = AquaticHurtPose.recoil(boss, partial, 0);
        float trailFlinch = AquaticHurtPose.recoil(boss, partial, 2);
        float lateral = AquaticHurtPose.lateral(boss);
        float backward = AquaticHurtPose.backward(boss);
        boolean committed = attack == Attack.STRIKE && tick >= Attack.STRIKE.windup - 6;
        // The pelvis anchors the lower limbs. Torso and shoulders shift independently.
        float high = boss.highPose() ? tall : tall * 0.19F;
        torsoTop += 12 * contraction - 17 * high;
        float pelvis = -8 + 3 * contraction - 2 * high;
        float lean = state == EntityAbyssalCustodian.SWIM || attack == Attack.COMPRESSION
                ? (float) Math.max(-0.25, Math.min(0.25, (boss.lastTickPosY - boss.posY) * 2)) : 0;
        GL11.glPushMatrix();
        float size = boss.animatedScale(partial);
        GL11.glTranslatef(0, 24 * scale, 0);
        GL11.glScalef(size, size, size);
        GL11.glTranslatef(0, -24 * scale, 0);
        GL11.glTranslatef(0, pelvis * scale, 0);
        // Preserve the world-aligned strike rig from commitment through recoil.
        GL11.glRotatef((float) Math.toDegrees(lean) + (committed ? 0 : 12 * backward * flinch), 1, 0, 0);
        if (!committed) GL11.glRotatef(-12 * lateral * flinch, 0, 0, 1);
        GL11.glTranslatef(0, -pelvis * scale, 0);
        head.setRotationPoint(0, torsoTop - 5 - high, 0);
        head.rotateAngleY = attack == Attack.STRIKE ? 0 : boss.gaze() * 0.017453292F;
        head.rotateAngleX = Math.max(-0.42F, Math.min(0.42F, pitch * 0.011F - lean))
                - (0.14F + 0.32F * backward) * flinch;
        head.rotateAngleZ = -0.24F * lateral * flinch;
        if (attack != null && attack != Attack.STRIKE)
            head.rotateAngleY *= 0.6F;
        head.render(scale);
        if (!eyes) {
            scaled(neck, scale, torsoTop - 1, 1 + 0.25F * high);
            scaled(torso, scale, torsoTop, (pelvis - torsoTop) / 22);
            hips.setRotationPoint(0, pelvis, 0);
            for (int i = 0; i < 4; i++) {
                float sign = i % 2 == 0 ? -1 : 1;
                float end = i < 2 ? -1 : 1;
                float phase = age * 0.105F + i * 0.8F;
                float drift = (float) Math.sin(phase);
                float middle = (float) Math.sin(phase - 0.65F);
                float distal = (float) Math.sin(phase - 1.3F);
                float trail = lean * 0.55F;
                tentacle[i].rotateAngleX = end * (-0.42F - 0.35F * spread) + 0.14F * drift + trail;
                tentacle[i].rotateAngleZ = sign * ((i < 2 ? -0.48F : -0.81F) + contraction * 0.55F - spread * 0.58F);
                joint[i].rotateAngleX = end * (0.20F - contraction * 0.72F) + middle * 0.24F
                        + (0.32F * backward - 0.15F * end) * flinch;
                joint[i].rotateAngleZ = sign * (-0.06F + contraction * 0.85F);
                taper[i].rotateAngleX = end * (0.23F - contraction * 0.83F) + distal * 0.22F;
                taper[i].rotateAngleZ = sign * (0.47F + contraction * 0.48F + 0.48F * trailFlinch);
                tip[i].rotateAngleX = end * 0.26F + 0.26F * (float) Math.sin(phase - 1.95F) + (end * 0.24F + 0.32F * backward) * trailFlinch;
                tip[i].rotateAngleZ = sign * (0.21F - 0.19F * wave);
            }
            hips.render(scale);
            for (int index = 0; index < 2; index++) {
                int side = index == 0 ? -1 : 1;
                boolean chosen = attack == Attack.STRIKE && side == boss.strikingArm() && boss.deathTime == 0;
                float reach = chosen ? boss.strikeReach() : 0;
                float armY = chosen && tick >= Attack.STRIKE.windup ? (float) (24 - CustodianBalance.SHOULDER_Y * 16) : torsoTop;
                float armX = side * (float) CustodianBalance.SHOULDER * 16;
                float angle = -0.15F + wave * 0.10F - lift * 2.45F - contraction * 0.65F;
                float out = -side * (0.36F + 0.44F * spread - 0.20F * contraction);
                float bend = -0.28F - 0.58F * contraction;
                if (chosen && tick < Attack.STRIKE.windup) {
                    angle += lunge * 1.24F;
                    bend -= lunge * 0.8F;
                }
                float units = 30;
                if (chosen && tick >= Attack.STRIKE.windup) {
                    float recoil = CustodianBalance.ease((tick - Attack.STRIKE.windup - Attack.STRIKE.active - 4) / 10);
                    float locked = -1.5707963F + boss.strikePitch() * 0.017453292F;
                    angle = locked + (angle - locked) * recoil;
                    out *= recoil;
                    bend *= recoil;
                    units = Math.max(0.05F, reach * 16 + 30 * recoil);
                }
                if (!chosen || !committed) {
                    angle -= (0.28F + side * 0.25F * lateral) * flinch;
                    bend -= 0.73F * flinch;
                }
                arm(index, scale, armX, armY, angle, out, bend, units);
            }
        }
        GL11.glPopMatrix();
    }

    private void scaled(ModelRenderer part, float pixel, float y, float stretch) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, y * pixel, 0);
        GL11.glScalef(1, stretch, 1);
        part.render(pixel);
        GL11.glPopMatrix();
    }

    private void arm(int index, float pixel, float x, float y, float pitch, float flare, float bend, float length) {
        float first = length * (0.42F - 0.06F * Math.max(0, Math.min(1, (length - 30) / 48)));
        float second = length - first - Math.min(5, length * 0.16F);
        GL11.glPushMatrix();
        GL11.glTranslatef(x * pixel, y * pixel, 0);
        GL11.glRotatef((float) Math.toDegrees(flare), 0, 0, 1);
        GL11.glRotatef((float) Math.toDegrees(pitch), 1, 0, 0);
        scaled(upper[index], pixel, 0, first / 13);
        GL11.glTranslatef(0, first * pixel, 0);
        GL11.glRotatef((float) Math.toDegrees(bend), 1, 0, 0);
        scaled(fore[index], pixel, 0, second / 12);
        scaled(hands[index], pixel, second, (length - first - second) / 5);
        GL11.glPopMatrix();
    }
}
