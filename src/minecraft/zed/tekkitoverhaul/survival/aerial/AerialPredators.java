package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.Item;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.ItemSurvivalSpawnEgg;

public final class AerialPredators {
    public static final String ENTITY_NAME = "ZTO.AerialPredator";
    public static final String SKELEBIRD_NAME = "ZTO.Skelebird";

    private AerialPredators() {}

    public static void register() {
        ModLoader.registerEntityID(EntityAerialPredator.class, ENTITY_NAME, ModLoader.getUniqueEntityId());
        ModLoader.addLocalization("entity." + ENTITY_NAME + ".name", "Fugl");
        ModLoader.registerEntityID(EntitySkelebird.class, SKELEBIRD_NAME, ModLoader.getUniqueEntityId());
        ModLoader.addLocalization("entity." + SKELEBIRD_NAME + ".name", "Skelebird");
        ModLoader.registerEntityID(EntityMagpie.class, "ZTO.Magpie", ModLoader.getUniqueEntityId());
        ModLoader.addLocalization("entity.ZTO.Magpie.name", "Magpie");
    }

    public static void registerEgg(int id) {
        registerEgg(id, ENTITY_NAME, "aerialPredatorEgg", "ztoAerialPredatorEgg", "Fugl", 0x235A56, 0xE6A761);
    }

    public static void registerSkelebirdEgg(int id) {
        registerEgg(id, SKELEBIRD_NAME, "skelebirdEgg", "ztoSkelebirdEgg", "Skelebird", 0x989898, 0x494949);
    }

    private static void registerEgg(int id, String entity, String setting, String key, String name, int shell, int spots) {
        if (id < 4096 - 256 || id >= Item.itemsList.length - 256 || Item.itemsList[id + 256] != null) {
            throw new IllegalStateException("ZTO: Invalid or occupied " + setting + " ID " + id
                    + " in config/ZedsTekkitOverhaul.cfg");
        }
        Item egg = new ItemSurvivalSpawnEgg(id, entity, key, shell, spots) {
            @Override
            protected boolean isSpawnClear(EntityLiving mob, World world) { return mob.getCanSpawnHere(); }
        };
        ModLoader.addName(egg, name + " Spawn Egg");
    }
}
