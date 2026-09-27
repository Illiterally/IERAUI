package forge.toolbox.special;

import forge.card.mana.ManaAtom;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.game.card.CounterType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Labeled, live player resources above the hand. GPL-3.0-or-later. */
@SuppressWarnings("serial")
public final class PlayerStatusBar extends JPanel {
    private static final Color BACKGROUND = new Color(21, 31, 38);
    private static final Color TEXT = new Color(239, 245, 249);
    private static final Color ACCENT = new Color(101, 208, 233);
    private static final Color BUTTON_BACKGROUND = new Color(38, 54, 65);
    private static final Color READY_BACKGROUND = new Color(16, 112, 148);
    private static final ZoneType[] ZONES = { ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard,
            ZoneType.Exile, ZoneType.Command, ZoneType.Flashback, ZoneType.Sideboard };
    private final PlayerView player;
    private final Function<String, String> text;
    private final Function<ZoneType, String> zoneName;
    private final Function<ZoneType, Icon> zoneIcon;
    private final EnumSet<ZoneType> extraZones;
    private final Map<ZoneType, JButton> zones = new EnumMap<>(ZoneType.class);
    private final Map<Byte, JButton> mana = new LinkedHashMap<>();
    private final List<JComponent> strip = new ArrayList<>();
    private final List<JComponent> manaStrip = new ArrayList<>();
    private final ResourceChanges changes = new ResourceChanges();
    private final java.util.Set<CounterType> knownCounters = new java.util.LinkedHashSet<>();
    private final Timer changeTimer = new Timer(150, event -> updateStatus());
    private boolean statusInitialized;
    private boolean commanderCastable;
    private static final int GAP = 4;
    private static final int PAD = 6;
    private final JLabel pool;
    private final JLabel life;
    private final JLabel counters;
    private final JButton more;
    private Function<ZoneType, Runnable> zoneAction;
    private BiConsumer<ZoneType, MouseEvent> zoneContext;
    private Function<Byte, Boolean> manaAction;

