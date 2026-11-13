import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.survival.tactical.TacticalHostiles;

/** Runs against the reobfuscated mod and pack on the local game thread. */
public final class Alpha7RuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215;
    private static String save, mode;
    private static File directory;
    private static Field focus, target, impacts;
    private boolean done, aimed, retreated, moved;
    private int stage, shots, firstShot, health, measured, peakEntities;
    private long since, previousTick, elapsedNanos;
    private double initialX, initialZ;
    private EntitySkirmisher skirmisher;
    private EntityBreacher breacher;
    private final Set<nm> observed = Collections.newSetFromMap(new IdentityHashMap<nm, Boolean>());

    /** Keep prototype class resolution after FML has added the mod JAR to the classpath. */
    public static final class Bootstrap implements ITickHandler {
        private boolean ready;

        public static void main(String[] args) throws Exception {
            File game = new File(args[0]).getCanonicalFile();
            String world = args[1];
            if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                    || !world.matches("ZTO-Test-[a-f0-9]{32}")
                    || !new File(game, "saves/" + world + "/.zto-disposable-test").isFile()) {
                throw new IllegalArgumentException("Use a marked save in the isolated Tekkit Dev copy");
            }
            Field gameDirectory = Minecraft.class.getDeclaredField("aj");
            gameDirectory.setAccessible(true);
            gameDirectory.set(null, game);
            System.setProperty("zto.alpha7.save", world);
            System.setProperty("zto.alpha7.mode", args[2]);
            FMLCommonHandler.instance().registerTickHandler(new Bootstrap());
            Minecraft.main(new String[] {"ZTOTest"});
        }

        public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
        public String getLabel() { return "Alpha 7 bootstrap"; }
        public void tickStart(EnumSet<TickType> ticks, Object... data) {}
        public void tickEnd(EnumSet<TickType> ticks, Object... data) {
            if (ready) return;
            Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
            if (game == null || !(game.s instanceof xt)) return;
            FMLCommonHandler.instance().registerTickHandler(new Alpha7RuntimeProbe());
            ready = true;
        }
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "Alpha 7 tactical runtime"; }

    public void tickStart(EnumSet<TickType> ticks, Object... data) {
        if (done || focus == null) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        try {
            focus.setLong(game.u, System.currentTimeMillis());
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null) return;
        try {
            if (stage == 0) {
                directory = (File) field(Minecraft.class, "aj").get(null);
                save = System.getProperty("zto.alpha7.save");
                mode = System.getProperty("zto.alpha7.mode");
                focus = field(lr.class, "Y");
                target = field(TacticalHostile.class, "target");
                impacts = field(EntityBreacher.class, "impacts");
                check(!Boolean.getBoolean("zto.devBridge.enabled"), "diagnostics disabled");
                System.out.println("ALPHA7 Java " + System.getProperty("java.version"));
                open(game);
                arena(game);
                next(game);
            } else if (game.f.w() > previousTick) {
                previousTick = game.f.w();
                if ("stress".equals(mode)) stress(game);
                else combat(game);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            game.f();
            done = true;
        }
    }

    private void combat(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        switch (stage) {
            case 1:
                if (age < 25) return;
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 8, 0);
                initialZ = skirmisher.q;
                next(game);
                break;
            case 2:
                observeArrows(game);
                aimed |= skirmisher.isAiming();
                moved |= Math.abs(skirmisher.q - initialZ) > 0.75D;
                if (shots < 2 && age < 180) return;
                check(aimed && moved && shots >= 2, "Skirmisher aims, shoots twice and strafes");
                check(target.get(skirmisher) == game.h, "Skirmisher acquires the player");
                System.out.println("ALPHA7 Skirmisher aimed, strafed and fired two spaced shots");
                screenshot(game);
                player(game, skirmisher.o - X - 2, skirmisher.q - Z);
                initialX = skirmisher.o;
                initialZ = skirmisher.q;
                next(game);
                break;
            case 3:
                retreated |= (skirmisher.o - initialX) > 0.75D && skirmisher.f(game.h) > 9.0D;
                if (!retreated && age < 60) return;
                check(retreated, "Skirmisher retreats from a close player");
                System.out.println("ALPHA7 Skirmisher retreated");
                game.f.b(skirmisher);
                clearArrows(game);
                player(game, 0.5D, 0.5D);
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 8, 0);
                shots = 0;
                next(game);
                break;
            case 4:
                if (!skirmisher.isAiming() && age < 60) return;
                check(skirmisher.isAiming(), "Skirmisher prepares the covered shot");
                wall(game, 4, 1);
                next(game);
                break;
            case 5:
                observeArrows(game);
                if (age < 125) return;
                check(shots == 0 && !skirmisher.isAiming() && target.get(skirmisher) == null,
                        "cover cancels the shot and breaks pursuit");
                System.out.println("ALPHA7 cover cancelled the shot and pursuit");
                game.f.b(skirmisher);
                clearArrows(game);
                wall(game, 4, 0);
                breacher = (EntityBreacher) spawn(game, TacticalHostiles.BREACHER, 8, 0);
                game.h.l(20);
                health = game.h.bb();
                next(game);
                break;
            case 6:
                if (breacher.o > X + 6 && age < 65) return;
                check(breacher.o <= X + 6 && target.get(breacher) == game.h, "Breacher approaches in the open");
                wall(game, 4, 1);
                // Set the upper half first so lower-half neighbor updates see a complete door.
                game.f.b(X + 4, Y + 1, Z, 64, 8);
                game.f.d(X + 4, Y, Z, 64, 0);
                next(game);
                break;
            case 7:
                if (age < 165) return;
                check(impacts.getInt(breacher) == 3, "Breacher applies exactly three impacts, got "
                        + impacts.getInt(breacher) + " at " + breacher.o + "," + breacher.p + "," + breacher.q
                        + " door=" + game.f.a(X + 4, Y, Z) + ":" + game.f.e(X + 4, Y, Z));
                check(game.f.a(X + 4, Y, Z) == 64 && game.f.e(X + 4, Y, Z) == 0
                        && game.f.a(X + 4, Y + 1, Z) == 64 && breacher.o > X + 4 && game.h.bb() == health,
                        "closed door and player behind it remain intact");
                System.out.println("ALPHA7 Breacher stopped after three impacts without changing the door");
                game.f.f(X + 4, Y, Z, 4);
                next(game);
                break;
            case 8:
                if (age < 105 && game.h.bb() == health) return;
                check(breacher.o < X + 4 && game.h.bb() < health, "Breacher uses the reopened entrance and lands melee");
                check(game.f.a(X + 4, Y, Z) == 64 && game.f.e(X + 4, Y, Z) == 4,
                        "open door survives melee");
                System.out.println("ALPHA7 Breacher traversed the reopened entrance and landed melee");
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 7, -3);
                skirmisher.l(9);
                breacher.l(13);
                saveQuit(game);
                check(game.f == null && game.s instanceof xt, "Save and Quit returned to title");
                open(game);
                next(game);
                break;
            case 9:
                if (age < 10) return;
                List<TacticalHostile> loaded = mobs(game);
                check(loaded.size() == 2, "exactly two prototypes loaded from the save");
                for (TacticalHostile mob : loaded) {
                    check(mob.bb() == (mob instanceof EntitySkirmisher ? 9 : 13), "health persists through Save and Quit");
                    checkRenderer(game, mob);
                    check(mob.a(md.j, 100), "normal damage path kills the prototype");
                }
                next(game);
                break;
            case 10:
                if (age < 30) return;
                check(mobs(game).isEmpty(), "death removes both prototypes");
                finish(game);
                break;
            default:
                throw new IllegalStateException("Unexpected combat stage");
        }
    }

    private void observeArrows(Minecraft game) {
        for (Object entity : game.f.b) {
            if (!(entity instanceof nm)) continue;
            nm arrow = (nm) entity;
            if (arrow.c != skirmisher || !observed.add(arrow)) continue;
            if (shots > 0) check(skirmisher.V - firstShot >= 62, "shot interval includes cooldown and windup");
            firstShot = skirmisher.V;
            check(arrow.n() == 1.5D, "projectile damage coefficient");
            shots++;
        }
    }

    private static void clearArrows(Minecraft game) {
        for (Object entity : game.f.b) if (entity instanceof nm) ((nm) entity).A();
    }

    private void stress(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        if (stage == 1 && age >= 40) {
            for (int i = 0; i < 6; i++) {
                spawn(game, TacticalHostiles.SKIRMISHER, 8, i - 3);
                spawn(game, TacticalHostiles.BREACHER, -8, i - 3);
            }
            next(game);
        } else if (stage == 2) {
            long now = System.nanoTime();
            if (elapsedNanos != 0) measured++;
            if (elapsedNanos == 0) elapsedNanos = now;
            List<TacticalHostile> loaded = mobs(game);
            peakEntities = Math.max(peakEntities, loaded.size());
            if (age < 140) return;
            check(peakEntities == 12 && loaded.size() == 12, "all twelve prototypes survive the stress window");
            int active = 0;
            for (TacticalHostile mob : loaded) {
                if (target.get(mob) == game.h) active++;
            }
            check(active >= 8, "most prototypes still pursue the player");
            System.out.println("ALPHA7 stress ticks=" + age + " sampledTicks=" + measured
                    + " wallMillis=" + (now - elapsedNanos) / 1000000 + " active=" + active + " peak=" + peakEntities);
            screenshot(game);
            finish(game);
        }
    }

    private static nn spawn(Minecraft game, String name, int dx, int dz) {
        TacticalHostile mob = (TacticalHostile) aao.a(name, game.f);
        check(mob != null && name.equals(aao.b(mob)), "entity registry round trip " + name);
        mob.c(X + dx + 0.5D, Y, Z + dz + 0.5D, 90, 0);
        check(game.f.a(mob), "world insertion " + name);
        checkRenderer(game, mob);
        return mob;
    }

    private static void checkRenderer(Minecraft game, nn mob) {
        check(ahu.a.a(mob).getClass().getName().contains("TacticalRenderer"), "custom renderer registry");
        check(TacticalHostile.class.getResource(mob.v_()) != null, "packaged skin " + mob.v_());
        check(game.p.b(mob.v_()) > 0, "texture uploaded");
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL reports no error");
    }

    private static List<TacticalHostile> mobs(Minecraft game) {
        List<TacticalHostile> found = new ArrayList<TacticalHostile>();
        for (Object entity : game.f.b) if (entity instanceof TacticalHostile) found.add((TacticalHostile) entity);
        return found;
    }

    private static void player(Minecraft game, double dx, double dz) {
        game.h.c(X + dx, Y, Z + dz, -90, 0);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private static void arena(Minecraft game) {
        game.f.a(6000L);
        for (int dx = -23; dx <= 23; dx++) {
            for (int dz = -23; dz <= 23; dz++) game.f.g(X + dx, Y - 1, Z + dz, 1);
        }
        player(game, 0.5D, 0.5D);
    }

    private static void wall(Minecraft game, int dx, int id) {
        for (int dz = -17; dz <= 17; dz++) {
            for (int dy = 0; dy <= 2; dy++) game.f.g(X + dx, Y + dy, Z + dz, id);
        }
    }

    private static void screenshot(Minecraft game) {
        System.out.println("ALPHA7 " + zh.a(directory, game.d, game.e));
    }

    private static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
        game.f.q = 2;
    }

    private static void saveQuit(Minecraft game) throws Exception {
        lb menu = new lb();
        game.a(menu);
        Method action = lb.class.getDeclaredMethod("a", abp.class);
        action.setAccessible(true);
        action.invoke(menu, new abp(1, 0, 0, "Save and Quit"));
    }

    private void next(Minecraft game) { since = game.f.w(); stage++; }

    private void finish(Minecraft game) throws Exception {
        saveQuit(game);
        System.out.println("ALPHA7 PASS " + mode);
        done = true;
        game.f();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
