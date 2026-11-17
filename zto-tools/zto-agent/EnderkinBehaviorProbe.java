import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipeItemsWood;
import buildcraft.api.PowerFramework;
import buildcraft.core.RedstonePowerFramework;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import zed.tekkitoverhaul.survival.tactical.EnderkinEcology;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.EntityThief;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Focused encounters in a real Tekkit world, without starting the graphical client. */
public final class EnderkinBehaviorProbe {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        xd world = new xd(new aia(root, "combat", false), "combat", NaturalSpawnProbe.SETTINGS) {
            // The pack's audio hook queries LWJGL Display even without a GUI or sound backend.
            @Override public void a(nn entity, String sound, float volume, float pitch) {}
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        world.q = 2;
        for (int x = 15; x <= 21; x++)
            for (int z = 15; z <= 21; z++) world.d(x, z);
        yw player = NaturalSpawnProbe.join(world);
        EntityTank tank = new EntityTank(world);
        check(tank.d() == 64 && tank.bb() == 64 && tank.d() > new jg(world).d(), "tank health");
        tank.a(player, 5, 1, 0);
        check(Math.abs(tank.r + 0.16) < 0.001 && Math.abs(tank.s - 0.16) < 0.001, "resists the native knockback impulse");
        tank.r = tank.s = 0;
        place(tank, player, 2.5);
        int health = player.bb();
        fight(tank, 1, true);
        for (int tick = 2; tick < 25; tick++) fight(tank, tick, true);
        check(player.bb() == health && tank.heavyPose(0) == 23, "windup precedes damage");
        fight(tank, 25, true);
        check(player.bb() <= health - 8 && tank.heavyPose(0) == EntityTank.WINDUP, "strike keyframe damages");
        int after = player.bb();
        fight(tank, 40, true);
        check(player.bb() == after, "recovery prevents another strike");
        EntityTank dodged = new EntityTank(world);
        place(dodged, player, 2.5);
        fight(dodged, 1, true);
        player.c(288.5, 4, 291, 0, 0);
        fight(dodged, 25, true);
        check(player.bb() == after, "dodging the committed arc prevents damage");
        check(Math.abs(dodged.r) < 0.01, "stays at the committed position during the windup");

        for (int difficulty : new int[] {2, 3}) {
            world.q = difficulty;
            EntityTank blocked = new EntityTank(world);
            place(blocked, player, 5);
            for (int y = 4; y <= 7; y++) {
                world.g(290, y, 288, 5);  // Planks.
                world.g(290, y, 289, 49); // Obsidian.
            }
            for (int tick = 1; tick <= 63; tick++) fight(blocked, tick, false);
            check(world.a(290, 4, 288) == 5, "no structural damage during windup");
            for (int tick = 64; tick <= 100; tick++) fight(blocked, tick, false);
            check(world.a(290, 4, 288) == (difficulty == 3 ? 0 : 5), "only Hard can breach the plank column");
            check(world.a(290, 4, 289) == 49, "obsidian protection");
            ady nbt = new ady();
            blocked.b(nbt);
            EntityTank reopened = new EntityTank(world);
            reopened.a(nbt);
            ady roundTrip = new ady();
            reopened.b(roundTrip);
            check(roundTrip.f("ZtoBreachStrikes") == (difficulty == 3 ? 1 : 0)
                    && reopened.heavyPose(0) < 0, "breach budget survives reload and windup clears");
        }
        EntityTank interrupted = new EntityTank(world);
        place(interrupted, player, 5);
        world.g(290, 5, 288, 5);
        for (int tick = 1; tick <= 55; tick++) fight(interrupted, tick, false);
        check(interrupted.heavyPose(0) > 0, "Tank begins a breach windup");
        check(interrupted.a(md.a(player), 6), "ordinary player damage still applies");
        for (int tick = 56; tick <= 74; tick++) fight(interrupted, tick, false);
        check(interrupted.heavyPose(0) < 0 && world.a(290, 5, 288) == 5, "a strong hit interrupts the breach");
        world.g(290, 5, 288, 0);
        for (int y = 4; y <= 7; y++) world.g(290, y, 289, 0);
        EntitySkirmisher skirmisher = new EntitySkirmisher(world);
        place(skirmisher, player, 4);
        fight(skirmisher, 33, true);
        check(Math.abs(skirmisher.q - 288.5) > 1, "skirmisher lateral blink");
        EntityThief thief = new EntityThief(world);
        place(thief, player, 1.5);
        fight(thief, 1, true);
        fight(thief, 2, true);
        check(thief.o < 287.5, "thief escapes after melee contact");
        EntityBreacher breacher = new EntityBreacher(world);
        place(breacher, player, 5);
        player.c(293.5, 7, 288.5, 0, 0);
        for (int x = 290; x <= 294; x++)
            for (int z = 287; z <= 290; z++) world.g(x, 6, z, 1);
        fight(breacher, 25, true);
        check(breacher.y.b >= 6.99, "breacher follows to a clear, grounded platform");
        EntityTank climbing = new EntityTank(world);
        climbing.c(286.5, 4, 288.5, 0, 0);
        climbing.z = true;
        climbing.engageEncounterPlayer(player);
        for (int z = 287; z <= 290; z++) world.g(289, 6, z, 1);
        player.z = false;
        for (int tick = 1; tick <= 85; tick++) fight(climbing, tick, true);
        check(climbing.heavyPose(0) < 0, "an airborne target does not trigger relocation");
        player.z = true;
        for (int tick = 86; tick <= 97; tick++) fight(climbing, tick, true);
        check(climbing.y.b < 5, "Tank stays grounded until its relocation windup finishes");
        for (int tick = 98; tick <= 125; tick++) fight(climbing, tick, true);
        check(climbing.y.b >= 6.99, "Tank can relocate to an open platform after a prolonged stall");
        System.out.println("PASS Tank windup, impact, dodge, recovery, Hard breach, protected blocks and NBT; role blinks");

