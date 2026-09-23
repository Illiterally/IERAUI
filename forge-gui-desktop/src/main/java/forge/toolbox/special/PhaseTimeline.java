package forge.toolbox.special;

import forge.game.phase.PhaseType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** A compact view of the existing phase-stop controls. GPL-3.0-or-later. */
@SuppressWarnings("serial")
public final class PhaseTimeline extends JPanel {
    private static final Color ACTIVE = new Color(255, 220, 85);
    private static final Color IDLE = new Color(35, 43, 48);
    private final JLabel owner = new JLabel();
    private final JButton overflow = new JButton();
    private final Map<PhaseType, JButton> steps = new EnumMap<>(PhaseType.class);
    private final Function<String, String> text;
    private final PropertyChangeListener stateListener = event -> refresh();
    private PhaseIndicator source;
    private PhaseType current;
    private boolean listening;

    public PhaseTimeline(final Font font, final Function<String, String> text) {
        super(null);
        this.text = text;
        setOpaque(false);
        setPreferredSize(new Dimension(950, 20));
        setMinimumSize(new Dimension(0, 20));
        owner.setFont(font.deriveFont(Font.BOLD, 11f));
        owner.setForeground(Color.WHITE);
        add(owner);
        for (final PhaseType phase : PhaseType.values()) {
            final JButton button = new JButton();
            style(button, font);
            button.setName("phase-" + phase.name());
            button.addActionListener(event -> {
                final PhaseLabel model = model(phase);
                if (model != null) { model.toggleStop(); refresh(); }
            });
            button.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(final MouseEvent event) {
                    if (SwingUtilities.isRightMouseButton(event) && model(phase) != null) {
                        model(phase).requestYieldMarker();
                        refresh();
                    }
                }
            });
            steps.put(phase, button);
            add(button);
        }
        style(overflow, font);
        overflow.setName("phase-overflow");
        overflow.addActionListener(event -> createPhaseMenu().show(overflow, 0, overflow.getHeight()));
        add(overflow);
        refresh();
    }

    private static void style(final JButton button, final Font font) {
        button.setFont(font.deriveFont(11f));
        button.setMargin(new Insets(0, 2, 0, 2));
        button.setBorder(BorderFactory.createLineBorder(new Color(84, 106, 113)));
        button.setFocusable(false);
        button.setFocusPainted(false);
    }

    public void bind(final String playerName, final PhaseIndicator indicator, final PhaseType phase) {
        if (source != indicator) {
            detach();
            source = indicator;
            if (isDisplayable()) { attach(); }
        }
        current = phase;
        owner.setText(playerName + "  \u203a");
        owner.setToolTipText(playerName + " \u00b7 " + text.apply("lblIERATimelineHelp"));
        refresh();
    }

    private PhaseLabel model(final PhaseType phase) { return source == null ? null : source.getLabelFor(phase); }

    private void attach() {
        if (source == null || listening) { return; }
        for (final PhaseLabel label : source.allLabels()) { label.addPropertyChangeListener(stateListener); }
        listening = true;
    }

    private void detach() {
        if (!listening || source == null) { return; }
        for (final PhaseLabel label : source.allLabels()) { label.removePropertyChangeListener(stateListener); }
        listening = false;
    }

    @Override public void addNotify() { super.addNotify(); attach(); refresh(); }
    @Override public void removeNotify() { detach(); super.removeNotify(); }

    private String caption(final PhaseType phase) {
        return text.apply("lblIERATimeline" + phase.name());
    }

    private boolean active(final PhaseType phase) {
        return phase == current;
    }

    private void refresh() {
        for (final var entry : steps.entrySet()) {
            final PhaseType phase = entry.getKey();
            final JButton button = entry.getValue();
            final PhaseLabel model = model(phase);
            final boolean isActive = active(phase);
            final boolean stopping = model != null && model.getEnabled();
            button.setEnabled(model != null); // Untap has no priority stop.
            button.setBackground(isActive ? ACTIVE : IDLE);
            button.setForeground(isActive ? Color.BLACK : Color.WHITE);
            button.setIcon(stopMarker(model != null, stopping, isActive));
            button.setIconTextGap(3);
            final String state = model == null ? text.apply("lblIERANoPhaseStop")
                    : text.apply(stopping ? "lblIERAStopOn" : "lblIERAStopOff");
            final String hint = phase.nameForUi + " \u00b7 " + state + ". " + text.apply("lblIERATimelineHelp");
            button.setToolTipText(hint);
            button.getAccessibleContext().setAccessibleName((isActive ? "\u25b6 " : "") + hint);
        }
        overflow.setText((current == null ? text.apply("lblIERAPhases") : caption(current)) + " \u25be");
        overflow.setToolTipText(text.apply("lblIERATimelineHelp"));
        doLayout();
        repaint();
    }

    @Override public void doLayout() {
        final int width = getWidth();
        final int ownerWidth = Math.min(110, width / 5);
        owner.setBounds(0, 0, ownerWidth, getHeight());
        final int start = ownerWidth + 4;
        final int available = Math.max(0, width - start);
        final boolean compact = available < 510;
        overflow.setVisible(compact);
        overflow.setBounds(start, 0, available, getHeight());
        int index = 0;
        for (final var entry : steps.entrySet()) {
            final JButton button = entry.getValue();
            button.setVisible(!compact);
            final int left = start + available * index / steps.size();
            final int right = start + available * ++index / steps.size();
            button.setBounds(left, 0, Math.max(0, right - left - 2), getHeight());
            final PhaseType phase = entry.getKey();
            final PhaseLabel model = model(phase);
            final String prefix = active(phase) ? "> " : "";
            final String full = prefix + caption(phase);
            final String shortName = phase == PhaseType.UNTAP ? "UT" : model == null ? "" : model.getText();
            button.setText(button.getFontMetrics(button.getFont()).stringWidth(full) + 17 <= button.getWidth()
                    ? full : prefix + shortName);
            // A yield marker remains visible without replacing the phase name.
            button.setBorder(BorderFactory.createLineBorder(model != null && model.isYieldMarked()
                    ? new Color(255, 165, 40) : new Color(84, 106, 113), model != null && model.isYieldMarked() ? 2 : 1));
        }
    }

    private static Icon stopMarker(final boolean available, final boolean stopping, final boolean active) {
        return new Icon() {
            @Override public int getIconWidth() { return 6; }
            @Override public int getIconHeight() { return 6; }
            @Override public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                if (!available) { return; }
                g.setColor(active ? new Color(40, 38, 24) : stopping ? new Color(126, 224, 234) : new Color(106, 123, 135));
                if (stopping) { g.fillOval(x, y, 6, 6); }
                else { g.drawOval(x, y, 5, 5); }
            }
        };
    }

    JPopupMenu createPhaseMenu() {
        final JPopupMenu menu = new JPopupMenu();
        for (final PhaseType phase : PhaseType.values()) {
            final PhaseLabel model = model(phase);
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem((active(phase) ? "\u25b8 " : "") + caption(phase),
                    model != null && model.getEnabled());
            item.setName("phase-" + phase.name());
            item.setEnabled(model != null);
            item.addActionListener(event -> { if (model != null) { model.toggleStop(); refresh(); } });
            item.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(final MouseEvent event) {
                    if (SwingUtilities.isRightMouseButton(event) && model != null) { model.requestYieldMarker(); }
                }
            });
            menu.add(item);
        }
        return menu;
    }
}
