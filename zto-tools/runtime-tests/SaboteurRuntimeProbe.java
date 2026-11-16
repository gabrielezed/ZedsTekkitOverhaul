import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.TacticalHostiles;

/** Drives disposable fixtures through the installed 1.2.5 pack and the packaged mod. */
public final class SaboteurRuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215, HOPPER_X = X + 6, ITEM = 4, TOTAL = 320;
    private static File directory;
    private static String save, mode;
    private Field focus;
    private boolean done, walked, aimed, capturedCancellation;
    private int stage, priorBlinks, pausedOutput, pausedInput, hopperId;
    private long tick = Long.MIN_VALUE, since, stressStarted;
    private double priorX, priorZ;
    private EntitySaboteur mob, contender;
    private TileHopper hopper;
    private io input, output;

    public static final class Bootstrap implements ITickHandler {
        private boolean ready;

        public static void main(String[] args) throws Exception {
            File game = new File(args[0]).getCanonicalFile();
            String world = args[1];
            if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                    || !world.matches("ZTO-Test-[a-f0-9]{32}")
                    || !new File(game, "saves/" + world + "/.zto-disposable-test").isFile())
                throw new IllegalArgumentException("Use a marked save in the isolated Tekkit Dev copy");
            Field gameDirectory = Minecraft.class.getDeclaredField("aj");
            gameDirectory.setAccessible(true);
            gameDirectory.set(null, game);
            System.setProperty("zto.saboteur.save", world);
            System.setProperty("zto.saboteur.mode", args[2]);
            FMLCommonHandler.instance().registerTickHandler(new Bootstrap());
            Minecraft.main(new String[] {"ZTOTest"});
        }

        public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
        public String getLabel() { return "Saboteur bootstrap"; }
        public void tickStart(EnumSet<TickType> ticks, Object... data) {}
        public void tickEnd(EnumSet<TickType> ticks, Object... data) {
            if (ready) return;
            Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
            if (game == null || !(game.s instanceof xt)) return;
            FMLCommonHandler.instance().registerTickHandler(new SaboteurRuntimeProbe());
            ready = true;
        }
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "Saboteur runtime"; }
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
            SilentRuntimeAudio.verify(game);
            if (stage == 0) {
                directory = (File) field(Minecraft.class, "aj").get(null);
                save = System.getProperty("zto.saboteur.save");
                mode = System.getProperty("zto.saboteur.mode");
                focus = field(lr.class, "Y");
                System.out.println("SABOTEUR Java " + System.getProperty("java.version")
                        + ", audio backend uninitialized, Bridge=" + Boolean.getBoolean("zto.devBridge.enabled"));
                open(game);
                hopperId = HopperRegistry.block.bO;
                if ("reopen".equals(mode)) confirmReopen(game);
                else setup(game);
                return;
            }
            if (game.f.w() == tick) return;
            tick = game.f.w();
            long age = tick - since;
            check(age < 340, "stage " + stage + " timed out" + state());
            if ("reopen".equals(mode)) {
                if (count(output) > pausedOutput) finish(game);
            } else if ("stress".equals(mode)) {
                if (age % 20 == 0) game.h.l(20);
                if (age >= 120) stress(game);
            } else {
                exercise(game, age);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            done = true;
            game.f();
        }
    }

    private void setup(Minecraft game) throws Exception {
        game.f.a(6000L);
        for (int x = X - 12; x <= X + 14; x++) {
            for (int z = Z - 11; z <= Z + 10; z++) {
                game.f.g(x, Y - 1, z, 1);
                for (int y = Y; y <= Y + 3; y++) {
                    if (game.f.a(x, y, z) != 0) game.f.g(x, y, z, 0);
                }
            }
        }
        // A lit machinery alcove and a straight, open approach.
        for (int x = HOPPER_X - 1; x <= HOPPER_X + 2; x++) {
            for (int y = Y; y <= Y + 2; y++) game.f.g(x, y, Z - 2, 5);
        }
        game.f.g(HOPPER_X - 1, Y, Z - 1, 50);
        player(game, X + 1.5D, Z + 2.4D, -65);
        check(!game.h.aT.a && !game.h.aT.d, "player is vulnerable in Survival");
        if ("stress".equals(mode)) {
            game.f.d(HOPPER_X, Y, Z - 1, 61, 3);
            ((io) game.f.b(HOPPER_X, Y, Z - 1)).a(2, new aan(ITEM, 5, 0));
            for (int i = 0; i < 8; i++) spawn(game, X + 4.5D + (i % 4), Z - 0.5D + (i / 4) * 2);
            stressStarted = System.nanoTime();
        } else {
            blinkGeometry(game);
            game.f.g(HOPPER_X, Y + 1, Z, 54);
            game.f.g(HOPPER_X + 1, Y, Z, 54);
            game.f.d(HOPPER_X, Y, Z, hopperId, 5);
            readFixture(game);
            for (int i = 0; i < TOTAL / 64; i++) input.a(i, new aan(ITEM, 64, 0));
            mob = spawn(game, X - 2.5D, Z + 0.5D);
            priorX = mob.o;
            priorZ = mob.q;
            identity(game);
        }
        advance(game);
    }

    private void exercise(Minecraft game, long age) throws Exception {
        identity(game);
        double distance = Math.hypot(mob.o - priorX, mob.q - priorZ);
        if (mob.blinks() == priorBlinks && distance > 0.025D && distance < 0.8D) walked = true;
        if (mob.blinks() > priorBlinks)
            check(distance > 2 && distance <= 4, "actual blink displacement " + distance);
        priorX = mob.o;
        priorZ = mob.q;
        priorBlinks = mob.blinks();
        switch (stage) {
            case 1:
                if (mob.pose() != EntitySaboteur.WINDUP || mob.disruptionTarget() == null) return;
                if (!aimed) {
                    lookAtFixture(game, HOPPER_X - 2.5D, Z + 3.7D);
                    aimed = true;
                    return;
                }
                if (mob.windupRemaining() > 19) return;
                check(mob.disruptionTarget() == hopper, "acquired the exact Hopper");
                check(walked && mob.blinks() > 0 && count(output) > 0, "normal movement, blink and real transfer before windup" + state());
                check(mob.blinkChecks() <= mob.blinkAttempts() * 3, "bounded blink attempts");
                check(mob.scannedBlocks() <= mob.scans() * 17 * 17 * 5, "bounded scan work");
                check(mob.pathPlans() > 0 && mob.pathPlans() <= mob.V / 20 + 1, "bounded path requests");
                screenshot(game, "windup");
                if ("halt".equals(mode)) { advance(game); return; }
                player(game, mob.o - 0.9D, mob.q + 0.75D, -120);
                int health = mob.bb();
                game.c.b(game.h, mob);
                check(mob.bb() < health && mob.pose() == EntitySaboteur.CANCELLED && "HIT".equals(mob.lastOutcome()),
                        "landed Survival hit cancels windup" + state());
                check(mob.actionCooldown() == 120 && !hopper.isDisrupted(), "cancelled action imposes cooldown without pause");
                advance(game);
                return;
            case 2:
                if (!"halt".equals(mode) && !capturedCancellation && age >= 3) {
                    screenshot(game, "cancelled");
                    capturedCancellation = true;
                    player(game, X + 1.5D, Z + 2.4D, -65);
                }
                if (!hopper.isDisrupted()) return;
                check(mob.disruptions() == 1 && "APPLIED".equals(mob.lastOutcome()), "windup applied a real pause");
                check(hopper.disruptionTicksRemaining() >= 78 && hopper.disruptionTicksRemaining() <= 80,
                        "pause begins with finite duration");
                pausedOutput = count(output);
                pausedInput = count(input);
                lookAtFixture(game, HOPPER_X - 2.5D, Z + 3.7D);
                System.out.println("SABOTEUR applied at output=" + pausedOutput + state());
                if ("halt".equals(mode)) {
                    game.f.a(true, (rw) null);
                    System.out.println("SABOTEUR PASS halt");
                    System.out.flush();
                    Runtime.getRuntime().halt(23);
                }
                advance(game);
                return;
            case 3:
                if (age == 4) screenshot(game, "applied");
                check(count(output) == pausedOutput, "no outgoing transfer throughout disruption");
                if (hopper.isDisrupted()) return;
                check(age >= 77 && age <= 81 && count(input) < pausedInput, "timed pause expired while input held or advanced");
                advance(game);
                return;
            case 4:
                if (count(output) == pausedOutput) return;
                System.out.println("SABOTEUR timeout recovery, conserved " + TOTAL + " items; resume ticks=" + age);
                game.f.a(18000L);
                advance(game);
                return;
            case 5:
                if (mob.pose() != EntitySaboteur.WINDUP || mob.disruptionTarget() != hopper) return;
                TileHopper replacement = new TileHopper();
                for (int i = 0; i < hopper.a(); i++) replacement.a(i, hopper.k_(i));
                game.f.a(HOPPER_X, Y, Z, replacement);
                hopper = replacement;
                advance(game);
                return;
            case 6:
                check(mob.pose() == EntitySaboteur.CANCELLED && "TARGET_CHANGED".equals(mob.lastOutcome())
                        && !hopper.isDisrupted(), "new tile at the same block cancels stale windup");
                advance(game);
                return;
            case 7:
                if (!hopper.isDisrupted()) return;
                check(mob.disruptions() == 2, "next valid windup applied a second isolated lease");
                pausedOutput = count(output);
                check(mob.a(md.j, 100) && !hopper.isDisrupted(), "death ends the active lease");
                advance(game);
                return;
            case 8:
                if (count(output) == pausedOutput) return;
                check(age <= 8, "output resumed after death");
                mob = spawn(game, HOPPER_X - 1.5D, Z + 0.5D);
                contender = spawn(game, HOPPER_X + 0.5D, Z + 2.2D);
                priorBlinks = 0;
                priorX = mob.o;
                priorZ = mob.q;
                advance(game);
                return;
            case 9:
                if (!hopper.isDisrupted()) return;
                EntitySaboteur winner = mob.disruptions() == 1 ? mob : contender;
                EntitySaboteur loser = winner == mob ? contender : mob;
                if (!"ALREADY_PAUSED".equals(loser.lastOutcome())) return;
                check(winner.disruptions() == 1 && loser.disruptions() == 0 && loser.cancelled() > 0,
                        "overlapping windups produce one lease and one cancellation");
                mob = winner;
                check(loser.a(md.j, 100) && hopper.isDisrupted(), "loser's death leaves the winner's lease in force");
                contender = null;
                lookAtFixture(game, HOPPER_X - 2.5D, Z + 3.7D);
                advance(game);
                return;
            case 10:
                if (age < 4) return;
                check(hopper.isDisrupted(), "winning lease still present before Save and Quit");
                if (age == 4) screenshot(game, "torchlight");
                if (age < 23) return;
                pausedOutput = count(output);
                saveQuit(game);
                open(game);
                confirmReopen(game);
                return;
            case 11:
                if (count(output) <= pausedOutput) return;
                if (!hopper.isDisrupted()) return;
                verifyChunkUnload(game);
                advance(game);
                return;
            case 12:
                if (!hopper.isDisrupted()) return;
                pausedOutput = count(output);
                game.A.C = game.f.q = 0;
                advance(game);
                return;
            case 13:
                check(!hopper.isDisrupted() && mobs(game).isEmpty(), "native Peaceful cleanup");
                if (count(output) > pausedOutput) finish(game);
                return;
            default:
                throw new AssertionError("Unexpected exercise stage " + stage + ", age=" + age);
        }
    }

    private void verifyChunkUnload(Minecraft game) throws Exception {
        ko provider = (ko) game.f.z();
        int chunkX = HOPPER_X >> 4, chunkZ = Z >> 4;
        game.f.a(new uh(HOPPER_X + 256, Y, Z + 256));
        provider.d(chunkX, chunkZ);
        provider.a();
        check(!provider.a(chunkX, chunkZ) && !hopper.isDisrupted(), "native chunk unload cleared live lease");
        provider.b(chunkX, chunkZ);
        TileHopper old = hopper;
        readFixture(game);
        check(hopper != old && !hopper.isDisrupted(), "chunk reopened with a new Hopper and no lease");
        identity(game);
        System.out.println("SABOTEUR native chunk unload and reload conserved " + TOTAL + " items");
        mob = mobs(game).get(mobs(game).size() - 1);
        priorBlinks = 0;
        priorX = mob.o;
        priorZ = mob.q;
    }

    private void confirmReopen(Minecraft game) throws Exception {
        readFixture(game);
        identity(game);
        check(!hopper.isDisrupted(), "lease absent as soon as the save reopens");
        List<EntitySaboteur> saved = mobs(game);
        check(saved.size() == 1, "one persisted Saboteur");
        mob = saved.get(0);
        check(TacticalHostiles.SABOTEUR.equals(aao.b(mob)) && mob.pose() == EntitySaboteur.IDLE
                && mob.disruptionTarget() == null && mob.actionCooldown() == 0,
                "native name and clean tactical state after world reload" + state());
        priorBlinks = 0;
        priorX = mob.o;
        priorZ = mob.q;
        pausedOutput = count(output);
        advance(game);
    }

    private void blinkGeometry(Minecraft game) throws Exception {
        Class<?> blink = Class.forName("zed.tekkitoverhaul.survival.tactical.SaboteurBlink");
        Method check = blink.getDeclaredMethod("check", EntitySaboteur.class, double.class, double.class);
        check.setAccessible(true);
        EntitySaboteur sample = new EntitySaboteur(game.f);
        int z = Z + 5;
        sample.c(X + 0.5D, Y, z + 0.5D, 0, 0);
        sample.z = true;
        expectBlink(check, sample, 2.75D, "CLEAR");
        expectBlink(check, sample, 4.01D, "RANGE");
        for (int x = X; x <= X + 3; x++) {
            game.f.g(x, Y, z - 1, 1);
            game.f.g(x, Y, z + 1, 1);
        }
        expectBlink(check, sample, 2.75D, "CLEAR");
        game.f.g(X + 1, Y, z, 1);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 0);
        game.f.b(X + 1, Y + 1, z, 64, 8);
        game.f.d(X + 1, Y, z, 64, 0);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 0);
        game.f.g(X + 1, Y + 1, z, 0);
        game.f.g(X + 1, Y, z, 44);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 0);
        game.f.g(X + 2, Y - 1, z, 0);
        expectBlink(check, sample, 2.75D, "FLOOR");
        sample.c(X + 0.5D, Y + 0.5D, z + 0.5D, 0, 0);
        expectBlink(check, sample, 2.75D, "HEIGHT");
        sample.c(X + 0.5D, 255, z + 0.5D, 0, 0);
        expectBlink(check, sample, 2.75D, "HEIGHT");
        int edge = (X >> 4) + 80;
        check(!game.f.z().a(edge, z >> 4), "probe chunk starts unloaded");
        sample.c(edge * 16 + 1.5D, Y, z + 0.5D, 0, 0);
        expectBlink(check, sample, 2.75D, "CHUNK_UNLOADED");
        check(!game.f.z().a(edge, z >> 4), "blink validation never loads the probe chunk");
        System.out.println("SABOTEUR blink open, narrow, wall, closed door, half step, gap, range, height and unloaded cases passed");
    }

    private static void expectBlink(Method check, EntitySaboteur mob, double dx, String expected) throws Exception {
        Object result = check.invoke(null, mob, dx, 0.0D);
        check(expected.equals(result.toString()), "blink " + expected + ", got " + result);
    }

    private void stress(Minecraft game) throws Exception {
        List<EntitySaboteur> list = mobs(game);
        int scans = 0, plans = 0, checks = 0, diversions = 0;
        check(list.size() == 8, "eight live Saboteurs and no natural duplicates");
        for (EntitySaboteur entity : list) {
            scans += entity.scans();
            plans += entity.pathPlans();
            checks += entity.blinkChecks();
            diversions += entity.diversions();
            check(entity.disruptions() == 0, "unsupported target cannot be disrupted");
            check(entity.blinkChecks() <= 3 * entity.blinkAttempts(), "stress candidate bound");
        }
        check(diversions > 0, "fallback produces harmless diversion");
        check(game.f.a(HOPPER_X, Y, Z - 1) == 61 && game.f.e(HOPPER_X, Y, Z - 1) == 3
                && count((io) game.f.b(HOPPER_X, Y, Z - 1)) == 5, "unsupported furnace state preserved");
        System.out.println("SABOTEUR stress actors=8 ticks=120 realMs=" + (System.nanoTime() - stressStarted) / 1000000
                + " scans=" + scans + " pathPlans=" + plans + " blinkChecks=" + checks + " diversions=" + diversions
                + " entities=" + game.f.b.size());
        screenshot(game, "stress");
        finish(game);
    }

    private static EntitySaboteur spawn(Minecraft game, double x, double z) {
        EntitySaboteur actor = (EntitySaboteur) aao.a(TacticalHostiles.SABOTEUR, game.f);
        check(actor != null && TacticalHostiles.SABOTEUR.equals(aao.b(actor)), "native Saboteur registration");
        actor.c(x, Y, z, 0, 0);
        check(game.f.a(actor) && Math.abs(actor.I - 0.45F) < 0.001F && Math.abs(actor.J - 1.1F) < 0.001F,
                "native entity insertion and hitbox");
        check(ahu.a.a(actor).getClass().getName().contains("TacticalRenderer"), "custom native renderer");
        check(game.p.b(actor.v_()) > 0 && GL11.glIsTexture(game.p.b(actor.v_()))
                && GL11.glIsTexture(game.p.b("/zed/textures/saboteur_eyes.png")), "uploaded skin and glow mask");
        return actor;
    }

    private static List<EntitySaboteur> mobs(Minecraft game) {
        List<EntitySaboteur> found = new ArrayList<EntitySaboteur>();
        for (Object entity : game.f.b) {
            if (entity instanceof EntitySaboteur && !((EntitySaboteur) entity).G) found.add((EntitySaboteur) entity);
        }
        return found;
    }

    private void readFixture(Minecraft game) {
        hopper = TileHopper.loadedAt(game.f, HOPPER_X, Y, Z);
        check(hopper != null, "Hopper loaded");
        input = (io) game.f.b(HOPPER_X, Y + 1, Z);
        output = (io) game.f.b(HOPPER_X + 1, Y, Z);
    }

    private void identity(Minecraft game) {
        check(game.f.a(HOPPER_X, Y, Z) == hopperId && game.f.e(HOPPER_X, Y, Z) == 5
                && TileHopper.loadedAt(game.f, HOPPER_X, Y, Z) == hopper,
                "Hopper block, metadata and loaded identity unchanged");
        check(count(input) + count(hopper) + count(output) == TOTAL, "resource conservation at every inspected tick");
    }

    private static int count(io inventory) {
        int count = 0;
        for (int i = 0; i < inventory.a(); i++) {
            aan stack = inventory.k_(i);
            if (stack != null) {
                check(stack.c == ITEM && stack.i() == 0, "fixture item and metadata unchanged");
                count += stack.a;
            }
        }
        return count;
    }

    private String state() {
        return mob == null ? "" : " pose=" + mob.pose() + " outcome=" + mob.lastOutcome() + " scans=" + mob.scans()
                + "/" + mob.scannedBlocks() + " paths=" + mob.pathPlans()
                + " blink=" + mob.blinks() + "/" + mob.blinkAttempts() + "/" + mob.blinkChecks()
                + " lastBlink=" + mob.lastBlinkResult() + " at=" + mob.o + "," + mob.p + "," + mob.q;
    }

    private static void player(Minecraft game, double x, double z, float yaw) {
        game.h.c(x, Y, z, yaw, 9);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private void lookAtFixture(Minecraft game, double x, double z) {
        double dx = (mob.o + HOPPER_X + 0.5D) * 0.5D - x;
        double dz = (mob.q + Z + 0.5D) * 0.5D - z;
        player(game, x, z, (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        game.h.v = 15;
    }

    private static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
        game.A.C = game.f.q = 2;
    }

    private static void saveQuit(Minecraft game) throws Exception {
        lb menu = new lb();
        game.a(menu);
        Method action = lb.class.getDeclaredMethod("a", abp.class);
        action.setAccessible(true);
        action.invoke(menu, new abp(1, 0, 0, "Save and Quit"));
        check(game.f == null, "Save and Quit unloaded the world");
    }

    private static void screenshot(Minecraft game, String label) {
        String name = save + "-" + label + ".png";
        System.out.println("SABOTEUR " + label + " " + zh.a(directory, name, game.d, game.e));
        check(new File(directory, "screenshots/" + name).isFile(), "captured actual rendered frame");
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL remains valid");
    }

    private void advance(Minecraft game) { since = game.f.w(); stage++; }

    private void finish(Minecraft game) throws Exception {
        saveQuit(game);
        System.out.println("SABOTEUR PASS " + mode);
        done = true;
        game.f();
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
