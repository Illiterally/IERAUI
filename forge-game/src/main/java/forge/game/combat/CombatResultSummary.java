package forge.game.combat;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

/** Reports the actual outcome separately from attack power that never dealt damage. */
public final class CombatResultSummary {
    private CombatResultSummary() { }

    public static List<String> describe(Game game) {
        List<String> lines = new ArrayList<>();
        boolean firstStrike = game.getPhaseHandler().is(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        Combat combat = game.getCombat();
        for (Player player : game.getRegisteredPlayers()) {
            if (player.getOutcome() == null || player.getOutcome().lossState != GameLossReason.LifeReachedZero) {
                continue;
            }
            long overkill = Math.max(0L, -(long) player.getLife());
            lines.add(player.getName() + ": final life " + player.getLife() + "; actual overkill " + overkill + ".");
            if (firstStrike && combat != null) {
                lines.add("First strike ended the game before regular combat damage.");
                long remaining = combat.getRemainingUnblockedPower(player);
                if (remaining > 0) {
                    lines.add("Unblocked attacking power left against " + player.getName() + ": " + remaining + " (not dealt).");
                }
            }
        }
        return lines;
    }
}
