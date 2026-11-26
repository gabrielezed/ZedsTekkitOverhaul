package zed.tekkitoverhaul.exploration.motorboat.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ModLoader;
import net.minecraft.src.MovementInput;
import net.minecraft.src.Render;
import zed.tekkitoverhaul.exploration.motorboat.EntityMotorboat;

public final class MotorboatClient implements EntityMotorboat.Controls {
    private final Minecraft minecraft;

    private MotorboatClient(Minecraft minecraft) { this.minecraft = minecraft; }

    public static void register(Minecraft minecraft) {
        EntityMotorboat.setControls(new MotorboatClient(minecraft));
        MotorboatArtwork.reserveSlots();
    }

    public static void registerTextures() { MotorboatArtwork.registerIcons(); }

    public static void registerRenderer(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityMotorboat.class, new RenderMotorboat());
    }

    private MovementInput movement(EntityPlayer rider) {
        return rider == minecraft.thePlayer && minecraft.currentScreen == null
                ? minecraft.thePlayer.movementInput : null;
    }

    @Override
    public float throttle(EntityPlayer rider) {
        MovementInput movement = movement(rider);
        return movement == null || movement.sneak ? 0F : movement.moveForward;
    }

    @Override
    public float steering(EntityPlayer rider) {
        MovementInput movement = movement(rider);
        return movement == null ? 0F : movement.moveStrafe;
    }

    @Override
    public void openFueling(EntityPlayer player, EntityMotorboat boat) {
        if (player == minecraft.thePlayer) ModLoader.openGUI(player, new GuiMotorboat(player.inventory, boat));
    }
}
