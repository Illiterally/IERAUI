# What IERAUI changes

IERAUI changes desktop presentation and input handling. Forge's rules engine and card scripts remain the basis of the simulation. This release is IERAUI 0.1.21 for Forge 2.0.16.

| Area | Current behavior | Limit |
| --- | --- | --- |
| Priority and phases | Contextual Resolve/next-phase words, full phase name, turn/action ownership | A label describes the current priority action, not a guaranteed result after other responses. |
| Accidental main-phase skips | A brief guard rejects stale/rapid clicks at main-phase entry and when the last stack item clears there; tracked held keys must be released | Not an undo system. Deliberate yield shortcuts keep their normal purpose. |
| Turn and phase cues | Gold turn portrait, horizontal timeline, separate phase-stop markers | Timeline clicks change stops, not game state. |
| Hand and resources | One compact row with zones, More, colored mana amounts and a pool tally | The pool is already-produced mana. It isn't a count of every source you could tap. |
| Manual payment | Tap Mana highlights source choices; Auto remains optional | Source eligibility and payment are still checked by Forge. Cost taxes/restrictions aren't fully explained yet. |
| Custom X (local 0.1.18) | After **Other**, the custom input shows **Auto-pay maximum X** and **Use max**. It includes usable floating mana and sources supported by Forge's auto-payment, with colored costs, repeated X symbols, taxes, reductions, and spending restrictions. Use max fills the field; OK continues to normal payment. | Special sources such as Signets with a mana activation cost must be activated manually first. This is the maximum for Forge's auto-payment choices, not an exhaustive search of all possible mana combinations. Small-number choices, non-mana X choices, mandatory prompts, and remote clients keep their existing flow. If calculation is unavailable, manual entry remains. |
| Local prompt | Instructions and actions appear beside a visible source; drag from the top area | Falls back to the main Prompt when there isn't space or a suitable visible source. |
| Combat interactions | Explicit attack/block confirmation; a single non-mana tap ability during a combat response requires an Activate choice | Once blocks are confirmed, creature clicks may activate abilities. They no longer assign blockers. |
| Cards and selections | Larger current P/T, outlined action opportunities, distinct selection cues, bounded keyword reminders | Available-action cues are not strategic recommendations or universal affordability proofs. Flash alone doesn't add a ready glow. |
| AI performance (local 0.1.21) | Impossible blocking pairs are rejected before scanning combat requirements. Spell decisions unwind on their calling thread when the configured time budget expires. | Cooperative checks cover the observed expensive combat loops; this is not a hard time guarantee for every card interaction. |
| Combat results (local 0.1.21) | Reports final life, actual overkill, and unblocked attacking power left when first strike ends the game. | Remaining power is not damage and does not change life totals or achievements. |
| Bestow stats (local 0.1.19) | When a bestowed Aura becomes a creature again, its displayed power/toughness refreshes, including its counters. | Corrects a stale 0/0 readout; does not alter combat calculations or undo earlier choices. |
| Small feedback | Brief signed changes beside resource totals and a pulse/count on Stack arrivals | Not board-wide floating damage animation or a forecast of pending damage. |
| Layout | Validated temporary writes, a previous-good backup, recovery from an unreadable layout | Cannot reconstruct a layout lost before a valid backup existed. |
| Hover help | 1.5-second delay, no instant reshow window, no long priority tooltip on the main action button | Detailed help can still appear over Prompt text. A fully bounded help view is future work. |
| Sideboarding | An oversized sideboard offers Continue Anyway or Edit Sideboard, with its count and Forge's limit shown. Keep Deck, Escape, and Close discard pending swaps. | Proceeding requires confirmation while legality checks are enabled. It applies to this game only; saved decks and the legality setting stay unchanged. A main deck below the required minimum still needs correction. |
| Deck quantities | Right-click a card in the main deck or sideboard: Add copies / Remove copies offer 1–4, with unavailable quantities disabled. Multiple selections apply the stated quantity to each selected card. Other amount remains available when removing more than four copies. | Available in editors with an unlimited catalog. Existing copy limits still apply, including across printings and sections. Commander slots and finite collections retain their existing controls. Use Save to persist edits. |
| Player setup | Names, avatars, and Human/AI choices persist per local seat. Either mouse button opens the avatar picker. Returning from avatar settings applies the selected picture. Deck-list restoration suppresses intermediate selection callbacks. | An old second-seat name cannot be recovered if Forge never saved it. Missing decks require a new selection. Preference writes retain a backup and merge unrelated changes from other updated sessions; the last explicit edit to the same preference wins. |

## Learning through the interface

The addon tries to answer practical questions at the point where they matter: whose turn is it, what input is being requested, which things can be clicked, and what changed? It keeps Forge's normal choices and lets you decide what to try.

For example, **Resolve** is clearer than a generic **OK**, but resolution still follows the actual priority rules. Similarly, highlighting a usable mana source is useful while you pay; it doesn't choose the source for you.

## Compatibility and testing

The release is pinned to Forge 2.0.16, commit `583714b8bfbd9cb04f8bc39ad95861190caf7691`. The supported download path is Windows desktop. Other desktop platforms can build the source, but this package has not been validated on them. Mobile UI support is not claimed.

See VALIDATION.md for this release’s checks. These checks cover prompts, mulligans, layout recovery, payment cues, combat confirmation, phase input guards, the resource bar, timeline, and keyword presentation. See the [release validation record](VALIDATION.md) for the packaging and launch checks.

The full upstream test suite and every Magic card interaction have not been tested. The [roadmap](ROADMAP.md) lists important gaps plainly.
