# Commander ready cue

Incremental local update after v0.1.12; a new public release has not been published.

In the enhanced desktop UI, the Commander button above the hand turns blue when
at least one of your commanders can be cast from the command zone during your
priority window. The check uses Forge's timing, casting restrictions, targets,
and payment calculation, including colored mana, cost changes, and commander tax.
It does not tap sources or cast anything. Clicking still opens the command zone.

The cue clears when priority ends or payment starts. If Commander is hidden in
More at a narrow width, More and the Commander menu item carry the cue. The bar's
height and button dimensions do not change. Command-zone activated abilities do
not count as casting a commander. This presentation extension is for local desktop
matches; it does not introduce a new remote-client protocol message.

Validation: 59 focused tests passed in the six-module Maven desktop build, including
7 real-card commander tests and 7 hand-bar tests. Coverage includes Rosheen's hybrid
cost, repeated commander tax, tapped sources, wrong colors, sorcery timing, flash,
partner commanders, casting restrictions, and a commander returning with a new
card object. Existing prompt, floating-action-panel, and phase-input tests also pass.

The standalone component render verifies normal, ready, and overflow states. Live
matches were not restarted. The local launcher update preserves decks, preferences,
layouts, and the prior executable, with backups for both launchers and the added
English tooltip string.
