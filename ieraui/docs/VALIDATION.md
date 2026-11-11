# Release validation — 0.1.12

Checked on Windows on September 22, 2026, using Temurin JDK 21.0.12.1+1 and Maven 3.9.16.

## Source and build

- Prepared a clean checkout at Forge 2.0.14 (`a37a865a53280dd8ad6fad3384d69611e8c5a42f`) and applied the shipped addon source.
- Confirmed the 40 changed/new main Java files and English language resource match the locally used 0.1.12 version.
- Built all six desktop reactor modules successfully.
- Ran 66 focused checks: **zero failures, errors, or skips**. The exact selection is in `ieraui/scripts/Build-Addon.ps1`.
- Checked the staged diff and confirmed the cumulative UI patch reverses cleanly from the release source.

## Addon package

- Packaged 93 classes, including nested classes, from the 40 source files listed in `ieraui/source-files.txt`.
- Verified all 93 classes load from the addon JAR and that the unchanged game engine loads from the original Forge JAR. Reflected over their declared methods to check linkage against that distribution.
- Verified the required official Forge JAR's SHA-256: `3C0A7C385B9207E593FE149255E00D1CD7E556809F138568D07E3828B78E5DD9`.
- Launched the supplied Windows launcher in a separate folder containing the official Forge resources, original JAR, addon files, and a fresh portable profile. Confirmed the home screen and IERAUI window title.
- Started a default preconstructed match and checked the mulligan prompt, horizontal phase strip, hand/resource bar, and enlarged current power/toughness at desktop size. This was a startup smoke check, not a complete match test.
- Checked that settings were created in the isolated profile. The test did not use an existing personal profile or decks.

The launcher checksum check uses Windows PowerShell's .NET cryptography support, so it doesn't depend on PowerShell module discovery. This was corrected during the fresh-install check.

## Scope

These checks cover this Windows package and the selected UI regressions. They are not the full upstream test suite, an exhaustive card-interaction test, or validation on macOS, Linux, mobile, or another Forge version. The checksum file beside the release covers the final downloadable ZIP; its included manifest lists the actual addon classes and file hashes.

## 0.1.14 local update — 2026-10-07

