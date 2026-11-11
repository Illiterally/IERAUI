package forge.gamemodes.match;

import forge.gui.GuiBase;
import forge.gui.EnhancedRulesUi;
import forge.gui.interfaces.IGuiGame;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.util.Collections;
import java.util.Arrays;

public final class LocalLobby extends GameLobby {

    private IGuiGame gui = null;
    public LocalLobby() {
        if (EnhancedRulesUi.isEnabled()) {
            final ForgePreferences prefs = FModel.getPreferences();
            // Migrate the existing first-seat name once. Starting a match as the
            // second human can change PLAYER_NAME, which is shared by other modes.
            if (prefs.getPref(FPref.LOCAL_PLAYER_1_NAME).isBlank() && !localName().isBlank()) {
                prefs.setPref(FPref.LOCAL_PLAYER_1_NAME, localName());
                prefs.save();
            }
            final int count = Math.max(2, Math.min(getSlotLimit(),
                    savedIndex(FModel.getPreferences().getPref(FPref.LOCAL_PLAYER_COUNT), 2)));
            for (int i = 0; i < count; i++) {
                addSlot(savedSlot(i));
            }
            return;
        }
        final String humanName = localName();
        final int[] avatarIndices = localAvatarIndices();
        final int[] sleeveIndices = localSleeveIndices();

        final LobbySlot slot0 = new LobbySlot(LobbySlotType.LOCAL, humanName, avatarIndices[0], sleeveIndices[0],0, true, true, Collections.emptySet());
        addSlot(slot0);

        final LobbySlot slot1 = new LobbySlot(LobbySlotType.AI, null, avatarIndices[1], sleeveIndices[1],1, false, true, Collections.emptySet());
        addSlot(slot1);
    }

    private static FPref namePreference(final int index) {
        return FPref.valueOf("LOCAL_PLAYER_" + (index + 1) + "_NAME");
    }

    private static int savedIndex(final String value, final int fallback) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String entry(final FPref preference, final int index, final String fallback) {
        final String[] values = FModel.getPreferences().getPref(preference).split(",", -1);
        return index < values.length && !values[index].isBlank() ? values[index] : fallback;
    }

    private static void setEntry(final FPref preference, final int index, final String value) {
        final ForgePreferences prefs = FModel.getPreferences();
        final String[] old = prefs.getPref(preference).split(",", -1);
        final String[] values = Arrays.copyOf(old, Math.max(index + 1, old.length));
        for (int i = old.length; i < values.length; i++) {
            values[i] = preference == FPref.LOCAL_PLAYER_TYPES ? "AI" : Integer.toString(i);
        }
        values[index] = value;
        prefs.setPref(preference, String.join(",", values));
    }

    private LobbySlot savedSlot(final int index) {
        final ForgePreferences prefs = FModel.getPreferences();
        String name = prefs.getPref(namePreference(index));
        if (name.isBlank()) {
            name = index == 0 && !localName().isBlank() ? localName() : "Player " + (index + 1);
        }
        final String defaultType = index == 0 ? "LOCAL" : "AI";
        final LobbySlotType type = "LOCAL".equals(entry(FPref.LOCAL_PLAYER_TYPES, index, defaultType))
                ? LobbySlotType.LOCAL : LobbySlotType.AI;
        return new LobbySlot(type, name,
                savedIndex(entry(FPref.UI_AVATARS, index, ""), index),
                savedIndex(entry(FPref.UI_SLEEVES, index, ""), index),
                index, index == 0, true, Collections.emptySet());
    }

    @Override
    public void applyToSlot(final int index, final UpdateLobbyPlayerEvent event) {
        if (EnhancedRulesUi.isEnabled() && getSlot(index) != null && event != null) {
            final ForgePreferences prefs = FModel.getPreferences();
            final LobbySlot slot = getSlot(index);
            boolean changed = false;
            // Only an identity edit saves identity. Deck/ready/view updates must never
            // promote a generated fallback name into the user's permanent profile.
            if (event.getName() != null && !event.getName().isBlank()
                    && !event.getName().equals(slot.getName())) {
                prefs.setPref(namePreference(index), event.getName().trim());
                changed = true;
            }
            if (event.getAvatarIndex() >= 0 && event.getAvatarIndex() != slot.getAvatarIndex()) {
                setEntry(FPref.UI_AVATARS, index, Integer.toString(event.getAvatarIndex()));
                changed = true;
            }
            if (event.getSleeveIndex() >= 0 && event.getSleeveIndex() != slot.getSleeveIndex()) {
                setEntry(FPref.UI_SLEEVES, index, Integer.toString(event.getSleeveIndex()));
                changed = true;
            }
            if (event.getType() != null && event.getType() != slot.getType()) {
                setEntry(FPref.LOCAL_PLAYER_TYPES, index, event.getType().name());
                changed = true;
            }
            if (changed) { prefs.save(); }
        }
        super.applyToSlot(index, event);
    }

