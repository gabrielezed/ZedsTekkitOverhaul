package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** One admission and one terminal transition per stamped Observatory, on the world thread. */
public final class CustodianRecord {
    private static final int MAGIC = 0x5a544f43;
    private static final int SCHEMA = 1;
    private final File file;
    private final long site;
    private UUID actor;
    private boolean defeated, unavailable;

    CustodianRecord(File siteFile, long site) {
        this.site = site;
        file = new File(siteFile.getParentFile(), siteFile.getName() + ".custodian");
        try {
            DataInputStream data = new DataInputStream(Files.newInputStream(file.toPath()));
            try {
                if (data.readInt() != MAGIC || data.readInt() != SCHEMA || data.readLong() != site)
                    throw new IOException("Unexpected Custodian record identity");
                actor = new UUID(data.readLong(), data.readLong());
                defeated = data.readBoolean();
                if (data.read() != -1) throw new IOException("Trailing Custodian record data");
            } finally {
                data.close();
            }
        } catch (NoSuchFileException absent) {
            // Existing Observatories have no record until first admission.
        } catch (IOException failure) {
            invalidate(failure);
        }
    }

    public boolean available() { return !unavailable && actor == null; }
    public boolean owns(UUID id) { return !unavailable && !defeated && actor != null && actor.equals(id); }

    public UUID reserve() {
        if (!available()) return null;
        actor = UUID.randomUUID();
        return persist() ? actor : null;
    }

    public boolean defeat(UUID id) {
        if (!owns(id)) return false;
        defeated = true;
        return persist();
    }

    public String status() {
        return unavailable ? "unavailable" : defeated ? "defeated / reward consumed"
                : actor == null ? "unvisited" : "reserved";
    }

    private boolean persist() {
        File pending = new File(file.getParentFile(), file.getName() + ".pending");
        try {
            FileOutputStream output = new FileOutputStream(pending);
            DataOutputStream data = new DataOutputStream(output);
            try {
                data.writeInt(MAGIC);
                data.writeInt(SCHEMA);
                data.writeLong(site);
                data.writeLong(actor.getMostSignificantBits());
                data.writeLong(actor.getLeastSignificantBits());
                data.writeBoolean(defeated);
                data.flush();
                output.getFD().sync();
            } finally {
                data.close();
            }
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException failure) {
            invalidate(failure);
            return false;
        }
    }

    private void invalidate(IOException failure) {
        unavailable = true;
        System.err.println("[ZTO] Custodian record unavailable for " + Long.toHexString(site) + ": " + failure);
    }
}
