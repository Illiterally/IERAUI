package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.card.CardView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.util.Localizer;

/** Describes one priority pass; never resolves a spell or advances the game itself. */
record PriorityPrompt(String label, String explanation, CardView sourceCard, String help) {
    private PriorityPrompt(final String label, final String explanation, final String help) {
        this(label, explanation, null, help);
    }
    static PriorityPrompt forGame(final Game game, final PlayerView viewer) {
        final Localizer loc = Localizer.getInstance();
        if (!game.getStack().isEmpty()) {
            final SpellAbilityStackInstance top = game.getStack().peek();
            final CardView source = top == null || top.getView() == null ? null : top.getView().getSourceCard();
            final boolean visible = source != null && !source.isFaceDown() && viewer != null && source.canBeShownTo(viewer);
            final String name = visible ? source.getName() : loc.getMessage("lblIERATopStackItem");
            return new PriorityPrompt(loc.getMessage(game.getStack().size() > 1
                    ? "lblIERAResolveNext" : "lblIERAResolve"), loc.getMessage("lblIERAResolveShort", name),
                    visible ? source : null, loc.getMessage("lblIERAResolveHint", name));
        }

        final PhaseHandler phases = game.getPhaseHandler();
        final PhaseType current = phases.getPhase();
        if (current == null || phases.getPlayerTurn() == null) {
            return new PriorityPrompt(loc.getMessage("lblIERAContinue"), loc.getMessage("lblIERAContinue"), loc.getMessage("lblIERAContinueHint"));
        }
        PhaseType next = null;
        // Inspect the existing schedule without consuming extra phases or simulating a pass.
        for (final PhaseType candidate : PhaseType.values()) {
            if (phases.hasExtraPhaseAfter(current, candidate)) {
                next = candidate;
                break;
            }
        }
        if (next == null && current == PhaseType.CLEANUP) {
            return new PriorityPrompt(loc.getMessage("lblIERANextTurn"), loc.getMessage("lblIERANextTurn"), loc.getMessage("lblIERAContinueHint"));
        }
        if (next == null) {
            next = PhaseType.getNext(current, phases.getPlayerTurn().isPhasesReversed());
        }
        if (next == PhaseType.COMBAT_FIRST_STRIKE_DAMAGE) {
            return new PriorityPrompt(loc.getMessage("lblIERAToCombatDamage"),
                    loc.getMessage("lblIERABlocksConfirmedShort"),
                    loc.getMessage("lblIERABlocksConfirmedHint") + "\n" + loc.getMessage("lblIERACombatDamageHint"));
        }
        if (current == PhaseType.COMBAT_DECLARE_ATTACKERS && next == PhaseType.COMBAT_DECLARE_BLOCKERS) {
            return new PriorityPrompt(loc.getMessage("lblIERAPhase" + next.name()),
                    loc.getMessage("lblIERABeforeBlocksShort"),
                    loc.getMessage("lblIERABeforeBlocksHint") + "\n" + loc.getMessage("lblIERANextPhaseHint", next.nameForUi));
        }
        return new PriorityPrompt(loc.getMessage("lblIERAPhase" + next.name()),
                loc.getMessage("lblIERANextShort", loc.getMessage("lblIERAPhase" + next.name())),
                loc.getMessage("lblIERANextPhaseHint", next.nameForUi));
    }
}