    @Override
    public void addSlot() {
        if (!EnhancedRulesUi.isEnabled()) { super.addSlot(); return; }
        if (getNumberOfSlots() >= getSlotLimit()) { return; }
        addSlot(savedSlot(getNumberOfSlots()));
        FModel.getPreferences().setPref(FPref.LOCAL_PLAYER_COUNT, Integer.toString(getNumberOfSlots()));
        FModel.getPreferences().save();
    }

    @Override
    public void removeSlot(final int index) {
        if (!EnhancedRulesUi.isEnabled()) { super.removeSlot(index); return; }
        if (!mayRemove(index) || getSlot(index) == null) { return; }
        final ForgePreferences prefs = FModel.getPreferences();
        final FPref[][] deckStates = {FPref.CONSTRUCTED_DECK_STATES, FPref.COMMANDER_DECK_STATES,
                FPref.TINY_LEADER_DECK_STATES, FPref.OATHBREAKER_DECK_STATES, FPref.BRAWL_DECK_STATES};
        for (int i = index; i < getNumberOfSlots() - 1; i++) {
            prefs.setPref(namePreference(i), prefs.getPref(namePreference(i + 1)));
            for (final FPref[] format : deckStates) {
                prefs.setPref(format[i], prefs.getPref(format[i + 1]));
            }
            for (final FPref key : new FPref[] {FPref.UI_AVATARS, FPref.UI_SLEEVES, FPref.LOCAL_PLAYER_TYPES}) {
                setEntry(key, i, entry(key, i + 1, key == FPref.LOCAL_PLAYER_TYPES ? "AI" : Integer.toString(i + 1)));
            }
        }
        final int last = getNumberOfSlots() - 1;
        prefs.setPref(namePreference(last), "");
        for (final FPref[] format : deckStates) {
            prefs.setPref(format[last], "");
        }
        setEntry(FPref.UI_AVATARS, last, Integer.toString(last));
        setEntry(FPref.UI_SLEEVES, last, Integer.toString(last));
        setEntry(FPref.LOCAL_PLAYER_TYPES, last, "AI");
        super.removeSlot(index);
        prefs.setPref(FPref.LOCAL_PLAYER_COUNT, Integer.toString(getNumberOfSlots()));
        prefs.save();
    }

    /** Apply explicit choices made in the separate Avatars settings screen. */
    public void refreshSavedAvatars() {
        if (!EnhancedRulesUi.isEnabled()) { return; }
        boolean changed = false;
        for (int i = 0; i < getNumberOfSlots(); i++) {
            final LobbySlot slot = getSlot(i);
            final int avatar = savedIndex(entry(FPref.UI_AVATARS, i, ""), slot.getAvatarIndex());
            if (avatar != slot.getAvatarIndex()) {
                slot.setAvatarIndex(avatar);
                changed = true;
            }
        }
        if (changed) { updateView(false); }
    }

    @Override
    public boolean isAllowNetworking() {
        return false;
    }

    @Override public boolean hasControl() {
        return true;
    }

    @Override public boolean mayEdit(final int index) {
        return true;
    }

    @Override public boolean mayControl(final int index) {
        return true;
    }

    @Override public boolean mayRemove(final int index) {
        return index >= 2;
    }

    @Override protected IGuiGame getGui(final int index) {
        if (gui == null) {
            gui = GuiBase.getInterface().getNewGuiGame();
        }
        return gui;
    }

    @Override protected void onGameStarted() {
        gui = null;
        // Re-randomize random decks after starting a game
        updateView(true);
    }
}
