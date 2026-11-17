import buildcraft.api.EntityPassiveItem;
import buildcraft.api.Orientations;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipeItemsCobblestone;
import buildcraft.transport.pipes.PipeItemsStone;
import buildcraft.transport.pipes.PipeLiquidsStone;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Random;
import zed.tekkitoverhaul.hopper.BlockHopper;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.client.EnderTrail;

/** Pinned native pipe teardown, cargo recovery and collision-aware trail on disposable terrain. */
public final class IndustrialTheftProbe {
    public static void main(String[] args) throws Exception {
        try {
            run(new File(args[0]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0); // Anvil's native IO thread lives as long as the Java process.
    }

    @SuppressWarnings("unchecked")
    private static void run(File root) throws Exception {
        Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
        register.setAccessible(true);
        register.invoke(null, EntitySaboteur.class, "ZTO.Saboteur", 209);
        TestWorld world = new TestWorld(root);
        yw player = NaturalSpawnProbe.join(world);
        player.c(289, 4, 289.5, 0, 0);
        BlockGenericPipe block = new BlockGenericPipe(192);
        int stone = BlockGenericPipe.registerPipe(22311, PipeItemsStone.class).bQ;
        int cobble = BlockGenericPipe.registerPipe(22312, PipeItemsCobblestone.class).bQ;
        int fluid = BlockGenericPipe.registerPipe(22313, PipeLiquidsStone.class).bQ;
        Pipe target = install(world, 292, stone);
        install(world, 293, stone);
        world.g(294, 4, 289, 54);
        hb receiver = (hb) world.b(294, 4, 289);
        Pipe separate = install(world, 299, cobble);
        Pipe unsupported = install(world, 303, fluid);
        check(block.isPipeConnected(world, 293, 4, 289, 292, 4, 289), "native connection");

        Method candidate = Class.forName("zed.tekkitoverhaul.survival.tactical.BuildCraftPipeTheft")
                .getDeclaredMethod("find", kw.class);
        candidate.setAccessible(true);
        check(candidate.invoke(null, separate.container) != null, "empty cobblestone pipe is supported");
        check(candidate.invoke(null, unsupported.container) == null, "fluid transport remains outside physical theft");
        world.g(303, 4, 289, 0);
        PipeTransportItems transport = (PipeTransportItems) target.transport;
        Field pending = PipeTransportItems.class.getDeclaredField("entitiesToLoad");
        pending.setAccessible(true);
        Collection<Object> queued = (Collection<Object>) pending.get(transport);
        queued.add(null);
        check(candidate.invoke(null, target.container) == null, "cargo awaiting the first tick after reload makes pipe ineligible");
        queued.clear();

        EntitySaboteur contested = start(world, player);
        check(contested.pose() == EntitySaboteur.DISMANTLE, "pipe is empty when dismantling starts");
        EntityPassiveItem cargo = new EntityPassiveItem(world, 292.1, 4.25, 289.5, new aan(4, 5, 0));
        transport.entityEntering(cargo, Orientations.XPos);
        check(candidate.invoke(null, target.container) == null, "in-flight payload makes pipe ineligible");
        act(contested, 65);
        check(contested.pose() == EntitySaboteur.CANCELLED && world.a(292, 4, 289) == 192,
                "incoming transport interrupts the windup at contact");
        for (int tick = 0; tick < 220; tick++) world.n();
        int delivered = 0;
        for (int slot = 0; slot < receiver.a(); slot++) {
            aan stack = receiver.k_(slot);
            if (stack != null && stack.c == 4) delivered += stack.a;
        }
        check(delivered == 5 && world.b(fq.class) == 0, "native transport delivers all five payload items to the chest");

        EntitySaboteur interrupted = start(world, player);
        check(interrupted.pose() == EntitySaboteur.DISMANTLE && world.a(292, 4, 289) == 192, "actual pipe acquired");
        interrupted.a(md.a(player), 2);
        check(interrupted.pose() == EntitySaboteur.CANCELLED && world.a(292, 4, 289) == 192,
                "player hit cancels before removal");

        EntitySaboteur thief = start(world, player);
        act(thief, 64);
        check(world.a(292, 4, 289) == 192 && thief.pose() == EntitySaboteur.DISMANTLE, "pipe remains through windup");
        act(thief, 65);
        check(world.a(292, 4, 289) == 0 && world.b(292, 4, 289) == null
                && BlockGenericPipe.getPipe(world, 292, 4, 289) == null && target.container.l(),
                "block, tile and persistent pipe removed");
        check(!block.isPipeConnected(world, 293, 4, 289, 292, 4, 289) && world.b(fq.class) == 0,
                "real connection severed and no duplicate harvest");
        check(thief.pose() == EntitySaboteur.CARRY && world.a(thief), "cargo escape state");
        double originX = thief.o, originZ = thief.q;
        for (int tick = 0; tick < 45; tick++) world.n();
        check(Math.hypot(thief.o - originX, thief.q - originZ) > 4, "uses native movement to leave the pipe");
        // The native network assertion above uses synthetic pipe registrations. Remove its
        // remaining pieces before saving the carrier in a normal Anvil world.
        world.g(293, 4, 289, 0);
        world.g(299, 4, 289, 0);
        world.a(true, null);
        ce.a.a();
        agp.a();
        TestWorld reopened = new TestWorld(root);
        EntitySaboteur restored = null;
        for (Object entity : reopened.b) if (entity instanceof EntitySaboteur) restored = (EntitySaboteur) entity;
        check(restored != null && reopened.a(292, 4, 289) == 0, "Anvil reload preserves the carrier and missing pipe");
        yw returning = NaturalSpawnProbe.join(reopened);
        check(restored.a(md.a(returning), 20), "reloaded carrier can be killed");
        restored.A();
        check(reopened.b(fq.class) == 1, "serialized cargo released exactly once");
        for (Object entity : reopened.b) if (entity instanceof fq) {
            aan stack = ((fq) entity).a;
            check(stack.a == 1 && stack.c == stone && stack.i() == 0, "exact registered component and metadata");
        }

        HopperRegistry.block = new BlockHopper(193);
        check(world.d(292, 4, 289, 193, 5) && world.g(293, 4, 289, 54), "native hopper feeds a chest");
        TileHopper hopper = (TileHopper) world.b(292, 4, 289);
        hopper.a(0, new aan(4, 3, 0));
        EntitySaboteur pausing = start(world, player);
        check(pausing.pose() == EntitySaboteur.WINDUP && world.a(pausing), "temporary sabotage still uses its own windup");
        act(pausing, 55);
        check(hopper.isDisrupted() && world.a(292, 4, 289) == 193, "temporary sabotage pauses without removing the Hopper");
        for (int tick = 0; tick < 18; tick++) world.n();
        check(hopper.k_(0).a == 3, "three items remain while Hopper output is paused");
        for (int tick = 0; tick < 95; tick++) world.n();
        check(!hopper.isDisrupted() && hopper.k_(0) == null, "Hopper sends its items after lease expiry");

        player.c(290.5, 4, 303.5, 0, 0);
        for (int z = 302; z <= 304; z++) {
            world.g(293, 4, z, 1);
            world.g(293, 5, z, 1);
        }
        EnderTrail trail = new EnderTrail();
        Method activate = EnderTrail.class.getDeclaredMethod("start", xd.class, yw.class, uh.class);
        Method emit = EnderTrail.class.getDeclaredMethod("emit", xd.class, int.class);
        activate.setAccessible(true);
        emit.setAccessible(true);
        player.V = 100;
        check((Boolean) activate.invoke(trail, world, player, new uh(302, 4, 303)), "temporary trail finds a detour");
        world.markers = 0;
        world.checkObstacle = true;
        emit.invoke(trail, world, 100);
        check(world.markers > 2 && world.markers <= 12 && world.furthest > 293 && world.detour, "markers go around the wall");
        int count = world.markers;
        emit.invoke(trail, world, 101);
        emit.invoke(trail, world, 185);
        check(world.markers == count, "throttles and expires automatically");
        player.V = 190;
        check((Boolean) activate.invoke(trail, world, player, new uh(302, 4, 303)), "can request guidance again");
        emit.invoke(trail, world, 190);
        check(world.markers > count, "repeated activation emits markers");
        player.c(750, 4, 750, 0, 0);
        check(!(Boolean) activate.invoke(trail, world, player, new uh(780, 4, 750)), "unloaded corridor rejected");
        System.out.println("PASS Saboteur item delivery, interruption, teardown, escape, Anvil cargo, and temporary Hopper pause; Ender Trail detour and expiry");
    }

    private static Pipe install(TestWorld world, int x, int item) {
        Pipe pipe = BlockGenericPipe.createPipe(world, x, 4, 289, item);
        check(world.g(x, 4, 289, 192), "install pipe block");
        TileGenericPipe tile = (TileGenericPipe) world.b(x, 4, 289);
        tile.pipe = pipe;
        pipe.setTile(tile);
        pipe.setWorld(world);
        return pipe;
    }

    private static EntitySaboteur start(TestWorld world, yw player) throws Exception {
        EntitySaboteur mob = new EntitySaboteur(world);
        mob.c(290.5, 4, 289.5, 0, 0);
        mob.z = true;
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(mob, new Random() {
            @Override public int nextInt(int limit) { return limit == 3 ? 0 : super.nextInt(limit); }
        });
        mob.engageEncounterPlayer(player);
        act(mob, 30);
        act(mob, 30);
        return mob;
    }

    private static void act(EntitySaboteur mob, int tick) throws Exception {
        mob.V = tick;
        Method fight = EntitySaboteur.class.getDeclaredMethod("fight", double.class, boolean.class);
        fight.setAccessible(true);
        fight.invoke(mob, 4.0, true);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestWorld extends xd {
        int markers;
        boolean checkObstacle, detour;
        double furthest;

        TestWorld(File root) {
            super(new aia(root, "pipe-and-trail", false), "pipe-and-trail", NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 14; x <= 22; x++)
                for (int z = 14; z <= 22; z++) d(x, z);
        }

        @Override public void a(nn entity, String key, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String key, float volume, float pitch) {}

        @Override
        public void a(String particle, double x, double y, double z, double r, double g, double b) {
            if (!checkObstacle || !"reddust".equals(particle)) return;
            check(!(x >= 293 && x < 294 && z >= 302 && z < 305 && y < 6), "never emits markers through the wall");
            detour |= z < 302 || z >= 305 || y >= 6;
            furthest = Math.max(furthest, x);
            markers++;
        }
    }
}
