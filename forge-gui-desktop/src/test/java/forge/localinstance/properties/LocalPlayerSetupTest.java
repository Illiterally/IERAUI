package forge.localinstance.properties;

import forge.gamemodes.match.LocalLobby;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LocalPlayerSetupTest {
    private Path file;
    private ForgePreferences prefs;
    private MockedStatic<FModel> model;
    private String previousFlag;
    private IGuiBase previousGui;

    @BeforeMethod
    public void prepare() throws Exception {
        previousFlag = System.getProperty("forge.enhancedRulesUi");
        System.setProperty("forge.enhancedRulesUi", "true");
        previousGui = GuiBase.getInterface();
        IGuiBase platform = mock(IGuiBase.class);
        when(platform.getAssetsDir()).thenReturn("../forge-gui/");
        GuiBase.setInterface(platform);
        forge.util.Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        file = Files.createTempDirectory("ieraui-player-test").resolve("forge.preferences");
        Files.writeString(file, "PLAYER_NAME=Also M@\nUI_AVATARS=48,101\nUI_SLEEVES=0,0\n");
        prefs = new ForgePreferences(file.toString());
        model = mockStatic(FModel.class, withSettings().mockMaker("mock-maker-inline"));
        model.when(FModel::getPreferences).thenAnswer(call -> prefs);
    }

    @AfterMethod
    public void restore() throws Exception {
        if (model != null) { model.close(); }
        GuiBase.setInterface(previousGui);
        if (previousFlag == null) { System.clearProperty("forge.enhancedRulesUi"); }
        else { System.setProperty("forge.enhancedRulesUi", previousFlag); }
        try (var paths = Files.walk(file.getParent())) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private LocalLobby lobby() {
        LocalLobby result = new LocalLobby();
        result.setListener(mock(IUpdateable.class));
        return result;
    }

    private void restart() { prefs = new ForgePreferences(file.toString()); }

    @Test
    public void eachSeatsNameAndAvatarSurviveRestart() {
        LocalLobby first = lobby();
        first.applyToSlot(0, UpdateLobbyPlayerEvent.nameUpdate("Karn = M@"));
        first.applyToSlot(1, UpdateLobbyPlayerEvent.nameUpdate("Rosheen's Hydra"));
        first.applyToSlot(0, UpdateLobbyPlayerEvent.avatarUpdate(61));
        first.applyToSlot(1, UpdateLobbyPlayerEvent.avatarUpdate(49));
        restart();
        LocalLobby restored = lobby();
        assertEquals(restored.getSlot(0).getName(), "Karn = M@");
        assertEquals(restored.getSlot(1).getName(), "Rosheen's Hydra");
        assertEquals(restored.getSlot(0).getAvatarIndex(), 61);
        assertEquals(restored.getSlot(1).getAvatarIndex(), 49);
    }

    @Test
    public void humanAiSwitchKeepsIdentityAndDeckPreferences() {
        prefs.setPref(FPref.CONSTRUCTED_P2_DECK_STATE, "CUSTOM_DECK;Ye New Angels");
        prefs.setPref(FPref.COMMANDER_P2_DECK_STATE, "COMMANDER_DECK;Timey Wimey");
        LocalLobby first = lobby();
        first.applyToSlot(1, UpdateLobbyPlayerEvent.create(LobbySlotType.LOCAL, "Captain Alcatraz", 83, 0,
                1, false, false, Collections.emptySet(), ""));
        restart();
        LocalLobby restored = lobby();
        assertEquals(restored.getSlot(1).getType(), LobbySlotType.LOCAL);
        assertEquals(restored.getSlot(1).getName(), "Captain Alcatraz");
        assertEquals(restored.getSlot(1).getAvatarIndex(), 83);
        assertEquals(prefs.getPref(FPref.CONSTRUCTED_P2_DECK_STATE), "CUSTOM_DECK;Ye New Angels");
        assertEquals(prefs.getPref(FPref.COMMANDER_P2_DECK_STATE), "COMMANDER_DECK;Timey Wimey");
    }

    @Test
    public void firstSeatDoesNotBecomeTheOtherHumanAfterAMatch() {
        lobby();
        prefs.setPref(FPref.PLAYER_NAME, "Other human");
        prefs.save();
        restart();
        assertEquals(lobby().getSlot(0).getName(), "Also M@");
    }

    @Test
    public void unknownSecondNameIsNeutralAndNotSavedByRefresh() {
        LocalLobby first = lobby();
        assertEquals(first.getSlot(1).getName(), "Player 2");
        first.applyToSlot(1, UpdateLobbyPlayerEvent.isReadyUpdate(true));
        first.applyToSlot(1, UpdateLobbyPlayerEvent.avatarUpdate(49));
        assertEquals(prefs.getPref(FPref.LOCAL_PLAYER_2_NAME), "");
    }

    @Test
    public void additionalSeatsKeepIdentityAfterRestartAndRemoval() {
        LocalLobby first = lobby();
        first.addSlot();
        first.addSlot();
        first.applyToSlot(2, UpdateLobbyPlayerEvent.nameUpdate("Third"));
        first.applyToSlot(3, UpdateLobbyPlayerEvent.nameUpdate("Fourth"));
        first.applyToSlot(3, UpdateLobbyPlayerEvent.avatarUpdate(72));
        prefs.setPref(FPref.CONSTRUCTED_DECK_STATES[3], "CUSTOM_DECK;Fourth deck");
        prefs.setPref(FPref.COMMANDER_DECK_STATES[3], "COMMANDER_DECK;Fourth commander");
        prefs.save();
        restart();
        LocalLobby restored = lobby();
        assertEquals(restored.getNumberOfSlots(), 4);
        assertEquals(restored.getSlot(2).getName(), "Third");
        restored.removeSlot(2);
        restart();
        LocalLobby afterRemoval = lobby();
        assertEquals(afterRemoval.getNumberOfSlots(), 3);
        assertEquals(afterRemoval.getSlot(2).getName(), "Fourth");
        assertEquals(afterRemoval.getSlot(2).getAvatarIndex(), 72);
        assertEquals(prefs.getPref(FPref.CONSTRUCTED_DECK_STATES[2]), "CUSTOM_DECK;Fourth deck");
        assertEquals(prefs.getPref(FPref.COMMANDER_DECK_STATES[2]), "COMMANDER_DECK;Fourth commander");
        assertEquals(prefs.getPref(FPref.CONSTRUCTED_DECK_STATES[3]), "");
    }

    @Test
    public void shortOrInvalidAvatarSettingsDoNotCrashOrOverwriteSavedChoices() {
        prefs.setPref(FPref.UI_AVATARS, "bad");
        prefs.save();
        LocalLobby restored = lobby();
        assertEquals(restored.getSlot(0).getAvatarIndex(), 0);
        assertEquals(restored.getSlot(1).getAvatarIndex(), 1);
        assertEquals(prefs.getPref(FPref.UI_AVATARS), "bad");
    }

    @Test
    public void staleSessionDoesNotOverwriteAnotherSessionsChoices() {
        ForgePreferences second = new ForgePreferences(file.toString());
        prefs.setPref(FPref.LOCAL_PLAYER_2_NAME, "Rosheen");
        prefs.setPref(FPref.UI_AVATARS, "61,49");
        prefs.setPref(FPref.CONSTRUCTED_P1_DECK_STATE, "CUSTOM_DECK;New deck");
        prefs.save();
        second.setPref(FPref.UI_MATCHES_PER_GAME, "1");
        second.save();
        restart();
        assertEquals(prefs.getPref(FPref.LOCAL_PLAYER_2_NAME), "Rosheen");
        assertEquals(prefs.getPref(FPref.UI_AVATARS), "61,49");
        assertEquals(prefs.getPref(FPref.CONSTRUCTED_P1_DECK_STATE), "CUSTOM_DECK;New deck");
        assertEquals(prefs.getPref(FPref.UI_MATCHES_PER_GAME), "1");
    }

    @Test
    public void completePreviousPreferencesAreBackedUp() throws Exception {
        String previous = Files.readString(file);
        prefs.setPref(FPref.LOCAL_PLAYER_2_NAME, "Rosheen");
        prefs.save();
        assertEquals(Files.readString(file.resolveSibling("forge.preferences.bak")), previous);
        restart();
        assertEquals(prefs.getPref(FPref.LOCAL_PLAYER_2_NAME), "Rosheen");
    }

    @Test
    public void avatarSettingsAreAppliedWhenReturningToTheLobby() {
        LocalLobby first = lobby();
        prefs.setPref(FPref.UI_AVATARS, "61,49");
        prefs.save();
        first.refreshSavedAvatars();
        assertEquals(first.getSlot(0).getAvatarIndex(), 61);
        assertEquals(first.getSlot(1).getAvatarIndex(), 49);
        assertEquals(first.getSlot(0).getName(), "Also M@");
    }

    @Test
    public void explicitResetStillWorks() {
        prefs.setPref(FPref.LOCAL_PLAYER_2_NAME, "Rosheen");
        prefs.save();
        prefs.reset();
        prefs.save();
        restart();
        assertEquals(prefs.getPref(FPref.LOCAL_PLAYER_2_NAME), "");
        assertEquals(prefs.getPref(FPref.UI_AVATARS), "0,1");
    }
}
