package forge.gamemodes.match.input;

import java.util.List;
import forge.gamemodes.match.Selection;

import forge.game.Game;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class InputLondonMulliganTest {
    private String previousFlag;
    private IGuiBase previousGui;
    private IGuiGame gui;
    private PlayerControllerHuman controller;
    private Player player;
    private PlayerView playerView;
    private Card first;
    private Card second;
    private Card third;

    @BeforeMethod
    public void prepare() {
        previousFlag = System.getProperty("forge.enhancedRulesUi");
        System.setProperty("forge.enhancedRulesUi", "true");
        previousGui = GuiBase.getInterface();
        final IGuiBase platform = mock(IGuiBase.class);
        when(platform.getAssetsDir()).thenReturn("../forge-gui/");
        GuiBase.setInterface(platform);
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");

        gui = mock(IGuiGame.class);
        controller = mock(PlayerControllerHuman.class);
        player = mock(Player.class);
        playerView = mock(PlayerView.class);
        final Game game = mock(Game.class);
        when(game.getView()).thenReturn(mock(GameView.class));
        when(player.getGame()).thenReturn(game);
        when(player.getView()).thenReturn(playerView);
        when(controller.getGui()).thenReturn(gui);
        when(controller.getPlayer()).thenReturn(player);
        when(controller.getGame()).thenReturn(game);
        when(controller.getInputQueue()).thenReturn(mock(InputQueue.class));

        first = card();
        second = card();
        third = card();
        final CardCollection hand = new CardCollection(List.of(first, second, third));
        final PlayerZone zone = mock(PlayerZone.class);
        when(zone.getCards()).thenReturn(hand);
        when(zone.contains(any(Card.class))).thenAnswer(call -> hand.contains(call.getArgument(0)));
        when(player.getZone(ZoneType.Hand)).thenReturn(zone);
    }

    private Card card() {
        final Card result = mock(Card.class);
        when(result.getView()).thenReturn(mock(CardView.class));
        return result;
    }

    @AfterMethod
    public void restore() {
        GuiBase.setInterface(previousGui);
        if (previousFlag == null) System.clearProperty("forge.enhancedRulesUi");
        else System.setProperty("forge.enhancedRulesUi", previousFlag);
    }

    @Test
    public void manualChoiceExposesHandAndCanBeUndoneBeforeConfirming() {
        final InputLondonMulligan input = new InputLondonMulligan(controller, player, 1);
        input.showMessage();
        verify(gui).setSelection(argThat(s -> s.cards().equals(List.of(first.getView(), second.getView(), third.getView())) && s.min() == 1 && s.max() == 1));
        verify(gui).updateButtons(playerView, "Return selected", "Auto-pick & return", false, true, true);
        Assert.assertFalse(input.selectCard(card(), null, null));
        Assert.assertTrue(input.selectCard(second, null, null));
        Assert.assertEquals(input.getSelectedCards().getFirst(), second);
        verify(gui).updateButtons(playerView, "Return selected", "Auto-pick & return", true, false, true);
        verify(gui).showPromptMessage(eq(playerView), contains("Selected: 1 / 1"), isNull());
        verify(gui, never()).setSelection(Selection.NONE);
        input.selectCard(second, null, null);
        Assert.assertTrue(input.getSelectedCards().isEmpty());
        input.selectCard(third, null, null);
        input.selectButtonOK();
        Assert.assertEquals(input.getSelectedCards().size(), 1);
        Assert.assertEquals(input.getSelectedCards().getFirst(), third);
        verify(gui, atLeastOnce()).setSelection(Selection.NONE);
        verify(gui, atLeastOnce()).setSelection(argThat(s -> s.chosen().contains(third.getView())));
    }

    @Test
    public void autoKeepsManualChoiceAndFillsFromHandOrderThenClearsCues() {
        final InputLondonMulligan input = new InputLondonMulligan(controller, player, 2);
        input.showMessage();
        input.selectCard(third, null, null);
        input.selectButtonCancel();
        Assert.assertEquals(input.getSelectedCards().size(), 2);
        Assert.assertEquals(input.getSelectedCards().get(0), third);
        Assert.assertEquals(input.getSelectedCards().get(1), first);
        Assert.assertFalse(input.getSelectedCards().contains(second));
        verify(gui, atLeastOnce()).setSelection(Selection.NONE);
    }

    @Test
    public void interruptedSelectionAlsoClearsHighlightsAndCues() {
        final InputLondonMulligan input = new InputLondonMulligan(controller, player, 2);
        input.showMessage();
        input.selectCard(second, null, null);
        input.stop();
        verify(gui, atLeastOnce()).setSelection(Selection.NONE);
        verify(gui, atLeastOnce()).setSelection(argThat(s -> s.chosen().contains(second.getView())));
    }

    @Test
    public void originalUiKeepsItsExistingButtonsAndSelectionBehavior() {
        System.clearProperty("forge.enhancedRulesUi");
        final InputLondonMulligan input = new InputLondonMulligan(controller, player, 1);
        input.showMessage();
        verify(gui).updateButtons(playerView, "OK", "Auto", false, true, true);
        verify(gui, never()).setSelection(argThat(s -> !s.cards().isEmpty()));
        input.selectCard(first, null, null);
        input.selectButtonOK();
        Assert.assertEquals(input.getSelectedCards().getFirst(), first);
        verify(gui, never()).setSelection(Selection.NONE);
    }
}
