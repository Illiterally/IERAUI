package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.phase.PhaseType;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

public class TokenCombatPerformanceTest extends AITest {
    @Test public void deadlineUnwindsOnTheCallingThreadAndRunsCleanup() {
        Thread caller = Thread.currentThread();
        AtomicBoolean cleaned = new AtomicBoolean();
        long start = System.nanoTime();
        String result = AiEvaluation.run(20, () -> {
            assertSame(Thread.currentThread(), caller);
            try {
                while (true) { AiEvaluation.check(); }
            } finally { cleaned.set(true); }
        }, "expired");
        assertEquals(result, "expired");
        assertTrue(cleaned.get());
        assertTrue((System.nanoTime() - start) / 1_000_000 < 1000);
        assertEquals(AiEvaluation.run(1000, () -> "next decision", "expired"), "next decision");
    }

    @Test public void nestedDeadlineCannotSilentlyResumeAnExpiredDecision() {
        AtomicBoolean resumed = new AtomicBoolean();
        assertEquals(AiEvaluation.run(1000, () -> {
            AiEvaluation.run(0, () -> "never", "inner fallback");
            resumed.set(true);
            return "wrong";
        }, "outer fallback"), "outer fallback");
        assertFalse(resumed.get());
    }

    @Test public void sixtyFourAngelsVersusTwentyFourSnakesFinishPredictionPromptly() {
        Game game = initAndCreateGame();
        Player defender = game.getPlayers().get(0);
        Player attacker = game.getPlayers().get(1);
        defender.setLife(1, null);
        addTokens("w_4_4_angel_flying", 64, attacker).forEach(c -> c.setSickness(false));
        addTokens("g_1_1_snake", 24, defender).forEach(c -> c.setSickness(false));
        addCard("Matsu-Tribe Sniper", defender).setSickness(false);
        game.getAction().checkStaticAbilities();
        long start = System.nanoTime();
        int predicted = ComputerUtil.predictNextCombatsRemainingLife(defender, false, false, 0, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("TOKEN_COMBAT_PREDICTION_MS=" + millis);
        assertEquals(predicted, Integer.MIN_VALUE, "Ground snakes cannot stop the flying army");
        assertTrue(millis < 4000, "Prediction took " + millis + " ms");
    }

    @Test public void impossibleFlyingBlockDoesNotOverrideLureAndLegalBlockStillMustObeyIt() {
        Game game = initAndCreateGame();
        Player attacking = game.getPlayers().get(1), defending = game.getPlayers().get(0);
        Card lure = addCard("Taunting Elf", attacking);
        Card ordinary = addCard("Runeclaw Bear", attacking);
        Card flyer = addToken("w_4_4_angel_flying", attacking);
        Card blocker = addCard("Runeclaw Bear", defending);
        Combat combat = new Combat(attacking);
        combat.addAttacker(lure, defending);
        combat.addAttacker(ordinary, defending);
        combat.addAttacker(flyer, defending);
        game.getAction().checkStaticAbilities();
        assertFalse(CombatUtil.canBlock(flyer, blocker, combat));
        assertFalse(CombatUtil.canBlock(ordinary, blocker, combat));
        assertTrue(CombatUtil.canBlock(lure, blocker, combat));
    }

    @Test public void fullSnakeAttackSelectionAgainstMixedAngelBoardFinishesPromptly() {
        Game game = initAndCreateGame();
        Player snakes = game.getPlayers().get(1), angels = game.getPlayers().get(0);
        snakes.setLife(1, null);
        angels.setLife(39, null);
        addTokens("w_4_4_angel_flying", 55, angels).forEach(c -> c.setSickness(false));
        addCard("Ojer Taq, Deepest Foundation", angels).setSickness(false);
        addCard("Speaker of the Heavens", angels).setSickness(false);
        addTokens("g_1_1_snake", 15, snakes).forEach(c -> c.setSickness(false));
        for (String name : new String[]{"Ohran Viper", "Kashi-Tribe Reaver", "Hornet Cobra", "Marsh Viper",
                "Sosuke, Son of Seshiro", "River Boa", "Tangle Asp", "Matsu-Tribe Sniper"}) {
            addCard(name, snakes).setSickness(false);
        }
        game.getAction().checkStaticAbilities();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, snakes);
        Combat combat = new Combat(snakes);
        game.getPhaseHandler().setCombat(combat);
        long start = System.nanoTime();
        ((PlayerControllerAi) snakes.getController()).getAi().declareAttackers(snakes, combat);
        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("TOKEN_ATTACK_SELECTION_MS=" + millis);
        assertTrue(millis < 5000, "Full attack selection took " + millis + " ms");
        for (Card card : combat.getAttackers()) {
            assertSame(card.getController(), snakes);
        }
    }

    @Test public void flyingExceptionsAndReachStillAllowLegalBlocks() {
        Game game = initAndCreateGame();
        Player attacking = game.getPlayers().get(1), defending = game.getPlayers().get(0);
        Card dragon = addCard("Shivan Dragon", attacking);
        Card hunter = addCard("Dragon Hunter", defending);
        Card spider = addCard("Giant Spider", defending);
        Combat combat = new Combat(attacking);
        combat.addAttacker(dragon, defending);
        game.getAction().checkStaticAbilities();
        assertTrue(CombatUtil.canBlock(dragon, hunter, combat));
        assertTrue(CombatUtil.canBlock(dragon, spider, combat));
        spider.setTapped(true);
        assertFalse(CombatUtil.canBlock(dragon, spider, combat));
    }

    @Test public void actualCombatPlanningHonorsAnExpiredBudgetWithoutChangingTheBoard() {
        Game game = initAndCreateGame();
        Player attacking = game.getPlayers().get(1), defending = game.getPlayers().get(0);
        Combat combat = new Combat(attacking);
        addTokens("w_4_4_angel_flying", 128, attacking).forEach(c -> combat.addAttacker(c, defending));
        addTokens("g_1_1_snake", 32, defending);
        game.getAction().checkStaticAbilities();
        String result = AiEvaluation.run(1, () -> {
            new AiBlockController(defending, false).assignBlockersForCombat(combat);
            return "finished";
        }, "expired");
        assertEquals(result, "expired");
        assertEquals(attacking.getCreaturesInPlay().size(), 128);
        assertEquals(defending.getCreaturesInPlay().size(), 32);
        assertNull(game.getCombat(), "The prediction must not become the live combat");
    }
}