        xd habitat = NaturalSpawnProbe.open(root, "habitat", 18000);
        for (int x = 17; x <= 19; x++)
            for (int z = 17; z <= 19; z++) habitat.d(x, z);
        ajg original = new ajg(habitat);
        original.c(290.5, 4, 290.5, 0, 0);
        habitat.r = new Random() {
            @Override public int nextInt(int bound) { return bound == 1000 ? 100 : super.nextInt(bound); }
        };
        check(EnderkinEcology.select(original, 0) == original, "contextual roll in wilderness keeps vanilla");
        TileGenericPipe pipe = new TileGenericPipe();
        PowerFramework.currentFramework = new RedstonePowerFramework();
        pipe.pipe = new PipeItemsWood(0);
        pipe.i = habitat;
        pipe.j = 294;
        pipe.k = 4;
        pipe.l = 290;
        // Bind the real installed pipe tile in the fixture's already loaded chunk.
        habitat.d(18, 18).i.put(new qo(6, 4, 2), pipe);
        check(EnderkinEcology.select(original, 0) instanceof EntitySaboteur, "same roll near exposed BuildCraft pipe");
        habitat.g(292, 4, 290, 1);
        habitat.g(292, 5, 290, 1);
        check(EnderkinEcology.select(original, 0) == original, "solid wall hides the industrial signal");

        int natural = 0;
        for (int run = 0; run < 3 && natural == 0; run++) {
            xd ordinary = NaturalSpawnProbe.open(root, "ecology-" + run, 18000);
            yw resident = NaturalSpawnProbe.join(ordinary);
            TerritorialActivityService territory = new TerritorialActivityService();
            territory.bind(ordinary);
            for (int tick = 0; tick < 4800; tick++) territory.tick(resident, false);
            NaturalSpawnPressure pressure = new NaturalSpawnPressure(territory, 70, true);
            pressure.bind(ordinary);
            pressure.register();
            NaturalSpawnProbe.populate(ordinary, pressure);
            natural += ordinary.b(TacticalHostile.class);
            System.out.printf("Native population: %d monsters, %d slimes, %d zombies, %d skeletons, %d Enderkin%n",
                    ordinary.b(xf.class), ordinary.b(aja.class), ordinary.b(ajg.class), ordinary.b(xr.class),
                    ordinary.b(TacticalHostile.class));
            check(ordinary.b(xf.class) >= 120 && ordinary.b(xf.class) <= 138
                    && ordinary.b(TacticalHostile.class) < 20, "ecology shares the boosted hostile cap");
        }
        check(natural > 0, "pinned native spawn pass inserts Enderkin");
        System.out.printf("PASS native Enderkin coexist with vanilla, %d insertions; confirmed exposed pipe preference%n", natural);
    }

    private static void place(TacticalHostile mob, yw player, double distance) {
        mob.c(288.5, 4, 288.5, 0, 0);
        player.c(288.5 + distance, 4, 288.5, 0, 0);
        mob.z = player.z = true;
        mob.engageEncounterPlayer(player);
    }

    private static void fight(TacticalHostile mob, int tick, boolean visible) throws Exception {
        mob.V = tick;
        // The fixture holds position so each attack and route timer can be checked independently.
        Method method = mob.getClass().getDeclaredMethod("fight", double.class, boolean.class);
        method.setAccessible(true);
        Field target = TacticalHostile.class.getDeclaredField("target");
        target.setAccessible(true);
        method.invoke(mob, mob.f((yw) target.get(mob)), visible);
    }

    private static void check(boolean passed, String detail) {
        if (!passed) throw new AssertionError(detail);
    }
}
