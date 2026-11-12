package zed.tekkitoverhaul.devbridge;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import zed.tekkitoverhaul.equipment.MobEquipment;
import zed.tekkitoverhaul.equipment.MobEquipment.Slot;

/** One opt-in viewing pen in an explicitly marked disposable save. */
public final class MobEquipmentFixture {
    private static final String SAVE = System.getProperty("zto.mobEquipmentFixture", "");
    private static boolean attempted;

    private MobEquipmentFixture() {}

    public static void tick(Minecraft game) {
        if (SAVE.length() == 0 || attempted || game.thePlayer == null || game.isMultiplayerWorld()) return;
        World world = game.theWorld;
        if (world == null || world.isRemote || world.worldProvider.worldType != 0 || world.difficultySetting == 0
                || !SAVE.equals(world.getSaveHandler().getSaveDirectoryName())) return;

        // Stay inside the player's loaded chunk. Build above its highest occupied block.
        int x = (MathHelper.floor_double(game.thePlayer.posX) & ~15) + 2;
        int z = (MathHelper.floor_double(game.thePlayer.posZ) & ~15) + 4;
        if (!world.blockExists(x, 64, z)) return;
        attempted = true;
        if (!HopperTestFixture.markedSave(game.mcDataDir, SAVE, world.getWorldInfo().getSeed())) {
            System.out.println("[ZTO] Mob equipment fixture refused an unmarked save");
            return;
        }
        int y = 80;
        for (int dx = 0; dx <= 11; dx++) {
            for (int dz = 0; dz <= 7; dz++) {
                y = Math.max(y, world.getHeightValue(x + dx, z + dz) + 2);
            }
        }
        if (y > 249) {
            System.out.println("[ZTO] Mob equipment fixture has insufficient space above the player chunk");
            return;
        }
        for (int dx = 0; dx <= 11; dx++) {
            for (int dz = 0; dz <= 7; dz++) {
                world.setBlockWithNotify(x + dx, y - 1, z + dz, Block.stone.blockID);
                Block ceiling = dz == 2 && dx % 3 == 1 ? Block.glowStone : Block.stone;
                world.setBlockWithNotify(x + dx, y + 3, z + dz, ceiling.blockID);
                for (int dy = 0; dy < 3; dy++) {
                    if (dx == 0 || dx == 11 || dz == 0 || dz == 3 || dz == 7
                            || (dz < 3 && (dx == 3 || dx == 6 || dx == 9))) {
                        world.setBlockWithNotify(x + dx, y + dy, z + dz, Block.glass.blockID);
                    }
                }
            }
        }
        world.setBlockWithNotify(x + 5, y, z + 6, Block.torchWood.blockID);

        EntityZombie zombie = new EntityZombie(world);
        EntitySkeleton skeleton = new EntitySkeleton(world);
        ItemStack sword = new ItemStack(Item.swordSteel, 1, 7);
        sword.setTagCompound(new NBTTagCompound());
        sword.getTagCompound().setString("ZtoFixture", "mob-equipment");
        equip(zombie, Slot.MAIN_HAND, sword);
        equip(zombie, Slot.HEAD, new ItemStack(Item.helmetGold));
        equip(zombie, Slot.CHEST, new ItemStack(Item.plateDiamond));
        equip(zombie, Slot.LEGS, new ItemStack(Item.legsChain));
        equip(zombie, Slot.FEET, new ItemStack(Item.bootsSteel));
        equip(skeleton, Slot.HEAD, new ItemStack(Item.helmetDiamond));
        equip(skeleton, Slot.CHEST, new ItemStack(Item.plateSteel));
        equip(skeleton, Slot.LEGS, new ItemStack(Item.legsGold));
        equip(skeleton, Slot.FEET, new ItemStack(Item.bootsLeather));

        spawn(world, zombie, x + 4.5, y, z + 1.5);
        spawn(world, skeleton, x + 7.5, y, z + 1.5);
        spawn(world, new EntityZombie(world), x + 1.5, y, z + 1.5);
        spawn(world, new EntitySkeleton(world), x + 10.5, y, z + 1.5);
        game.thePlayer.setPositionAndRotation(x + 6.0, y + game.thePlayer.yOffset, z + 6.5, 180, 0);
        System.out.println("[ZTO] Mob equipment fixture ready at " + x + ", " + y + ", " + z);
    }

    private static void equip(EntityLiving mob, Slot slot, ItemStack stack) {
        if (!MobEquipment.assign(mob, slot, stack)) throw new IllegalStateException("Fixture equipment rejected");
    }

    private static void spawn(World world, EntityLiving mob, double x, double y, double z) {
        mob.setLocationAndAngles(x, y, z, 0, 0);
        if (!world.spawnEntityInWorld(mob)) throw new IllegalStateException("Fixture entity rejected");
    }
}
