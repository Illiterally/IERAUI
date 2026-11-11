package forge.deckchooser;

import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.gui.UiCommand;
import forge.itemmanager.DeckManager;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DeckSelectionPersistenceTest {
    private FDeckChooser chooser;
    private ForgePreferences prefs;
    private DeckManager decks;
    private DeckProxy temporary;
    private String previousFlag;

    @BeforeMethod
    public void prepare() throws Exception {
        previousFlag = System.getProperty("forge.enhancedRulesUi");
        System.setProperty("forge.enhancedRulesUi", "true");
        forge.util.Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        chooser = mock(FDeckChooser.class, CALLS_REAL_METHODS);
        prefs = mock(ForgePreferences.class);
        try (MockedStatic<FSkin> skin = mockStatic(FSkin.class)) {
            FSkin.SkinIcon icon = mock(FSkin.SkinIcon.class);
            when(icon.resize(anyInt(), anyInt())).thenReturn(icon);
            skin.when(() -> FSkin.getIcon(any(FSkinProp.class))).thenReturn(icon);
            decks = mock(DeckManager.class);
        }
        for (String field : List.of("prefs", "lstDecks", "SELECTED_DECK_DELIMITER")) {
            Field member = FDeckChooser.class.getDeclaredField(field);
            member.setAccessible(true);
            member.set(chooser, field.equals("prefs") ? prefs : field.equals("lstDecks") ? decks : "::");
        }
        chooser.initialize(FPref.CONSTRUCTED_P1_DECK_STATE, DeckType.CUSTOM_DECK);
        when(prefs.getPref(FPref.CONSTRUCTED_P1_DECK_STATE)).thenReturn("CUSTOM_DECK;Chosen deck");
        temporary = mock(DeckProxy.class);
        when(temporary.toString()).thenReturn("First row");
        when(decks.getSelectedItems()).thenReturn(List.of(temporary));
    }

    @AfterMethod
    public void restore() {
        if (previousFlag == null) { System.clearProperty("forge.enhancedRulesUi"); }
        else { System.setProperty("forge.enhancedRulesUi", previousFlag); }
    }

    @Test
    public void restoringSelectionCannotSaveTemporaryRow() {
        when(decks.setSelectedStrings(any())).thenAnswer(call -> {
            chooser.saveState(); // selection callback during restoration
            return true;
        });
        UiCommand updated = mock(UiCommand.class);
        chooser.setDeckSelectionCommand(updated);
        chooser.restoreSavedState();
        verify(prefs, never()).setPref(any(FPref.class), anyString());
        verify(decks).setSelectedStrings(List.of("Chosen deck"));
        verify(updated, times(1)).run();
    }

    @Test
    public void missingDeckClearsSelectionWithoutSavingASubstitute() {
        when(decks.setSelectedStrings(any())).thenReturn(false);
        chooser.restoreSavedState();
        verify(decks).setSelectedIndices(new Integer[0]);
        when(decks.getSelectedItems()).thenReturn(List.of());
        chooser.saveState();
        verify(prefs, never()).save();
    }

    @Test
    public void deliberateDeckSelectionIsSaved() {
        chooser.saveState();
        verify(prefs).setPref(FPref.CONSTRUCTED_P1_DECK_STATE, "CUSTOM_DECK;First row");
        verify(prefs).save();
    }

    @Test
    public void semicolonInDeckNameSurvivesRestoration() {
        when(prefs.getPref(FPref.CONSTRUCTED_P1_DECK_STATE)).thenReturn("CUSTOM_DECK;Karn; revised");
        when(decks.setSelectedStrings(any())).thenReturn(true);
        chooser.restoreSavedState();
        verify(decks).setSelectedStrings(List.of("Karn; revised"));
    }
}
