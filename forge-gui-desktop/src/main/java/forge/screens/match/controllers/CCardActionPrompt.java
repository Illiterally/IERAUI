package forge.screens.match.controllers;

import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import forge.Singletons;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.CardActionPanel;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/** Mirrors a card-specific input without taking focus or capturing battlefield clicks. GPL-3.0-or-later. */
final class CCardActionPrompt {
    private record Context(String message, CardView card) { }
    private final CPrompt controller;
    private final CMatchUI matchUI;
    private final VPrompt source;
    private final AtomicLong revision = new AtomicLong();
    private final AtomicBoolean queued = new AtomicBoolean();
    private final Timer follow = new Timer(150, e -> position());
    private volatile Context context = new Context("", null);
    private CardActionPanel panel;
    private Integer dismissedCard;
    private volatile boolean disposed;

    CCardActionPrompt(final CPrompt controller) {
        this.controller = controller;
        matchUI = controller.getMatchUI();
        source = controller.getView();
        source.getBtnOK().addPropertyChangeListener(e -> {
            if ("text".equals(e.getPropertyName()) || "enabled".equals(e.getPropertyName())) { refresh(); }
        });
        source.getBtnCancel().addPropertyChangeListener(e -> {
            if ("text".equals(e.getPropertyName()) || "enabled".equals(e.getPropertyName())) { refresh(); }
        });
        source.getBtnManaAuto().addPropertyChangeListener(e -> refresh());
    }

    void setContext(final String text, final CardView card) {
        revision.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (!disposed) {
                final CardView previous = context.card();
                if (panel != null && (card == null || previous == null || card.getId() != previous.getId())) {
                    panel.resetPlacement();
                }
                context = new Context(text == null ? "" : text, card);
                refresh();
            }
        });
    }

    void refresh() {
        revision.incrementAndGet(); // Invalidate an old mouse press before the EDT update.
        if (!disposed && queued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                queued.set(false);
                if (!disposed) { synchronize(); }
            });
        }
    }

    void blockButtons() {
        revision.incrementAndGet();
        if (panel != null) { panel.setPending(); }
    }

    private void synchronize() {
        final long currentRevision = revision.get();
        final Context current = context;
        if (current.card() == null) { dismissedCard = null; hide(true); return; }
        if (dismissedCard != null && dismissedCard != current.card().getId()) { dismissedCard = null; }
        if (dismissedCard != null) { hide(true); return; }
        if (panel == null) {
            panel = new CardActionPanel(FSkin.getFont(14).getBaseFont(),
                    Localizer.getInstance().getMessage("lblIERACloseCardPrompt"),
                    Localizer.getInstance().getMessage("lblIERADragCardPrompt"), this::submit, () -> {
                        final CardView card = context.card();
                        dismissedCard = card == null ? null : card.getId();
                        hide(true);
                    });
            Singletons.getView().getLpnDocument().add(panel, FView.TARGETING_LAYER, 0);
        }
        panel.showPrompt(new CardActionPanel.Prompt(currentRevision, current.card().getName(),
                FSkin.encodeSymbols(current.message(), false), source.getBtnOK().getText(),
                source.getBtnCancel().getText(), source.getBtnOK().isEnabled(), source.getBtnCancel().isEnabled(),
                source.getBtnManaAuto().isVisible() ? source.getBtnManaAuto().getText() : null,
                source.getBtnManaAuto().isEnabled()));
        position();
        final GameView game = matchUI.getGameView();
        if (game != null && !game.isGameOver()) { follow.start(); }
    }

    private boolean submit(final CardActionPanel.Action action) {
        if (disposed || action.revision() != revision.get() || panel == null || !panel.isShowing()
                || !matchUI.isCurrentScreen() || matchUI.getGameView() == null || matchUI.getGameView().isGameOver()) {
            return false;
        }
        return action.autoMana() ? controller.submitManaAuto(action.pressedAt())
                : controller.submitAction(action.primary(), action.pressedAt());
    }

    private void position() {
        final GameView game = matchUI.getGameView();
        if (disposed || dismissedCard != null || context.card() == null || game == null || game.isGameOver()) { hide(true); return; }
        if (panel == null) { return; }
        if (!matchUI.isCurrentScreen() || (!source.getBtnOK().isEnabled() && !source.getBtnCancel().isEnabled()
                && !source.getBtnManaAuto().isEnabled())) {
            hide(false); return;
        }
        final JLayeredPane host = Singletons.getView().getLpnDocument();
        final Rectangle viewport = new Rectangle(8, 8, Math.max(0, host.getWidth() - 16),
                Math.max(0, host.getHeight() - 16));
        // Once dragged, stop following the card. Payment refreshes and the timer
        // must preserve the chosen position, including outside the card's field.
        if (panel.isManuallyPlaced()) {
            final Rectangle bounds = panel.manualBounds(viewport, panel.getPreferredSize());
            if (bounds == null) { hide(false); return; }
            showAt(bounds);
            return;
        }
        final CardPanel anchor = matchUI.findVisibleCardPanel(context.card());
        if (anchor == null) { hide(false); return; }
        if (!SwingUtilities.isDescendingFrom(anchor, host)) { hide(false); return; }
        // Untapped CardPanels include spare space for rotation; anchor to the drawn card itself.
        final Rectangle cardBounds = context.card().isTapped() ? anchor.getBounds()
                : new Rectangle(anchor.getCardX(), anchor.getCardY(), anchor.getCardWidth(), anchor.getCardHeight());
        final Rectangle card = SwingUtilities.convertRectangle(anchor.getParent(), cardBounds, host);
        // Stay within the visible card area: do not cover the hand, portrait, log, or sidebar.
        final Rectangle area = SwingUtilities.convertRectangle(anchor.getParent(),
                ((javax.swing.JComponent) anchor.getParent()).getVisibleRect(), host);
        final Rectangle board = area.intersection(viewport);
        final Rectangle bounds = CardActionPanel.place(card, board, panel.getPreferredSize());
        if (bounds == null) { hide(false); return; }
        showAt(bounds);
    }

    private void showAt(final Rectangle bounds) {
        panel.setBounds(bounds);
        panel.setVisible(true);
        panel.revalidate();
        panel.repaint();
    }

    private void hide(final boolean stop) {
        if (panel != null) { panel.setVisible(false); }
        if (stop) { follow.stop(); }
    }

    void dispose() {
        disposed = true;
        revision.incrementAndGet();
        follow.stop();
        if (panel != null && panel.getParent() != null) {
            final java.awt.Container parent = panel.getParent();
            parent.remove(panel);
            parent.repaint();
        }
    }
}