- Upstream base: official Forge 2.0.15, commit `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Official release archive SHA-256: `4468f182b60f6ca62d89697baa208981085af744f116781af751c611826db600`.
- Official desktop JAR SHA-256: `85a4c31e07d4d5621a84bb03b6d75fd08e9f4ba289e575c870579b49c0033766`.
- All six desktop build modules passed; 75 focused IERAUI tests passed, zero failures/errors/skips.
- Packaged addon: 97 class files from 42 source files; runtime audit used this addon on the unmodified official base JAR.
- Runtime initialized the actual installed card database. All unique names from the normal card sections loaded and constructed successfully: FIN 299, FIC 339, FCA 64, FRA 285. Alternate printings are not counted separately.
- Timey Wimey loaded as 98 main cards plus The Tenth Doctor and Rose Tyler as commanders; all 100 cards use WHO printings. Forge's Commander deck validation passed and both commanders survived player registration.
- Validation ran headlessly with an isolated test profile. No live match or mouse/keyboard input was used.
- The official 2.0.15 artifact displays `2.0.15-SNAPSHOT-09.28` internally; its provenance and checksum match the tagged stable release.
- Merge retained Forge's new confirmation recording and revealed-card interfaces alongside IERAUI labels and Commander cues.
- This update is local; no GitHub release or website publication was performed.

## 0.1.15 local sideboarding update — 2026-10-09

- Reproduced the validation-loop condition with a 99-card main deck and 42-card sideboard while deck legality is enabled, for both Constructed and Commander in Forge.
- Oversized sideboards now require an explicit **Continue Anyway** choice or return to **Edit Sideboard**. The edit option preserves pending swaps. Keep Deck, Escape, and Close discard pending swaps and validate the original deck instead.
- Ten new regression cases cover accepting or declining the exception, unchanged decks, cancellation, pending swaps, duplicate-card counts, main-deck minimum checks, and enabled/disabled legality checks. Saved deck pools and the global preference remain untouched.
- All six desktop build modules passed; **85 focused tests passed, zero failures/errors/skips**.
- Packaged 108 class files from 44 source files. All 108 link against the unmodified official Forge 2.0.15 desktop JAR; the game engine class is loaded from that original JAR.
- Official base JAR SHA-256 remains `85a4c31e07d4d5621a84bb03b6d75fd08e9f4ba289e575c870579b49c0033766`.
- Addon JAR SHA-256: `0c3286d5ba564fe69549799093e19446cf285ef04ec94971e37ee85d9a3a1e40`.
- Checks used controller-level tests and headless linkage inspection. The live modal was not operated, and no live match was closed or restarted. The new dialog still needs an interactive check after relaunch.
- This update is local; no GitHub release or website publication was performed.

## 0.1.16 local deck-quantity update — 2026-10-10

- Right-clicking cards in an unlimited-catalog editor's main deck or sideboard offers Add copies and Remove copies with explicit quantities 1, 2, 3, and 4. Unavailable quantities remain visible but disabled. Removal of larger amounts is still available.
- Quantity actions capture the deck selection independently of the catalog selection and recheck the model, section, quantities, and limits when activated. Multiple selections apply the requested amount to each distinct selected printing; printings with the same card name share the copy limit.
- Fourteen new checks exercise real constructed-editor add/remove handlers, exact 1–4 changes, unchanged unrelated cards, dirty notifications, statistics refresh, disabled choices, singleton/basic-land rules, shared limits across sections and printings, sideboard edits, and stale menus. Finite pools, commander slots, and stock UI mode retain the original controls.
- The complete focused suite passed: **99 tests, zero failures/errors/skips**. The initial test fixture needed desktop icon stubs; this was corrected before the successful run.
- All six desktop build modules passed. Packaged 111 class files from 45 source files; all link against the unchanged official Forge 2.0.15 JAR. Addon SHA-256: `46b429302d5aadcf663542fa326ffba676ae9e2afcb5420ffdd8ffce2825efe7`.
- These checks execute the menu callbacks without operating the user's desktop. No live editor session or match was closed or restarted; interactive use after relaunch remains to be checked.
- This update is local; no GitHub release or website publication was performed.

## 0.1.17 local player-setup update — 2026-10-19

- Confirmed the original lobby recreated the second seat with a random name and AI control on startup. Only the first name was saved, and its focus-loss handler rejected punctuation. The local lobby now retains names and control types per seat; the first name is migrated independently of the shared name written by other game modes.
- Avatar choices persist for additional seats. Avatar settings no longer save a skin's display fallback over the stored choice. Returning from settings refreshes the local lobby. Right-clicking a local avatar opens the picker instead of randomizing it.
- Deck-list restoration suppresses temporary first-row selection callbacks. An unavailable saved deck leaves the selection empty without replacing the preference. Names containing semicolons are preserved in deck-selection restoration.
- Preference writes use a temporary file and replacement, keep the previous file as a backup, and merge only edits from this session into the latest saved file. Explicit reset remains available. Legacy unpatched processes do not use this merge behavior.
- Fourteen new regressions cover restart persistence, punctuation, Human/AI switching, the shared-name overwrite, additional seats and removal, avatar-setting refresh, malformed avatar indices, neutral unsaved names, stale preference writers, backups, explicit reset, intermediate deck callbacks, missing decks, and deliberate deck selection. The test fixture's missing localization initialization was fixed before the final run.
- All six build modules passed. **113 tests passed, zero failures/errors/skips.** Source-checkout deck-generation data warnings are unrelated to these focused tests.
- Packaged **134 class files from 54 source files**. All 134 link against the unchanged official Forge 2.0.15 desktop JAR. Addon SHA-256: `b02605d285993e461b0eaa0148921ed256f7c548a824cef3ecc5f863d8b8c03d`.
- Automated checks used temporary preference files and headless class linking. The live desktop was not operated or restarted; interactive verification remains for the user's next launch. No personal deck or preference file is included in the addon source patch.
- This update is local; no GitHub release or website publication was performed.

## 0.1.18 local X-mana update — 2026-10-19

- The ordinary small-number chooser is unchanged. Only **Other** for an optional X mana announcement opens the maximum-aware input. **Use max** fills the field without confirming; valid manual input, OK, and cancellation retain their separate purposes.
- Prediction runs Forge's mana-payment check in a copied game. It reserves fixed/colored costs, divides repeated X costs correctly, and applies existing taxes, reductions, mana replacement effects, and spending restrictions. The search supports values above 99 and refuses to call its computation ceiling a maximum.
- **133 focused tests passed, zero failures/errors/skips**, including 20 new checks. Cases cover Earthquake, Rosheen (available, sick, tapped, and floating restricted mana), Neverwinter Hydra, Astral Cornucopia, Gargos, Thalia, Sol Ring/Mana Reflection, creature-only mana, Treasure Vault, pooled mana without double-counting, a pending cast that successfully pays the predicted X, bounds, invalid input, manual overrides, and cancellation. Calculation leaves live X, life, controller, pool, and tap state unchanged in the covered cases.
- All six modules compiled. Windows sandbox restrictions prevented Mockito's self-attachment and atomic writes in its default temp directory; the passing run preloaded the test agent and used an isolated workspace temp directory. Test-fixture localization, null-mock behavior, and stack-zone setup were corrected before that run.
- Upstream's optional native EXE wrapper could not run its resource compiler in the sandbox. The addon uses the existing CMD launcher and official Forge JAR. Its **139 class files from 57 sources** were packaged directly from the compiled, tested module outputs. All 139 classes link against the official Forge 2.0.15 JAR; Game, ComputerUtilMana, and GameCopier still load from that unchanged JAR.
- Six further runtime scenarios passed with the packaged addon on the official engine, using an isolated profile and source card resources. Source-checkout deck-generation warnings are unrelated to these cases. Addon SHA-256: `3ebad86ebd686d260f6b0407ecc23a866363ce534e990bd051d18f8cca9da052`.
- The cumulative source patch passes a reverse-application check. Automated checks were headless; the new modal's interactive appearance remains for the next launch. No live game or desktop input was operated, and no saved deck, player preference, avatar, or profile was changed by installation.
- The displayed value is **Auto-pay maximum X**: it is bounded by Forge's source-selection logic, not an exhaustive mana-combination solver. In particular, mana sources with mana activation costs, such as Signets, must be activated manually before starting the cast. Their floating mana is then included. Unknown calculations leave manual input available.
- This update is local; no GitHub release or website publication was performed.

## 0.1.19 local bestow stat refresh — 2026-10-19

- Reproduced a stale 0/0 display with the official Forge 2.0.15 base and the previous addon: Nyxborn Hydra has six counters while bestowed; its host leaves the battlefield; the engine correctly restores a 6/7 creature, but the view retains the Aura's 0/0 values. It survives six damage while continuing to display 0/0. This matches the reported screenshot; the live match's prior actions were not replayed.
- Refresh power/toughness when bestow ends and view updates are enabled. This changes the cached display, not combat calculations, counters, card scripts, or survival rules. The addon now includes the Card class and its nested classes for this specific refresh.
- The new regression suite failed on both Nyxborn Hydra and Chromanticore before the fix. Afterward all **137 focused tests passed**, with zero failures, errors, or skips. Additional cases cover continuous creature bonuses and an ordinary, non-bestowed Hydra.
- Packaged **149 class files from 58 sources** directly from the tested outputs. All classes link against the official Forge 2.0.15 base; four packaged-runtime cases pass. Game, CardView, ComputerUtilMana, and GameCopier continue to load from the official base JAR.
- Validation uses an isolated test profile and headless execution. No desktop input or live match restart is performed. A live visual check remains for the next launch. This update is local and has not been published.


## 0.1.20 local combat performance and results update — 2026-11-03

- Read-only thread snapshots of the reported session showed repeated AI blocking predictions, including ground blockers checked against flying token armies. Three `Game AI Eval` workers were still running after their timeouts. Java 21 does not support the old `Thread.stop()` fallback.
- Spell decisions now run on their calling game thread with cooperative deadline checks inside the expensive combat loops. Timeout unwinding clears temporary last-state references and prediction caches. This removes abandoned evaluation workers; it is not a hard runtime guarantee for every possible card interaction.
- Blocking checks reject impossible pairs before scanning whole-board blocking limits and requirements. The existing blocking and lure rules are retained.
- **156 focused regression tests passed**, with zero failures, errors, or skips. These include existing names/avatars, deck-selection persistence, safe preference/layout saves, maximum X, sideboarding, quantity menus, and bestow cases, plus 19 combat/shuffle checks.
- A 64-Angel versus 24-Snake-plus-Sniper prediction took 1.170 seconds; full attack selection on a mixed 55-Angel/23-Snake board took 2.758 seconds in the final regression run. These are isolated scenarios, not a timing guarantee for all live games.
- A real first-strike damage test recreated 1 life to -10 from Healer's Hawk plus an Angelic Destiny-enhanced Serra Ascendant. The actual game-over path reported 10 overkill and 220 unblocked attacking power from 55 Angels that never dealt damage. Blocked attackers are excluded. Normal lethal damage and concession are also covered.
- A fixed-seed audit called Forge's normal player shuffle 1,500 times with three Mondrak and three Anointed Procession among 60 cards. Every card and copy was retained; 34 runs had none of those six in the first 27 cards (expected probability approximately 2.21%). This audit found no evidence warranting shuffle changes and is not a proof about every random sequence.
- Tests used an isolated profile. The Windows test agent was preloaded, and test temporary files were directed to a writable workspace folder because the sandbox's default temporary directory denied atomic file renames.
- Card scripts, damage rules, achievement thresholds, and shuffling are unchanged. No live desktop interaction or game restart is part of the update. Runtime linkage and install hashes are recorded separately when installing the package. This is a local update, not a public GitHub release.

## 0.1.21 — Forge 2.0.16, 2026-11-11

- Official base commit: `583714b8bfbd9cb04f8bc39ad95861190caf7691`. Official desktop JAR SHA-256: `16b954c64eeb8444e99b109d91ec9c57ca394a0a07041b01b83b268e92d81a75`.
- Ported prompts to upstream Selection messages; network protocol and serialized view classes remain official Forge classes.
- Includes the local improvements through 0.1.20. Upstream AI changes retained, with same-thread cooperative evaluation.
- Regression checks cover UI, persistence, mana, combat, serialization and a real TCP network game. Packaged-runtime checks use the untouched official release JAR. These local network checks do not verify Internet routing between two households.
- ZIP defaults are initialize-only: existing profiles and preferences are never replaced.
- Final focused suite: **168 tests passed; zero failures, errors or skips**.
