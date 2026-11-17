package zed.tekkitoverhaul.survival.tactical;

/** A local simulation boundary with a transient pause. The machine retains its resources. */
public interface SabotageTarget {
    String activeIndustrialOperation();
    boolean isDisrupted();
    boolean beginDisruption(EntitySaboteur owner);
}
