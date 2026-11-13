package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.ModLoader;

public final class TacticalHostiles {
    public static final String SKIRMISHER = "ZTO.Skirmisher";
    public static final String BREACHER = "ZTO.Breacher";

    private TacticalHostiles() {}

    public static void register() {
        ModLoader.registerEntityID(EntitySkirmisher.class, SKIRMISHER, ModLoader.getUniqueEntityId());
        ModLoader.registerEntityID(EntityBreacher.class, BREACHER, ModLoader.getUniqueEntityId());
    }
}
