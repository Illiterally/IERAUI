package forge.screens.match.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/** Nonmodal, card-local copy of the authoritative prompt. GPL-3.0-or-later. */
@SuppressWarnings("serial")
public final class CardActionPanel extends JPanel {
    public record Action(boolean primary, boolean autoMana, long revision, long pressedAt) { }
    public record Prompt(long revision, String title, String html, String primary, String secondary,
                         boolean primaryEnabled, boolean secondaryEnabled, String autoMana, boolean autoEnabled) {
        public Prompt(long revision, String title, String html, String primary, String secondary,
                      boolean primaryEnabled, boolean secondaryEnabled) {
            this(revision, title, html, primary, secondary, primaryEnabled, secondaryEnabled, null, false);
        }
    }

    private final JLabel title = new JLabel();
    private final JEditorPane message = new JEditorPane("text/html", "");
    private final JScrollPane scroll = new JScrollPane(message);
    private final JButton primary = new JButton();
    private final JButton secondary = new JButton();
    private final JButton autoMana = new JButton();
    private final JButton close = new JButton("\u00d7");
    private Prompt prompt;
    private boolean pending;
    private final String dragHint;
    private Point manualLocation;
    private Point dragOffset;

    public CardActionPanel(final Font font, final String closeHint, final String dragHint,
                           final Predicate<Action> submit, final Runnable dismiss) {
        super(new BorderLayout(0, 9));
        this.dragHint = dragHint;
        final Color background = new Color(23, 34, 40);
        final Color foreground = new Color(244, 247, 250);
        final Color accent = new Color(83, 198, 230);
        setBackground(background);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 2),
                BorderFactory.createEmptyBorder(9, 11, 11, 11)));
        final JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        header.setToolTipText(dragHint);
        title.setFont(font.deriveFont(Font.BOLD));
        title.setForeground(foreground);
        title.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        final MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) || getParent() == null) { return; }
                final Point pointer = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), getParent());
                dragOffset = new Point(pointer.x - getX(), pointer.y - getY());
            }
            @Override public void mouseDragged(final MouseEvent event) {
                if (dragOffset == null || getParent() == null || !isVisible()) { return; }
                final Point pointer = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), getParent());
                manualLocation = new Point(pointer.x - dragOffset.x, pointer.y - dragOffset.y);
                final Rectangle bounds = manualBounds(new Rectangle(8, 8,
                        Math.max(0, getParent().getWidth() - 16), Math.max(0, getParent().getHeight() - 16)), getSize());
                if (bounds != null) {
                    final Rectangle dirty = getBounds().union(bounds);
                    setBounds(bounds);
                    getParent().repaint(dirty.x, dirty.y, dirty.width, dirty.height);
                }
            }
            @Override public void mouseReleased(final MouseEvent event) { dragOffset = null; }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        title.addMouseListener(drag);
        title.addMouseMotionListener(drag);
        header.add(title, BorderLayout.CENTER);
        styleButton(close, font, background, foreground);
        close.setPreferredSize(new Dimension(25, 25));
        close.setToolTipText(closeHint);
        close.getAccessibleContext().setAccessibleName(closeHint);
        close.addActionListener(e -> dismiss.run());
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        message.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        message.setFont(font);
        message.setForeground(foreground);
        message.setBackground(background);
        message.setEditable(false);
        message.setFocusable(false);
        message.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 5));
        buttons.setOpaque(false);
        styleButton(primary, font, new Color(22, 102, 126), foreground);
        styleButton(secondary, font, new Color(42, 58, 68), foreground);
        final JPanel paymentRow = new JPanel(new BorderLayout(5, 0));
        paymentRow.setOpaque(false);
        styleButton(autoMana, font, new Color(42, 58, 68), foreground);
        autoMana.setVisible(false);
        autoMana.setPreferredSize(new Dimension(58, 30));
        paymentRow.add(primary, BorderLayout.CENTER);
        paymentRow.add(autoMana, BorderLayout.EAST);
        buttons.add(paymentRow);
        buttons.add(secondary);
        add(buttons, BorderLayout.SOUTH);
        installAction(primary, true, false, submit);
        installAction(secondary, false, false, submit);
        installAction(autoMana, false, true, submit);
        setVisible(false);
    }

    private static void styleButton(final JButton button, final Font font, final Color bg, final Color fg) {
        button.setFont(font);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setFocusable(false); // Keyboard navigation remains with the main Prompt.
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bg.brighter()),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }

    private void installAction(final JButton button, final boolean isPrimary, final boolean isAutoMana, final Predicate<Action> submit) {
        final long[] press = {-1, 0};
        final boolean[] wasPressed = {false};
        button.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent event) {
                // A second click must not become a different action after payment changes to Resolve.
                if (event.getClickCount() > 1) { disarm(button); }
            }
        });
        button.getModel().addChangeListener(e -> {
            if (button.getModel().isPressed() && !wasPressed[0]) {
                press[0] = prompt == null ? -1 : prompt.revision();
                press[1] = System.currentTimeMillis();
            }
            wasPressed[0] = button.getModel().isPressed();
        });
        button.addActionListener(e -> {
            if (prompt == null || pending || press[0] != prompt.revision()) { return; }
            final long revision = prompt.revision();
            setPending(); // One dispatch until the engine supplies another prompt.
            if (!submit.test(new Action(isPrimary, isAutoMana, revision, press[1]))
                    && prompt.revision() == revision) {
                pending = false;
                updateEnabled();
            }
        });
    }

    public void showPrompt(final Prompt next) {
        if (prompt == null || next.revision() != prompt.revision()) {
            disarm(primary);
            disarm(secondary);
            disarm(autoMana);
            pending = false;
        }
        prompt = next;
        title.setText(next.title());
        title.setToolTipText(next.title() + " — " + dragHint);
        message.setText(next.html().replace("\n", "<br>"));
        message.setCaretPosition(0);
        primary.setText(next.primary());
        secondary.setText(next.secondary());
        primary.setToolTipText(next.primary());
        secondary.setToolTipText(next.secondary());
        autoMana.setText(next.autoMana());
        autoMana.setVisible(next.autoMana() != null);
        updateEnabled();
        message.setSize(270, 10000);
        scroll.setPreferredSize(new Dimension(270, Math.max(60, Math.min(180, message.getPreferredSize().height))));
        setPreferredSize(null);
        setPreferredSize(new Dimension(300, super.getPreferredSize().height));
    }

    public void setPending() {
        pending = true;
        disarm(primary);
        disarm(secondary);
        disarm(autoMana);
        updateEnabled();
    }

    private static void disarm(final JButton button) {
        button.getModel().setArmed(false);
        button.getModel().setPressed(false);
    }

    private void updateEnabled() {
        primary.setEnabled(prompt != null && !pending && prompt.primaryEnabled());
        secondary.setEnabled(prompt != null && !pending && prompt.secondaryEnabled());
        autoMana.setEnabled(prompt != null && !pending && prompt.autoMana() != null && prompt.autoEnabled());
    }

    /** Keep the player's placement through cost/button refreshes, within the match window. */
    public Rectangle manualBounds(final Rectangle viewport, final Dimension size) {
        if (manualLocation == null || size.width > viewport.width || size.height > viewport.height) { return null; }
        return new Rectangle(Math.max(viewport.x, Math.min(manualLocation.x, viewport.x + viewport.width - size.width)),
                Math.max(viewport.y, Math.min(manualLocation.y, viewport.y + viewport.height - size.height)),
                size.width, size.height);
    }

    public boolean isManuallyPlaced() { return manualLocation != null; }

    public void resetPlacement() {
        manualLocation = null;
        dragOffset = null;
    }

    @Override public void setVisible(final boolean visible) {
        if (!visible) { dragOffset = null; }
        super.setVisible(visible);
    }

    /** Prefer the card's right side, then left; never cover the source or leave the board. */
    public static Rectangle place(final Rectangle card, final Rectangle board, final Dimension size) {
        final int gap = 10;
        if (size.width > board.width || size.height > board.height || !board.intersects(card)) { return null; }
        final int y = Math.max(board.y, Math.min(card.y, board.y + board.height - size.height));
        if (card.x + card.width + gap + size.width <= board.x + board.width) {
            return new Rectangle(card.x + card.width + gap, y, size.width, size.height);
        }
        if (card.x - gap - size.width >= board.x) {
            return new Rectangle(card.x - gap - size.width, y, size.width, size.height);
        }
        return null; // The full Prompt remains available in cramped layouts.
    }
}
