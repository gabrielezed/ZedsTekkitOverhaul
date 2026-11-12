package zed.tekkitoverhaul.equipment;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModelBiped;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.ModelSkeleton;
import net.minecraft.src.ModelZombie;
import net.minecraft.src.Render;
import net.minecraft.src.RenderBiped;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.equipment.MobEquipment.Slot;

public final class RenderMobEquipment extends RenderBiped {
    private static final String[] TEXTURES = {"cloth", "chain", "iron", "diamond", "gold"};
    private static final Slot[] ARMOR = {Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET};

    private final Render original;
    private final ModelBiped outerArmor;
    private final ModelBiped leggings;

    private RenderMobEquipment(Render original, ModelBiped body) {
        super(body, 0.5F);
        this.original = original;
        outerArmor = new ArmorModel(1.0F, body);
        leggings = new ArmorModel(0.5F, body);
    }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        register(renderers, EntityZombie.class, new ModelZombie());
        register(renderers, EntitySkeleton.class, new ModelSkeleton());
    }

    private static void register(Map<Class<? extends Entity>, Render> renderers,
                                 Class<? extends EntityLiving> type, ModelBiped model) {
        Render previous = renderers.get(type);
        if (previous != null && previous.getClass() == RenderBiped.class) {
            renderers.put(type, new RenderMobEquipment(previous, model));
        } else {
            System.out.println("[ZTO] Skipping mob equipment renderer because the stock biped renderer was not found");
        }
    }

    @Override
    public void doRenderLiving(EntityLiving mob, double x, double y, double z, float yaw, float partialTick) {
        if (MobEquipment.hasEquipment(mob)) {
            super.doRenderLiving(mob, x, y, z, yaw, partialTick);
        } else {
            original.doRender(mob, x, y, z, yaw, partialTick);
        }
    }

    @Override
    protected int shouldRenderPass(EntityLiving mob, int pass, float partialTick) {
        ItemStack stack = MobEquipment.get(mob, ARMOR[pass]);
        if (stack == null) return -1;

        ItemArmor item = (ItemArmor) stack.getItem();
        loadTexture("/armor/" + TEXTURES[item.renderIndex] + (pass == 2 ? "_2.png" : "_1.png"));
        ModelBiped model = pass == 2 ? leggings : outerArmor;
        model.bipedHead.showModel = pass == 0;
        model.bipedHeadwear.showModel = pass == 0;
        model.bipedBody.showModel = pass == 1 || pass == 2;
        model.bipedRightArm.showModel = model.bipedLeftArm.showModel = pass == 1;
        model.bipedRightLeg.showModel = model.bipedLeftLeg.showModel = pass >= 2;
        setRenderPassModel(model);
        return 1;
    }

    @Override
    protected void renderEquippedItems(EntityLiving mob, float partialTick) {
        super.renderEquippedItems(mob, partialTick);
        if (mob.getClass() != EntityZombie.class) return;
        ItemStack sword = MobEquipment.get(mob, Slot.MAIN_HAND);
        if (sword == null) return;

        // Match RenderBiped's full-3D item transform on the animated right arm.
        GL11.glPushMatrix();
        try {
            modelBipedMain.bipedRightArm.postRender(0.0625F);
            GL11.glTranslatef(-0.0625F, 0.625F, 0.0625F);
            GL11.glScalef(0.625F, -0.625F, 0.625F);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            renderManager.itemRenderer.renderItem(mob, sword, 0);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static final class ArmorModel extends ModelBiped {
        private final ModelBiped body;

        ArmorModel(float scale, ModelBiped body) {
            super(scale);
            this.body = body;
        }

        @Override
        public void setRotationAngles(float swing, float amount, float age, float yaw, float pitch, float scale) {
            copyPose(body.bipedHead, bipedHead);
            copyPose(body.bipedHeadwear, bipedHeadwear);
            copyPose(body.bipedBody, bipedBody);
            copyPose(body.bipedRightArm, bipedRightArm);
            copyPose(body.bipedLeftArm, bipedLeftArm);
            copyPose(body.bipedRightLeg, bipedRightLeg);
            copyPose(body.bipedLeftLeg, bipedLeftLeg);
        }

        private static void copyPose(ModelRenderer source, ModelRenderer target) {
            target.setRotationPoint(source.rotationPointX, source.rotationPointY, source.rotationPointZ);
            target.rotateAngleX = source.rotateAngleX;
            target.rotateAngleY = source.rotateAngleY;
            target.rotateAngleZ = source.rotateAngleZ;
        }
    }
}
