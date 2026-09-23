package forge.screens.match.views;

import java.util.HashSet;
import java.util.Set;

/** Detect new stack objects rather than treating a repeated refresh as new activity. GPL-3.0-or-later. */
final class StackActivity {
    private Integer gameId;
    private Set<Integer> previous = Set.of();

    boolean update(final int game, final Set<Integer> current) {
        final boolean added = gameId != null && gameId == game && !previous.containsAll(current);
        gameId = game;
        previous = new HashSet<>(current);
        return added;
    }
}
