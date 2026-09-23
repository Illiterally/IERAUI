# What IERAUI changes

IERAUI 0.1.12 changes desktop presentation and input handling. Forge's rules engine and card scripts remain the basis of the simulation.

| Area | Current behavior | Limit |
| --- | --- | --- |
| Priority and phases | Contextual Resolve/next-phase words, full phase name, turn/action ownership | A label describes the current priority action, not a guaranteed result after other responses. |
| Accidental main-phase skips | A brief guard rejects stale/rapid clicks at main-phase entry and when the last stack item clears there; tracked held keys must be released | Not an undo system. Deliberate yield shortcuts keep their normal purpose. |
| Turn and phase cues | Gold turn portrait, horizontal timeline, separate phase-stop markers | Timeline clicks change stops, not game state. |
| Hand and resources | One compact row with zones, More, colored mana amounts and a pool tally | The pool is already-produced mana. It isn't a count of every source you could tap. |
| Manual payment | Tap Mana highlights source choices; Auto remains optional | Source eligibility and payment are still checked by Forge. Cost taxes/restrictions aren't fully explained yet. |
| Local prompt | Instructions and actions appear beside a visible source; drag from the top area | Falls back to the main Prompt when there isn't space or a suitable visible source. |
| Combat interactions | Explicit attack/block confirmation; a single non-mana tap ability during a combat response requires an Activate choice | Once blocks are confirmed, creature clicks may activate abilities. They no longer assign blockers. |
| Cards and selections | Larger current P/T, outlined action opportunities, distinct selection cues, bounded keyword reminders | Available-action cues are not strategic recommendations or universal affordability proofs. Flash alone doesn't add a ready glow. |
| Small feedback | Brief signed changes beside resource totals and a pulse/count on Stack arrivals | Not board-wide floating damage animation or a forecast of pending damage. |
| Layout | Validated temporary writes, a previous-good backup, recovery from an unreadable layout | Cannot reconstruct a layout lost before a valid backup existed. |
| Hover help | 1.5-second delay, no instant reshow window, no long priority tooltip on the main action button | Detailed help can still appear over Prompt text. A fully bounded help view is future work. |

## Learning through the interface

The addon tries to answer practical questions at the point where they matter: whose turn is it, what input is being requested, which things can be clicked, and what changed? It keeps Forge's normal choices and lets you decide what to try.

For example, **Resolve** is clearer than a generic **OK**, but resolution still follows the actual priority rules. Similarly, highlighting a usable mana source is useful while you pay; it doesn't choose the source for you.

## Compatibility and testing

The release is pinned to Forge 2.0.14, commit `a37a865a53280dd8ad6fad3384d69611e8c5a42f`. The supported download path is Windows desktop. Other desktop platforms can build the source, but this package has not been validated on them. Mobile UI support is not claimed.

The public release passed 66 focused checks and a clean six-module build. These checks cover prompts, mulligans, layout recovery, payment cues, combat confirmation, phase input guards, the resource bar, timeline, and keyword presentation. See the [release validation record](VALIDATION.md) for the packaging and launch checks.

The full upstream test suite and every Magic card interaction have not been tested. The [roadmap](ROADMAP.md) lists important gaps plainly.
