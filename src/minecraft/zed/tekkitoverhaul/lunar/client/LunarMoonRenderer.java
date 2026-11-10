package zed.tekkitoverhaul.lunar.client;

import java.util.function.ToIntBiFunction;
import java.lang.reflect.Method;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Supplies only the atlas cell, for the exact local world being rendered. */
final class LunarMoonRenderer {
    private static Method agentStatus;

    static void register(final LocalLunarCalendar local) {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.lunaragent.MoonAgent", true,
                    ClassLoader.getSystemClassLoader());
            ToIntBiFunction<Object, Integer> callback = new ToIntBiFunction<Object, Integer>() {
                public int applyAsInt(Object world, Integer original) {
                    if (!(world instanceof World)) return original;
                    LunarSnapshot snapshot = local.snapshot((World) world);
                    return snapshot != null && snapshot.schedulingEligible ? atlasCell(snapshot) : original;
                }
            };
            agent.getMethod("register", ToIntBiFunction.class).invoke(null, callback);
            agentStatus = agent.getMethod("status");
        } catch (ClassNotFoundException absent) {
            // The launch agent is optional.
        } catch (ReflectiveOperationException failure) {
            agentStatus = null;
        }
    }

    static int atlasCell(LunarSnapshot snapshot) { return (snapshot.phase.id + 4) % 8; }
    static String status() {
        if (agentStatus == null) return "Agent unavailable; vanilla phase";
        try {
            return (String) agentStatus.invoke(null);
        } catch (ReflectiveOperationException failure) {
            return "Agent bridge unavailable; vanilla phase";
        }
    }
}
