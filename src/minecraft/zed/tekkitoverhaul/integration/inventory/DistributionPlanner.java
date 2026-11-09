package zed.tekkitoverhaul.integration.inventory;

/** Even, capacity-aware allocation. No Minecraft dependencies. */
public final class DistributionPlanner {
    private DistributionPlanner() { }

    public static int[] drag(int items, int[] capacities, boolean singles) {
        int[] result = new int[capacities.length];
        int eligible = 0;
        for (int capacity : capacities) if (capacity > 0) eligible++;
        if (items <= 0 || eligible == 0) return result;
        int share = singles ? 1 : items / eligible;
        for (int index = 0; index < capacities.length && items > 0; index++) {
            result[index] = Math.min(items, Math.min(share, Math.max(0, capacities[index])));
            items -= result[index];
        }
        return result;
    }

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
