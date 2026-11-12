package zed.tekkitoverhaul.assault;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.SaveHandlerMP;
import net.minecraft.src.World;
import net.minecraft.src.WorldProviderSurface;
import net.minecraft.src.WorldSettings;
import net.minecraft.src.WorldType;
import zed.tekkitoverhaul.assault.SiegeDirector.State;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Exercises the pending patrol through the same tick entry point used by the director. */
public final class LunarPatrolSelfTest {
    public static void main(String[] args) {
        World world = new World(new SaveHandlerMP(), "Patrol test", new WorldProviderSurface(),
                new WorldSettings(0L, 0, false, false, WorldType.DEFAULT));
        world.difficultySetting = 2;
        EntityPlayer player = new EntityPlayer(world) {
            public void func_6420_o() { }
        };
        LunarPatrol patrol = new LunarPatrol(true);
        long dusk = 2L * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
        LunarCalendar calendar = new LunarCalendar(1, 0, 0, Long.MIN_VALUE, () -> { }, failure -> {
            throw new AssertionError(failure);
        });
        calendar.view().subscribe(event -> patrol.onEvent(event, null));
        calendar.rebaseline(0, dusk - 1, LunarEvent.Cause.LOADED);
        calendar.observe(dusk);
        LunarSnapshot moon = calendar.view().snapshot();
        patrol.tick(world, moon, player, false, null);
        for (int tick = 0; tick < 9; tick++) {
            patrol.tick(world, moon, player, false, null);
        }
        for (int tick = 0; tick < 40; tick++) patrol.tick(world, moon, player, true, null);
        check(patrol.state == State.ACTIVE && patrol.paused && patrol.elapsed == 9
                && patrol.attempts == 0 && patrol.spawned == 0, "Pause changed or canceled the patrol");

        patrol.tick(world, moon, player, false, null);
        check(patrol.state == State.ACTIVE && !patrol.paused && patrol.elapsed == 10,
                "The same patrol did not resume");
        for (int tick = 11; tick < PatrolSchedule.SPAWN_DELAY; tick++) {
            patrol.tick(world, moon, player, false, null);
        }
        patrol.tick(world, moon, player, true, null);
        check(patrol.elapsed == PatrolSchedule.SPAWN_DELAY - 1 && patrol.attempts == 0,
                "A paused tick started placement");

        world.difficultySetting = 0;
        patrol.tick(world, moon, player, true, null);
        check(patrol.state == State.ABORTED && "PEACEFUL".equals(patrol.reason),
                "Peaceful must still terminate the patrol");
        System.out.println("Patrol pause lifecycle checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
