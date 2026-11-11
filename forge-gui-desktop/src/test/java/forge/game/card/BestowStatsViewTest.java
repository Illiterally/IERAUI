package forge.game.card;

import com.google.common.collect.HashMultiset;
import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BestowStatsViewTest extends AITest {
    private void counters(Card card, int count) {
        var counters = HashMultiset.<CounterType>create();
        counters.add(CounterEnumType.P1P1, count);
        card.setCounters(counters);
    }

    private Card bestow(String name, int count, Card target) {
        Card aura = addCard(name, target.getController());
        aura.animateBestow();
        aura.attachToEntity(target, null, true);
        counters(aura, count);
        target.getGame().getAction().checkStaticAbilities();
        assertTrue(aura.isBestowed());
        assertFalse(aura.isCreature());
        return aura;
    }

    private void assertStats(Card card, int power, int toughness) {
        assertEquals(card.getNetPower(), power, "Engine power");
        assertEquals(card.getNetToughness(), toughness, "Engine toughness");
        assertEquals(card.getView().getCurrentState().getPower(), power, "Displayed power");
        assertEquals(card.getView().getCurrentState().getToughness(), toughness, "Displayed toughness");
    }

    @Test public void detachedNyxbornShowsSixSevenAndSurvivesSixDamage() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);
        Card target = addCard("Elvish Mystic", player);
        Card hydra = bestow("Nyxborn Hydra", 6, target);

        game.getAction().moveToGraveyard(target, null);
        game.getAction().checkStateEffects(true);
        assertTrue(hydra.isCreature());
        assertFalse(hydra.isBestowed());
        assertStats(hydra, 6, 7);

        hydra.setDamage(6);
        game.getAction().checkStateEffects(true);
        assertTrue(hydra.isInZone(ZoneType.Battlefield));
        assertEquals(hydra.getView().getDamage(), 6);
        assertStats(hydra, 6, 7);
    }

    @Test public void detachedChromanticoreRefreshesItsStatsIncludingCounters() {
        Game game = initAndCreateGame();
        Card target = addCard("Elvish Mystic", game.getPlayers().get(1));
        Card chromanticore = bestow("Chromanticore", 2, target);
        game.getAction().moveToGraveyard(target, null);
        game.getAction().checkStateEffects(true);
        assertStats(chromanticore, 6, 6);
    }

    @Test public void detachedHydraAlsoShowsContinuousCreatureBonuses() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);
        Card target = addCard("Elvish Mystic", player);
        addCard("Gaea's Anthem", player);
        Card hydra = bestow("Nyxborn Hydra", 6, target);
        game.getAction().moveToGraveyard(target, null);
        game.getAction().checkStateEffects(true);
        assertStats(hydra, 7, 8);
    }

    @Test public void ordinaryNyxbornKeepsItsCountersAndStats() {
        Game game = initAndCreateGame();
        Card hydra = addCard("Nyxborn Hydra", game.getPlayers().get(1));
        counters(hydra, 6);
        game.getAction().checkStateEffects(true);
        hydra.unanimateBestow();
        assertStats(hydra, 6, 7);
    }
}
