package forge.screens.match.views;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CardActionPanelTest {
    private static CardActionPanel.Prompt payment(final long revision, final boolean enabled) {
        return new CardActionPanel.Prompt(revision, "Savageborn Hydra",
                "Put a +1/+1 counter on Savageborn Hydra. Activate only as a sorcery.\n\nPay Mana Cost: {1}{R/G}",
                "Tap Mana", "Cancel", enabled, true);
    }

    private static CardActionPanel create(final List<CardActionPanel.Action> actions, final Runnable dismiss) {
        return new CardActionPanel(new Font(Font.SANS_SERIF, Font.PLAIN, 14), "Hide card prompt", "Drag to move", a -> {
            actions.add(a);
            return true;
        }, dismiss);
    }

    private static List<Component> descendants(final Container parent) {
        final List<Component> all = new ArrayList<>();
        for (final Component c : parent.getComponents()) {
            all.add(c);
            if (c instanceof Container container) { all.addAll(descendants(container)); }
        }
        return all;
    }

    private static JButton button(final CardActionPanel panel, final String label) {
        return descendants(panel).stream().filter(c -> c instanceof JButton b && label.equals(b.getText()))
                .map(c -> (JButton)c).findFirst().orElseThrow();
    }

    private static String plainText(final JEditorPane editor) {
        try { return editor.getDocument().getText(0, editor.getDocument().getLength()).replaceAll("\\s+", " "); }
        catch (final javax.swing.text.BadLocationException e) { throw new AssertionError(e); }
    }

    @Test
    public void paymentMirrorsAvailabilityAndDispatchesExactlyOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final CardActionPanel panel = create(actions, () -> { });
            panel.showPrompt(payment(1, false));
            final JButton auto = button(panel, "Tap Mana");
            Assert.assertFalse(auto.isEnabled());
            Assert.assertTrue(button(panel, "Cancel").isEnabled());
            auto.doClick(0);
            Assert.assertTrue(actions.isEmpty());
            panel.showPrompt(payment(2, true));
            auto.doClick(0);
            auto.doClick(0);
            Assert.assertEquals(actions.size(), 1);
            Assert.assertTrue(actions.get(0).primary());
            Assert.assertEquals(actions.get(0).revision(), 2L);
            Assert.assertFalse(button(panel, "Cancel").isEnabled());
            Assert.assertFalse(auto.isFocusable());
        });
    }

    @Test
    public void pressStartedOnPaymentCannotResolveTheNextPrompt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final CardActionPanel panel = create(actions, () -> { });
            panel.showPrompt(payment(1, true));
            final JButton auto = button(panel, "Tap Mana");
            auto.getModel().setArmed(true);
            auto.getModel().setPressed(true);
            panel.showPrompt(new CardActionPanel.Prompt(2, "Savageborn Hydra", "Next to resolve: Savageborn Hydra",
                    "Resolve", "End Turn", true, true));
            auto.getModel().setPressed(false);
            Assert.assertTrue(actions.isEmpty());
            button(panel, "Resolve").doClick(0);
            Assert.assertEquals(actions.size(), 1);
            Assert.assertEquals(actions.get(0).revision(), 2L);
        });
    }

    @Test
    public void secondClickOfDoubleClickCannotTakeTheNewAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final CardActionPanel panel = create(actions, () -> { });
            panel.showPrompt(payment(1, true));
            final JButton auto = button(panel, "Tap Mana");
            auto.setSize(250, 32);
            auto.dispatchEvent(new MouseEvent(auto, MouseEvent.MOUSE_PRESSED, 1000, 0, 12, 12, 1, false, MouseEvent.BUTTON1));
            auto.dispatchEvent(new MouseEvent(auto, MouseEvent.MOUSE_RELEASED, 1010, 0, 12, 12, 1, false, MouseEvent.BUTTON1));
            Assert.assertEquals(actions.size(), 1);
            panel.showPrompt(new CardActionPanel.Prompt(2, "Savageborn Hydra", "Resolve next", "Resolve", "End Turn", true, true));
            auto.dispatchEvent(new MouseEvent(auto, MouseEvent.MOUSE_PRESSED, 1100, 0, 12, 12, 2, false, MouseEvent.BUTTON1));
            auto.dispatchEvent(new MouseEvent(auto, MouseEvent.MOUSE_RELEASED, 1110, 0, 12, 12, 2, false, MouseEvent.BUTTON1));
            Assert.assertEquals(actions.size(), 1);
        });
    }

    @Test
    public void closeOnlyDismissesButCancelDispatchesTheSecondaryAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final AtomicInteger dismisses = new AtomicInteger();
            final CardActionPanel panel = create(actions, dismisses::incrementAndGet);
            panel.showPrompt(payment(1, true));
            button(panel, "\u00d7").doClick(0);
            Assert.assertEquals(dismisses.get(), 1);
            Assert.assertTrue(actions.isEmpty());
            button(panel, "Cancel").doClick(0);
            Assert.assertEquals(actions.size(), 1);
            Assert.assertFalse(actions.get(0).primary());
        });
    }

    @Test
    public void rejectedGuardedActionRemainsUsableAndExternalSubmissionBlocksIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final AtomicInteger attempts = new AtomicInteger();
            final CardActionPanel panel = new CardActionPanel(new Font(Font.SANS_SERIF, 0, 14), "Hide", "Drag to move", a -> {
                attempts.incrementAndGet(); return false;
            }, () -> { });
            panel.showPrompt(payment(1, true));
            button(panel, "Tap Mana").doClick(0);
            Assert.assertTrue(button(panel, "Tap Mana").isEnabled());
            panel.setPending();
            button(panel, "Tap Mana").doClick(0);
            Assert.assertEquals(attempts.get(), 1);
        });
    }

    @Test
    public void mirrorsUpdatedCostAndKeepsLongMessagesBounded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final CardActionPanel panel = create(new ArrayList<>(), () -> { });
            panel.showPrompt(new CardActionPanel.Prompt(1, "Hydra", "Long instruction<br>".repeat(80),
                    "Tap Mana", "Cancel", true, true));
            Assert.assertTrue(panel.getPreferredSize().height <= 330);
            final int longHeight = panel.getPreferredSize().height;
            panel.showPrompt(payment(2, true));
            Assert.assertTrue(panel.getPreferredSize().height < longHeight);
            final JEditorPane editor = descendants(panel).stream().filter(c -> c instanceof JEditorPane)
                    .map(c -> (JEditorPane)c).findFirst().orElseThrow();
            Assert.assertTrue(plainText(editor).contains("Pay Mana Cost: {1}{R/G}"));
            panel.showPrompt(new CardActionPanel.Prompt(3, "Hydra", "Pay Mana Cost: {R/G}", "Tap Mana", "Cancel", false, true));
            Assert.assertTrue(plainText(editor).contains("Pay Mana Cost: {R/G}"));
            Assert.assertFalse(plainText(editor).contains("{1}"));
        });
    }

    @Test
    public void placementPrefersRightFlipsLeftAndClampsVertically() {
        final Rectangle board = new Rectangle(200, 25, 1000, 320);
        final Dimension size = new Dimension(300, 250);
        final Rectangle card = new Rectangle(220, 40, 110, 150);
        Assert.assertEquals(CardActionPanel.place(card, board, size), new Rectangle(340, 40, 300, 250));
        final Rectangle edge = new Rectangle(1050, 210, 110, 120);
        final Rectangle result = CardActionPanel.place(edge, board, size);
        Assert.assertEquals(result, new Rectangle(740, 95, 300, 250));
        Assert.assertTrue(board.contains(result));
        Assert.assertFalse(result.intersects(edge));
    }

    @Test
    public void optionalAutoIsSeparateAndCannotLeakIntoTheNextPrompt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final CardActionPanel panel = create(actions, () -> { });
            panel.showPrompt(new CardActionPanel.Prompt(1, "Hydra", "Choose blue mana sources",
                    "Tap Mana", "Cancel", true, true, "Auto", false));
            final JButton auto = button(panel, "Auto");
            Assert.assertTrue(auto.isVisible());
            Assert.assertFalse(auto.isEnabled());
            button(panel, "Tap Mana").doClick(0);
            Assert.assertTrue(actions.get(0).primary());
            Assert.assertFalse(actions.get(0).autoMana());
            panel.showPrompt(new CardActionPanel.Prompt(2, "Hydra", "Pay Mana Cost: {G}",
                    "Tap Mana", "Cancel", true, true, "Auto", true));
            auto.doClick(0);
            auto.doClick(0);
            Assert.assertEquals(actions.size(), 2);
            Assert.assertTrue(actions.get(1).autoMana());
            Assert.assertFalse(actions.get(1).primary());
            panel.showPrompt(new CardActionPanel.Prompt(3, "Hydra", "Resolve", "Resolve", "End Turn", true, true));
            Assert.assertFalse(auto.isVisible());
            Assert.assertFalse(auto.isEnabled());
            auto.doClick(0);
            Assert.assertEquals(actions.size(), 2);
        });
    }

    @Test
    public void titleDragMovesWithoutDispatchAndSurvivesPaymentRefresh() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<CardActionPanel.Action> actions = new ArrayList<>();
            final CardActionPanel panel = create(actions, () -> { });
            final JPanel host = new JPanel(null);
            host.setSize(1200, 800);
            host.add(panel);
            panel.showPrompt(payment(1, true));
            panel.setBounds(300, 200, 300, 250);
            panel.setVisible(true);
            panel.doLayout();
            final JLabel title = descendants(panel).stream().filter(c -> c instanceof JLabel)
                    .map(c -> (JLabel)c).findFirst().orElseThrow();
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_PRESSED, 1000, 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_DRAGGED, 1020, MouseEvent.BUTTON1_DOWN_MASK,
                    410, -90, 0, false, MouseEvent.NOBUTTON));
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_RELEASED, 1040, 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            Assert.assertEquals(panel.getBounds(), new Rectangle(700, 100, 300, 250));
            Assert.assertTrue(actions.isEmpty());
            panel.showPrompt(payment(2, false));
            Assert.assertEquals(panel.manualBounds(new Rectangle(8, 8, 1184, 784), new Dimension(300, 290)),
                    new Rectangle(700, 100, 300, 290));
            Assert.assertTrue(panel.isManuallyPlaced());
            Assert.assertFalse(button(panel, "Tap Mana").isEnabled());
            panel.resetPlacement();
            Assert.assertFalse(panel.isManuallyPlaced());
        });
    }

    @Test
    public void draggedPromptStaysReachableAfterResizingAndResetEndsTheDrag() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final CardActionPanel panel = create(new ArrayList<>(), () -> { });
            final JPanel host = new JPanel(null);
            host.setSize(1000, 700);
            host.add(panel);
            panel.showPrompt(payment(1, true));
            panel.setBounds(100, 100, 300, 250);
            panel.setVisible(true);
            panel.doLayout();
            final JLabel title = descendants(panel).stream().filter(c -> c instanceof JLabel)
                    .map(c -> (JLabel)c).findFirst().orElseThrow();
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_PRESSED, 1000, 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_DRAGGED, 1020, MouseEvent.BUTTON1_DOWN_MASK,
                    2010, 2010, 0, false, MouseEvent.NOBUTTON));
            Assert.assertEquals(panel.getBounds(), new Rectangle(692, 442, 300, 250));
            Assert.assertEquals(panel.manualBounds(new Rectangle(8, 8, 484, 384), new Dimension(300, 250)),
                    new Rectangle(192, 142, 300, 250));
            Assert.assertNull(panel.manualBounds(new Rectangle(8, 8, 284, 200), new Dimension(300, 250)));
            panel.resetPlacement();
            title.dispatchEvent(new MouseEvent(title, MouseEvent.MOUSE_DRAGGED, 1040, MouseEvent.BUTTON1_DOWN_MASK,
                    20, 20, 0, false, MouseEvent.NOBUTTON));
            Assert.assertFalse(panel.isManuallyPlaced());
        });
    }

    @Test
    public void crampedOrOffscreenCardsFallBackToMainPrompt() {
        final Rectangle board = new Rectangle(0, 0, 420, 320);
        Assert.assertNull(CardActionPanel.place(new Rectangle(160, 40, 100, 150), board, new Dimension(300, 250)));
        Assert.assertNull(CardActionPanel.place(new Rectangle(20, 40, 100, 150), board, new Dimension(300, 400)));
        Assert.assertNull(CardActionPanel.place(new Rectangle(440, 40, 100, 150), board, new Dimension(300, 250)));
    }
}
