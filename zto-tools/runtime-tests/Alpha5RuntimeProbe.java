import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import zed.tekkitoverhaul.equipment.MobEquipment;
import zed.tekkitoverhaul.equipment.MobEquipment.Slot;
import zed.tekkitoverhaul.equipment.MobEquipmentHooks;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;

/** Runs against the assembled obfuscated Tekkit Dev client in a disposable copy. */
public final class Alpha5RuntimeProbe implements ITickHandler {
    private static final int[] ARMOR = {298, 299, 300, 301};
    private static String save;
    private static boolean vanillaSleep;
    private final boolean bootstrap;
    private boolean done;
    private int stage, x, y, z;
    private long time, deadline;

    private Alpha5RuntimeProbe(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    public static void main(String[] args) throws Exception {
        File game = new File(args[0]).getCanonicalFile();
        save = args[1];
        vanillaSleep = args.length == 3 && "vanilla".equals(args[2]);
        if (!game.getParentFile().getParentFile().getName().equals("survival-hud-runtime")
                || !save.matches("ZTO-Test-[a-f0-9]{32}")
                || !new File(game, "saves/" + save + "/.zto-disposable-test").isFile()) {
            throw new IllegalArgumentException("Expected a marked disposable save in the Tekkit Dev copy");
        }
        field(Minecraft.class, "aj").set(null, game);
        FMLCommonHandler.instance().registerTickHandler(new Alpha5RuntimeProbe(true));
        Minecraft.main(new String[] {"ZTOTest"});
    }

    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    @Override public String getLabel() { return "Alpha 5 runtime acceptance"; }
    @Override public void tickStart(EnumSet<TickType> ticks, Object... data) { }

    @Override
    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null) return;
        try {
            if (bootstrap) {
                if (!(game.s instanceof xt)) return;
                FMLCommonHandler.instance().registerTickHandler(new Alpha5RuntimeProbe(false));
                done = true;
                return;
            }
            if (stage == 0) {
                deadline = System.nanoTime() + 45_000_000_000L;
                check(mod_ZedsTekkitOverhaul.disableNightSkipping != vanillaSleep, "bed configuration loaded");
                check(!mod_ZedsTekkitOverhaul.survivalDebugHud && !Boolean.getBoolean("zto.devBridge.enabled"),
                        "diagnostics disabled");
                System.out.println("ALPHA5 JAR "
                        + mod_ZedsTekkitOverhaul.class.getProtectionDomain().getCodeSource().getLocation());
                open(game);
                setupBed(game);
                game.f.a(vanillaSleep ? 15000L : 25000L);
                time = game.f.w();
                stage++;
                return;
            }
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out at stage " + stage);
            if (stage == 1) {
                if (game.f.w() < time + 3) return;
                time = game.f.w();
                activate(game, false);
                if (vanillaSleep) {
                    check(game.h.az(), "legacy night interaction enters sleep");
                } else {
                    checkAnchor(game);
                    check(!game.h.az() && game.s == null && game.f.w() == time, "daytime use anchors without sleep");
                    game.f.a(39000L);
                    time = game.f.w();
                    activate(game, true);
                    check(!game.h.az() && game.s == null && game.f.w() == time, "nighttime use leaves clock unchanged");
                }
                stage++;
                return;
            }
            if (stage == 2) {
                if (vanillaSleep) {
                    if (game.f.w() < 24000L) return;
                    check(!game.h.az(), "legacy sleep advanced to morning and woke player");
                    checkAnchor(game);
                    finish(game);
                    return;
                }
                if (game.f.w() < time + 110) return;
                check(game.f.w() < time + 160 && !game.h.az(), "night advances by ordinary ticks past sleep threshold");
                verifyGear(game);
                verifyInventory(game);
                saveQuit(game);
                stage++;
            } else if (stage == 3) {
                check(game.f == null && game.s instanceof xt, "Save and Quit returned to title");
                open(game);
                checkAnchor(game);
                verifyArmor(game);
                game.h.a(md.i, 1000);
                stage++;
            } else if (stage == 4 && game.s instanceof uy) {
                check(game.h.bb() <= 0, "native death screen after lethal damage");
                uh exit = yw.a(game.f, game.h.aC());
                game.a(false, 0, false);
                check(exit != null && Math.abs(game.h.o - exit.a - .5) < 1 && Math.abs(game.h.q - exit.c - .5) < 1,
                        "native respawn selected the persisted bed exit");
                game.f.g(x, y, z, 0);
                check(yw.a(game.f, game.h.aC()) == null, "destroyed bed rejected by native resolver");
                game.h.a(md.i, 1000);
                stage++;
            } else if (stage == 5 && game.s instanceof uy) {
                game.a(false, 0, false);
                check(game.h.aC() == null && game.h.bb() > 0, "destroyed bed fell back to world spawn");
                finish(game);
            }
        } catch (Throwable failure) {
            done = true;
            failure.printStackTrace();
            game.f();
        }
    }

    private void setupBed(Minecraft game) {
        x = (int) Math.floor(game.h.o) + 2;
        y = 180;
        z = (int) Math.floor(game.h.q);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            game.f.g(x + dx, y - 1, z + dz, 1);
        }
        game.f.b(x, y, z - 1, 26, 0);
        game.f.b(x, y, z, 26, 8);
        game.h.b(x - .5, y + 1.62, z + .5, 0, 0);
        game.h.aT.a = false;
        game.f.q = 0;
    }

    private void activate(Minecraft game, boolean head) {
        check(((pm) pb.m[26]).b(game.f, x, y, head ? z : z - 1, game.h), "bed activation consumed");
    }

    private void checkAnchor(Minecraft game) {
        uh anchor = game.h.aC();
        check(anchor != null && anchor.a == x && anchor.b == y && anchor.c == z, "vanilla anchor stores bed head");
    }

    private static void verifyGear(Minecraft game) throws Exception {
        Class<?> loadouts = Class.forName("zed.tekkitoverhaul.assault.EncounterLoadouts");
        Class<?> profile = Class.forName("zed.tekkitoverhaul.assault.LoadoutPolicy$Profile");
        Method assign = loadouts.getDeclaredMethod("assign", yy.class, profile);
        assign.setAccessible(true);
        ajg sword = new ajg(game.f);
        ajg leather = new ajg(game.f);
        xr chain = new xr(game.f);
        for (Object value : profile.getEnumConstants()) {
            String name = value.toString();
            if (name.equals("LIGHT_ZOMBIE")) check((Boolean) assign.invoke(null, sword, value), name);
            if (name.equals("PROTECTED_ZOMBIE")) check((Boolean) assign.invoke(null, leather, value), name);
            if (name.equals("PROTECTED_SKELETON")) check((Boolean) assign.invoke(null, chain, value), name);
        }
        check(chain.ae().c == 261, "protected Skeleton retains the native bow");
        ed victim = new ed(game.f), comparison = new ed(game.f);
        sword.c((nn) victim);
        new ajg(game.f).c((nn) comparison);
        check(victim.bb() < comparison.bb(), "stone sword increased native Zombie melee damage");
        ajg bare = new ajg(game.f);
        leather.a(md.a(game.h), 12);
        bare.a(md.a(game.h), 12);
        check(leather.bb() > bare.bb(), "leather chestpiece mitigated incoming damage");
        xr reloaded = new xr(game.f);
        ady tag = new ady();
        chain.d(tag);
        reloaded.e(tag);
        check(MobEquipment.get(reloaded, Slot.CHEST).c == 303, "protected Skeleton gear survived native entity NBT");
        ArrayList<fq> drops = new ArrayList<fq>();
        MobEquipmentHooks hooks = new MobEquipmentHooks();
        hooks.onEntityLivingDrops(reloaded, md.i, drops, 0, true, 0);
        hooks.onEntityLivingDrops(reloaded, md.i, drops, 0, true, 0);
        check(drops.isEmpty() && MobEquipment.get(reloaded, Slot.CHEST) == null, "encounter gear emitted no loot");
        check(MobEquipment.assign(reloaded, Slot.CHEST, new aan(303, 1, 0)), "explicit gear assignment accepted");
        hooks.onEntityLivingDrops(reloaded, md.i, drops, 0, true, 0);
        check(drops.size() == 1, "explicit gear retains its death drop");
    }

    private static void verifyInventory(Minecraft game) throws Exception {
        ain gui = new ain(game.h);
        game.a(gui);
        check(gui.controller != null && game.h.aq == gui.d, "NEI player inventory opened on the original container");
        for (int type = 0; type < ARMOR.length; type++) {
            yu source = gui.d.b(9 + type);
            gui.d.a(5 + type, null);
            aan stack = new aan(ARMOR[type], 1, type + 1);
            stack.d = new ady();
            stack.o().a("Alpha5", "persisted");
            gui.d.a(9 + type, stack);
            shiftClick(game, gui, source);
            check(source.b() == null && gui.d.b(5 + type).b() == stack,
                    "equipped one armor item from slot " + (9 + type));
            shiftClick(game, gui, source);
            check(source.b() == null && gui.d.b(5 + type).b() == stack, "repeated click preserved the armor");
        }
        verifyArmor(game);
        gui.d.a(9, new aan(298, 1, 0));
        shiftClick(game, gui, gui.d.b(9));
        check(gui.d.b(9).b() == null, "NEI normal shift transfer moved the spare helmet");
        verifyArmor(game);
        gui.d.a(9, new aan(1, 4, 0));
        shiftClick(game, gui, gui.d.b(9));
        check(gui.d.b(9).b() == null, "ordinary stack used normal shift transfer");
        game.a((vp) null);
    }

    // Stage the current LWJGL event and dispatch through NEI's real GuiContainer override.
    private static void shiftClick(Minecraft game, gb gui, yu slot) throws Exception {
        int x = (gui.e + slot.d + 8) * game.d / gui.q;
        int y = (gui.r - gui.f - slot.e - 9) * game.e / gui.r;
        ByteBuffer keys = (ByteBuffer) field(Keyboard.class, "keyDownBuffer").get(null);
        byte previousShift = keys.get(Keyboard.KEY_LSHIFT);
        try {
            keys.put(Keyboard.KEY_LSHIFT, (byte) 1);
            field(Mouse.class, "eventButton").setInt(null, 0);
            field(Mouse.class, "eventState").setBoolean(null, true);
            field(Mouse.class, "event_x").setInt(null, x);
            field(Mouse.class, "event_y").setInt(null, y);
            field(Mouse.class, "event_dwheel").setInt(null, 0);
            gui.f();
            field(Mouse.class, "eventState").setBoolean(null, false);
            gui.f();
        } finally {
            keys.put(Keyboard.KEY_LSHIFT, previousShift);
        }
    }

    private static void verifyArmor(Minecraft game) {
        for (int type = 0; type < ARMOR.length; type++) {
            aan item = game.h.aq.b(5 + type).b();
            check(item != null && item.c == ARMOR[type] && item.a == 1 && item.i() == type + 1
                    && "persisted".equals(item.o().j("Alpha5")), "armor slot " + type + " retains damage and NBT");
        }
    }

    private static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
    }

    private static void saveQuit(Minecraft game) throws Exception {
        lb menu = new lb();
        game.a(menu);
        List<?> controls = (List<?>) field(vp.class, "s").get(menu);
        Method action = lb.class.getDeclaredMethod("a", abp.class);
        action.setAccessible(true);
        action.invoke(menu, controls.get(0));
    }

    private void finish(Minecraft game) throws Exception {
        saveQuit(game);
        done = true;
        System.out.println("ALPHA5 PASS");
        game.f();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("ALPHA5 OK " + message);
    }

}
