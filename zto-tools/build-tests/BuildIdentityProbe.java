import zed.tekkitoverhaul.BuildIdentity;

/** Run in a fresh JVM for each code source. */
public final class BuildIdentityProbe {
    public static void main(String[] args) throws Exception {
        String label = BuildIdentity.label();
        if (label != BuildIdentity.label() || !args[0].equals(label)) {
            throw new AssertionError("Expected [" + args[0] + "], got [" + label + "]");
        }
        System.out.println("PASS " + label);
    }
}
