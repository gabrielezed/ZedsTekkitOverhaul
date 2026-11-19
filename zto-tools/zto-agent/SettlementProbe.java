import buildcraft.api.PowerFramework;
import buildcraft.core.TileBuildCraft;
import buildcraft.energy.PneumaticPowerFramework;
import buildcraft.factory.EntityMechanicalArm;
import buildcraft.factory.TileQuarry;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import forge.ISaveEventHandler;
import forge.MinecraftForge;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.settlement.SettlementRecognition;
import zed.tekkitoverhaul.settlement.SettlementRecognition.Snapshot;
import zed.tekkitoverhaul.settlement.WorldProvenance;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Exercises native placement, movement, utility activation and Save & Quit persistence without a window. */
public final class SettlementProbe implements ISaveEventHandler {
    private static final int X = 320, Y = 4, Z = 320, PLANK = 5, BENCH = 58, CHEST = 54, FURNACE = 61;
    private final File root;
    private final Minecraft game;
    private final aes controller;
    private final WorldProvenance provenance = new WorldProvenance();
    private final TerritorialActivityService territory = new TerritorialActivityService();
    private final SettlementRecognition settlements = new SettlementRecognition(territory, provenance);
    private Fixture world;
    private vq player;
    private int utilityOpens;

    public static void main(String[] args) {
        try {
            SettlementProbe probe = new SettlementProbe(new File(args[0]));
            if (args.length == 2 && "cabin".equals(args[1])) {
                probe.suspendedCabins();
                System.exit(0);
            }
            if (args.length == 2 && "mutations".equals(args[1])) {
                probe.surveyMutations();
                System.exit(0);
            }
            if (args.length == 2 && "settlement-profile".equals(args[1])) {
                probe.profile();
                System.exit(0);
            }
            probe.suspendedCabins();
            probe.surveyMutations();
            probe.sites();
            probe.negatives();
            probe.village();
            probe.stress();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private void profile() throws Exception {
        int feet = 83;
        lowCabin("profile-small-cabin", feet);
        position(103, feet, 232);
        walk();
        measureTicks("cabin center", 103, feet, 232, false, false);
        measureTicks("cabin entrance", 104, feet, 236, true, false);
        for (int z = 237; z < 242; z++) world.g(104, feet - 1, z, 1);
        measureTicks("cabin movement", 104, feet, 236, false, true);
        SettlementProfile.component("cabin center", world, provenance, 103, feet, 232);

        open("profile-industrial", false);
        prepare(X, Z);
        industrialWorkshop(X, feet, Z);
        measureTicks("dense industrial workshop", X, feet, Z, false, false);
        SettlementProfile.component("dense industrial workshop", world, provenance, X, feet, Z);
        profileRemoteQuarry(feet);
        WorldProvenance.Snapshot store = provenance.snapshot(world, X, feet, Z);
        check(store.hooks == 7, "profile uses the three native mutation hooks", here());
        profilePressure(feet);
        SettlementProfile.provenance(root.toPath());
    }

    private void industrialWorkshop(int centerX, int feet, int centerZ) {
        for (int x = centerX - 6; x <= centerX + 6; x++) {
            for (int z = centerZ - 6; z <= centerZ + 6; z++) {
                for (int y = feet - 3; y <= feet + 5; y++) {
                    if (Math.abs(x - centerX) > 2 || Math.abs(z - centerZ) > 2 || y < feet || y > feet + 1) {
                        int block = y == feet && ((x + z) & 3) == 0 ? FURNACE
                                : y == feet + 1 && ((x + z) & 3) == 1 ? 33 : PLANK;
                        world.g(x, y, z, block);
                    }
                }
            }
        }
    }

    private void measureTicks(String label, int x, int y, int z, boolean recentered, boolean moving) {
        position(x, y, z);
        ticks(3000);
        nextSurvey();
        int centers = 0, followUps = 0, cancelled = 0;
        SettlementProfile.Samples idle = new SettlementProfile.Samples();
        SettlementProfile.Samples slice = new SettlementProfile.Samples();
        SettlementProfile.Samples finish = new SettlementProfile.Samples();
        SettlementProfile.Samples shiftedSlice = new SettlementProfile.Samples();
        SettlementProfile.Samples shiftedFinish = new SettlementProfile.Samples();
        SettlementProfile.Samples cancel = new SettlementProfile.Samples();
        Snapshot before = here();
        boolean shifted = false;
        int remaining = 3599;
        while (remaining-- > 0) {
            if (moving && before.queued) player.b(0, 0, 0.6);
            boolean leaving = moving && player.q >= z + 4;
            territory.tick(player, false);
            long start = System.nanoTime();
            settlements.tick(player, false);
            long elapsed = System.nanoTime() - start;
            Snapshot step = here();
            if (leaving) {
                check(!step.queued && step.reads == 0, "movement cancels without an extra slice", step);
                cancelled++;
                cancel.add(elapsed);
                position(x, y, z);
            } else if (step.reads == 0) {
                idle.add(elapsed);
            } else {
                check(step.reads == 169, "one survey slice per tick", step);
                if (step.assessmentAge == 0) {
                    if (shifted) {
                        shiftedFinish.add(elapsed);
                        followUps++;
                        shifted = false;
                    } else {
                        finish.add(elapsed);
                        centers++;
                        shifted = step.queued;
                    }
                } else {
                    (shifted ? shiftedSlice : slice).add(elapsed);
                }
            }
            before = step;
        }
        check(moving ? cancelled == 36 && centers == 0 : centers == 36 && followUps == (recentered ? 36 : 0),
                "repeatable survey workload for " + label, here());
        idle.report(label + " idle");
        slice.report(label + " initial slice");
        finish.report(label + " initial final");
        shiftedSlice.report(label + " recentered slice");
        shiftedFinish.report(label + " recentered final");
        cancel.report(label + " cancellation");
    }

    private void profileRemoteQuarry(int y) throws Exception {
        int remoteX = X + 512;
        prepare(remoteX, Z);
        position(X, y, Z);
        for (int i = 0; i < 12; i++) world.g(remoteX + i, y, Z, 1);
        ticks(600);
        SettlementProfile.Samples quietIdle = new SettlementProfile.Samples(), quietSurvey = new SettlementProfile.Samples();
        SettlementProfile.Samples quarryIdle = new SettlementProfile.Samples(), quarrySurvey = new SettlementProfile.Samples();
        world.auditChunks = true;
        world.remoteChunk = remoteX >> 4;
        for (int i = 0; i < 400; i++) measureRemoteTick(quietIdle, quietSurvey);
        check(world.remoteReads == 0 && world.chunkMisses == 0, "loaded remote terrain schedules no remote reads", here());
        for (int i = 0; i < 120; i++) {
            settlements.tick(null, false);
            check(here().reads == 0 && !here().queued, "no player means no survey", here());
        }
        world.auditChunks = false;

        PowerFramework.currentFramework = new PneumaticPowerFramework();
        TileQuarry quarry = new TileQuarry();
        quarry.i = world;
        quarry.j = remoteX;
        quarry.k = y + 1;
        quarry.l = Z + 2;
        // Supply a post-blueprint arm and targets. The pinned tile still drives it using real MJ.
        Field initialized = TileBuildCraft.class.getDeclaredField("init");
        initialized.setAccessible(true);
        initialized.setBoolean(quarry, true);
        quarry.arm = new EntityMechanicalArm(world, remoteX + 0.5, y + 3, Z + 0.5, 10, 5);
        Field listener = EntityMechanicalArm.class.getDeclaredField("listener");
        listener.setAccessible(true);
        listener.set(quarry.arm, quarry);
        Field digging = TileQuarry.class.getDeclaredField("isDigging");
        digging.setAccessible(true);
        digging.setBoolean(quarry, true);
        int powerTicks = 0, spent = 0;
        for (int i = 0; i < 12; i++) {
            quarry.targetX = remoteX + i;
            quarry.targetY = y + 1;
            quarry.targetZ = Z;
            quarry.arm.setTarget(remoteX + i + 0.5, y + 1, Z + 0.5);
            quarry.inProcess = true;
            for (int tick = 0; tick < 80 && quarry.inProcess; tick++) {
                quarry.powerProvider.receiveEnergy(20);
                int energy = quarry.powerProvider.energyStored;
                quarry.n_();
                spent += energy - quarry.powerProvider.energyStored;
                powerTicks++;
                world.auditChunks = true;
                measureRemoteTick(quarryIdle, quarrySurvey);
                world.auditChunks = false;
            }
            check(!quarry.inProcess && quarry.isActive() && world.a(remoteX + i, y, Z) == 0,
                    "powered BuildCraft arm reached its target and excavated", here());
        }
        int removals = provenance.snapshot(world, remoteX, y, Z).quarryRemoval
                + provenance.snapshot(world, remoteX + 8, y, Z).quarryRemoval;
        check(removals == 12 && spent > 0 && world.remoteReads == 0 && world.chunkMisses == 0,
                "remote excavation delivered provenance without recognition loading or reading remote chunks", here());
        System.out.printf("PASS remote load control 400 ticks, no observer 120 ticks, powered quarry %d ticks / %d removals / %d MJ, "
                + "%d remote chunk accesses / %d chunk misses in recognition%n", powerTicks, removals, spent,
                world.remoteReads, world.chunkMisses);
        quietIdle.report("loaded remote terrain / local idle");
        quietSurvey.report("loaded remote terrain / local sampled");
        quarryIdle.report("powered remote quarry / local idle");
        quarrySurvey.report("powered remote quarry / local sampled");

        SettlementProfile.mutations(world, player, remoteX, y, Z);
    }

    private void measureRemoteTick(SettlementProfile.Samples idle, SettlementProfile.Samples surveyed) {
        territory.tick(player, false);
        long start = System.nanoTime();
        settlements.tick(player, false);
        long elapsed = System.nanoTime() - start;
        (here().reads == 0 ? idle : surveyed).add(elapsed);
    }

    private void profilePressure(int y) throws Exception {
        open("profile-pressure", false);
        for (int i = 0; i < 32; i++) industrialWorkshop(X + i * 32, y, Z);
        for (boolean dispersed : new boolean[] {false, true}) {
            for (int count : new int[] {1, 8, 16, 32}) {
                SettlementRecognition[] origins = new SettlementRecognition[count];
                vq[] observers = new vq[count];
                for (int i = 0; i < count; i++) {
                    // Each replay owns its walk/use history and registry view. No writes or mutations.
                    WorldProvenance replayProvenance = new WorldProvenance();
                    replayProvenance.bind(world);
                    origins[i] = new SettlementRecognition(territory, replayProvenance);
                    origins[i].bind(world);
                    observers[i] = new vq(game, world, new fc("Replay" + i, ""), 0);
                    observers[i].c(X + 0.5 + (dispersed ? i * 32 : 0), y, Z + 0.5, 0, 0);
                }
                SettlementProfile.Samples idle = new SettlementProfile.Samples();
                SettlementProfile.Samples slice = new SettlementProfile.Samples();
                SettlementProfile.Samples finish = new SettlementProfile.Samples();
                world.auditChunks = true;
                world.remoteChunk = Integer.MAX_VALUE;
                for (int tick = 0; tick < 4200; tick++) {
                    if (tick == 600) world.auditChunks = false;
                    long start = System.nanoTime();
                    for (int i = 0; i < count; i++) origins[i].tick(observers[i], false);
                    long elapsed = System.nanoTime() - start;
                    int step = tick % 100;
                    for (int i = 0; i < count; i++) {
                        Snapshot state = origins[i].snapshot(world, (int) observers[i].o, y, Z);
                        check(state.reads == (step < 9 ? 169 : 0) && state.siteCount == 0
                                        && (step != 8 || state.inferred == 1266),
                                "bounded isolated origin replay of matched workshops", state);
                    }
                    if (tick < 600) continue;
                    (step == 8 ? finish : step < 8 ? slice : idle).add(elapsed);
                }
                world.auditChunks = false;
                check(world.chunkMisses == 0, "scaling replay observes only loaded terrain", here());
                String label = "scaling model / " + (dispersed ? "dispersed " : "overlapping ") + count;
                idle.report(label + " idle frame");
                slice.report(label + " slice frame");
                finish.report(label + " final frame");
            }
        }
    }

    private void surveyMutations() throws Exception {
        int feet = 83;
        lowCabin("mutate-initial-roof", feet);
        position(103, feet, 232);
        ticks(500); // Corroborate activity without satisfying the four-column requirement.
        walk(); // Tick 509 classifies before the walk reaches its fourth distinct column.
        check(here().id == 0 && here().activity >= 2 && here().cover == 9,
                "ready to promote on the next survey except for the roof edit", here());
        ticks(89); // Tick 605. The low roof (fifth sampled layer) is already cached.
        check(here().queued && here().reads == 169, "roof layer sampled", here());
        for (int x = 100; x <= 106; x++) {
            for (int z = 229; z <= 235; z++) {
                check(controller.b(x, feet + 2, z, 1), "native roof removal between slices", here());
            }
        }
        ticks(4);
        showSurvey("removed sampled roof", here());
        check(here().id == 0 && "Terrain changed".equals(here().assessment) && here().reads == 0,
                "stale roof cannot promote and the changed cycle stops reading", here());
        ticks(100);
        check(here().id == 0 && here().cover == 0 && "Complete".equals(here().assessment),
                "normal cadence refreshes the roofless cabin", here());

        lowCabin("mutate-follow-up", feet);
        position(103, feet, 232);
        walk();
        ticks(550);
        Snapshot known = recognized("cabin before interrupted follow-up", true);
        position(104, feet, 236);
        place(PLANK, 106, feet - 1, 232, 5);
        nextSurvey();
        ticks(8);
        check(here().queued && "Partial footprint".equals(here().assessment), "entered recentered window", here());
        ticks(3);
        check(controller.b(102, feet, 230, 1), "native linked furnace removal after follow-up sampled it", here());
        check(controller.b(103, feet - 1, 232, 1), "native floor support removal after follow-up sampled it", here());
        place(PLANK, 102, feet - 1, 232, 5);
        ticks(6);
        Snapshot changed = here();
        showSurvey("changed recentered evidence", changed);
        check(changed.id == known.id && changed.boundary.equals(known.boundary)
                        && changed.observedTick == known.observedTick && !changed.queued
                        && "Terrain changed".equals(changed.assessment),
                "changed follow-up cannot expand or re-observe an existing identity", changed);

        nextSurvey();
        ticks(2); // The entrance window has sampled loaded chunks north and south of Z=240.
        world.hideChunk(6, 15);
        ticks(1);
        Snapshot unavailable = here();
        showSurvey("neighbor became unavailable", unavailable);
        check(unavailable.id == known.id && unavailable.boundary.equals(known.boundary)
                        && unavailable.observedTick == known.observedTick && unavailable.reads == 0
                        && unavailable.sampledLayers == 3 && "Terrain unavailable".equals(unavailable.assessment)
                        && !world.j(104, feet, 240),
                "unavailable neighbor invalidates the current survey and retains known identity", unavailable);
        world.hideChunk(Integer.MIN_VALUE, Integer.MIN_VALUE);
        nextSurvey();
        ticks(17);
        Snapshot refreshed = here();
        check(refreshed.id == known.id && refreshed.boundary.endsWith("(107, 85, 235)")
                        && refreshed.furnace.found == 0 && "Complete".equals(refreshed.assessment),
                "valid refresh can expand the same identity without the removed utility", refreshed);

        position(103, feet, 232);
        for (int cycle = 0; cycle < 3; cycle++) {
            nextSurvey();
            ticks(1); // First two layers include the support at Y=82.
            check(controller.b(103, feet - 1, 232, 1), "native repeated support removal", here());
            place(PLANK, 102, feet - 1, 232, 5);
            ticks(1);
            check("Terrain changed".equals(here().assessment), "same-ID replacement invalidates sampled support", here());
            for (int i = 0; i < 97; i++) {
                ticks(1);
                check(here().reads == 0 && !here().queued, "invalidations cannot bypass the cycle deadline", here());
            }
        }
        System.out.println("PASS survey mutations stop stale promotion, expansion and re-observation within the original read budget");
    }

    private void nextSurvey() {
        for (int i = 0; i < 100; i++) {
            ticks(1);
            if (here().queued && here().reads == 169) return;
        }
        throw new AssertionError("survey did not start by its next deadline");
    }

    private void suspendedCabins() throws Exception {
        int feet = 83;
        lowCabin("suspended-cabin", feet);
        position(104, feet, 236);
        ticks(9);
        Snapshot partial = here();
        check(partial.queued && "Partial footprint".equals(partial.assessment) && "north".equals(partial.clipping)
                        && partial.anchorZ == 236 && partial.anchorCover == 3 && partial.sampledLayers == 9
                        && !partial.extent.equals(partial.pendingExtent) && partial.assessedTick >= 0,
                "entrance sample exposes clipping and scheduled follow-up", partial);
        showSurvey("entrance partial", partial);
        walkInside();
        for (int i = 0; i < 12; i++) {
            player.b(0, 0, .25);
            ticks(1);
        }
        ticks(550);
        Snapshot suspended = recognized("suspended cabin from the entrance", true);
        check(suspended.anchorCover == 9 && suspended.uses == 0 && suspended.utilities == 2
                        && suspended.workbench.found == 1 && suspended.workbench.associated == 1
                        && suspended.furnace.found == 1 && suspended.furnace.associated == 1
                        && suspended.furnace.opened == 0 && suspended.placed >= 8
                        && "Complete".equals(suspended.assessment) && suspended.assessmentAge >= 0,
                "follow-up sees low roof, selected component and unopened utilities", suspended);
        WorldProvenance.Snapshot storage = provenance.snapshot(world, 104, feet, 236);
        check(storage.capacity == 1024 && storage.sections < storage.capacity && storage.placed < suspended.placed,
                "storage usage and local section history have their own scale", suspended);
        showSurvey("entrance recentered", suspended);
        for (int[] point : new int[][] {{105, 234}, {103, 232}}) {
            position(point[0], feet, point[1]);
            ticks(130);
            check(here().id == suspended.id && here().cover >= 4 && here().utilities == 2,
                    "near-wall and middle surveys retain the same site", here());
            showSurvey("inside", here());
        }

        useUtility(101, feet, 230);
        ticks(25);
        useUtility(102, feet, 230);
        ticks(130);
        check(here().workbench.opened == 1 && here().furnace.opened == 1 && here().id == suspended.id,
                "native openings are counted separately", here());
        world.g(108, feet - 1, 232, 1);
        place(FURNACE, 108, feet - 1, 232, 1);
        position(108, feet, 233);
        useUtility(108, feet, 232);
        position(103, feet, 232);
        ticks(1250);
        Snapshot aged = here();
        check(aged.furnace.found == 2 && aged.furnace.associated == 1 && aged.furnace.opened == 0
                        && aged.furnace.expired == 1 && aged.furnace.outside == 1,
                "neighboring furnace and expired openings have distinct diagnostics", aged);

        place(PLANK, 106, feet - 1, 232, 5);
        walk();
        ticks(140);
        Snapshot extended = here();
        check(extended.id == suspended.id && extended.boundary.endsWith("(107, 85, 235)"),
                "a connected one-block extension updates bounds without a new ID", extended);
        save();
        open("suspended-cabin", false);
        world.d(6, 14);
        position(104, feet, 235);
        check(!world.j(104, feet, 240), "neighbor chunk starts unavailable", here());
        ticks(15);
        check("Terrain unavailable".equals(here().assessment) && here().sampledLayers == 0
                        && !world.j(104, feet, 240) && here().id == suspended.id
                        && here().boundary.equals(extended.boundary),
                "reopen retains extended ID and bounds without loading a neighbor", here());
        prepare(104, 235);
        walkInside();
        ticks(145);
        check(here().id == suspended.id, "native chunks reloaded and re-observed", here());

        lowCabin("ground-cabin-twin", 5);
        position(104, 5, 236);
        walkInside();
        ticks(480);
        Snapshot ground = recognized("ground-level cabin twin", true);
        check(ground.placed == suspended.placed && ground.cover == suspended.cover && ground.uses == 0,
                "same geometry and evidence above ground and on ground", ground);

        lowCabin("suspended-cabin-empty", feet);
        check(controller.b(101, feet, 230, 1) && controller.b(102, feet, 230, 1), "native utility removal", null);
        position(104, feet, 236);
        walkInside();
        ticks(480);
        check(recognized("furniture-free suspended cabin", true).utilities == 0,
                "covered suspended sites can corroborate use through movement", here());

        lowCabin("suspended-cabin-roof-removed", feet);
        for (int x = 100; x <= 106; x++) {
            for (int z = 229; z <= 235; z++) check(controller.b(x, feet + 2, z, 1), "native roof removal", null);
        }
        position(103, feet, 232);
        walk();
        ticks(480);
        Snapshot roofless = here();
        check(roofless.id == 0 && roofless.cover == 0 && roofless.utilities == 2 && roofless.uses == 0
                        && roofless.assessedTick >= 0 && roofless.observedTick < 0 && roofless.reason.startsWith("Little cover"),
                "roofless cabin reports a completed rejection with equipment present", roofless);

        open("suspended-platform", false);
        prepare(103, 232);
        position(98, feet, 232);
        platform(100, feet, 230);
        // Put an independent floor overhead. Its planks belong to another component.
        platform(100, feet + 4, 230);
        for (int x = 100; x < 105; x++) {
            for (int z = 230; z < 235; z++) world.g(x, feet + 2, z, 0);
        }
        position(101, feet, 231);
        walk();
        ticks(480);
        check(here().id == 0 && here().cover == 0 && here().uses == 0 && here().placed >= 8,
                "a bare platform cannot borrow cover from a separate floor", here());
        System.out.println("PASS suspended and ground twin, doorway follow-up, utility diagnostics, expansion, save/reopen and roofless controls");
    }

    private void showSurvey(String name, Snapshot sample) {
        System.out.printf("CABIN %s anchor (%d,%d,%d), feet Y %d, age %d, window %s, component %s, clip %s, "
                        + "cover %d/9 anchor %d/9, bench %d/%d/%d, furnace %d/%d/%d: %s / %s%n",
                name, sample.anchorX, sample.anchorY, sample.anchorZ, sample.footY, sample.assessmentAge,
                sample.extent, sample.component, sample.clipping, sample.cover, sample.anchorCover,
                sample.workbench.found, sample.workbench.associated, sample.workbench.opened,
                sample.furnace.found, sample.furnace.associated, sample.furnace.opened, sample.assessment, sample.reason);
    }

    private void lowCabin(String name, int feet) throws Exception {
        int x = 100, z = 229;
        open(name, false);
        prepare(x, z);
        position(x - 3, feet, z);
        world.g(x, feet - 2, z, 1);
        place(PLANK, x, feet - 2, z, 1);
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                if (dx > 0) place(PLANK, x + dx - 1, feet - 1, z + dz, 5);
                else if (dz > 0) place(PLANK, x, feet - 1, z + dz - 1, 3);
            }
        }
        if (feet > 5) world.g(x, feet - 2, z, 0);
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                if (dx != 0 && dx != 6 && dz != 0 && dz != 6 || dx == 4 && dz == 6) continue;
                place(PLANK, x + dx, feet - 1, z + dz, 1);
                place(PLANK, x + dx, feet, z + dz, 1);
            }
        }
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                if (dx > 0) place(PLANK, x + dx - 1, feet + 2, z + dz, 5);
                else place(PLANK, x, feet + 1, z + dz, 1);
            }
        }
        place(BENCH, x + 1, feet - 1, z + 1, 1);
        place(FURNACE, x + 2, feet - 1, z + 1, 1);
        place(50, x + 1, feet + 1, z + 3, 5);
        // An approach ledge allows movement through the doorway without adding authored floor area.
        world.g(x + 4, feet - 1, z + 7, 1);
    }

    private void walkInside() {
        for (int i = 0; i < 12; i++) {
            player.b(0, 0, -0.25);
            ticks(1);
        }
        walk();
    }

    private SettlementProbe(File root) throws Exception {
        this.root = root;
        Class.forName("pb"); // Match a local world starting the vanilla block registry before FML.
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja error) { throw new AssertionError(error); }
            @Override public void a(vp screen) {
                if (!(screen instanceof gb)) throw new AssertionError("Expected a utility container");
                h.ar = ((gb) screen).d; // Native GuiContainer handoff with no LWJGL window.
                utilityOpens++;
            }
        };
        game.R = true;
        game.c = controller = new aes(game);
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);
        provenance.register();
        settlements.register();
        MinecraftForge.registerSaveHandler(this);
    }

    private void open(String name, boolean structures) throws Exception {
        territory.bind(null);
        provenance.bind(null);
        settlements.bind(null);
        Path marker = new File(root, name + "/.zto-disposable").toPath();
        Fixture incoming = new Fixture(new aia(root, name, false), name, structures);
        if (!Files.exists(marker)) Files.createFile(marker);
        bind(incoming);
    }

    private void bind(Fixture incoming) {
        world = incoming;
        game.f = world;
        player = new vq(game, world, new fc("SettlementProbe", ""), 0) {
            @Override public void a(ajw statistic, int count) {}
        };
        game.h = player;
        territory.bind(world);
        provenance.bind(world);
        provenance.bindPlayer(player);
        settlements.bind(world);
    }

    private void sites() throws Exception {
        open("settlement-sites", false);
        prepare(X, Z);
        position(X - 3, Y, Z + 3);
        cabin(X, Y, Z, true, false);
        position(X + 2, Y, Z + 2);
        ticks(220);
        Snapshot candidate = here();
        check("Candidate".equals(candidate.state) && candidate.placed >= 8 && candidate.walked < 4,
                "cabin forms a candidate before corroboration", candidate);
        walk();
        ticks(340);
        Snapshot cabin = recognized("furniture-free cabin", true);
        check(cabin.uses == 0 && cabin.utilities == 0, "cabin has no utilities", cabin);
        check(controller.b(X, Y + 2, Z + 2, 4), "ordinary cabin wall edit", cabin);
        world.g(X + 1, Y + 3, Z + 2, 1); // Simulate one unobserved replacement by a different block ID.
        walk();
        ticks(120);
        check(here().id == cabin.id && here().placed == cabin.placed - 2,
                "wall edit retains identity and stale placement fails the live check", here());

        int remoteX = X + 512;
        save();
        // The pinned local provider does not evict on its ordinary unload poll.
        // Replace the native cache with the same save handler to exercise unavailable terrain.
        bind(new Fixture((aia) world.A(), "settlement-sites", false));
        prepare(remoteX, Z);
        check(!world.j(X + 2, Y, Z + 2), "previous site is unloaded", here());
        position(X + 2, Y, Z + 2);
        ticks(120);
        Snapshot missing = here();
        check(missing.id == cabin.id && !world.j(X + 2, Y, Z + 2)
                && "Terrain unavailable".equals(missing.reason), "survey retains unloaded site without loading", missing);
        position(remoteX, Y, Z);
        ticks(110);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(130);
        check(here().id == cabin.id, "return reuses identity", here());

        int caveX = X + 11;
        position(caveX - 2, Y, Z + 3);
        for (int x = caveX - 1; x <= caveX + 6; x++) {
            for (int z = Z - 1; z <= Z + 6; z++) {
                for (int y = Y + 3; y <= Y + 6; y++) world.g(x, y, z, 1);
            }
        }
        platform(caveX, Y, Z);
        position(caveX + 1, Y, Z + 1);
        walk();
        ticks(540);
        Snapshot cave = recognized("adapted underground workspace", true);
        check(cave.id != cabin.id && cave.cover >= 4, "nearby cabin and cave keep independent sites", cave);

        int elevated = 28;
        for (int x = X; x < X + 5; x++) {
            for (int z = Z; z < Z + 5; z++) world.g(x, elevated - 2, z, 1);
        }
        position(X - 2, elevated, Z + 2);
        platform(X, elevated, Z);
        place(BENCH, X + 4, elevated - 1, Z + 4, 1);
        place(CHEST, X + 4, elevated - 1, Z + 3, 1);
        place(FURNACE, X + 4, elevated - 1, Z + 2, 1);
        position(X + 1, elevated, Z + 1);
        walk();
        useUtility(X + 4, elevated, Z + 4);
        ticks(25);
        useUtility(X + 4, elevated, Z + 3);
        ticks(25);
        useUtility(X + 4, elevated, Z + 2);
        ticks(440);
        Snapshot workshop = recognized("open elevated workshop", true);
        check(workshop.id != cabin.id && workshop.id != cave.id && workshop.cover == 0 && workshop.uses == 3,
                "overlapping X/Z sites are distinct and open workshop has use evidence", workshop);

        int oldZ = Z + 17;
        prepare(X, oldZ);
        position(X - 2, Y, oldZ);
        cabin(X, Y, oldZ, false, true);
        world.g(X + 5, Y, oldZ + 4, CHEST);
        world.g(X + 5, Y + 1, oldZ + 4, 1);
        position(X + 2, Y, oldZ + 2);
        walk();
        int before = utilityOpens;
        check(controller.a(player, world, null, X + 5, Y, oldZ + 4, 1) && utilityOpens == before,
                "blocked chest consumes the click without opening", here());
        ticks(30);
        check(controller.a(player, world, null, X + 5, Y, oldZ + 4, 1) && utilityOpens == before,
                "repeated blocked activation opens no container", here());
        ticks(450);
        check(here().id == 0 && here().inferred > 16 && here().uses == 0, "old structure needs actual use", here());
        useUtility(X + 5, Y, oldZ + 5);
        ticks(30);
        useUtility(X + 5, Y, oldZ + 5);
        ticks(120);
        Snapshot old = recognized("old inhabited structure", false);
        check(old.placed == 0 && old.uses >= 2 && old.useHook, "unknown authorship is explicit", old);

        save();
        Path registry = new File(root, "settlement-sites/data/zto_settlement_sites.dat").toPath();
        check(Files.size(registry) == 158, "four bounded records serialized", here());
        open("settlement-sites", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        Snapshot reopened = here();
        check(reopened.id == cabin.id && reopened.boundary.equals(cabin.boundary)
                && world.a(X + 2, Y + 3, Z + 2) == PLANK, "native world reopened with stable cabin ID and bounds", reopened);
        walk();
        ticks(450);
        check(here().id == cabin.id && here().siteCount == 4, "re-observation does not duplicate an ID", here());

        corrupt(registry, new byte[] {1, 2, 3}, "UNREADABLE");
        corrupt(registry, ByteBuffer.allocate(8).putInt(0x5A544F53).putInt(999).array(), "UNSUPPORTED_SCHEMA");
        System.out.println("PASS four site identities, re-observation, native save/reopen, malformed and future files");
    }

    private void corrupt(Path registry, byte[] contents, String status) throws Exception {
        Files.write(registry, contents);
        open("settlement-sites", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(420);
        check(here().registry.equals(status) && here().siteCount == 0, "invalid registry quarantined", here());
        save();
        check(Arrays.equals(contents, Files.readAllBytes(registry)), "invalid registry preserved", here());
    }

    private void negatives() throws Exception {
        open("settlement-controls", false);
        prepare(X, Z);
        position(X, Y, Z);
        for (int visit = 0; visit < 3; visit++) {
            walk();
            ticks(800);
            check(here().id == 0 && here().activity >= 3, "untouched terrain receives activity without a site", here());
            position(X + 80, Y, Z);
            ticks(3700);
            position(X, Y, Z);
        }
        for (int z = Z; z < Z + 8; z++) {
            for (int y = Y; y < Y + 3; y++) place(PLANK, X + 3, y - 1, z, 1);
        }
        walk();
        ticks(500);
        check(here().siteCount == 0, "isolated wall cannot promote", here());
        for (int i = 0; i < 48; i++) {
            place(PLANK, X - 2, Y - 1, Z, 1);
            check(controller.b(X - 2, Y, Z, 1), "native replacement cycle", here());
        }
        walk();
        ticks(500);
        check(here().siteCount == 0, "replacement cycling cannot promote", here());

        PowerFramework.currentFramework = new PneumaticPowerFramework();
        TileQuarry quarry = new TileQuarry();
        quarry.i = world;
        quarry.j = X + 49;
        quarry.k = Y;
        quarry.l = Z;
        prepare(X + 45, Z);
        position(X + 45, Y, Z);
        int removed = 0;
        for (int x = X + 37; x < X + 53; x++) {
            for (int z = Z - 8; z < Z + 8; z++) {
                world.g(x, Y, z, 1);
                quarry.targetX = x;
                quarry.targetY = Y + 1;
                quarry.targetZ = z;
                quarry.positionReached(null);
                if (world.a(x, Y, z) == 0) removed++;
            }
        }
        walk();
        ticks(600);
        check(removed == 256 && here().siteCount == 0
                && provenance.snapshot(world, X + 47, Y, Z).quarryRemoval > 0, "quarry has removal evidence only", here());
        System.out.printf("PASS untouched revisits, isolated wall, replacement cycles and %d native Quarry removals%n", removed);
    }

    private void village() throws Exception {
        open("settlement-village", true);
        for (int regionX = 0; regionX < 4; regionX++) {
            Random region = world.A(regionX, 0, 10387312);
            int x = (regionX * 32 + region.nextInt(24)) << 4, z = region.nextInt(24) << 4;
            prepare(x, z);
            for (int bx = x - 32; bx < x + 40; bx++) {
                for (int bz = z - 32; bz < z + 40; bz++) {
                    int wall = world.a(bx, Y + 1, bz);
                    if (wall != PLANK && wall != 4 || world.a(bx, Y, bz + 1) != 0) continue;
                    position(bx, Y, bz + 1);
                    ticks(700);
                    Snapshot village = here();
                    if (village.inferred < 12) continue;
                    check(village.placed == 0 && village.id == 0 && village.activity >= 3,
                            "native generated village and visits stay unrecognized", village);
                    System.out.printf("PASS generated village control, %d inferred blocks and unknown authorship%n", village.inferred);
                    return;
                }
            }
        }
        throw new AssertionError("pinned flat-world generator did not yield a sampled village structure");
    }

    private void stress() throws Exception {
        open("settlement-dispersed", false);
        prepare(X + 96, Z);
        position(X + 93, Y, Z + 3);
        cabin(X + 96, Y, Z, true, false);
        position(X + 98, Y, Z + 2);
        walk();
        ticks(550);
        int original = recognized("cabin before provenance eviction", true).id;
        prepare(X + 30, Z + 30);
        position(X - 4, Y, Z);
        int placements = 0;
        for (int x = X; x < X + 64; x += 8) {
            for (int z = Z; z < Z + 64; z += 8) {
                for (int y = 8; y <= 136; y += 8) {
                    world.g(x, y - 1, z, 1);
                    place(PLANK, x, y - 1, z, 1);
                    placements++;
                }
            }
        }
        // Dense manufactured geometry drives the connected component pass as well as chunk reads.
        for (int x = X + 24; x <= X + 36; x++) {
            for (int z = Z + 24; z <= Z + 36; z++) {
                for (int y = 14; y <= 22; y++) {
                    if (x < X + 28 || x > X + 32 || z < Z + 28 || z > Z + 32 || y < 17 || y > 19)
                        world.g(x, y, z, PLANK);
                }
            }
        }
        position(X + 31, 17, Z + 31);
        ticks(100);
        int sampled = 0, queued = 0;
        long measured = 0, worst = 0;
        for (int i = 0; i < 3200; i++) {
            territory.tick(player, false);
            long start = System.nanoTime();
            settlements.tick(player, false);
            long elapsed = System.nanoTime() - start;
            Snapshot step = here();
            if (step.reads != 0) {
                sampled++;
                measured += elapsed;
                worst = Math.max(worst, elapsed);
            }
            if (step.queued) queued++;
        }
        WorldProvenance.Snapshot store = provenance.snapshot(world, X + 63, 136, Z + 63);
        check(here().peakReads == 169 && sampled == 288 && queued == 256
                && store.sections == 1024 && store.evicted && here().siteCount == 1, "measurement and capacity", here());
        save();
        long bytes = Files.size(new File(root, "settlement-dispersed/data/zto_world_provenance.dat").toPath());
        System.out.printf("PASS dispersed %d placements, %d sections, %d bytes, %d sampled / %d queued ticks, "
                        + "peak %d reads/tick, mean %.3f ms/sampled tick, max %.3f ms%n",
                placements, store.sections, bytes, sampled, queued, here().peakReads,
                measured / (1000000.0 * sampled), worst / 1000000.0);
        position(X + 98, Y, Z + 2);
        walk();
        ticks(450);
        check(here().id == original && here().placed == 0 && here().siteCount == 1,
                "eviction removes provenance evidence while site keeps its identity", here());
    }

    private void cabin(int x, int y, int z, boolean nativeBuild, boolean bench) {
        for (int dz = 0; dz < 7; dz++) {
            for (int dx = 0; dx < 7; dx++) {
                if (dx != 0 && dx != 6 && dz != 0 && dz != 6) continue;
                if (dx == 0 && dz == 3) continue;
                for (int dy = 0; dy < 3; dy++) {
                    if (nativeBuild) place(PLANK, x + dx, y + dy - 1, z + dz, 1);
                    else world.g(x + dx, y + dy, z + dz, PLANK);
                }
            }
        }
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                if (!nativeBuild) world.g(x + dx, y + 3, z + dz, PLANK);
                else if (dx > 0) place(PLANK, x + dx - 1, y + 3, z + dz, 5);
                else if (dz == 3) place(PLANK, x, y + 3, z + dz - 1, 3);
                else place(PLANK, x, y + 2, z + dz, 1);
            }
        }
        if (bench) world.g(x + 5, y, z + 5, BENCH);
    }

    private void platform(int x, int y, int z) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.g(x + dx, y - 2, z + dz, 1);
                world.g(x + dx, y - 1, z + dz, 0);
                place(PLANK, x + dx, y - 2, z + dz, 1);
            }
        }
    }

    private void place(int block, int x, int y, int z, int side) {
        check(controller.a(player, world, new aan(block, 1, 0), x, y, z, side),
                "native placement at " + x + "," + y + "," + z + "/" + side, null);
    }

    private void useUtility(int x, int y, int z) {
        int before = utilityOpens;
        check(controller.a(player, world, null, x, y, z, 1) && utilityOpens == before + 1,
                "normal utility activation handed off a container", here());
        player.ar = player.aq;
    }

    private void prepare(int x, int z) {
        int cx = x >> 4, cz = z >> 4;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) world.d(cx + dx, cz + dz);
    }

    private void position(int x, int y, int z) { player.c(x + 0.5, y, z + 0.5, 0, 0); }

    private void walk() {
        for (int segment = 0; segment < 4; segment++) {
            double dx = segment == 0 ? 0.25 : segment == 2 ? -0.25 : 0;
            double dz = segment == 1 ? 0.25 : segment == 3 ? -0.25 : 0;
            for (int step = 0; step < 4; step++) {
                player.b(dx, 0, dz);
                ticks(1);
            }
        }
    }

    private void ticks(int number) {
        for (int i = 0; i < number; i++) {
            territory.tick(player, false);
            settlements.tick(player, false);
        }
    }

    private Snapshot here() {
        return settlements.snapshot(world, (int) Math.floor(player.o),
                (int) Math.floor(player.y.b), (int) Math.floor(player.q));
    }

    private Snapshot recognized(String name, boolean observed) {
        Snapshot snapshot = here();
        check(snapshot.id > 0 && snapshot.confidence.startsWith(observed ? "Observed" : "Inferred"), name, snapshot);
        System.out.printf("PASS %s #%d at %s%n", name, snapshot.id, snapshot.boundary);
        return snapshot;
    }

    private void save() throws Exception {
        world.a(true, (rw) null);
        ce.a.a();
    }

    private static void check(boolean expected, String message, Snapshot details) {
        if (expected) return;
        throw new AssertionError(message + (details == null ? "" : " | " + details.state + " " + details.reason
                + " | placed=" + details.placed + " inferred=" + details.inferred + " cover=" + details.cover
                + " activity=" + details.activity + " steps=" + details.walked + " uses=" + details.uses
                + " registry=" + details.registry + " bounds=" + details.boundary));
    }

    @Override public void onWorldSave(xd context) {
        provenance.save(context);
        settlements.save(context);
    }
    @Override public void onWorldLoad(xd context) {}
    @Override public void onChunkLoad(xd context, ack chunk) {}
    @Override public void onChunkUnload(xd context, ack chunk) {}
    @Override public void onChunkSaveData(xd context, ack chunk, ady data) {}
    @Override public void onChunkLoadData(xd context, ack chunk, ady data) {}

    private static final class Fixture extends xd {
        private int hiddenX = Integer.MIN_VALUE, hiddenZ = Integer.MIN_VALUE;
        boolean auditChunks;
        int remoteChunk, remoteReads, chunkMisses;

        Fixture(aia save, String name, boolean structures) {
            super(save, name, new fj(32021L, 0, structures, false, vx.c));
        }

        @Override public void a(nn entity, String sound, float gain, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float gain, float pitch) {}

        void hideChunk(int x, int z) {
            hiddenX = x;
            hiddenZ = z;
        }

        @Override public boolean j(int x, int y, int z) {
            return ((x >> 4) != hiddenX || (z >> 4) != hiddenZ) && super.j(x, y, z);
        }

        @Override public ack d(int x, int z) {
            if (auditChunks) {
                if (x >= remoteChunk - 2) remoteReads++;
                if (!super.j(x << 4, 0, z << 4)) chunkMisses++;
            }
            return super.d(x, z);
        }
    }
}
