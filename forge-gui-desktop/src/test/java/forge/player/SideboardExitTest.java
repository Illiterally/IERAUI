package forge.player;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.Player;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SideboardExitTest {
    private IGuiBase previousGui;
    private MockedStatic<FModel> model;
    private IGuiGame gui;
    private PlayerControllerHuman controller;
    private Deck deck;
    private CardPool main;
    private CardPool sideboard;
    private PaperCard first;
    private PaperCard second;
    private ForgePreferences preferences;

    @BeforeMethod
    public void prepare() {
        previousGui = GuiBase.getInterface();
        IGuiBase platform = mock(IGuiBase.class);
        when(platform.getAssetsDir()).thenReturn("../forge-gui/");
        GuiBase.setInterface(platform);
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        model = mockStatic(FModel.class, withSettings().mockMaker("mock-maker-inline"));
        preferences = mock(ForgePreferences.class);
        when(preferences.getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)).thenReturn(true);
        model.when(FModel::getPreferences).thenReturn(preferences);
        Game game = mock(Game.class);
        when(game.getView()).thenReturn(mock(GameView.class));
        controller = new PlayerControllerHuman(game, mock(Player.class), mock(LobbyPlayer.class));
        gui = mock(IGuiGame.class);
        when(gui.showConfirmDialog(anyString(), anyString(), anyString(), anyString(), eq(false))).thenReturn(true);
        controller.setGui(gui);
        first = mock(PaperCard.class);
        second = mock(PaperCard.class);
        main = new CardPool();
        sideboard = new CardPool();
        deck = mock(Deck.class);
        when(deck.get(DeckSection.Main)).thenReturn(main);
        when(deck.get(DeckSection.Sideboard)).thenReturn(sideboard);
    }

    @AfterMethod
    public void restore() {
        if (model != null) { model.close(); }
        GuiBase.setInterface(previousGui);
    }

    @DataProvider
    public Object[][] formats() {
        return new Object[][] {{GameType.Constructed}, {GameType.Commander}};
    }

    @Test(dataProvider = "formats")
    public void keepDeckExitsDespiteOversizedExistingSideboard(GameType format) {
        main.add(first, 99);
        sideboard.add(second, 42);
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(null)
                .thenThrow(new AssertionError("Keep Deck reopened sideboarding"));
        assertNull(controller.sideboard(deck, format, "Player"));
        verify(gui, times(1)).sideboard(sideboard, main, "Player");
        verify(gui, never()).showErrorDialog(anyString(), anyString());
        assertEquals(main.countAll(), 99);
        assertEquals(sideboard.countAll(), 42);
        verifyOversizedPrompt(format);
    }

    @Test(dataProvider = "formats")
    public void unchangedOkDoesNotReopenTheSameDialog(GameType format) {
        main.add(first, 98);
        main.add(second, 1);
        sideboard.add(second, 42);
        List<PaperCard> unchanged = new ArrayList<>(main.toFlatList());
        Collections.reverse(unchanged); // The dialog sorts cards before returning them.
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(unchanged)
                .thenThrow(new AssertionError("Unchanged deck reopened sideboarding"));
        assertNull(controller.sideboard(deck, format, "Player"));
        verify(gui, times(1)).sideboard(sideboard, main, "Player");
        verify(gui, never()).showErrorDialog(anyString(), anyString());
        verifyOversizedPrompt(format);
    }

    @Test
    public void invalidEditCanStillBeCancelledWithoutChangingTheDeck() {
        main.add(first, 60);
        sideboard.add(second, 15);
        when(gui.sideboard(any(CardPool.class), any(CardPool.class), eq("Player")))
                .thenReturn(new ArrayList<>(Collections.nCopies(59, first)))
                .thenReturn(null);
        assertNull(controller.sideboard(deck, GameType.Constructed, "Player"));
        verify(gui, times(2)).sideboard(any(CardPool.class), any(CardPool.class), eq("Player"));
        verify(gui, times(1)).showErrorDialog(anyString(), anyString());
        verify(gui, never()).showConfirmDialog(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        assertEquals(main.count(first), 60);
        assertEquals(sideboard.count(second), 15);
    }

    @Test
    public void validSwapWithChangedDuplicateCountsIsAccepted() {
        main.add(first, 59);
        main.add(second, 1);
        sideboard.add(second, 15);
        List<PaperCard> changed = new ArrayList<>(Collections.nCopies(58, first));
        changed.add(second);
        changed.add(second);
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(changed);
        assertSame(controller.sideboard(deck, GameType.Constructed, "Player"), changed);
        verify(gui, never()).showErrorDialog(anyString(), anyString());
    }

    @Test
    public void editedOversizedSideboardCanProceedAfterConfirmation() {
        main.add(first, 99);
        sideboard.add(second, 42);
        List<PaperCard> changed = new ArrayList<>(Collections.nCopies(98, first));
        changed.add(second);
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(changed);
        assertSame(controller.sideboard(deck, GameType.Constructed, "Player"), changed);
        verifyOversizedPrompt(GameType.Constructed);
        verify(gui, times(1)).sideboard(sideboard, main, "Player");
        assertEquals(main.count(first), 99);
        assertEquals(sideboard.count(second), 42);
        verify(preferences, never()).setPref(any(FPref.class), anyBoolean());
    }

    @Test
    public void decliningConfirmationKeepsPendingSwapsForEditing() {
        main.add(first, 99);
        sideboard.add(second, 42);
        List<PaperCard> changed = new ArrayList<>(Collections.nCopies(98, first));
        changed.add(second);
        when(gui.showConfirmDialog(anyString(), anyString(), anyString(), anyString(), eq(false)))
                .thenReturn(false).thenReturn(true);
        when(gui.sideboard(any(CardPool.class), any(CardPool.class), eq("Player")))
                .thenReturn(changed).thenAnswer(invocation -> {
                    CardPool pendingSideboard = invocation.getArgument(0);
                    CardPool pendingMain = invocation.getArgument(1);
                    assertEquals(pendingMain.count(first), 98);
                    assertEquals(pendingMain.count(second), 1);
                    assertEquals(pendingSideboard.count(first), 1);
                    assertEquals(pendingSideboard.count(second), 41);
                    return null; // Keep Deck discards the pending swaps.
                });
        assertNull(controller.sideboard(deck, GameType.Constructed, "Player"));
        verify(gui, times(2)).showConfirmDialog(contains("42"), anyString(),
                eq("Continue Anyway"), eq("Edit Sideboard"), eq(false));
        assertEquals(main.count(first), 99);
        assertEquals(sideboard.count(second), 42);
    }

    @Test
    public void disabledLegalityDoesNotAskForAnException() {
        when(preferences.getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)).thenReturn(false);
        main.add(first, 99);
        sideboard.add(second, 42);
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(null);
        assertNull(controller.sideboard(deck, GameType.Constructed, "Player"));
        verify(gui, never()).showConfirmDialog(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void legalDeckCanBeKeptWithoutConfirmation() {
        main.add(first, 60);
        sideboard.add(second, 15);
        when(gui.sideboard(sideboard, main, "Player")).thenReturn(null);
        assertNull(controller.sideboard(deck, GameType.Constructed, "Player"));
        verify(gui, never()).showConfirmDialog(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    private void verifyOversizedPrompt(GameType format) {
        verify(gui).showConfirmDialog(contains("42 cards. Forge allows up to "
                + format.getDeckFormat().getSideRange().getMaximum()),
                eq("Proceed with oversized sideboard?"), eq("Continue Anyway"), eq("Edit Sideboard"), eq(false));
    }
}
