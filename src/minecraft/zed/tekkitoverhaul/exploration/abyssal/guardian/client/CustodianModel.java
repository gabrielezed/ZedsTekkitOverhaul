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
    private final ModelRenderer[] tentacle = new ModelRenderer[4], joint = new ModelRenderer[4], tip = new ModelRenderer[4];
    private float partial;

    CustodianModel(boolean eyes) {
        this.eyes = eyes;
        textureWidth = 128;
        textureHeight = 64;
        head = box(0, 0, -7, -10, -6, 14, 10, 12);
        ModelRenderer gillLeft = box(56, 0, -9, -6, -2, 2, 5, 5);
        ModelRenderer gillRight = box(70, 0, 7, -6, -2, 2, 5, 5);
        head.addChild(gillLeft);
        head.addChild(gillRight);
        neck = box(84, 0, -3, -3, -3, 6, 4, 6);
        torso = box(0, 24, -6, 0, -4, 12, 13, 8);
        hips = box(42, 25, -5, 0, -4, 10, 5, 8);
        for (int i = 0; i < 2; i++) {
            upper[i] = box(80, 18, -2, 0, -2, 4, 8, 4);
            fore[i] = box(96, 18, -1.5F, 0, -1.5F, 3, 8, 3);
            hands[i] = box(110, 18, -1.5F, 0, -1.5F, 3, 3, 3);
        }
        for (int i = 0; i < 4; i++) {
            tentacle[i] = box(0, 47, -2, 0, -2, 4, 6, 4);
            joint[i] = box(18, 47, -1.5F, 0, -1.5F, 3, 6, 3);
            tip[i] = box(32, 47, -1, 0, -1, 2, 6, 2);
            hips.addChild(tentacle[i]);
            tentacle[i].addChild(joint[i]);
            joint[i].addChild(tip[i]);
            tentacle[i].setRotationPoint((i % 2 == 0 ? -1 : 1) * 3.7F, 3, (i < 2 ? -1 : 1) * 2.8F);
            joint[i].setRotationPoint(0, 5.6F, 0);
            tip[i].setRotationPoint(0, 5.6F, 0);
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
        float contraction = state == EntityAbyssalCustodian.DORMANT ? 0.53F + 0.10F * wave : 0;
        float tall = 0, spread = 0, lift = 0, lunge = 0, recovery = 0;
        float torsoTop = -6;
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
                contraction = tick < attack.windup ? windup : 0;
                spread = tick >= attack.windup ? recovery : 0;
            } else if (attack == Attack.BLINK) {
                contraction = windup * recovery;
                tall = tick >= attack.windup ? (1 - recovery) * CustodianBalance.ease((attack.end() - tick) / 7) : 0;
            } else {
                lift = spread = windup * recovery;
                tall = 0.35F * lift;
            }
        }
        if (boss.deathTime > 0) {
            contraction = CustodianBalance.ease(boss.deathTime / 17F);
            tall = spread = lift = lunge = 0;
        }
        // The pelvis anchors the lower limbs. Torso and shoulders shift independently.
        float high = boss.highPose() ? tall : tall * 0.19F;
        torsoTop += 13 * contraction - 17 * high;
        float pelvis = 7 + 3 * contraction + high;
        float lean = state == EntityAbyssalCustodian.SWIM
                ? (float) Math.max(-0.25, Math.min(0.25, (boss.lastTickPosY - boss.posY) * 2)) : 0;
        GL11.glPushMatrix();
        GL11.glTranslatef(0, pelvis * scale, 0);
        GL11.glRotatef((float) Math.toDegrees(lean), 1, 0, 0);
        GL11.glTranslatef(0, -pelvis * scale, 0);
        head.setRotationPoint(0, torsoTop - 4 - high, 0);
        head.rotateAngleY = attack == Attack.STRIKE ? 0 : Math.max(-0.48F, Math.min(0.48F, yaw * 0.01745F));
        head.rotateAngleX = Math.max(-0.42F, Math.min(0.42F, pitch * 0.011F));
        head.render(scale);
        if (!eyes) {
            scaled(neck, scale, torsoTop - 2, 1 + 0.5F * high);
            scaled(torso, scale, torsoTop, (pelvis - torsoTop) / 13);
            hips.setRotationPoint(0, pelvis, 0);
            for (int i = 0; i < 4; i++) {
                float sign = i % 2 == 0 ? -1 : 1;
                float end = i < 2 ? -1 : 1;
                float drift = (float) Math.sin(age * 0.105F + i * 0.8F);
                tentacle[i].rotateAngleX = end * (-0.30F - 0.52F * spread) + 0.16F * drift;
                tentacle[i].rotateAngleZ = sign * (-0.45F + contraction * 0.53F - spread * 0.55F);
                joint[i].rotateAngleX = -0.20F + drift * 0.21F + 1.15F * contraction;
                joint[i].rotateAngleZ = sign * (0.26F + contraction * 0.6F);
                tip[i].rotateAngleX = -0.15F + wave * 0.25F;
            }
            hips.render(scale);
            for (int index = 0; index < 2; index++) {
                int side = index == 0 ? -1 : 1;
                boolean chosen = attack == Attack.STRIKE && side == boss.strikingArm() && boss.deathTime == 0;
                float reach = chosen ? boss.strikeReach() : 0;
                float armY = chosen && tick >= Attack.STRIKE.windup ? -6 : torsoTop;
                float armX = side * 8;
                float angle = -0.15F + wave * 0.10F - lift * 2.45F - contraction * 0.65F;
                float out = -side * (0.36F + 0.44F * spread - 0.20F * contraction);
                float bend = -0.28F - 0.58F * contraction;
                if (chosen && tick < Attack.STRIKE.windup) {
                    angle += lunge * 1.24F;
                    bend -= lunge * 0.8F;
                }
                float units = 19;
                if (chosen && tick >= Attack.STRIKE.windup) {
                    float recoil = CustodianBalance.ease((tick - Attack.STRIKE.windup - Attack.STRIKE.active - 4) / 10);
                    float locked = -1.5707963F + boss.strikePitch() * 0.017453292F;
                    angle = locked + (angle - locked) * recoil;
                    out *= recoil;
                    bend *= recoil;
                    units = Math.max(0.05F, reach * 16 + 19 * recoil);
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
        float first = length * (0.42F - 0.06F * Math.max(0, Math.min(1, (length - 19) / 45)));
        float second = length - first - Math.min(3, length * 0.16F);
        GL11.glPushMatrix();
        GL11.glTranslatef(x * pixel, y * pixel, 0);
        GL11.glRotatef((float) Math.toDegrees(flare), 0, 0, 1);
        GL11.glRotatef((float) Math.toDegrees(pitch), 1, 0, 0);
        scaled(upper[index], pixel, 0, first / 8);
        GL11.glTranslatef(0, first * pixel, 0);
        GL11.glRotatef((float) Math.toDegrees(bend), 1, 0, 0);
        scaled(fore[index], pixel, 0, second / 8);
        scaled(hands[index], pixel, second, (length - first - second) / 3);
        GL11.glPopMatrix();
    }
}
