package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.Item;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.ItemSurvivalSpawnEgg;

public final class AerialPredators {
    public static final String ENTITY_NAME = "ZTO.AerialPredator";

    private AerialPredators() {}

    public static void register() {
        ModLoader.registerEntityID(EntityAerialPredator.class, ENTITY_NAME, ModLoader.getUniqueEntityId());
    }

    public static void registerEgg(int id) {
        if (id < 4096 - 256 || id >= Item.itemsList.length - 256 || Item.itemsList[id + 256] != null) {
            throw new IllegalStateException("ZTO: Invalid or occupied aerialPredatorEgg ID " + id
                    + " in config/ZedsTekkitOverhaul.cfg");
        }
        Item egg = new ItemSurvivalSpawnEgg(id, ENTITY_NAME, "ztoAerialPredatorEgg", 0x235A56, 0xE6A761) {
            @Override
            protected boolean isSpawnClear(EntityLiving mob, World world) { return mob.getCanSpawnHere(); }
        };
        ModLoader.addName(egg, "Aerial Predator Spawn Egg");
    }
}
