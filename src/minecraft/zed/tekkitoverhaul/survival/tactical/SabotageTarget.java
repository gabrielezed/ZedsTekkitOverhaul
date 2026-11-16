package zed.tekkitoverhaul.survival.tactical;

/** A transfer boundary owned by ZTO. Its tile retains ownership of resources and the pause. */
public interface SabotageTarget {
    boolean isDisrupted();
    boolean beginDisruption(EntitySaboteur owner);
}
