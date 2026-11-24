package zed.tekkitoverhaul.exploration.motorboat;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumMovingObjectType;
import net.minecraft.src.ItemBoat;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

final class ItemMotorboat extends ItemBoat {
    ItemMotorboat(int id) {
        super(id);
        setItemName("ztoMotorboat");
        setIconIndex(boat.getIconFromDamage(0));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || stack.stackSize != 1 || player.ridingEntity != null) return stack;
        Vec3D eye = Vec3D.createVector(player.posX, player.posY + 1.62D - player.yOffset, player.posZ);
        Vec3D look = player.getLook(1F);
        // The legacy block ray trace advances the vector it receives as its start.
        MovingObjectPosition hit = world.rayTraceBlocks_do(eye.addVector(0D, 0D, 0D),
                eye.addVector(look.xCoord * 5D, look.yCoord * 5D, look.zCoord * 5D), true);
        if (hit == null || hit.typeOfHit != EnumMovingObjectType.TILE
                || world.getBlockMaterial(hit.blockX, hit.blockY, hit.blockZ) != Material.water) return stack;
        AxisAlignedBB reach = player.boundingBox.addCoord(hit.hitVec.xCoord - eye.xCoord,
                hit.hitVec.yCoord - eye.yCoord, hit.hitVec.zCoord - eye.zCoord).expand(1D, 1D, 1D);
        for (Object candidate : world.getEntitiesWithinAABBExcludingEntity(player, reach)) {
            Entity entity = (Entity) candidate;
            if (!entity.canBeCollidedWith()) continue;
            float border = entity.getCollisionBorderSize();
            AxisAlignedBB bounds = entity.boundingBox.expand(border, border, border);
            if (bounds.isVecInside(eye) || bounds.calculateIntercept(eye, hit.hitVec) != null) return stack;
        }

        EntityMotorboat boat = new EntityMotorboat(world, hit.blockX + 0.5D, hit.blockY + 1D, hit.blockZ + 0.5D);
        boat.rotationYaw = boat.prevRotationYaw = player.rotationYaw - 90F;
        if (!world.getCollidingBoundingBoxes(boat, boat.boundingBox.contract(0.02D, 0D, 0.02D)).isEmpty()) return stack;
        boat.readTank(stack.getTagCompound());
        if (world.spawnEntityInWorld(boat) && (!player.capabilities.isCreativeMode || boat.fuel() > 0)) {
            --stack.stackSize;
        }
        return stack;
    }
}
