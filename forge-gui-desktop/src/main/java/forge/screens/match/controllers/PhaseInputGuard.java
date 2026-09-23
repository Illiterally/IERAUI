package forge.screens.match.controllers;

import java.util.HashSet;
import java.util.Set;

/** Reject queued clicks and held-key repeats at a newly visible phase. */
final class PhaseInputGuard {
    private String context;
    private long enteredAt;
    private long acceptAfter;
    private final Set<Integer> heldKeys = new HashSet<>();
    private final Set<Integer> releaseRequired = new HashSet<>();

    void enter(final String next, final boolean protect, final long now) {
        if (next.equals(context)) { return; }
        context = next;
        enteredAt = now;
        acceptAfter = now;
        releaseRequired.clear();
        if (protect) { protect(now); }
    }

    /** Also protect a new main-phase action after its last stack item resolves. */
    void protect(final long now) {
        enteredAt = now;
        acceptAfter = now + 500;
        releaseRequired.addAll(heldKeys);
    }

    void keyPressed(final int code) { heldKeys.add(code); }

    void keyReleased(final int code) {
        heldKeys.remove(code);
        releaseRequired.remove(code);
    }

    void clearKeys() { heldKeys.clear(); releaseRequired.clear(); }

    boolean accepts(final long eventTime, final long now) {
        return now >= acceptAfter && (eventTime == 0 || eventTime >= enteredAt) && releaseRequired.isEmpty();
    }
}
