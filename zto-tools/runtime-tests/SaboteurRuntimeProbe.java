import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.SabotageTarget;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.survival.tactical.TacticalHostiles;

/** Drives disposable fixtures through the installed 1.2.5 pack and the packaged mod. */
public final class SaboteurRuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215, HOPPER_X = X + 6, ITEM = 4, TOTAL = 320;
    private static File directory;
    private static String save, mode;
    private Field focus;
    private boolean done, walked, aimed, capturedCancellation, leavingChunk, captureEvade;
    private int stage, priorBlinks, pausedOutput, pausedInput, hopperId;
    private long tick = Long.MIN_VALUE, since, stressStarted, stressCpu, stressHeap;
    private double priorX, priorZ;
    private EntitySaboteur mob, contender;
    private TileHopper hopper;
    private io input, output;
    private static final int FLUID_TOTAL = 6000;
    private TileEntityDistillationReceiver receiver;
    private ILiquidContainer tank, controlSource, controlTank;
    private Object woodPipe, pipeTransport, controlPipe, controlTransport;
    private int liquidId, paidOut, atPause, controlAtPause, blockedFor, realHits, finishedFights, combatBlinks;
    private int previousReserve = FLUID_TOTAL;
    private int rangedHits, lastCombatHealth;
    private long fightStart, combatTicks;
    private double initialGround, minGround, maxGround;
    private kw industrialSource, industrialSink, collector, extractor, engine;
    private SabotageTarget industrialTarget;
    private String industrialType;
    private int baselineDelivery, pauseDelivery, pauseReserve, pauseFlight, pauseCollector, deliveredWhilePaused;
    private int strokesDuringPause, priorStrokePart, redstoneEdgesDuringPause;
    private boolean engineWarmed, signal;
    private long baselineTick;

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
                File running = new File(EntitySaboteur.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI()).getCanonicalFile();
                check(running.equals(new File(directory, "mods/ZedsTekkitOverhaul.jar").getCanonicalFile()),
                        "load the entity from the deployed candidate archive");
                List<String> jvmOptions = ManagementFactory.getRuntimeMXBean().getInputArguments();
                for (String option : jvmOptions) check(!option.startsWith("-javaagent:"), "agent-free JVM");
                System.out.println("SABOTEUR JVM options=" + jvmOptions);
                System.out.println("SABOTEUR Java " + System.getProperty("java.version")
                        + ", audio backend uninitialized, Bridge=" + Boolean.getBoolean("zto.devBridge.enabled"));
                open(game);
                hopperId = HopperRegistry.block.bO;
                if ("reopen".equals(mode)) confirmReopen(game);
                else if ("transport-reopen".equals(mode)) reopenTransport(game);
                else if ("industrial-power-reopen".equals(mode)) reopenIndustrialPower(game);
                else if ("industrial-redpower-reopen".equals(mode)) reopenIndustrialRedPower(game);
                else setup(game);
                return;
            }
            if (game.f.w() == tick) return;
            tick = game.f.w();
            long age = tick - since;
            check(age < 340, "stage " + stage + " timed out" + state() + " target=" + industrialType
                    + " reserve=" + pauseReserve + " delivered=" + pauseDelivery);
            if ("reopen".equals(mode) || "dimension".equals(mode) && stage == 3) {
                if (count(output) > pausedOutput) finish(game);
            } else if ("stress".equals(mode)) {
                if (age % 20 == 0) game.h.l(20);
                if (age >= 120) stress(game);
            } else if (mode.startsWith("transport")) {
                transport(game, age);
            } else if (mode.startsWith("industrial")) {
                industrial(game, age);
            } else if ("combat".equals(mode)) {
                combat(game, age);
            } else if ("terrain".equals(mode)) {
                terrain(game, age);
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
        if ("terrain".equals(mode)) {
            setupTerrain(game);
            advance(game);
            return;
        }
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
        if ("smoke".equals(mode)) {
            spawn(game, HOPPER_X + 0.5D, Z + 0.5D);
            finish(game);
            return;
        } else if (mode.startsWith("industrial")) {
            setupIndustrial(game);
        } else if (mode.startsWith("transport")) {
            setupTransport(game);
        } else if ("combat".equals(mode)) {
            startFight(game, 1);
        } else if ("stress".equals(mode)) {
            game.f.d(HOPPER_X, Y, Z - 1, 61, 3);
            ((io) game.f.b(HOPPER_X, Y, Z - 1)).a(2, new aan(ITEM, 5, 0));
            for (int i = 0; i < 8; i++) spawn(game, X + 4.5D + (i % 4), Z - 0.5D + (i / 4) * 2);
            stressStarted = System.nanoTime();
            stressCpu = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
            stressHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
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

    private void setupTerrain(Minecraft game) {
        for (int x = X - 20; x <= X + 16; x++) {
            for (int z = Z - 20; z <= Z + 20; z++) {
                if (!game.f.z().a(x >> 4, z >> 4) || !game.f.z().a((x + 4) >> 4, z >> 4)) continue;
                int lower = game.f.g(x, z), upper = game.f.g(x + 4, z);
                if (upper - lower != 1 || game.f.a(x, lower - 1, z) != 2
                        || game.f.a(x + 4, upper - 1, z) != 2) continue;
                boolean continuous = true;
                for (int dx = 1; dx < 4; dx++) {
                    int y = game.f.g(x + dx, z);
                    if ((y != lower && y != upper) || game.f.a(x + dx, y - 1, z) != 2) continuous = false;
                }
                if (!continuous) continue;
                game.h.c(x + 4.5D, upper, z + 0.5D, 90, 8);
                mob = spawnAt(game, x + 0.5D, lower, z + 0.5D);
                initialGround = minGround = maxGround = mob.y.b;
                priorX = mob.o;
                priorZ = mob.q;
                System.out.println("SABOTEUR generated terrain ground " + lower + " to " + upper + " at " + x + "," + z);
                return;
            }
        }
        throw new AssertionError("No generated grass slope in the loaded fixture area");
    }

    private void terrain(Minecraft game, long age) throws Exception {
        minGround = Math.min(minGround, mob.y.b);
        maxGround = Math.max(maxGround, mob.y.b);
        if (age < 100) return;
        double travel = Math.hypot(mob.o - priorX, mob.q - priorZ);
        check(travel > 1 && mob.scans() > 0 && maxGround > initialGround + 0.75D,
                "actor moved and climbed in generated terrain" + state());
        System.out.println("SABOTEUR terrain traveled " + travel + " blocks; ground range=" + minGround + ".."
                + maxGround + "; " + state());
        finish(game);
    }

    private void setupTransport(Minecraft game) throws Exception {
        game.f.d(HOPPER_X - 1, Y, Z, DistillationRegistry.blockId(DistillationPartType.BOILER), 2);
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler) game.f.b(HOPPER_X - 1, Y, Z);
        for (int i = 0; i < 6; i++) {
            DistillationLayout.Placement p = DistillationLayout.placement(HOPPER_X - 1, Y, Z,
                    DistillationLayout.Facing.EAST, i);
            game.f.d(p.x, p.y, p.z, DistillationRegistry.blockId(p.part), 2);
        }
        // Seed the ordinary fluid buffer; all transfers below are native BuildCraft ticks.
        int fraction = DistillateType.GASOLINE.id;
        boiler.getProcess().restore(0, 0, FLUID_TOTAL, fraction, fraction, 0);
        int tankId = ((pb) staticField("BuildCraftFactory", "tankBlock")).bO;
        placeWoodLine(game, HOPPER_X, tankId);
        int controlX = HOPPER_X - 5;
        game.f.g(controlX, Y + 1, Z, tankId);
        placeWoodLine(game, controlX, tankId);
        readTransport(game);
        liquidId = receiver.getLiquidId();
        check(controlSource.fill(Orientations.YPos, FLUID_TOTAL, liquidId, true) == FLUID_TOTAL, "seed control tank");
        player(game, HOPPER_X + 4.4D, Z - 2.25D, 60);
    }

    private void setupIndustrial(Minecraft game) throws Exception {
        int x = HOPPER_X;
        if (powerFixture()) {
            aan collectorBlock = (aan) staticField("ee.EEBlock", "collector");
            aan receiverBlock = (aan) staticField("ic2.common.Ic2Items", "batBox");
            game.f.d(x, Y, Z - 1, collectorBlock.c, collectorBlock.i());
            game.f.g(x, Y, Z, 3024);
            game.f.d(x, Y, Z + 1, receiverBlock.c, receiverBlock.i());
            readIndustrialPower(game);
        } else {
            game.f.d(x, Y, Z, hopperId, 0);
            industrialSource = game.f.b(x, Y, Z);
            industrialTarget = (TileHopper) industrialSource;
            ((io) industrialSource).a(0, new aan(ITEM, 64, 0));
            game.f.g(x, Y, Z + 2, 54);
            industrialSink = game.f.b(x, Y, Z + 2);
            if (redPowerFixture()) {
                int machine = ((pb) staticField("RedPowerMachine", "blockMachine")).bO;
                game.f.d(x, Y, Z + 1, machine, 2);
                extractor = game.f.b(x, Y, Z + 1);
                extractor.getClass().getField("Rotation").setInt(extractor, 3);
                industrialType = "REDPOWER_HOPPER_EXTRACTION";
            } else {
                yr pipe = (yr) staticField("BuildCraftTransport", "pipeItemsWood");
                check(pipe.a(new aan(pipe), game.h, game.f, x, Y, Z, 3), "place native wood extractor");
                game.f.f(x, Y, Z + 1, 2);
                extractor = game.f.b(x, Y, Z + 1);
                pipeTransport = pipeTransport(extractor);
                game.f.d(x + 1, Y, Z + 1, ((pb) staticField("BuildCraftEnergy", "engineBlock")).bO, 0);
                engine = game.f.b(x + 1, Y, Z + 1);
                engine.getClass().getField("orientation").setInt(engine, 4);
                game.f.d(x + 2, Y, Z + 1, 76, 5);
                industrialType = "BUILDCRAFT_HOPPER_EXTRACTION";
            }
        }
        player(game, x + 4.3D, Z + 2.2D, 100);
        baselineTick = game.f.w();
        System.out.println("SABOTEUR " + industrialType + " source=" + industrialSource.getClass().getName()
                + " sink=" + industrialSink.getClass().getName()
                + (extractor == null ? " collector=" + collector.getClass().getName()
                        : " extractor=" + extractor.getClass().getName()));
    }

    private boolean powerFixture() { return mode.startsWith("industrial-power"); }
    private boolean redPowerFixture() { return mode.startsWith("industrial-redpower"); }

    private void readIndustrialPower(Minecraft game) {
        collector = game.f.b(HOPPER_X, Y, Z - 1);
        industrialSource = game.f.b(HOPPER_X, Y, Z);
        industrialTarget = (SabotageTarget) industrialSource;
        industrialSink = game.f.b(HOPPER_X, Y, Z + 1);
        industrialType = "ALCHEMICAL_POWER_LINK";
    }

    private void reopenIndustrialPower(Minecraft game) throws Exception {
        readIndustrialPower(game);
        check(!industrialTarget.isDisrupted(), "converter clears lease on reopen");
        pauseDelivery = number(industrialSink, "energy");
        System.out.println("SABOTEUR reopened with converter=" + converterReserve()
                + " packet units, collector=" + number(collector, "collectorSunTime") + ", BatBox=" + pauseDelivery + " EU");
        stage = 9;
        advance(game);
    }

    private void reopenIndustrialRedPower(Minecraft game) {
        industrialSource = game.f.b(HOPPER_X, Y, Z);
        extractor = game.f.b(HOPPER_X, Y, Z + 1);
        industrialSink = game.f.b(HOPPER_X, Y, Z + 2);
        industrialTarget = (TileHopper) industrialSource;
        check(!industrialTarget.isDisrupted(), "Hopper clears native extraction lease on reopen");
        pauseDelivery = count((io) industrialSink);
        signal = game.f.a(HOPPER_X + 1, Y, Z + 1) == 76;
        stage = 9;
        advance(game);
    }

    private void industrial(Minecraft game, long age) throws Exception {
        check(game.f.b(industrialSource.j, industrialSource.k, industrialSource.l) == industrialSource
                && game.f.b(industrialSink.j, industrialSink.k, industrialSink.l) == industrialSink, "endpoint identity");
        if (extractor != null)
            check(game.f.b(extractor.j, extractor.k, extractor.l) == extractor, "extractor identity");
        int reserve = powerFixture() ? converterReserve() : count((io) industrialSource);
        int delivered = powerFixture() ? number(industrialSink, "energy") : count((io) industrialSink);
        int collectorUnits = powerFixture() ? number(collector, "collectorSunTime") : 0;
        int flight = pipeTransport == null ? 0 : flyingItems();
        if (!powerFixture()) check(reserve + delivered + flight == 64, "account for all 64 items");
        if (redPowerFixture() && tick % 12 == 0) {
            signal = !signal;
            game.f.d(HOPPER_X + 1, Y, Z + 1, signal ? 76 : 0, signal ? 5 : 0);
            if (signal && stage == 3 && age <= 76) redstoneEdgesDuringPause++;
        }
        if (stage == 10) {
            if (delivered > pauseDelivery) {
                System.out.println("SABOTEUR " + (powerFixture() ? "BatBox EU" : "chest items")
                        + " after reopen " + pauseDelivery + " -> " + delivered);
                finish(game);
            }
            return;
        }
        if (engine != null && !engineWarmed) {
            Object nativeEngine = member(engine, "engine");
            if (nativeEngine == null) return;
            nativeEngine.getClass().getField("energy").setInt(nativeEngine, 530);
            engineWarmed = true;
        }
        if (stage == 1 && delivered > 0 && (engine == null || flight > 0)) {
            baselineDelivery = delivered;
            mob = spawn(game, HOPPER_X - 1.0D, Z + 0.35D);
            check(mob.canEngageEncounterPlayer(game.h), "vulnerable player in the native fixture");
            mob.engageEncounterPlayer(game.h);
            field(EntitySaboteur.class, "nextScan").setInt(mob, 1);
            System.out.println("SABOTEUR baseline ticks=" + (tick - baselineTick) + " delivered=" + delivered
                    + " reserve=" + reserve + " in-flight=" + flight + " collector=" + collectorUnits);
            advance(game);
        } else if (stage == 2 && mob.disruptions() == 1) {
            check(industrialType.equals(mob.targetType()) && industrialTarget.isDisrupted(),
                    "actor selected intended endpoint, got " + mob.targetType());
            pauseReserve = reserve;
            pauseDelivery = delivered;
            pauseFlight = flight;
            pauseCollector = collectorUnits;
            field(EntitySaboteur.class, "nextAction").setInt(mob, mob.V + 500);
            if (engine != null) priorStrokePart = number(engine, "progressPart");
            advance(game);
        } else if (stage == 3 && age <= 76) {
            check(reserve == pauseReserve, "new withdrawals paused, reserve=" + reserve + " expected=" + pauseReserve);
            check(delivered - pauseDelivery <= pauseFlight, "only committed in-flight payloads can reach the receiver");
            deliveredWhilePaused = delivered - pauseDelivery;
            if (engine != null) {
                int part = number(engine, "progressPart");
                if (part == 2 && priorStrokePart == 1) strokesDuringPause++;
                priorStrokePart = part;
            }
            if (powerFixture())
                check(collectorUnits - pauseCollector == 16 * age,
                        "EE2 stores the unaccepted production at 16 units per tick, observed "
                                + (collectorUnits - pauseCollector) + " at tick " + age);
            if (mode.endsWith("-quit") && age == 32) {
                System.out.println("SABOTEUR Save and Quit during pause: delivered=" + delivered
                        + ", reserve=" + reserve + ", EE2 packet units=" + collectorUnits);
                finish(game);
            }
        } else if (stage == 3 && age >= 80 && delivered > pauseDelivery + pauseFlight) {
            check(mob.disruptions() == 1 && mob.bb() > 0, "natural recovery while the actor is alive");
            if (engine != null) check(strokesDuringPause > 0, "native engine continued while source slots were withheld");
            if (extractor != null && engine == null) check(redstoneEdgesDuringPause > 0, "native redstone clock continued");
            System.out.println("SABOTEUR before=" + baselineDelivery + " at pause=" + pauseDelivery
                    + " held reserve=" + pauseReserve + " committed in-flight=" + pauseFlight
                    + " delivered during pause=" + deliveredWhilePaused + " engine strokes=" + strokesDuringPause
                    + " redstone rising edges=" + redstoneEdgesDuringPause + " collector at pause=" + pauseCollector
                    + " collector after=" + collectorUnits + " restored=" + delivered + " ticks since lease=" + age);
            finish(game);
        }
    }

    private int flyingItems() throws Exception {
        int flight = 0;
        Map<?, ?> traveling = (Map<?, ?>) member(pipeTransport, "travelingEntities");
        for (Object data : traveling.values()) {
            aan stack = (aan) member(member(data, "item"), "item");
            check(stack.c == ITEM && stack.i() == 0, "in-flight item identity");
            flight += stack.a;
        }
        return flight;
    }

    private int converterReserve() throws Exception {
        Method stored = industrialSource.getClass().getDeclaredMethod("storedUnits");
        stored.setAccessible(true);
        return ((Number) stored.invoke(industrialSource)).intValue();
    }

    private static int number(Object object, String name) throws Exception {
        return ((Number) member(object, name)).intValue();
    }

    private static Object member(Object object, String name) throws Exception {
        return object.getClass().getField(name).get(object);
    }

    private void readTransport(Minecraft game) throws Exception {
        receiver = (TileEntityDistillationReceiver) game.f.b(HOPPER_X, Y + 1, Z);
        tank = (ILiquidContainer) game.f.b(HOPPER_X, Y + 1, Z + 2);
        woodPipe = game.f.b(HOPPER_X, Y + 1, Z + 1);
        pipeTransport = pipeTransport(woodPipe);
        int controlX = HOPPER_X - 5;
        controlSource = (ILiquidContainer) game.f.b(controlX, Y + 1, Z);
        controlTank = (ILiquidContainer) game.f.b(controlX, Y + 1, Z + 2);
        controlPipe = game.f.b(controlX, Y + 1, Z + 1);
        controlTransport = pipeTransport(controlPipe);
    }

    private void placeWoodLine(Minecraft game, int x, int tankId) throws Exception {
        game.f.g(x, Y + 1, Z + 2, tankId);
        yr pipe = (yr) staticField("BuildCraftTransport", "pipeLiquidsWood");
        check(pipe.a(new aan(pipe), game.h, game.f, x, Y + 1, Z, Orientations.ZPos.ordinal()), "place native pipe");
        game.f.f(x, Y + 1, Z + 1, Orientations.ZNeg.ordinal());
        int engine = ((pb) staticField("BuildCraftEnergy", "engineBlock")).bO;
        game.f.d(x + 1, Y + 1, Z + 1, engine, 0);
        Object engineTile = game.f.b(x + 1, Y + 1, Z + 1);
        engineTile.getClass().getField("orientation").setInt(engineTile, Orientations.XNeg.ordinal());
        game.f.g(x + 1, Y, Z + 2, 1);
        game.f.d(x + 1, Y + 1, Z + 2, 76, 5);
    }

    private void reopenTransport(Minecraft game) throws Exception {
        readTransport(game);
        liquidId = receiver.getLiquidId();
        check(!receiver.isDisrupted() && receiver.blockedOffers() == 0 && receiver.getLiquidQuantity() > 0,
                "native reopen has a stocked receiver and a clean endpoint lease");
        atPause = receiver.getLiquidQuantity();
        paidOut = tank.getLiquidQuantity();
        stage = 3;
        advance(game);
    }

    private static Object staticField(String owner, String name) throws Exception {
        return Class.forName(owner).getField(name).get(null);
    }

    private static Object pipeTransport(Object tile) throws Exception {
        Object pipe = tile.getClass().getField("pipe").get(tile);
        return pipe.getClass().getField("transport").get(pipe);
    }

    private int pipeQuantity(Object transport) throws Exception {
        Object center = transport.getClass().getField("center").get(transport);
        int total = bufferQuantity(center);
        for (Object side : (Object[]) transport.getClass().getField("side").get(transport)) total += bufferQuantity(side);
        return total;
    }

    private int bufferQuantity(Object buffer) throws Exception {
        int amount = field(buffer.getClass(), "qty").getShort(buffer);
        check(amount >= 0 && (amount == 0 || buffer.getClass().getField("liquidId").getShort(buffer) == liquidId),
                "pipe conserves fluid type");
        return amount;
    }

    private void transport(Minecraft game, long age) throws Exception {
        check(game.f.b(HOPPER_X, Y + 1, Z + 1) == woodPipe
                && pipeTransport(woodPipe) == pipeTransport && game.f.b(HOPPER_X, Y + 1, Z) == receiver,
                "receiver, pipe tile and transport identities");
        int reserve = receiver.getLiquidQuantity(), delivered = tank.getLiquidQuantity();
        boolean extractedThisTick = reserve < previousReserve;
        previousReserve = reserve;
        int controlDelivered = controlTank.getLiquidQuantity();
        check(reserve + pipeQuantity(pipeTransport) + delivered == FLUID_TOTAL, "supported fluid conservation");
        check(controlSource.getLiquidQuantity() + pipeQuantity(controlTransport) + controlDelivered == FLUID_TOTAL,
                "standalone wood pipe fluid conservation");
        check((delivered == 0 || tank.getLiquidId() == liquidId)
                && (controlDelivered == 0 || controlTank.getLiquidId() == liquidId), "tank retains fluid type");
        if (stage == 1 && delivered > 0 && controlDelivered > 0 && extractedThisTick
                && receiver.activeExtractionPipe() == woodPipe) {
            player(game, HOPPER_X + 4.4D, Z - 2.25D, 60);
            mob = spawn(game, HOPPER_X + 1.5D, Z - 0.5D);
            check(mob.canEngageEncounterPlayer(game.h), "vulnerable player near the exposed receiver");
            mob.engageEncounterPlayer(game.h);
            // Align the first scan with the live, newly observed feed from the stock engine.
            field(EntitySaboteur.class, "nextScan").setInt(mob, 1);
            advance(game);
        } else if (stage == 2 && !receiver.isDisrupted()) {
            if (age % 10 == 0) player(game, HOPPER_X + 4.4D, Z - 2.25D, 60);
        } else if (stage == 2 && receiver.isDisrupted()) {
            check(mob.disruptions() == 1 && "BUILDCRAFT_RECEIVER".equals(mob.targetType()), "real endpoint lease");
            atPause = reserve;
            controlAtPause = controlDelivered;
            System.out.println("SABOTEUR BuildCraft lease began with " + pipeQuantity(pipeTransport) + " mB in flight");
            advance(game);
        } else if (stage == 3) {
            if (receiver.isDisrupted()) {
                check(reserve == atPause, "source stops draining at the gated endpoint");
                if (age >= 5 && receiver.blockedOffers() > 0 && !"transport".equals(mode)) {
                    System.out.println("SABOTEUR BuildCraft lifecycle checkpoint: "
                            + pipeQuantity(pipeTransport) + " mB in flight, blockedOffers=" + receiver.blockedOffers());
                    if ("transport-halt".equals(mode)) {
                        game.f.a((rw) null);
                        System.out.println("SABOTEUR PASS transport-halt");
                        System.out.flush();
                        Runtime.getRuntime().halt(23);
                    } else {
                        saveQuit(game);
                        open(game);
                        reopenTransport(game);
                    }
                    return;
                }
                if (age == 20) screenshot(game, "buildcraft-paused");
                if (pipeQuantity(pipeTransport) == 0) {
                    if (blockedFor++ == 0) paidOut = delivered;
                    check(delivered == paidOut, "finished pipe buffer stays empty during the pause");
                }
            } else {
                check(receiver.blockedOffers() > 0 && blockedFor > 0 && controlDelivered > controlAtPause,
                        "actual interrupted offers, exhausted in-flight payload and continued standalone line");
                System.out.println("SABOTEUR BuildCraft endpoint paused for " + age + " ticks, blockedOffers="
                        + receiver.blockedOffers() + ", sourceMB=" + atPause + ", deliveredMB=" + paidOut
                        + ", idleTicks=" + blockedFor + ", standaloneDeliveredMB=" + controlDelivered);
                advance(game);
            }
        } else if (stage == 4 && reserve < atPause && delivered > paidOut) {
            System.out.println("SABOTEUR BuildCraft resumed after " + age + " ticks, conserved " + FLUID_TOTAL
                    + " mB on each line, liquidId=" + liquidId);
            finish(game);
        }
    }

    private void startFight(Minecraft game, int fight) {
        double x = X + 0.5D, z = Z + 5.5D;
        if (fight == 4) {
            for (int dx = -4; dx <= 1; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = Y; y <= Y + 1; y++) game.f.g(X + dx, y, Z + 5 + dz, 0);
                }
            }
        }
        if (fight == 2 || fight == 3) {
            for (int dz = -2; dz <= 2; dz++)
                for (int y = Y; y <= Y + 1; y++) game.f.g(X + 1, y, Z + 5 + dz, 1);
        }
        if (fight == 3) {
            for (int dx = -4; dx <= 1; dx++) {
                for (int y = Y; y <= Y + 1; y++) {
                    game.f.g(X + dx, y, Z + 4, 1);
                    game.f.g(X + dx, y, Z + 6, 1);
                }
            }
        }
        if (fight == 5) {
            // Leave the pad edge and ordinary generated terrain ahead of the shooter.
            x = X - 9.5D;
            z = Z - 4.5D;
        }
        if (fight == 5) {
            player(game, x + 7, z, 90);
        } else {
            player(game, x - 1.15D, z, -90);
        }
        mob = spawn(game, x, z);
        lastCombatHealth = mob.bb();
        fightStart = game.f.w();
        priorBlinks = 0;
    }

    private void combat(Minecraft game, long age) throws Exception {
        if (captureEvade) {
            screenshot(game, "evade");
            captureEvade = false;
        }
        if (age % 20 == 0) game.h.l(20);
        if (stage == 5) {
            if (mob.bb() < lastCombatHealth) rangedHits++;
            lastCombatHealth = mob.bb();
            if (age == 8 || age == 25 || age == 42 || age == 59) {
                double dx = mob.o - game.h.o, dz = mob.q - game.h.q;
                double range = Math.hypot(dx, dz);
                nm arrow = new nm(game.f, game.h, 2.2F);
                double height = game.h.y.b + 1.5D;
                arrow.d(game.h.o, height, game.h.q);
                arrow.a(dx, mob.y.b + 0.55D - height + 0.02D * range, dz, 2.2F, 0);
                check(game.f.a(arrow), "native arrow projectile inserted");
            }
        }
        if (mob.blinks() > priorBlinks) {
            combatBlinks += mob.blinks() - priorBlinks;
            if (stage == 2 && priorBlinks == 0 && mob.o <= X + 0.78D
                    && Math.abs(mob.q - (Z + 5.5D)) > 2) {
                System.out.println("SABOTEUR combat lateral escape beside wall at " + mob.o + "," + mob.q);
                double dx = mob.o - game.h.o, dz = mob.q - game.h.q;
                player(game, game.h.o, game.h.q, (float) Math.toDegrees(Math.atan2(dz, dx)) - 90);
                game.h.v = 14;
                captureEvade = true;
            }
            priorBlinks = mob.blinks();
        }
        if (stage < 5 && age > 14 && age % 11 == 0 && mob.bb() > 0) {
            // A pursuing player closes on the new location; the native Survival controller lands the blow.
            if (stage == 1 || stage == 2) player(game, mob.o - 1.13D, mob.q, -90);
            if (Math.hypot(game.h.o - mob.o, game.h.q - mob.q) < 2) {
                int before = mob.bb();
                game.c.b(game.h, mob);
                if (before > mob.bb()) realHits++;
            }
        }
        if (stage == 3 && age < 12) check(mob.o < X + 1, "cannot evade through sealed rear wall");
        if (mob.bb() <= 0 || age >= (stage == 3 ? 175 : 100)) {
            if (mob.bb() <= 0) finishedFights++;
            int attempts = mob.blinkAttempts();
            long ticks = game.f.w() - fightStart;
            combatTicks += ticks;
            System.out.println("SABOTEUR combat engagement=" + stage + " ticks=" + ticks + " health=" + mob.bb()
                    + " blinks=" + mob.blinks() + "/" + attempts + " result=" + mob.lastBlinkResult());
            if (stage < 5) {
                if (mob.bb() > 0) mob.a(md.j, 100);
                startFight(game, stage + 1);
                advance(game);
            } else {
                check(realHits >= 6 && finishedFights > 0, "Survival strikes land and finish a Saboteur");
                check(rangedHits > 0, "arrows in flight can land meaningful damage");
                System.out.println("SABOTEUR combat engagements=5, landedHits=" + realHits + ", rangedHits="
                        + rangedHits + ", kills=" + finishedFights
                        + ", blinks=" + combatBlinks + ", blinksPerMinute=" + 1200.0 * combatBlinks / combatTicks
                        + ", blinksPerEngagement=" + combatBlinks / 5.0);
                finish(game);
            }
        }
    }

    private void exercise(Minecraft game, long age) throws Exception {
        identity(game);
        if (stage == 11 && leavingChunk) {
            player(game, HOPPER_X - 2.5D, Z + 10.5D, -180);
        } else if (stage >= 3 && stage <= 12 || stage == 2 && capturedCancellation) {
            double x = stage == 10 ? HOPPER_X + 2.5D : HOPPER_X - 2.5D;
            lookAtFixture(game, x, Z + 3.7D);
            if (age % 20 == 0) game.h.l(20);
        }
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
                check(mob.blinkChecks() <= mob.blinkAttempts() * 6, "bounded blink attempts");
                check(mob.scannedBlocks() <= mob.scans() * 17 * 17 * 5, "bounded scan work");
                check(mob.pathPlans() > 0 && mob.pathPlans() <= mob.V / 20 + 1, "bounded path requests");
                screenshot(game, "windup");
                if ("halt".equals(mode) || "dimension".equals(mode)) { advance(game); return; }
                player(game, mob.o - 0.9D, mob.q + 0.75D, -120);
                int health = mob.bb();
                game.c.b(game.h, mob);
                check(mob.bb() < health && mob.pose() == EntitySaboteur.CANCELLED && "HIT".equals(mob.lastOutcome()),
                        "landed Survival hit cancels windup" + state());
                check(mob.actionCooldown() == 120 && !hopper.isDisrupted(), "cancelled action imposes cooldown without pause");
                advance(game);
                return;
            case 2:
                if ("exercise".equals(mode) && !capturedCancellation && age >= 3) {
                    screenshot(game, "cancelled");
                    capturedCancellation = true;
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
                    game.f.a((rw) null); // Save world and wait for the native chunk I/O queue.
                    System.out.println("SABOTEUR PASS halt");
                    System.out.flush();
                    Runtime.getRuntime().halt(23);
                }
                if ("dimension".equals(mode)) {
                    switchDimension(game);
                    return;
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
                System.out.println("SABOTEUR replacement cancelled the pending windup");
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
                System.out.println("SABOTEUR death recovery ticks=" + age);
                mob = spawn(game, HOPPER_X - 1.5D, Z + 0.5D);
                contender = spawn(game, HOPPER_X + 0.5D, Z + 2.2D);
                // Fix two valid acquisition offsets so both candidates commit to the same target.
                Field scan = field(EntitySaboteur.class, "nextScan");
                scan.setInt(mob, 1);
                scan.setInt(contender, 3);
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
                        "overlapping windups produce one lease and one cancellation: loser windups="
                                + loser.windups() + " cancellations=" + loser.cancelled() + " pose=" + loser.pose());
                System.out.println("SABOTEUR overlapping attempts produced one lease and one cancellation");
                mob = winner;
                check(loser.a(md.j, 100) && hopper.isDisrupted(), "loser's death leaves the winner's lease in force");
                contender = null;
                lookAtFixture(game, HOPPER_X - 2.5D, Z + 3.7D);
                advance(game);
                return;
            case 10:
                if (age < 4) return;
                check(hopper.isDisrupted(), "winning lease still present before Save and Quit");
                if (age == 22) screenshot(game, "torchlight");
                if (age < 23) return;
                pausedOutput = count(output);
                saveQuit(game);
                open(game);
                confirmReopen(game);
                return;
            case 11:
                if (count(output) <= pausedOutput) return;
                if (!hopper.isDisrupted()) return;
                if (!leavingChunk) {
                    leavingChunk = true;
                    return;
                }
                if (game.h.ag == (Z >> 4)) return;
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

    private void switchDimension(Minecraft game) throws Exception {
        xd overworld = game.f;
        player(game, HOPPER_X - 9.5D, Z + 10.5D, -90);
        game.a(-1);
        check(game.f != overworld && !hopper.isDisrupted(), "native portal travel invalidated the old world's lease");
        game.a(0);
        game.f.z().c(HOPPER_X >> 4, Z >> 4);
        confirmReopen(game);
        System.out.println("SABOTEUR native dimension round trip discarded the prior lease");
    }

    private void confirmReopen(Minecraft game) throws Exception {
        readFixture(game);
        identity(game);
        check(!hopper.isDisrupted(), "lease absent as soon as the save reopens");
        List<EntitySaboteur> saved = mobs(game);
        check(saved.size() == 1, "one persisted Saboteur");
        mob = saved.get(0);
        check(TacticalHostiles.SABOTEUR.equals(aao.b(mob)) && mob.pose() == EntitySaboteur.IDLE
                && mob.disruptionTarget() == null && mob.actionCooldown() == 0
                && field(TacticalHostile.class, "target").get(mob) == null,
                "native name and clean tactical state after world reload" + state());
        System.out.println("SABOTEUR reopened with native name, clean target and all " + TOTAL + " items");
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
        long started = System.nanoTime();
        for (int i = 0; i < 96; i++) expectBlink(check, sample, 2.75D, "CLEAR");
        System.out.println("SABOTEUR candidate validation 96 open sweeps meanNs="
                + (System.nanoTime() - started) / 96 + " including harness reflection");
        for (int x = X; x <= X + 3; x++) {
            game.f.g(x, Y, z - 1, 1);
            game.f.g(x, Y, z + 1, 1);
        }
        expectBlink(check, sample, 2.75D, "CLEAR");
        game.f.g(X + 1, Y, z, 1);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 20);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 0);
        game.f.b(X + 1, Y + 1, z, 64, 8);
        game.f.d(X + 1, Y, z, 64, 0);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 0);
        game.f.g(X + 1, Y + 1, z, 0);
        game.f.g(X + 1, Y, z, 44);
        expectBlink(check, sample, 2.75D, "OBSTRUCTED");
        game.f.g(X + 1, Y, z, 53);
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
        Method scan = EntitySaboteur.class.getDeclaredMethod("scan");
        scan.setAccessible(true);
        scan.invoke(sample);
        check(sample.scans() == 1 && sample.scannedBlocks() == 0, "scan skips unloaded columns");
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
            check(entity.blinkChecks() <= 6 * entity.blinkAttempts(), "stress candidate bound");
            check(entity.scans() <= entity.V / 24 + 1 && entity.pathPlans() <= entity.V / 20 + 1,
                    "per-actor scan and planning cadence");
        }
        check(diversions > 0, "fallback produces harmless diversion");
        check(game.f.a(HOPPER_X, Y, Z - 1) == 61 && game.f.e(HOPPER_X, Y, Z - 1) == 3
                && count((io) game.f.b(HOPPER_X, Y, Z - 1)) == 5, "unsupported furnace state preserved");
        System.out.println("SABOTEUR stress actors=8 ticks=120 realMs=" + (System.nanoTime() - stressStarted) / 1000000
                + " scans=" + scans + " pathPlans=" + plans + " blinkChecks=" + checks + " diversions=" + diversions
                + " entities=" + game.f.b.size());
        long currentCpu = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
        System.out.println("SABOTEUR stress clientThreadCpuMs=" + (stressCpu < 0 || currentCpu < 0
                ? "unsupported" : Long.toString((currentCpu - stressCpu) / 1000000))
                + " heapBytesStart=" + stressHeap + " heapBytesEnd=" + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        screenshot(game, "stress");
        finish(game);
    }

    private static EntitySaboteur spawn(Minecraft game, double x, double z) {
        return spawnAt(game, x, Y, z);
    }

    private static EntitySaboteur spawnAt(Minecraft game, double x, int y, double z) {
        EntitySaboteur actor = (EntitySaboteur) aao.a(TacticalHostiles.SABOTEUR, game.f);
        check(actor != null && TacticalHostiles.SABOTEUR.equals(aao.b(actor)), "native Saboteur registration");
        actor.c(x, y, z, 0, 0);
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
