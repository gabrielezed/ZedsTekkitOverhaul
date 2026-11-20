package zed.tekkitoverhaul.settlement;

import java.util.List;
import net.minecraft.src.World;

/** Read-only authority for consumers on the owning world thread, after the service tick. */
public interface SiteQueries {
    SiteSnapshot resolve(World world, int siteId);
    List<SiteSnapshot> nearby(World world, int x, int y, int z, int radius);
    Point locate(World world, int x, int y, int z);
    ProtectedInterior.Assessment protection(World world, int x, int y, int z);
    /** True only outside every recorded horizontal envelope, including gaps between known parts. */
    boolean insertionExterior(World world, int x, int z, int margin);

    enum Recognition { RECOGNIZED, CANDIDATE, AMBIGUOUS, UNKNOWN }

    final class Point {
        public final Recognition state;
        public final SiteSnapshot site;
        public final String reason;

        Point(Recognition state, SiteSnapshot site, String reason) {
            this.state = state;
            this.site = site;
            this.reason = reason;
        }
    }
}
