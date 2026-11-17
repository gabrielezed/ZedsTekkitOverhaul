package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import zed.tekkitoverhaul.survival.tactical.ThiefTrail;

/** Optional one-shot trail hint on the ordinary sneak gesture. */
public final class ThiefCompass {
    private boolean active;

    public void tick(Minecraft game) {
        EntityPlayer player = game.thePlayer;
        ItemStack held = player == null ? null : player.getCurrentEquippedItem();
        boolean requested = ThiefTrail.local(game.theWorld) && !game.isGamePaused && game.currentScreen == null
                && player != null && player.worldObj == game.theWorld && player.isEntityAlive()
                && player.isSneaking() && held != null && held.itemID == Item.compass.shiftedIndex;
        if (requested && !active) ThiefTrail.inspect(game.theWorld, player);
        active = requested;
    }
}
