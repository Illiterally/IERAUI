package forge.ai;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Cooperative, same-thread AI decisions. Never leave a worker mutating a live game. */
public final class AiEvaluation {
    private static final ThreadLocal<Long> DEADLINE = new ThreadLocal<>();

    private AiEvaluation() { }

    public static <T> T run(long timeoutMillis, Supplier<T> decision, T fallback) {
        Long previous = DEADLINE.get();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0, timeoutMillis));
        DEADLINE.set(previous == null ? deadline : Math.min(previous, deadline));
        try {
            check();
            return decision.get();
        } catch (Expired expired) {
            // Nested decisions must unwind to the outer decision's fallback.
            if (previous != null) {
                throw expired;
            }
            AiCache.clear();
            return fallback;
        } catch (RuntimeException failed) {
            // Preserve the previous evaluator's error reporting and safe pass.
            if (previous != null) {
                throw failed;
            }
            failed.printStackTrace();
            AiCache.clear();
            return fallback;
        } finally {
            if (previous == null) {
                DEADLINE.remove();
            } else {
                DEADLINE.set(previous);
            }
        }
    }

    public static void check() {
        Long deadline = DEADLINE.get();
        if (deadline != null && (Thread.currentThread().isInterrupted() || System.nanoTime() - deadline >= 0)) {
            throw new Expired();
        }
    }

    private static final class Expired extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private Expired() { super(null, null, false, false); }
    }
}
