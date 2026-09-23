package forge.toolbox.special;

import java.util.HashMap;
import java.util.Map;

/** Short-lived net changes from observed public totals; never predicts game events. GPL-3.0-or-later. */
final class ResourceChanges {
    static final long DURATION_MILLIS = 3000;
    private record Value(int total, int change, long until) {}
    private final Map<String, Value> values = new HashMap<>();

    int observe(final String key, final int total, final long now) {
        final Value previous = values.get(key);
        if (previous == null) {
            values.put(key, new Value(total, 0, 0));
            return 0; // Loading a game or revealing a panel is not a gain.
        }
        final int change = (now < previous.until() ? previous.change() : 0) + total - previous.total();
        final long until = total != previous.total() ? now + DURATION_MILLIS : previous.until();
        values.put(key, new Value(total, change, until));
        return now < until ? change : 0;
    }

    boolean isActive(final long now) {
        return values.values().stream().anyMatch(v -> v.change() != 0 && now < v.until());
    }

    void clear() { values.clear(); }

    static String signed(final int amount) { return amount > 0 ? "+" + amount : "\u2212" + -(long) amount; }
}
