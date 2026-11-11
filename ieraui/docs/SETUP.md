# IERAUI 0.1.20 setup

This local update targets **Forge Desktop 2.0.15 on Windows** and requires Java 17 or later. It is a UI Addon built on Forge's rules simulation, card implementations, and deck tools. Forge's contributors supply the underlying program and content.

## Fresh installation

1. Extract the official [Forge 2.0.15 release](https://github.com/Card-Forge/forge/releases/tag/forge-2.0.15) into a new folder.
2. Extract the IERAUI 0.1.20 addon payload into that folder, alongside `forge-gui-desktop-2.0.15-jar-with-dependencies.jar`.
3. Run `Play IERAUI.cmd`. The launcher checks that the original Forge JAR matches the tested release and enables the addon.

The local candidate has not been published to GitHub. The public IERAUI v0.1.12 package targets the older Forge 2.0.14 release and is not interchangeable with this update.

## Existing installation

Close Forge before copying your existing `data` and `cache` folders into the new installation. Keep the old installation as a rollback copy. Do not replace your existing preferences with the minimal fresh-install defaults from the addon payload.

Your saved decks, commanders, achievements, and layouts live in `data`. Downloaded artwork lives in `cache`. The portable profile keeps both beside the launcher.

## Combat results

After a lethal attack, the result shows final life and actual overkill. If first strike ended the game, the report also identifies unblocked attacking power that did not deal damage. That power is not added to the opponent's life loss or counted toward achievements.

## Choosing a large X

Choose **Other** in an X mana prompt to see **Auto-pay maximum X**. Click **Use max** to fill in that amount, or type your own number. **OK** continues to the regular mana payment; the shortcut does not cast or spend mana by itself. The ordinary small-number list is unchanged.

The total uses Forge's auto-payment choices. For sources that need manual activation, such as Signets with a mana activation cost, activate them before starting the cast so their produced mana is included. If no total is available, enter X manually.

## Player setup

Name each local seat in the lobby and press Enter or leave the name field. Choose its avatar with either mouse button. The name, avatar, and Human/AI choice are retained for that seat, including when switching which side you control. An unnamed second seat now uses `Player 2`; previous versions did not save that seat's name, so an old unsaved name cannot be recovered automatically.

Deck selections remain separate for each seat and format. If a saved deck is unavailable, select a deck explicitly; the addon will not silently replace it with the first row. Preference saves retain the previous file as `forge.preferences.bak` and replace the live file only after the new contents are complete. Explicitly resetting preferences still resets them.

## Finding sets and decks

Final Fantasy is available as **FIN** (main set), **FIC** (Commander), and **FCA** (Through the Ages). Search the card catalog in the Deck Editor and choose the relevant set filter. Format and commander color filters can narrow the cards displayed.

Keep **Download Missing Images** enabled to fetch artwork as cards are viewed.

The personal deck **Timey Wimey** uses the official Doctor Who precon list, with 98 main-deck cards and The Tenth Doctor plus Rose Tyler in its Commander section. Select it from **Commander Decks** with the Commander variant enabled. The personal deck is not bundled in the addon package.

## Support and attribution

For an addon problem, report the IERAUI and Forge versions and the steps that led to it. The addon is independent of the Forge project. Original Forge notices and the GPL license are retained; see `NOTICE.md` and `LICENSE.txt`.