    public PlayerStatusBar(final PlayerView player, final EnumSet<ZoneType> supportedZones,
                           final Font font, final Function<String, String> text,
                           final Function<ZoneType, String> zoneName,
                           final Function<String, Icon> manaIcon,
                           final Function<ZoneType, Icon> zoneIcon) {
        super(null);
        this.player = player;
        this.text = text;
        this.zoneName = zoneName;
        this.zoneIcon = zoneIcon;
        extraZones = EnumSet.copyOf(supportedZones);
        extraZones.removeAll(java.util.List.of(ZONES));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(57, 85, 97)));

        pool = label(font, ACCENT);
        pool.setToolTipText(text.apply("lblIERAPoolExplanation"));
        pool.setName("mana-pool-total");
        for (final String color : new String[]{"W", "U", "B", "R", "G", "C"}) {
            final byte atom = ManaAtom.fromName(color);
            final JButton button = button(font);
            button.setIcon(manaIcon.apply(color));
            button.putClientProperty("manaColor", color);
            button.setName("mana-" + color);
            button.addActionListener(e -> { if (manaAction != null) { manaAction.apply(atom); } });
            mana.put(atom, button);
            manaStrip.add(button);
        }
        life = label(font, TEXT);
        life.setName("life-total");
        life.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        counters = label(font, TEXT);
        counters.setName("player-counters");
        addToStrip(life);
        addToStrip(counters);
        for (final ZoneType zone : ZONES) {
            final JButton button = button(font);
            button.setName("zone-" + zone.name());
            button.setIcon(zoneIcon.apply(zone));
            button.addActionListener(e -> { if (zoneAction != null) { zoneAction.apply(zone).run(); } });
            button.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(final MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e) && zoneContext != null) {
                        zoneContext.accept(zone, e);
                    }
                }
            });
            zones.put(zone, button);
            if (zone == ZoneType.Sideboard) {
                add(button);
                button.setVisible(false);
            } else {
                addToStrip(button);
            }
        }
        more = button(font);
        more.setName("extra-zones");
        more.setText(text.apply("lblIERAMore") + " +");
        more.setToolTipText(text.apply("lblIERAMoreResources"));
        more.addActionListener(e -> createOverflowMenu().show(more, 0, more.getHeight()));
        add(more);
        manaStrip.add(pool);
        for (final JComponent component : manaStrip) { addToStrip(component); }

        // One row at every width. Overflow goes into More rather than taking card space.
        final int height = Math.max(32, more.getPreferredSize().height + 6);
        setPreferredSize(new Dimension(600, height));
        setMinimumSize(new Dimension(0, height));
        updateStatus();
    }

    private void addToStrip(final JComponent component) {
        strip.add(component);
        add(component);
    }

    private int visibleWidth() {
        int width = 0;
        for (final JComponent component : strip) {
            if (component.isVisible()) { width += component.getPreferredSize().width + GAP; }
        }
        return width;
    }

    @Override public void doLayout() {
        for (final JComponent component : strip) {
            component.setVisible(component != counters || !counters.getText().isEmpty());
        }
        final int moreWidth = Math.min(more.getPreferredSize().width, Math.max(0, getWidth() - PAD * 2));
        final int available = Math.max(0, getWidth() - PAD * 2 - moreWidth - GAP);
        final List<JComponent> overflowOrder = new ArrayList<>();
        for (final ZoneType zone : new ZoneType[]{ZoneType.Flashback, ZoneType.Exile, ZoneType.Library,
                ZoneType.Graveyard, ZoneType.Hand, ZoneType.Command}) {
            overflowOrder.add(zones.get(zone));
        }
        for (final JButton button : mana.values()) {
            if (!button.isEnabled()) { overflowOrder.add(button); }
        }
        overflowOrder.add(counters);
        overflowOrder.addAll(mana.values());
        overflowOrder.add(life);
        overflowOrder.add(pool);
        int width = visibleWidth();
        for (final JComponent component : overflowOrder) {
            if (width <= available) { break; }
            if (component.isVisible()) {
                width -= component.getPreferredSize().width + GAP;
                component.setVisible(false);
            }
        }
        int x = PAD;
        final int height = Math.max(0, getHeight() - 6);
        for (final JComponent component : strip) {
            if (!component.isVisible() || manaStrip.contains(component)) { continue; }
            final int componentWidth = component.getPreferredSize().width;
            component.setBounds(x, 3, componentWidth, height);
            x += componentWidth + GAP;
        }
        int manaWidth = 0;
        for (final JComponent component : manaStrip) {
            if (component.isVisible()) { manaWidth += component.getPreferredSize().width + GAP; }
        }
        // Keep the payment resources next to the right-side action prompt.
        x = Math.max(x, getWidth() - PAD - manaWidth - moreWidth - GAP);
        more.setBounds(x, 3, moreWidth, height);
        x += moreWidth + GAP;
        for (final JComponent component : manaStrip) {
            if (!component.isVisible()) { continue; }
            final int componentWidth = component.getPreferredSize().width;
            component.setBounds(x, 3, componentWidth, height);
            x += componentWidth + GAP;
        }
        updateCommanderCue();
    }

    private static JLabel label(final Font font, final Color color) {
        final JLabel label = new JLabel();
        label.setFont(font.deriveFont(Font.BOLD));
        label.setForeground(color);
        return label;
    }

    private static JButton button(final Font font) {
        final JButton button = new JButton();
        button.setFont(font);
        button.setForeground(TEXT);
        button.setBackground(BUTTON_BACKGROUND);
        button.setMargin(new Insets(1, 5, 1, 5));
        button.setFocusable(false);
        button.setFocusPainted(false);
        final javax.swing.border.Border normalBorder = button.getBorder();
        button.putClientProperty("normalBorder", normalBorder);
        button.putClientProperty("readyBorder", new javax.swing.border.AbstractBorder() {
            @Override public Insets getBorderInsets(final Component c) { return normalBorder.getBorderInsets(c); }
            @Override public Insets getBorderInsets(final Component c, final Insets insets) {
                final Insets original = getBorderInsets(c);
                insets.set(original.top, original.left, original.bottom, original.right);
                return insets;
            }
            @Override public void paintBorder(final Component c, final Graphics g,
                                              final int x, final int y, final int width, final int height) {
                g.setColor(ACCENT);
                g.drawRect(x, y, width - 1, height - 1);
                g.drawRect(x + 1, y + 1, width - 3, height - 3);
            }
        });
        return button;
    }

    /** Mirrors the current priority window, not merely the command-zone count. */
    public void setCommanderCastable(final boolean castable) {
        commanderCastable = castable;
        updateCommanderCue();
    }

    private void updateCommanderCue() {
        final JButton commander = zones.get(ZoneType.Command);
        final boolean ready = commanderCastable && player.getZoneSize(ZoneType.Command) > 0;
        final String hint = ready ? text.apply("lblIERACommanderReady") : null;
        styleReady(commander, ready);
        commander.setToolTipText(ready ? hint : zoneName.apply(ZoneType.Command) + ": "
                + player.getZoneSize(ZoneType.Command) + ". " + text.apply("lblIERAZoneClickHint"));
        commander.getAccessibleContext().setAccessibleName(commander.getText() + (ready ? ". " + hint : ""));
        // A narrow window can move Commander into More; keep its cue visible there.
        final boolean inMore = ready && !commander.isVisible();
        styleReady(more, inMore);
        more.setToolTipText(inMore ? hint : text.apply("lblIERAMoreResources"));
        more.getAccessibleContext().setAccessibleName(more.getText() + (inMore ? ". " + hint : ""));
    }

    private static void styleReady(final JButton button, final boolean ready) {
        button.setBackground(ready ? READY_BACKGROUND : BUTTON_BACKGROUND);
        button.setBorder((javax.swing.border.Border) button.getClientProperty(ready ? "readyBorder" : "normalBorder"));
    }

    public void setupActions(final Function<ZoneType, Runnable> zoneAction,
                             final BiConsumer<ZoneType, MouseEvent> zoneContext,
                             final Function<Byte, Boolean> manaAction) {
        this.zoneAction = zoneAction;
        this.zoneContext = zoneContext;
        this.manaAction = manaAction;
    }

    public void setSummaryIcons(final Icon lifeIcon, final Icon extraZonesIcon) {
        life.setIcon(lifeIcon);
        more.setIcon(extraZonesIcon);
    }

    /** Visible origin for draw animations, including when Deck is in More. */
    public JComponent getZoneAnchor(final ZoneType zone) {
        final JButton button = zones.get(zone);
        return button != null && button.isShowing() ? button : more;
    }

    JPopupMenu createOverflowMenu() {
        final JPopupMenu menu = new JPopupMenu();
        for (final JComponent component : strip) {
            if (component.isVisible()) { continue; }
            if (component instanceof JButton button) {
                addMenuButton(menu, button);
            } else if (component instanceof JLabel label && !label.getText().isEmpty()) {
                final JMenuItem item = new JMenuItem(label.getText());
                item.setEnabled(false);
                menu.add(item);
            }
        }
        if (menu.getComponentCount() > 0) { menu.addSeparator(); }
        addMenuButton(menu, zones.get(ZoneType.Sideboard));
        for (final ZoneType zone : extraZones) {
            final JMenuItem item = new JMenuItem(zoneName.apply(zone) + " " + player.getZoneSize(zone), zoneIcon.apply(zone));
            item.addActionListener(e -> { if (zoneAction != null) { zoneAction.apply(zone).run(); } });
            menu.add(item);
        }
        return menu;
    }

    private void addMenuButton(final JPopupMenu menu, final JButton button) {
        final String color = (String) button.getClientProperty("manaColor");
        final String title = color == null ? button.getText()
                : text.apply("lblIERAMana" + color) + " " + player.getMana(ManaAtom.fromName(color));
        final JMenuItem item = new JMenuItem(title, button.getIcon());
        item.setName(button.getName());
        item.setEnabled(button.isEnabled());
        item.setToolTipText(button.getToolTipText());
        item.getAccessibleContext().setAccessibleName(button.getAccessibleContext().getAccessibleName());
        if (button == zones.get(ZoneType.Command) && commanderCastable
                && player.getZoneSize(ZoneType.Command) > 0) {
            item.setOpaque(true);
            item.setBackground(READY_BACKGROUND);
            item.setForeground(TEXT);
        }
        item.addActionListener(e -> button.doClick(0));
        menu.add(item);
    }

    /** Called by the same game-view events that update the portrait's details. */
    public void updateStatus() {
        final long now = System.currentTimeMillis();
        int total = 0;
        for (final Map.Entry<Byte, JButton> entry : mana.entrySet()) {
            final int amount = player.getMana(entry.getKey());
            final JButton button = entry.getValue();
            final String color = (String) button.getClientProperty("manaColor");
            total += amount;
            button.setText((button.getIcon() == null ? color + " " : "") + amount);
            button.setEnabled(amount > 0);
            button.setForeground(amount > 0 ? TEXT : new Color(158, 171, 179));
            final int change = changes.observe("mana-" + color, amount, now);
            button.setBackground(change == 0 ? new Color(38, 54, 65) : new Color(34, 87, 105));
            button.setToolTipText(text.apply("lblIERAMana" + color) + ": " + amount
                    + ". " + text.apply("lblIERAPoolClickHint"));
            button.getAccessibleContext().setAccessibleName(button.getToolTipText());
        }
        pool.setText(withChange(text.apply("lblIERAManaPool") + " " + total, changes.observe("pool", total, now)));
        final int lifeChange = changes.observe("life", player.getLife(), now);
        life.setText(withChange(text.apply("lblLife") + " " + player.getLife(), lifeChange));
        life.setForeground(lifeChange > 0 ? new Color(145, 230, 172)
                : lifeChange < 0 || player.getLife() <= 5 ? new Color(255, 150, 136) : TEXT);
        final StringBuilder counterText = new StringBuilder();
        if (player.getCounters() != null) {
            for (final CounterType counter : player.getCounters().elementSet()) {
                if (knownCounters.add(counter)) {
                    changes.observe("counter-" + counter.getName(), statusInitialized ? 0 : player.getCounters().count(counter), now);
                }
            }
        }
        for (final CounterType counter : knownCounters) {
                final int count = player.getCounters() == null ? 0 : player.getCounters().count(counter);
                final int change = changes.observe("counter-" + counter.getName(), count, now);
                if (count > 0 || change != 0) {
                    if (counterText.length() > 0) { counterText.append("   \u00b7   "); }
                    counterText.append(withChange(counter.getName() + " " + count, change));
                }
        }
        counters.setText(counterText.toString());
        statusInitialized = true;
        for (final Map.Entry<ZoneType, JButton> entry : zones.entrySet()) {
            final ZoneType zone = entry.getKey();
            final String count = String.valueOf(player.getZoneSize(zone));
            final String suffix = zone == ZoneType.Hand ? count + " / " + player.getMaxHandString() : count;
            final String caption = switch (zone) {
                case Flashback -> text.apply("lblIERAPlayable");
                case Library -> text.apply("lblDeck");
                case Command -> text.apply("lblCommander");
                default -> zoneName.apply(zone);
            };
            entry.getValue().setText(caption + " " + suffix);
            entry.getValue().setToolTipText(zoneName.apply(zone) + ": " + count
                    + ". " + text.apply("lblIERAZoneClickHint"));
        }
        updateCommanderCue();
        revalidate();
        repaint();
        if (isShowing() && changes.isActive(now)) { changeTimer.start(); }
        else { changeTimer.stop(); }
    }

    private static String withChange(final String text, final int change) {
        return change == 0 ? text : text + " (" + ResourceChanges.signed(change) + ")";
    }

    @Override public void addNotify() {
        super.addNotify();
        changes.clear();
        knownCounters.clear();
        statusInitialized = false;
        updateStatus();
    }

    @Override public void removeNotify() {
        changeTimer.stop();
        changes.clear();
        super.removeNotify();
    }
}
