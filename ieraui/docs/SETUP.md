# Set up IERAUI

This package is for **Forge Desktop 2.0.14 on Windows**. Start with a fresh extracted copy so your existing Forge installation stays available.

## 1. Get Forge

Open the [official Forge 2.0.14 release](https://github.com/Card-Forge/forge/releases/tag/forge-2.0.14) and download **forge-installer-2.0.14.tar.bz2**. Extract the archive completely using an archive utility that supports `.tar.bz2`.

Find the folder containing both `res` and `forge-gui-desktop-2.0.14-jar-with-dependencies.jar`. This is the folder you need in the next step. Put it somewhere you can write to, such as a folder under Documents.

## 2. Add IERAUI

Download **IERAUI-0.1.12-addon.zip** from the [IERAUI release](https://github.com/Illiterally/IERAUI/releases/tag/v0.1.12). Extract its contents into the Forge folder from step 1. Allow the included English language file to replace the one in this fresh copy.

You should now see these files together:

```text
Play IERAUI.cmd
ieraui-addon-0.1.12.jar
forge-gui-desktop-2.0.14-jar-with-dependencies.jar
forge.profile.properties
res/
IERAUI/
```

Don't extract this package over your established personal installation. The included portable profile keeps this new copy's decks, settings, and image cache in its own `data` and `cache` folders.

## 3. Check Java, then launch

Forge requires Java 17 or later. Java 21 was used for IERAUI's Windows checks. The launcher uses `runtime/bin/javaw.exe` if your copy has a runtime, then `JAVA_HOME`, then Java on your PATH. If none is available, install a suitable Java runtime from its official provider, such as [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64).

Double-click **Play IERAUI.cmd**. The window title should include **Illiterally's Enhanced Rules Analysis UI**. In a match, look for the horizontal phase strip and the labeled resource bar above your hand.

The launcher checks the expected Forge JAR before starting. A version mismatch means you need the official 2.0.14 files. Renaming a different version's JAR will not make it compatible.

## 4. Bring a deck and start experimenting

Use Forge's normal deck editor and match setup. Import or open a deck in the editor for the format you're using, save it, then select that deck in match setup.

**Constructed and Commander decks are stored separately.** Two decks can share a name while containing different cards. To edit the deck selected for a match, use the edit/pencil control beside that deck in match setup. When copying `.dck` files from another installation, put Commander decks in `data/decks/commander` and Constructed decks in `data/decks/constructed` in this new copy.

Forge handles its card resources and image downloads. IERAUI doesn't supply a separate collection of card artwork. Audio can be adjusted in Forge's preferences; this fresh addon profile starts muted.

## A few controls worth knowing

- **Resolve** passes priority normally. Another participant can still respond; it does not force the whole stack to resolve.
- The highlighted phase is where you are now. Clicking a phase in the timeline toggles a stop; it doesn't jump there.
- **Tap Mana** shows source choices. Click sources or spend mana from your pool. **Auto** is a separate shortcut.
- Drag a floating card prompt by its existing top area. Its close button hides the small prompt; the full Prompt remains available.
- Card-picture previews remain immediate. Tooltips wait 1.5 seconds.

## Updates and recovery

Finish the current match before changing files. Keep each supported Forge/addon pair together. This release has no automatic updater and has not been validated against other Forge versions.

IERAUI saves its settings and decks in this copy's `data` folder. Back that folder up before a future upgrade. If a panel layout becomes unreadable, the layout recovery code tries the previous valid backup before using Forge's default layout.

To return to your previous setup, close this copy and use your original Forge launcher. No system-wide Forge settings are changed by extracting this addon into a fresh portable folder.

## If it won't start

- Make sure the two JARs and launcher are beside `res`, rather than inside an extra nested ZIP folder.
- Read the launcher's error text. Missing Java, the wrong Forge version, or a missing addon JAR are checked before launch.
- If Windows blocks a downloaded file, inspect its source and properties before deciding whether to unblock it. No administrator access is needed by IERAUI.
- Report the IERAUI version, Forge version, Java version, and the relevant error in [IERAUI issues](https://github.com/Illiterally/IERAUI/issues). Review logs before sharing them; omit private paths or information you don't want public.
