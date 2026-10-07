# Illiterally's Enhanced Rules Analysis UI (IERAUI)

**Local update: IERAUI 0.1.14 on Forge Desktop 2.0.15.** This branch preserves the existing UI addon and Commander cue on the September 28, 2026 Forge release. Public v0.1.12 below remains the older Forge 2.0.14 package. See [current setup](ieraui/docs/SETUP.md) and [build instructions](ieraui/docs/BUILD.md).

**A UI Addon for Forge — an Illiterally Custom.**

I made this because I was enjoying Forge and kept getting lost in the interface. I would resolve an upkeep trigger, click the same button again, and miss my main phase. An ability would be available and I would overlook it. I knew what I wanted to try, but finding the right interaction kept getting in the way.

IERAUI is my attempt to make that part easier. It puts clearer action words, timing, resources, and feedback around Forge's existing rules simulation. The aim is to help newer users get to experimenting sooner and understand what happened when they do.

[Download 0.1.12](https://github.com/Illiterally/IERAUI/releases/tag/v0.1.12) · [Setup guide](ieraui/docs/SETUP.md) · [What changes](ieraui/docs/FEATURES.md) · [Build from source](ieraui/docs/BUILD.md)

## Built on Forge

[Forge](https://github.com/Card-Forge/forge) supplies the rules engine, card implementation, simulation, AI, and deck tools. Its contributors have done an enormous amount of work. IERAUI adds a different desktop UI flow on top of that foundation.

This is an independent addon. It isn't an official Forge release or an endorsement by Forge's maintainers or Wizards of the Coast. The full Forge source and its history are retained in this fork; the [original README](ieraui/UPSTREAM-README.md) is preserved too.

## What you get

- Action labels such as **Resolve**, **Confirm blocks**, and **Combat**, with a brief input guard around main-phase transitions.
- A highlighted active-turn portrait and a horizontal phase timeline.
- A compact hand bar for zones, mana, and life, with overflow in **More**.
- **Tap Mana** for choosing sources yourself, plus a small optional **Auto** button.
- A draggable prompt beside the card you're using.
- More readable current power/toughness, selection cues, and available-action outlines.
- Brief signed changes beside resource totals, Stack arrival cues, and keyword reminders.
- Safer layout saving and recovery, plus slower tooltips that stay off the main action button.

The cues explain where you can interact. Decisions stay with you. A blue outline is not a guarantee that every ability on that card is affordable; [the feature guide](ieraui/docs/FEATURES.md) explains the current limits.

## Try it

The first public package targets **Forge Desktop 2.0.14 on Windows**, using **Java 17 or later**. Local testing used Java 21. New UI text is currently in English.

Download Forge 2.0.14 from its official release, extract it into a fresh folder, then extract the IERAUI addon ZIP into that same folder. Launch **Play IERAUI.cmd**. The [setup guide](ieraui/docs/SETUP.md) walks through the files, Java, and importing a deck.

The addon download contains the changed Forge classes, English UI text, a launcher, and documentation. Get Forge and its resources from Forge's own release. No personal decks, saved layouts, card-image cache, or Java runtime are included in this download.

## Still rough

This is an early release built through actual use, and some of the most useful ideas are still ahead: explanations for cost increases and restricted mana, reliable potential-mana/MAX-X displays, conditional combat forecasts, and larger floating feedback. Those are [plans](ieraui/docs/ROADMAP.md), not features of this download.

If something is unclear, an [issue here](https://github.com/Illiterally/IERAUI/issues) with the action you attempted and the version you used would help. Please report addon problems here first so Forge's maintainers don't inherit support for my changes.

## Source, credit, and contributions

Forge code and these changes are distributed under [GPL-3.0-or-later](LICENSE). Existing copyright notices remain in place. The release links to its exact corresponding source and [build instructions](ieraui/docs/BUILD.md). Magic artwork, names, and other third-party material have their own rights; the code license doesn't transfer those rights.

I used OpenAI Codex to help implement and check this addon. That assistance is disclosed here and will be disclosed in any contribution offered upstream, as Forge requests in its [contribution guidance](CONTRIBUTING.md).

Small fixes and clear reports are welcome. See [contributing to IERAUI](ieraui/docs/CONTRIBUTING.md). If Forge's maintainers find individual changes useful, I'd be glad to help separate them into manageable patches.
