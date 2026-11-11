package forge.game.combat;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CombatResultSummaryTest extends AITest {
    @Test public void firstStrikeReportsActualOverkillAndUnresolvedArmySeparately() {
        Game game = initAndCreateGame();
        Player attacking = game.getPlayers().get(1), defending = game.getPlayers().get(0);
        attacking.setLife(39, null);
        defending.setLife(1, null);
        Card hawk = addCard("Healer's Hawk", attacking);
        hawk.addIntrinsicKeyword("First strike");
        Card serra = addCard("Serra Ascendant", attacking);
        Card destiny = addCard("Angelic Destiny", attacking);
        destiny.attachToEntity(serra, null, true);
        Combat combat = new Combat(attacking);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE, attacking);
        game.getPhaseHandler().setCombat(combat);
        for (Card card : addTokens("w_4_4_angel_flying", 55, attacking)) {
            combat.addAttacker(card, defending);
            combat.setBlocked(card, false);
        }
        combat.addAttacker(hawk, defending); combat.setBlocked(hawk, false);
        combat.addAttacker(serra, defending); combat.setBlocked(serra, false);
        game.getAction().checkStaticAbilities();
        assertEquals(serra.getNetPower(), 10);
        combat.assignCombatDamage(true);
        combat.dealAssignedDamage();
        assertEquals(defending.getLife(), -10);
        defending.loseConditionMet(GameLossReason.LifeReachedZero, null);
        var lines = CombatResultSummary.describe(game);
        assertTrue(lines.get(0).contains("actual overkill 10"));
        assertTrue(lines.contains("First strike ended the game before regular combat damage."));
        assertTrue(lines.get(2).contains("220 (not dealt)"));
        assertEquals(defending.getLife(), -10, "Reporting must not apply hypothetical damage");
        String previous = System.getProperty("forge.enhancedRulesUi");
        try {
            System.setProperty("forge.enhancedRulesUi", "true");
            game.getAction().checkGameOverCondition();
            assertTrue(game.isGameOver());
            var outcome = game.getGameLog().getLogEntriesExact(GameLogEntryType.GAME_OUTCOME);
            assertTrue(outcome.stream().anyMatch(e -> e.message().contains("220 (not dealt)")),
                    "The actual game-over path must retain the pending army in its result log");
        } finally {
            if (previous == null) System.clearProperty("forge.enhancedRulesUi");
            else System.setProperty("forge.enhancedRulesUi", previous);
        }
    }

    @Test public void normalLethalDamageAndConcessionDoNotClaimMissingCombat() {
        Game game = initAndCreateGame();
        Player loser = game.getPlayers().get(0);
        loser.setLife(-240, null);
        loser.loseConditionMet(GameLossReason.LifeReachedZero, null);
        assertEquals(CombatResultSummary.describe(game).size(), 1);
        assertTrue(CombatResultSummary.describe(game).get(0).contains("actual overkill 240"));
        loser.concede();
        assertTrue(CombatResultSummary.describe(game).isEmpty());
    }

    @Test public void blockedCreaturesAreNotCountedAsUnblockedPower() {
        Game game = initAndCreateGame();
        Player attacking = game.getPlayers().get(1), defending = game.getPlayers().get(0);
        Combat combat = new Combat(attacking);
        Card angel = addToken("w_4_4_angel_flying", attacking);
        Card blocker = addCard("Healer's Hawk", defending);
        combat.addAttacker(angel, defending);
        combat.addBlocker(angel, blocker);
        combat.setBlocked(angel, true);
        assertEquals(combat.getRemainingUnblockedPower(defending), 0L);
    }
}
