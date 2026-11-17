package zed.tekkitoverhaul.survival.tactical;

import java.util.Set;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkCoordIntPair;
import net.minecraft.src.Entity;
import net.minecraft.src.Item;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.config.ZtoConfig;
import net.minecraft.src.forge.IChunkLoadHandler;
import net.minecraft.src.forge.MinecraftForge;

public final class TacticalHostiles {
    public static final String SKIRMISHER = "ZTO.Skirmisher";
    public static final String BREACHER = "ZTO.Breacher";
    public static final String SABOTEUR = "ZTO.Saboteur";
    public static final String TANK = "ZTO.Tank";
    public static final String THIEF = "ZTO.Thief";

    private TacticalHostiles() {}

    public static void register() {
        IndustrialSabotage.register();
        ModLoader.registerEntityID(EntitySkirmisher.class, SKIRMISHER, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityBreacher.class, BREACHER, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityEnderPulse.class, "ZTO.EnderPulse", ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntitySaboteur.class, SABOTEUR, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityTank.class, TANK, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityThief.class, THIEF, ModLoader.getUniqueEntityId());
        MinecraftForge.registerChunkLoadHandler(new IChunkLoadHandler() {
            @Override
            public void addActiveChunks(World world, Set<ChunkCoordIntPair> chunks) {}

            @Override
            public boolean canUnloadChunk(Chunk chunk) { return true; }

            @Override
            public boolean canUpdateEntity(Entity entity) {
                // Let the pulse retire if the 32-block vanilla update neighborhood is incomplete.
                return entity instanceof EntityEnderPulse;
            }
        });
    }

    public static void registerEggs(ZtoConfig config) {
        int skirmisher = config.skirmisherEgg;
        int breacher = config.breacherEgg;
        int saboteur = config.saboteurEgg;
        int[] ids = {skirmisher, breacher, saboteur, config.tankEgg, config.thiefEgg};
        for (int i = 0; i < ids.length; i++) {
            checkItemSlot(ids[i]);
            for (int j = 0; j < i; j++) {
                if (ids[i] == ids[j]) throw new IllegalStateException("ZTO: Duplicate tactical spawn egg item ID " + ids[i]);
            }
        }
        ModLoader.addName(new ItemTacticalSpawnEgg(skirmisher, SKIRMISHER, "ztoSkirmisherEgg", 0x17171C, 0x535170),
                "Skirmisher Spawn Egg");
        ModLoader.addName(new ItemTacticalSpawnEgg(breacher, BREACHER, "ztoBreacherEgg", 0x1C171B, 0x785469),
                "Breacher Spawn Egg");
        ModLoader.addName(new ItemTacticalSpawnEgg(saboteur, SABOTEUR, "ztoSaboteurEgg", 0x211A32, 0x65D9C0),
                "Saboteur Spawn Egg");
        ModLoader.addName(new ItemTacticalSpawnEgg(config.tankEgg, TANK, "ztoTankEgg", 0x161616, 0xCC00FA),
                "Tank Spawn Egg");
        ModLoader.addName(new ItemTacticalSpawnEgg(config.thiefEgg, THIEF, "ztoThiefEgg", 0x101015, 0xE079FA),
                "Thief Spawn Egg");
    }

    private static void checkItemSlot(int id) {
        if (id < 4096 - 256 || id >= Item.itemsList.length - 256 || Item.itemsList[id + 256] != null)
            throw new IllegalStateException("ZTO: Invalid or occupied tactical spawn egg item ID " + id
                    + " in config/ZedsTekkitOverhaul.cfg");
    }
}
