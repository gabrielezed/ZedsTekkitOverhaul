package zed.tekkitoverhaul.survival.tactical.client;

import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.tactical.ThiefTrail;

/** Optional one-shot trail hint on the ordinary sneak gesture. */
public final class ThiefCompass {
    private final EnderTrail trail = new EnderTrail();
    private WeakReference<World> world = new WeakReference<World>(null);
    private WeakReference<EntityPlayer> owner = new WeakReference<EntityPlayer>(null);
    private boolean active;
    private int ready;

    public void tick(Minecraft game) {
        EntityPlayer player = game.thePlayer;
        if (world.get() != game.theWorld || owner.get() != player) {
            world = new WeakReference<World>(game.theWorld);
            owner = new WeakReference<EntityPlayer>(player);
            trail.clear();
            active = false;
            ready = 0;
        }
        if (!ThiefTrail.local(game.theWorld) || game.isGamePaused || game.currentScreen != null
                || player == null || player.worldObj != game.theWorld || !player.isEntityAlive()) return;
        ItemStack held = player.getCurrentEquippedItem();
        boolean requested = player.isSneaking() && held != null && held.itemID == Item.compass.shiftedIndex;
        if (requested && !active && player.ticksExisted >= ready) {
            ready = player.ticksExisted + 22;
            ChunkCoordinates destination = ThiefTrail.inspect(game.theWorld, player);
            if (!trail.start(game.theWorld, player, destination) && destination != null)
                player.addChatMessage("The trail fades here. Try another approach.");
        }
        active = requested;
        trail.emit(game.theWorld, player.ticksExisted);
    }
}
