package zed.tekkitoverhaul.lunar.client;

import java.util.function.ToIntBiFunction;
import java.lang.reflect.Method;
import net.minecraft.src.RenderGlobal;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Supplies only the atlas cell, for the exact local world being rendered. */
final class LunarMoonRenderer {
    private static Method agentStatus;
    private static ToIntBiFunction<Object, Integer> callback;

    static void register(final LocalLunarCalendar local) {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false,
                    ClassLoader.getSystemClassLoader());
            callback = new ToIntBiFunction<Object, Integer>() {
                public int applyAsInt(Object world, Integer original) {
                    if (!(world instanceof World)) return original;
                    LunarSnapshot snapshot = local.snapshot((World) world);
                    return snapshot != null && snapshot.schedulingEligible ? atlasCell(snapshot) : original;
                }
            };
            agent.getMethod("registerMoon", ToIntBiFunction.class).invoke(null, callback);
            agentStatus = agent.getMethod("moonStatus", ClassLoader.class);
        } catch (ClassNotFoundException absent) {
            // The launch agent is optional.
        } catch (ReflectiveOperationException failure) {
            agentStatus = null;
        } catch (LinkageError failure) {
            agentStatus = null;
        }
    }

    static int atlasCell(LunarSnapshot snapshot) { return (snapshot.phase.id + 4) % 8; }
    static String status() {
        if (agentStatus == null) return "Agent unavailable; vanilla phase";
        try {
            return (String) agentStatus.invoke(null, RenderGlobal.class.getClassLoader());
        } catch (ReflectiveOperationException failure) {
            return "Agent bridge unavailable; vanilla phase";
        }
    }
}
