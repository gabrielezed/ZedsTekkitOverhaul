import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import zed.tekkitoverhaul.survival.tactical.EntityThief;
import zed.tekkitoverhaul.survival.tactical.ThiefTrail;

/** Real inventory, navigating entities and save files in the pinned client, with audio muted. */
public final class ThiefBehaviorProbe {
    public static void main(String[] args) {
        try {
            run(new File(args[0]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0); // Anvil's native IO thread lives as long as the Java process.
    }

    private static void run(File root) throws Exception {
        Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
        register.setAccessible(true);
        register.invoke(null, EntityThief.class, "ZTO.Thief", 210);
        Method fight = EntityThief.class.getDeclaredMethod("fight", double.class, boolean.class);
        fight.setAccessible(true);
        SilentWorld world = new SilentWorld(root, "thief");
        ProbePlayer player = new ProbePlayer(world);
        EntityThief interrupted = atContact(world, player);
        aan source = new aan(35, 3, 14);
        ady payload = new ady();
        ady capacitor = new ady();
        capacitor.a("Energy", 7654321);
        payload.a("Fixture", capacitor);
        source.d = payload;
        player.ap.a[20] = source;
        reach(fight, interrupted, 35);
        check(interrupted.theftPose(0.5F) > 0 && source.a == 3, "readable windup precedes transfer");
        check(interrupted.a(md.a(player), 2), "player can hit during windup");
        reach(fight, interrupted, 51);
        check(source.a == 3 && !interrupted.carryingLoot() && interrupted.theftPose(0) == 0, "interruption keeps inventory");

        EntityThief thief = atContact(world, player);
        reach(fight, thief, 35);
        reach(fight, thief, 50);
        check(source.a == 3 && !thief.carryingLoot(), "last windup tick keeps inventory");
        reach(fight, thief, 51);
        check(source.a == 2 && thief.carryingLoot() && thief.theftPose(0) == 1, "one item changes custody at contact");
        check(world.sounds.contains("random.pop") && player.lastMessage.contains("stole"), "ordinary theft cue");
        check(world.a(thief), "spawn the acting Thief");
        uh lastSeen = ThiefTrail.inspect(world, player);
        check(lastSeen != null && lastSeen.a == (int) thief.o && lastSeen.c == (int) thief.q
                && player.lastMessage.contains("last seen") && world.b(fq.class) == 0,
                "compass directs the trail to the live carrier's saved position without releasing loot");

        double startX = thief.o, startZ = thief.q;
        int beforeChase = player.bb();
        for (int tick = 0; tick < 270; tick++) {
            double dx = thief.o - player.o, dz = thief.q - player.q;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 12) player.c(player.o + dx * 0.24 / distance, 4, player.q + dz * 0.24 / distance, 0, 0);
            world.n();
        }
        double travelled = Math.hypot(thief.o - startX, thief.q - startZ);
        check(travelled >= 32 && thief.carryingLoot() && player.bb() == beforeChase,
                "native navigation sustains the chase across chunks: " + travelled);
        player.c(600, 4, 288, 0, 0);
        world.n();
        check(thief.carryingLoot(), "distance despawn leaves carrier alive");
        player.c(thief.o - 2, 4, thief.q, 0, 0);
        world.a(true, null);
        ce.a.a();
        agp.a();

        SilentWorld reopened = new SilentWorld(root, "thief");
        ProbePlayer returned = new ProbePlayer(reopened);
        returned.a(reopened.B().h());
        EntityThief restored = null;
        for (Object candidate : reopened.b)
            if (candidate instanceof EntityThief) restored = (EntityThief) candidate;
        check(restored != null && returned.ap.a[20].a == 2, "native Anvil reload restores entity and debited inventory");
        reopened.n();
        check(restored.carryingLoot(), "restored entity owns the saved cargo");
        check(restored.a(md.a(returned), 20), "player can kill the reloaded carrier");
        restored.A();
        check(reopened.b(fq.class) == 1, "death and removal release exactly one drop");
        fq drop = null;
        for (Object entity : reopened.b) if (entity instanceof fq) drop = (fq) entity;
        check(drop != null && drop.a.a == 1 && drop.a.c == 35 && drop.a.i() == 14
                && drop.a.d.equals(payload), "native drop preserves count, id, metadata and nested NBT");

        SilentWorld hazard = new SilentWorld(root, "thief-cache");
        ProbePlayer survivor = new ProbePlayer(hazard);
        EntityThief hotbar = atContact(hazard, survivor);
        survivor.ap.a[1] = new aan(276, 1, 17);
        survivor.ap.b[0] = new aan(313, 1, 4);
        reach(fight, hotbar, 35);
        reach(fight, hotbar, 51);
        check(survivor.ap.a[1] == null && survivor.ap.b[0] != null && hotbar.carryingLoot()
                && survivor.lastMessage.contains("hotbar") && hazard.sounds.contains("mob.endermen.portal"), "hotbar cue and armor exclusion");
        hotbar.c(310.5, 120, 288.5, 0, 0);
        hotbar.A(); // Simulate forced removal with no safe surface near the carrier.
        check(hazard.b(fq.class) == 0, "unsafe removal retains a cache");
        hazard.z.a();
        SilentWorld cached = new SilentWorld(root, "thief-cache");
        ProbePlayer recovering = new ProbePlayer(cached);
        recovering.c(270.5, 4, 288.5, 0, 0);
        uh cache = ThiefTrail.inspect(cached, recovering);
        check(cache != null && cache.a == 288 && cache.c == 288 && cached.b(fq.class) == 0,
                "compass directs the trail to the recovery cache from outside recovery range");
        recovering.c(288.5, 4, 288.5, 0, 0);
        check(ThiefTrail.inspect(cached, recovering) == null && ThiefTrail.inspect(cached, recovering) == null,
                "the trail clears when the cache is recovered");
        check(cached.b(fq.class) == 1, "cache survives save and can be recovered exactly once");
        System.out.printf("PASS Thief theft, interruption, hotbar cue, Anvil reload and one-shot recovery; chased %.1f blocks%n", travelled);
    }

    private static EntityThief atContact(SilentWorld world, ProbePlayer player) {
        EntityThief thief = new EntityThief(world);
        thief.c(290.0, 4, 288.5, 0, 0);
        player.c(288.5, 4, 288.5, 0, 0);
        thief.z = player.z = true;
        thief.engageEncounterPlayer(player);
        return thief;
    }

    private static void reach(Method fight, EntityThief thief, int tick) throws Exception {
        thief.V = tick;
        fight.invoke(thief, 5.0, true);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class SilentWorld extends xd {
        final List<String> sounds = new ArrayList<>();

        SilentWorld(File root, String name) {
            super(new aia(root, name, false), name, NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 13; x <= 32; x++)
                for (int z = 14; z <= 22; z++) d(x, z);
        }

        @Override public void a(nn entity, String sound, float volume, float pitch) { sounds.add(sound); }
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
    }

    private static final class ProbePlayer extends yw {
        String lastMessage = "";

        @SuppressWarnings("unchecked")
        ProbePlayer(xd world) {
            super(world);
            aA = "ThiefProbe";
            world.d.add(this);
        }

        @Override public void ab() {}
        @Override public void b(String message) { lastMessage = message; }
    }
}
