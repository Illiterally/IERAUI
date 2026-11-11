package forge.player;

import forge.ai.AITest;
import forge.ai.ComputerUtilMana;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MaximumManaXTest extends AITest {
    private SpellAbility spell(String name, Player p) {
        Card card = addCardToZone(name, p, ZoneType.Hand);
        SpellAbility ability = card.getFirstSpellAbility();
        ability.setActivatingPlayer(p);
        return ability;
    }

    @Test public void earthquakeReservesRedAndDoesNotSpendAnything() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 11, p);
        Card mountain = addCard("Mountain", p);
        SpellAbility quake = spell("Earthquake", p);
        var controller = p.getController();
        int life = p.getLife();
        assertEquals(MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE), Integer.valueOf(11));
        assertNull(quake.getXManaCostPaid());
        assertEquals(p.getLife(), life);
        assertEquals(p.getManaPool().totalMana(), 0);
        assertSame(p.getController(), controller);
        assertFalse(mountain.isTapped());
        assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().noneMatch(Card::isTapped));
    }

    @Test public void missingRequiredColorDoesNotProduceAMaximum() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Forest", 15, p);
        assertNull(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE));
    }

    @Test public void repeatedXSymbolsSplitTheRemainingMana() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Forest", 13, p);
        assertEquals(MaximumManaX.calculate(spell("Neverwinter Hydra", p), 0, Integer.MAX_VALUE), Integer.valueOf(5));
        assertEquals(MaximumManaX.calculate(spell("Astral Cornucopia", p), 0, Integer.MAX_VALUE), Integer.valueOf(4));
    }

    @Test public void hydraReductionIsAppliedAfterChoosingX() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 6, p);
        addCard("Gargos, Vicious Watcher", p);
        game.getAction().checkStateEffects(true);
        assertEquals(MaximumManaX.calculate(spell("Neverwinter Hydra", p), 0, Integer.MAX_VALUE), Integer.valueOf(4));
    }

    @Test public void taxesAndTappedSourcesReduceTheMaximum() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 9, p);
        addCard("Mountain", p);
        addCard("Mountain", p).setTapped(true);
        addCard("Thalia, Guardian of Thraben", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        assertEquals(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE), Integer.valueOf(8));
    }

    @Test public void rosheenCountsOnlyWhenAbleToTap() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Mountain", 3, p);
        Card rosheen = addCard("Rosheen Meanderer", p);
        rosheen.setSickness(false);
        SpellAbility quake = spell("Earthquake", p);
        assertEquals(MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE), Integer.valueOf(6));
        rosheen.setSickness(true);
        assertEquals(MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE), Integer.valueOf(2));
        rosheen.setSickness(false);
        rosheen.setTapped(true);
        assertEquals(MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE), Integer.valueOf(2));
    }

    @Test public void floatingRestrictedManaIsCountedWithoutConsumingIt() {
        Player p = initAndCreateGame().getPlayers().get(1);
        Card rosheen = addCard("Rosheen Meanderer", p);
        rosheen.setTapped(true);
        SpellAbility manaAbility = rosheen.getManaAbilities().get(0);
        manaAbility.setActivatingPlayer(p);
        for (int i = 0; i < 4; i++) {
            p.getManaPool().addMana(new Mana(MagicColor.COLORLESS, rosheen, manaAbility.getManaPart(), p));
        }
        addCard("Mountain", p);
        assertEquals(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE), Integer.valueOf(4));
        assertEquals(p.getManaPool().totalMana(), 4);
        assertTrue(rosheen.isTapped());
    }

    @Test public void manaRocksAndManaDoublersAreIncluded() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Mountain", p);
        addCards("Forest", 3, p);
        addCard("Sol Ring", p);
        addCard("Mana Reflection", p);
        game.getAction().checkStateEffects(true);
        assertEquals(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE), Integer.valueOf(11));
    }

    @Test public void largePoolsAreNotCappedAtNinetyNineAndBoundsAreRespected() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Mountain", 121, p);
        SpellAbility quake = spell("Earthquake", p);
        assertEquals(MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE), Integer.valueOf(120));
        assertEquals(MaximumManaX.calculate(quake, 0, 13), Integer.valueOf(13));
        assertNull(MaximumManaX.calculate(quake, 121, Integer.MAX_VALUE));
    }

    @Test public void nonXManaCostsNeverOfferTheShortcut() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Forest", 10, p);
        assertNull(MaximumManaX.calculate(spell("Unbound Flourishing", p), 0, Integer.MAX_VALUE));
    }

    @Test public void creatureOnlyManaCannotPayForEarthquake() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Mountain", p);
        addCard("Forest", p);
        Card sage = addCard("Somberwald Sage", p);
        sage.setSickness(false);
        assertEquals(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE), Integer.valueOf(1));
        assertEquals(MaximumManaX.calculate(spell("Mistcutter Hydra", p), 0, Integer.MAX_VALUE), Integer.valueOf(4));
        assertFalse(sage.isTapped());
    }

    @Test public void activatedDoubleXDoesNotCountTheSourceItMustTap() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Mountain", 11, p);
        Card vault = addCard("Treasure Vault", p);
        SpellAbility ability = vault.getSpellAbilities().stream().filter(SpellAbility::costHasManaX)
                .findFirst().orElseThrow();
        ability.setActivatingPlayer(p);
        assertEquals(MaximumManaX.calculate(ability, 0, Integer.MAX_VALUE), Integer.valueOf(5));
        assertFalse(vault.isTapped());
    }

    @Test public void pendingCastOnStackCanUseTheCalculatedValue() {
        Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Forest", 7, p);
        addCard("Mountain", p);
        SpellAbility quake = spell("Earthquake", p);
        p.getZone(ZoneType.Hand).remove(quake.getHostCard());
        p.getGame().getStackZone().add(quake.getHostCard());
        quake.getHostCard().setCastFrom(p.getZone(ZoneType.Hand));
        Integer maximum = MaximumManaX.calculate(quake, 0, Integer.MAX_VALUE);
        assertEquals(maximum, Integer.valueOf(7));
        assertNull(quake.getXManaCostPaid());
        quake.setXManaCostPaid(maximum);
        assertTrue(ComputerUtilMana.payManaCost(quake.getPayCosts(), p, quake, false));
        assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().allMatch(Card::isTapped));
    }

    @Test public void manaAlreadyInThePoolDoesNotCountItsTappedSourceAgain() {
        Player p = initAndCreateGame().getPlayers().get(1);
        Card mountain = addCard("Mountain", p);
        mountain.setTapped(true);
        SpellAbility mana = mountain.getManaAbilities().get(0);
        mana.setActivatingPlayer(p);
        p.getManaPool().addMana(new Mana(MagicColor.RED, mountain, mana.getManaPart(), p));
        addCards("Forest", 4, p);
        assertEquals(MaximumManaX.calculate(spell("Earthquake", p), 0, Integer.MAX_VALUE), Integer.valueOf(4));
        assertEquals(p.getManaPool().totalMana(), 1);
        assertTrue(mountain.isTapped());
    }

    @Test public void searchDoesNotPresentAnArtificialCeilingAsAMaximum() {
        assertNull(MaximumManaX.search(0, Integer.MAX_VALUE, x -> true));
        assertEquals(MaximumManaX.search(0, Integer.MAX_VALUE, x -> x <= 16384), Integer.valueOf(16384));
        assertEquals(MaximumManaX.search(0, Integer.MAX_VALUE, x -> x == 0), Integer.valueOf(0));
    }
}
