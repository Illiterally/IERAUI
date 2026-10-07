# IERAUI 0.1.14 setup

This local update targets **Forge Desktop 2.0.15 on Windows** and requires Java 17 or later. It is a UI Addon built on Forge's rules simulation, card implementations, and deck tools. Forge's contributors supply the underlying program and content.

## Fresh installation

1. Extract the official [Forge 2.0.15 release](https://github.com/Card-Forge/forge/releases/tag/forge-2.0.15) into a new folder.
2. Extract the IERAUI 0.1.14 addon payload into that folder, alongside `forge-gui-desktop-2.0.15-jar-with-dependencies.jar`.
3. Run `Play IERAUI.cmd`. The launcher checks that the original Forge JAR matches the tested release and enables the addon.

The local candidate has not been published to GitHub. The public IERAUI v0.1.12 package targets the older Forge 2.0.14 release and is not interchangeable with this update.

## Existing installation

Close Forge before copying your existing `data` and `cache` folders into the new installation. Keep the old installation as a rollback copy. Do not replace your existing preferences with the minimal fresh-install defaults from the addon payload.

Your saved decks, commanders, achievements, and layouts live in `data`. Downloaded artwork lives in `cache`. The portable profile keeps both beside the launcher.

## Finding sets and decks

Final Fantasy is available as **FIN** (main set), **FIC** (Commander), and **FCA** (Through the Ages). Search the card catalog in the Deck Editor and choose the relevant set filter. Format and commander color filters can narrow the cards displayed.

Keep **Download Missing Images** enabled to fetch artwork as cards are viewed.

The personal deck **Timey Wimey** uses the official Doctor Who precon list, with 98 main-deck cards and The Tenth Doctor plus Rose Tyler in its Commander section. Select it from **Commander Decks** with the Commander variant enabled. The personal deck is not bundled in the addon package.

## Support and attribution

For an addon problem, report the IERAUI and Forge versions and the steps that led to it. The addon is independent of the Forge project. Original Forge notices and the GPL license are retained; see `NOTICE.md` and `LICENSE.txt`.
