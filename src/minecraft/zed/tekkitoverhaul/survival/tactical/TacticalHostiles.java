package zed.tekkitoverhaul.survival.tactical;

import java.io.File;
import net.minecraft.src.Item;
import net.minecraft.src.ModLoader;
import net.minecraft.src.forge.Configuration;

public final class TacticalHostiles {
    public static final String SKIRMISHER = "ZTO.Skirmisher";
    public static final String BREACHER = "ZTO.Breacher";

    private TacticalHostiles() {}

    public static void register() {
        ModLoader.registerEntityID(EntitySkirmisher.class, SKIRMISHER, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityBreacher.class, BREACHER, ModLoader.getUniqueEntityId());
    }

    public static void registerEggs() {
        Configuration config = new Configuration(new File("config/ZedsTekkitOverhaul-Tactical.cfg"));
        config.load();
        int skirmisher = config.getOrCreateIntProperty("skirmisherEgg", "item", 22004).getInt();
        int breacher = config.getOrCreateIntProperty("breacherEgg", "item", 22005).getInt();
        config.save();
        if (skirmisher == breacher) throw new IllegalStateException("ZTO: Duplicate tactical spawn egg item IDs");
        checkItemSlot(skirmisher);
        checkItemSlot(breacher);
        ModLoader.addName(new ItemTacticalSpawnEgg(skirmisher, SKIRMISHER, "ztoSkirmisherEgg", 0x17171C, 0x535170),
                "Skirmisher Spawn Egg");
        ModLoader.addName(new ItemTacticalSpawnEgg(breacher, BREACHER, "ztoBreacherEgg", 0x1C171B, 0x785469),
                "Breacher Spawn Egg");
    }

    private static void checkItemSlot(int id) {
        if (id < 4096 - 256 || id >= Item.itemsList.length - 256 || Item.itemsList[id + 256] != null)
            throw new IllegalStateException("ZTO: Invalid or occupied tactical spawn egg item ID " + id
                    + " in ZedsTekkitOverhaul-Tactical.cfg");
    }
}
