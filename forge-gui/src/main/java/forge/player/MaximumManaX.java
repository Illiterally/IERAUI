package forge.player;

import forge.ai.ComputerUtilMana;
import forge.ai.simulation.GameCopier;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.function.IntPredicate;

/** Predicts the largest X supported by Forge's auto-payment, without spending live resources. */
public final class MaximumManaX {
    private MaximumManaX() { }

    // Bound work for unbounded/repeatable mana engines; manual input remains available.
    static final int SEARCH_LIMIT = 100_000;

    public static Integer calculate(final SpellAbility ability, final int min, final int max) {
        if (ability.getPayCosts() == null || !ability.costHasManaX() || min > max) {
            return null;
        }
        try {
            // Prediction can adjust mana choices and payment bookkeeping. Keep all of it
            // in an isolated game, including when the user subsequently cancels.
            final GameCopier copier = new GameCopier(ability.getHostCard().getGame());
            copier.makeCopy();
            final Player payer = copier.find(ability.getActivatingPlayer());
            final Card host = copier.find(ability.getHostCard());
            final SpellAbility probe = ability.copy(host, payer, false);
            return search(min, max, x -> {
                probe.setXManaCostPaid(x);
                return ComputerUtilMana.canPayManaCost(probe, payer, x, probe.isTrigger());
            });
        } catch (RuntimeException ex) {
            // Some special costs cannot be predicted. Do not block or change a cast.
            System.err.println("IERAUI: automatic X unavailable for " + ability.getHostCard().getName()
                    + ": " + ex.getMessage());
            return null;
        }
    }

    /** Exponential bracketing avoids Forge AI's 99-X ceiling; binary search avoids a check per mana. */
    static Integer search(final int min, final int max, final IntPredicate canPay) {
        if (min < 0 || min > max || min > SEARCH_LIMIT || !canPay.test(min)) { return null; }
        if (min == max) { return min; }
        final int ceiling = Math.min(max, SEARCH_LIMIT);
        int low = min;
        int high = Math.min(ceiling, Math.max(1, min + 1));
        while (canPay.test(high)) {
            low = high;
            if (high == ceiling) {
                // Never label the computation limit as the actual maximum.
                return max <= SEARCH_LIMIT ? high : null;
            }
            high = (int) Math.min(ceiling, (long) high * 2);
        }
        while (low + 1 < high) {
            final int mid = low + (high - low) / 2;
            if (canPay.test(mid)) { low = mid; } else { high = mid; }
        }
        return low;
    }
}
