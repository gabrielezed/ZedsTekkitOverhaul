import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import zed.tekkitoverhaul.inputagent.MouseAgent;

/** Verifies the transformed dispatcher on the production client ABI without opening a world. */
public final class MouseAgentProbe {
    public static void main(String[] args) throws Exception {
        Class<?> gui = Class.forName("vp");
        final Object screen = gui.newInstance();
        final AtomicInteger calls = new AtomicInteger();
        if (!MouseAgent.register(new Predicate<Object>() {
            @Override
            public boolean test(Object actual) {
                if (actual != screen) throw new AssertionError("Wrong screen instance");
                calls.incrementAndGet();
                return true;
            }
        })) throw new AssertionError("The agent did not transform GuiScreen");
        gui.getMethod("f").invoke(screen);
        if (calls.get() != 1) throw new AssertionError("Expected exactly one dispatch");
        Class<?> container = Class.forName("gb", false, gui.getClassLoader());
        if (container.getDeclaredMethod("f").getDeclaringClass() != container) {
            throw new AssertionError("NEI mouse method is missing");
        }
        if (args.length != 0) {
            Class<?> integration = Class.forName(
                    "zed.tekkitoverhaul.integration.inventory.DragDistributionIntegration");
            integration.getMethod("register").invoke(null);
            if (MouseAgent.handle(screen)) throw new AssertionError("Mod intercepted a non-container screen");
        }
        System.out.println("PASS: production GuiScreen hook verified with NEI gb intact.");
    }
}
