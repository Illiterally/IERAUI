package forge.toolbox.special;

import com.google.common.collect.HashMultiset;
import forge.card.mana.ManaAtom;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.*;

public class PlayerStatusBarTest {
    private static PlayerStatusBar create(final PlayerView player) {
        return new PlayerStatusBar(player, EnumSet.of(ZoneType.Hand, ZoneType.Command, ZoneType.PlanarDeck),
                new Font(Font.SANS_SERIF, Font.PLAIN, 13), key -> switch (key) {
                    case "lblIERAManaPool" -> "Mana pool";
                    case "lblLife" -> "Life";
                    case "lblIERAMore" -> "More";
                    case "lblIERAPlayable" -> "Playable";
                    case "lblDeck" -> "Deck";
                    case "lblCommander" -> "Commander";
                    case "lblIERACommanderReady" -> "Commander ready to cast. Click to open the command zone.";
                    default -> key;
                }, ZoneType::name, color -> null, zone -> new Icon() {
                    @Override public int getIconWidth() { return 16; }
                    @Override public int getIconHeight() { return 16; }
                    @Override public void paintIcon(Component c, Graphics g, int x, int y) {}
                });
    }

    private static List<Component> descendants(final Container root) {
        final List<Component> result = new ArrayList<>();
        for (final Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container container) { result.addAll(descendants(container)); }
        }
        return result;
    }

    private static JButton button(final PlayerStatusBar bar, final String name) {
        return descendants(bar).stream().filter(c -> c instanceof JButton && name.equals(c.getName()))
                .map(c -> (JButton)c).findFirst().orElseThrow();
    }

    @Test
    public void displaysLivePoolZoneLifeAndSimultaneousCounters() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getMaxHandString()).thenReturn("7");
            when(player.getZoneSize(ZoneType.Hand)).thenReturn(5);
            when(player.getMana((byte) ManaAtom.GREEN)).thenReturn(2);
            when(player.getMana((byte) ManaAtom.COLORLESS)).thenReturn(12);
            when(player.getLife()).thenReturn(18);
            final var counters = HashMultiset.<CounterType>create();
            counters.add(CounterEnumType.POISON, 3);
            counters.add(CounterEnumType.ENERGY, 5);
            when(player.getCounters()).thenReturn(counters);
            final PlayerStatusBar bar = create(player);
            Assert.assertEquals(button(bar, "mana-C").getText(), "C 12");
            Assert.assertEquals(button(bar, "zone-Hand").getText(), "Hand 5 / 7");
            Assert.assertEquals(button(bar, "zone-Library").getText(), "Deck 0");
            Assert.assertEquals(button(bar, "zone-Command").getText(), "Commander 0");
            Assert.assertNotNull(button(bar, "zone-Library").getIcon());
            Assert.assertNotNull(button(bar, "zone-Command").getIcon());
            final List<String> labels = descendants(bar).stream().filter(c -> c instanceof JLabel)
                    .map(c -> ((JLabel)c).getText()).toList();
            Assert.assertTrue(labels.contains("Mana pool 14"));
            Assert.assertTrue(labels.contains("Life 18"));
            Assert.assertTrue(labels.stream().anyMatch(s -> s.contains("3") && s.contains("5")));
            when(player.getMana((byte) ManaAtom.COLORLESS)).thenReturn(0);
            when(player.getZoneSize(ZoneType.Hand)).thenReturn(4);
            bar.updateStatus();
            Assert.assertEquals(button(bar, "mana-C").getText(), "C 0");
            Assert.assertFalse(button(bar, "mana-C").isEnabled());
            Assert.assertEquals(button(bar, "zone-Hand").getText(), "Hand 4 / 7");
        });
    }

    @Test
    public void zoneAndManaActionsUseTheExistingHandlersExactlyOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getMana((byte) ManaAtom.GREEN)).thenReturn(2);
            final PlayerStatusBar bar = create(player);
            final List<ZoneType> opened = new ArrayList<>();
            final List<ZoneType> context = new ArrayList<>();
            final List<Byte> spent = new ArrayList<>();
            bar.setupActions(zone -> () -> opened.add(zone), (zone, e) -> context.add(zone), spent::add);
            button(bar, "zone-Command").doClick(0);
            button(bar, "mana-G").doClick(0);
            button(bar, "mana-C").doClick(0);
            final JButton graveyard = button(bar, "zone-Graveyard");
            graveyard.dispatchEvent(new MouseEvent(graveyard, MouseEvent.MOUSE_CLICKED, 10, 0,
                    5, 5, 1, true, MouseEvent.BUTTON3));
            Assert.assertEquals(opened, List.of(ZoneType.Command));
            Assert.assertEquals(context, List.of(ZoneType.Graveyard));
            Assert.assertEquals(spent, List.of((byte) ManaAtom.GREEN));
            Assert.assertFalse(graveyard.isFocusable());
        });
    }

    @Test
    public void widePanelFitsOneRowAndKeepsSpaceForCards() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getMaxHandString()).thenReturn("7");
            when(player.getLife()).thenReturn(40);
            when(player.getZoneSize(ZoneType.Library)).thenReturn(92);
            final PlayerStatusBar bar = create(player);
            bar.setSize(1275, bar.getPreferredSize().height);
            bar.doLayout();
            Assert.assertTrue(bar.getHeight() <= 34);
            for (final String name : List.of("mana-W", "mana-C", "zone-Hand", "zone-Library",
                    "zone-Graveyard", "zone-Exile", "zone-Command", "zone-Flashback")) {
                Assert.assertTrue(button(bar, name).isVisible(), name);
            }
            assertSingleRowWithinBounds(bar);
        });
    }

    @Test
    public void resizingMovesOverflowToMenuWithoutLosingActionsOrGrowingHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getMana((byte) ManaAtom.GREEN)).thenReturn(12);
            final PlayerStatusBar bar = create(player);
            final List<ZoneType> opened = new ArrayList<>();
            final List<Byte> spent = new ArrayList<>();
            bar.setupActions(zone -> () -> opened.add(zone), (zone, e) -> {}, spent::add);
            bar.setSize(360, bar.getPreferredSize().height);
            bar.doLayout();
            assertSingleRowWithinBounds(bar);
            Assert.assertFalse(button(bar, "zone-Command").isVisible());
            menuItem(bar.createOverflowMenu(), "zone-Command").doClick(0);
            Assert.assertEquals(opened, List.of(ZoneType.Command));
            Assert.assertNotNull(menuItem(bar.createOverflowMenu(), "zone-Sideboard"));
            bar.setSize(90, bar.getHeight());
            bar.doLayout();
            assertSingleRowWithinBounds(bar);
            menuItem(bar.createOverflowMenu(), "mana-G").doClick(0);
            menuItem(bar.createOverflowMenu(), "mana-C").doClick(0);
            Assert.assertEquals(spent, List.of((byte) ManaAtom.GREEN));
            bar.setSize(1275, bar.getHeight());
            bar.doLayout();
            assertSingleRowWithinBounds(bar);
            Assert.assertTrue(button(bar, "zone-Command").isVisible());
            Assert.assertTrue(button(bar, "mana-G").isVisible());
            Assert.assertTrue(bar.getHeight() <= 34);
        });
    }

    private static JMenuItem menuItem(final JPopupMenu menu, final String name) {
        return java.util.Arrays.stream(menu.getComponents())
                .filter(c -> c instanceof JMenuItem && name.equals(c.getName()))
                .map(c -> (JMenuItem)c).findFirst().orElseThrow();
    }

    @Test public void resourcesStayRightOfZonesAndMoreDuringLiveChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getLife()).thenReturn(40);
            when(player.getMaxHandString()).thenReturn("7");
            final var counters = HashMultiset.<CounterType>create();
            when(player.getCounters()).thenReturn(counters);
            final PlayerStatusBar bar = create(player);
            bar.setSize(1400, bar.getPreferredSize().height);
            bar.doLayout();
            Assert.assertTrue(button(bar, "zone-Hand").getX() < button(bar, "extra-zones").getX());
            Assert.assertTrue(button(bar, "extra-zones").getX() < button(bar, "mana-W").getX());
            final JLabel total = (JLabel) descendants(bar).stream().filter(c -> "mana-pool-total".equals(c.getName())).findFirst().orElseThrow();
            Assert.assertTrue(total.getX() > button(bar, "mana-C").getX());
            when(player.getLife()).thenReturn(34);
            when(player.getMana((byte) ManaAtom.GREEN)).thenReturn(2);
            counters.add(CounterEnumType.POISON, 1);
            bar.updateStatus();
            final List<String> labels = descendants(bar).stream().filter(c -> c instanceof JLabel).map(c -> ((JLabel)c).getText()).toList();
            Assert.assertTrue(labels.contains("Life 34 (\u22126)"));
            Assert.assertTrue(labels.contains("Mana pool 2 (+2)"));
            Assert.assertTrue(labels.stream().anyMatch(s -> s.contains("Poison 1 (+1)")));
            for (final int width : new int[]{1400, 700, 360, 90}) {
                bar.setSize(width, bar.getHeight());
                bar.doLayout();
                assertSingleRowWithinBounds(bar);
            }
        });
    }

    private static void assertSingleRowWithinBounds(final PlayerStatusBar bar) {
        int right = 0;
        for (final Component component : bar.getComponents()) {
            if (!component.isVisible()) { continue; }
            Assert.assertEquals(component.getY(), 3);
            Assert.assertTrue(component.getX() >= right, component.getName());
            right = component.getX() + component.getWidth();
            Assert.assertTrue(right <= bar.getWidth(), component.getName());
            Assert.assertTrue(component.getY() + component.getHeight() <= bar.getHeight());
        }
    }

    @Test public void castableCommanderHasBlueCueWithoutChangingSizeOrClickBehavior() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getZoneSize(ZoneType.Command)).thenReturn(1);
            final PlayerStatusBar bar = create(player);
            final JButton commander = button(bar, "zone-Command");
            final Color normal = commander.getBackground();
            final Dimension size = commander.getPreferredSize();
            final List<ZoneType> opened = new ArrayList<>();
            bar.setupActions(zone -> () -> opened.add(zone), (zone, e) -> {}, color -> false);
            bar.setCommanderCastable(true);
            bar.updateStatus(); // resource updates must not erase the cue
            Assert.assertNotEquals(commander.getBackground(), normal);
            Assert.assertEquals(commander.getPreferredSize(), size);
            Assert.assertTrue(commander.getToolTipText().contains("ready to cast"));
            Assert.assertTrue(commander.getAccessibleContext().getAccessibleName().contains("ready to cast"));
            commander.doClick(0);
            Assert.assertEquals(opened, List.of(ZoneType.Command));
            bar.setCommanderCastable(false); // priority ended / payment began
            Assert.assertEquals(commander.getBackground(), normal);
            Assert.assertFalse(commander.getToolTipText().contains("ready to cast"));
            bar.setCommanderCastable(true);
            when(player.getZoneSize(ZoneType.Command)).thenReturn(0);
            bar.updateStatus();
            Assert.assertEquals(commander.getBackground(), normal);
        });
    }

    @Test public void readyCueFollowsCommanderIntoMoreAndBack() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PlayerView player = mock(PlayerView.class);
            when(player.getZoneSize(ZoneType.Command)).thenReturn(2);
            final PlayerStatusBar bar = create(player);
            final JButton more = button(bar, "extra-zones");
            final Color normal = more.getBackground();
            bar.setCommanderCastable(true);
            bar.setSize(360, bar.getPreferredSize().height);
            bar.doLayout();
            Assert.assertFalse(button(bar, "zone-Command").isVisible());
            Assert.assertNotEquals(more.getBackground(), normal);
            Assert.assertEquals(menuItem(bar.createOverflowMenu(), "zone-Command").getBackground(), more.getBackground());
            Assert.assertTrue(more.getAccessibleContext().getAccessibleName().contains("ready to cast"));
            assertSingleRowWithinBounds(bar);
            bar.setSize(1400, bar.getHeight());
            bar.doLayout();
            Assert.assertTrue(button(bar, "zone-Command").isVisible());
            Assert.assertEquals(more.getBackground(), normal);
            Assert.assertNotEquals(button(bar, "zone-Command").getBackground(), normal);
            bar.setSize(360, bar.getHeight());
            bar.doLayout();
            bar.setCommanderCastable(false);
            Assert.assertEquals(more.getBackground(), normal);
            Assert.assertFalse(more.getAccessibleContext().getAccessibleName().contains("ready to cast"));
        });
    }
}
