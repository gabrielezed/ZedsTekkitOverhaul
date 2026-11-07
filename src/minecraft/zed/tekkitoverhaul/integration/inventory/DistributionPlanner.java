package zed.tekkitoverhaul.integration.inventory;

/** Even, capacity-aware allocation. No Minecraft dependencies. */
public final class DistributionPlanner {
    private DistributionPlanner() { }

    public static int[] distribute(int items, int[] capacities) {
        int[] result = new int[capacities.length];
        if (items <= 0) return result;
        // Round-robin allocation: counts differ by at most one when limits permit.
        boolean progress;
        do {
            progress = false;
            for (int i = 0; i < capacities.length && items > 0; i++) {
                if (result[i] < capacities[i]) {
                    result[i]++;
                    items--;
                    progress = true;
                }
            }
        } while (items > 0 && progress);
        return result;
    }
}
