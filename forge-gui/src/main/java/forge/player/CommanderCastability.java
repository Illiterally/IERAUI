package forge.player;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.cost.CostPayment;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.concurrent.atomic.AtomicBoolean;

/** A command-zone cast cue, evaluated on the game thread. GPL-3.0-or-later. */
public final class CommanderCastability {
    private CommanderCastability() { }

    public static boolean canCast(final Player player) {
        if (player.getCardsIn(ZoneType.Command).stream().noneMatch(c -> isOwnCommander(c, player))) {
            return false;
        }
        final AtomicBoolean result = new AtomicBoolean();
        // Cost prediction must not ask the human to choose X or payment options.
        // Unlike the general actionable-card scan, this never assumes success on timeout.
        player.runWithController(() -> result.set(checkCommanders(player)),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result.get();
    }

    private static boolean checkCommanders(final Player player) {
        // The original commander list can retain an old Card object after zone changes.
        for (final Card commander : player.getCardsIn(ZoneType.Command)) {
            if (!isOwnCommander(commander, player)) { continue; }
            for (final SpellAbility ability : commander.getAllPossibleAbilities(player, true)) {
                // Eminence and other command-zone activations are not casting a commander.
                if (ability.isSpell() && passesCastRestrictions(commander, ability, player)
                        && ComputerUtilAbility.isFullyTargetable(ability)
                        && (ability.getPayCosts() == null
                        || (ComputerUtilMana.canPayManaCost(ability, player, 0, false)
                        && CostPayment.canPayAdditionalCosts(ability.getPayCosts(), ability, false, player)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOwnCommander(final Card card, final Player player) {
        return card.isCommander() && card.getOwner() == player;
    }

    private static boolean passesCastRestrictions(final Card commander, final SpellAbility ability,
                                                  final Player player) {
        // canPlay checks timing/zone permission. Global cast prohibitions are checked
        // separately by Forge, with the prospective cast origin already set.
        Card prospective = ability.getAlternateHost(commander);
        if (prospective == null) { prospective = CardCopyService.getLKICopy(commander); }
        prospective.setCastFrom(commander.getZone());
        return ability.checkRestrictions(prospective, player);
    }
}
