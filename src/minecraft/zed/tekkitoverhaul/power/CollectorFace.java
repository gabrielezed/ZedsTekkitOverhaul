package zed.tekkitoverhaul.power;

/** Source offsets relative to the EE2 packet receiver, in packet side order. */
enum CollectorFace {
    BELOW(0, -1, 0), ABOVE(0, 1, 0),
    SOUTH(0, 0, 1), NORTH(0, 0, -1), WEST(-1, 0, 0), EAST(1, 0, 0);

    static final CollectorFace[] PACKET_SIDES = values();
    final int dx;
    final int dy;
    final int dz;

    CollectorFace(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}
