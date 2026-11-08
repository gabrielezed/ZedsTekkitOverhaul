package zed.tekkitoverhaul.emc;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Versioned profile syntax, independent of Minecraft and EE2. */
final class EmcProfile {
    // Condenser accounting scales values by 80; leave room for a full stack.
    static final int MAX_VALUE = Integer.MAX_VALUE / 80 / 64;

    enum Action { INHERIT, SET, DISABLE }
    enum Mode { AUDIT, APPLY }

    static final class Rule {
        final int line;
        final String identity;
        final int meta;
        final Action action;
        final int value;

        Rule(int line, String identity, int meta, Action action, int value) {
            this.line = line;
            this.identity = identity;
            this.meta = meta;
            this.action = action;
            this.value = value;
        }
    }

    final Mode mode;
    final List<Rule> rules = new ArrayList<Rule>();

    private EmcProfile(Mode mode) {
        this.mode = mode;
    }

    static EmcProfile read(BufferedReader input) throws IOException {
        EmcProfile profile = null;
        String line;
        int number = 0;
        while ((line = input.readLine()) != null) {
            number++;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            String[] columns = line.split("\\s+");
            try {
                if (profile == null) {
                    if (columns.length != 3 || !columns[0].equals("zto-emc") || !columns[1].equals("1")) {
                        throw new IllegalArgumentException("expected zto-emc 1 AUDIT or zto-emc 1 APPLY");
                    }
                    profile = new EmcProfile(Mode.valueOf(columns[2]));
                } else {
                    profile.rules.add(parseRule(number, columns));
                }
            } catch (IllegalArgumentException failure) {
                throw new IOException("line " + number + ": " + failure.getMessage(), failure);
            }
        }
        if (profile == null) throw new IOException("missing profile header");
        return profile;
    }

    private static Rule parseRule(int line, String[] columns) {
        if (columns.length < 3 || columns.length > 4) {
            throw new IllegalArgumentException("expected action, identity, metadata and value for SET");
        }
        Action action = Action.valueOf(columns[0]);
        String identity = columns[1];
        if (!identity.matches("(id:[0-9]+|name:[^:]+)")) {
            throw new IllegalArgumentException("expected id:<number> or name:<internal-name>");
        }
        int meta = columns[2].equals("*") ? -1 : unsigned(columns[2], 32767);
        if (meta == -1 && action != Action.DISABLE) {
            throw new IllegalArgumentException("only DISABLE accepts * metadata");
        }
        if ((action == Action.SET) != (columns.length == 4)) {
            throw new IllegalArgumentException("SET requires one positive value");
        }
        int value = action == Action.SET ? unsigned(columns[3], MAX_VALUE) : 0;
        if (action == Action.SET && value == 0) throw new IllegalArgumentException("SET requires a positive value");
        return new Rule(line, identity, meta, action, value);
    }

    static int unsigned(String text, int max) {
        if (!text.matches("[0-9]+")) throw new IllegalArgumentException("expected an unsigned integer: " + text);
        int value = Integer.parseInt(text);
        if (value > max) throw new IllegalArgumentException("value exceeds " + max + ": " + text);
        return value;
    }
}
