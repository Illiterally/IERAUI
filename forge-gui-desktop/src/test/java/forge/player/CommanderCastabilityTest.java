package forge.player;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CommanderCastabilityTest extends AITest {
    private Card commander(final String name, final Player player) {
        final Card card = addCardToZone(name, player, ZoneType.Command);
        player.addCommander(card);
        player.getGame().getAction().checkStateEffects(true);
        return card;
    }

    @Test public void rosheenNeedsFourUsableManaAndPredictionDoesNotSpendIt() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        commander("Rosheen Meanderer", player);
        addCards("Forest", 3, player);
        assertFalse(CommanderCastability.canCast(player));
        final Card fourth = addCard("Forest", player);
        final var controller = player.getController();
        assertTrue(CommanderCastability.canCast(player));
        assertSame(player.getController(), controller);
        assertFalse(fourth.isTapped(), "Prediction must not tap any sources");
        fourth.setTapped(true);
        assertFalse(CommanderCastability.canCast(player));
    }

    @Test public void commanderTaxIsIncludedOnEveryRecast() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card rosheen = commander("Rosheen Meanderer", player);
        addCards("Forest", 4, player);
        assertTrue(CommanderCastability.canCast(player));
        player.incCommanderCast(rosheen);
        assertFalse(CommanderCastability.canCast(player), "Four mana cannot pay a six-mana recast");
        addCards("Forest", 2, player);
        assertTrue(CommanderCastability.canCast(player));
        player.incCommanderCast(rosheen);
        assertFalse(CommanderCastability.canCast(player));
        addCards("Forest", 2, player);
        assertTrue(CommanderCastability.canCast(player));
    }

    @Test public void returnedCommanderUsesItsCurrentCardObject() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card original = commander("Rosheen Meanderer", player);
        addCards("Forest", 4, player);
        final Card permanent = game.getAction().moveToPlay(original, null, java.util.Map.of());
        assertFalse(CommanderCastability.canCast(player));
        final Card returned = game.getAction().moveTo(ZoneType.Command, permanent, null, java.util.Map.of());
        assertNotSame(returned, original);
        assertTrue(CommanderCastability.canCast(player));
        player.incCommanderCast(returned);
        assertFalse(CommanderCastability.canCast(player));
        addCards("Forest", 2, player);
        assertTrue(CommanderCastability.canCast(player));
    }

    @Test public void wrongColorsAndWrongTimingDoNotLightTheButton() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        commander("Rosheen Meanderer", player);
        addCards("Island", 4, player);
        assertFalse(CommanderCastability.canCast(player));
        addCard("Forest", player);
        assertTrue(CommanderCastability.canCast(player));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, player);
        assertFalse(CommanderCastability.canCast(player));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        assertFalse(CommanderCastability.canCast(player));
    }

    @Test public void flashStillRequiresEnoughMana() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        commander("Yeva, Nature's Herald", player);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, game.getPlayers().get(0));
        addCards("Forest", 3, player);
        assertFalse(CommanderCastability.canCast(player));
        addCard("Forest", player);
        assertTrue(CommanderCastability.canCast(player));
    }

    @Test public void eitherPartnerCanLightTheButtonButOtherZonesCannot() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        commander("Kamahl, Heart of Krosa", player);
        final Card partner = commander("Rograkh, Son of Rohgahh", player);
        assertTrue(CommanderCastability.canCast(player));
        player.getZone(ZoneType.Command).remove(partner);
        player.getZone(ZoneType.Graveyard).add(partner);
        assertFalse(CommanderCastability.canCast(player));
        player.getZone(ZoneType.Graveyard).remove(partner);
        player.getZone(ZoneType.Battlefield).add(partner);
        assertFalse(CommanderCastability.canCast(player));
    }

    @Test public void commandZoneActivationDoesNotMasqueradeAsCasting() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card derevi = commander("Derevi, Empyrial Tactician", player);
        addCards("Forest", 2, player);
        addCard("Island", player);
        addCard("Plains", player);
        assertTrue(CommanderCastability.canCast(player));
        addCard("Drannith Magistrate", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        assertTrue(derevi.getAllPossibleAbilities(player, true).stream().anyMatch(a -> !a.isSpell()),
                "Derevi still has its command-zone activation");
        assertFalse(CommanderCastability.canCast(player), "The cast restriction must still prevent the cue");
    }
}
