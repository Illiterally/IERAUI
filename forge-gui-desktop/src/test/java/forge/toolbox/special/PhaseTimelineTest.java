package forge.toolbox.special;

import forge.game.phase.PhaseType;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class PhaseTimelineTest {
    @BeforeClass public void localize() {
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
    }

    private PhaseTimeline timeline() {
        return new PhaseTimeline(new Font(Font.SANS_SERIF, Font.PLAIN, 11),
                key -> Localizer.getInstance().getMessage(key));
    }

    private JButton button(final PhaseTimeline timeline, final PhaseType phase) {
        return Arrays.stream(timeline.getComponents()).filter(c -> ("phase-" + phase.name()).equals(c.getName()))
                .map(c -> (JButton)c).findFirst().orElseThrow();
    }

    @Test public void phaseClicksUseTheSameStopsAndYieldHandlers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PhaseIndicator source = new PhaseIndicator();
            final PhaseLabel main = source.getLabelFor(PhaseType.MAIN1);
            final AtomicInteger stops = new AtomicInteger();
            final AtomicInteger yields = new AtomicInteger();
            main.setOnToggled(stops::incrementAndGet);
            main.setOnRightClick(yields::incrementAndGet);
            main.setEnabled(true);
            final PhaseTimeline timeline = timeline();
            timeline.bind("Rosheen", source, PhaseType.MAIN1);
            final JButton mainButton = button(timeline, PhaseType.MAIN1);
            mainButton.doClick(0);
            Assert.assertFalse(main.getEnabled());
            Assert.assertEquals(stops.get(), 1);
            mainButton.dispatchEvent(new MouseEvent(mainButton, MouseEvent.MOUSE_PRESSED, 10, 0,
                    2, 2, 1, true, MouseEvent.BUTTON3));
            Assert.assertEquals(yields.get(), 1);
            Assert.assertEquals(stops.get(), 1);
            Assert.assertFalse(button(timeline, PhaseType.UNTAP).isEnabled());
        });
    }

    @Test public void turnChangeBindsToTheCorrectPlayersStops() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PhaseIndicator human = new PhaseIndicator();
            final PhaseIndicator opponent = new PhaseIndicator();
            human.getLabelFor(PhaseType.UPKEEP).setEnabled(false);
            opponent.getLabelFor(PhaseType.UPKEEP).setEnabled(true);
            final PhaseTimeline timeline = timeline();
            timeline.bind("Rosheen", human, PhaseType.MAIN1);
            timeline.bind("Karn", opponent, PhaseType.UPKEEP);
            button(timeline, PhaseType.UPKEEP).doClick(0);
            Assert.assertFalse(opponent.getLabelFor(PhaseType.UPKEEP).getEnabled());
            Assert.assertFalse(human.getLabelFor(PhaseType.UPKEEP).getEnabled());
            Assert.assertTrue(button(timeline, PhaseType.UPKEEP).getAccessibleContext().getAccessibleName().startsWith("\u25b6"));
            Assert.assertFalse(button(timeline, PhaseType.MAIN1).getAccessibleContext().getAccessibleName().startsWith("\u25b6"));
        });
    }

    @Test public void timelineFitsTheExistingTwentyPixelHeaderAndNarrowMenuKeepsActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final PhaseIndicator source = new PhaseIndicator();
            final PhaseTimeline timeline = timeline();
            timeline.bind("Rosheen", source, PhaseType.MAIN2);
            for (final int width : new int[]{1150, 650, 240}) {
                timeline.setSize(width, 20);
                timeline.doLayout();
                int right = 0;
                for (final Component component : timeline.getComponents()) {
                    if (!component.isVisible()) { continue; }
                    Assert.assertTrue(component.getX() >= right);
                    right = component.getX() + component.getWidth();
                    Assert.assertTrue(right <= width);
                    Assert.assertEquals(component.getY(), 0);
                    Assert.assertEquals(component.getHeight(), 20);
                }
            }
            Assert.assertFalse(button(timeline, PhaseType.MAIN2).isVisible());
            final JMenuItem main = Arrays.stream(timeline.createPhaseMenu().getComponents())
                    .filter(c -> "phase-MAIN2".equals(c.getName())).map(c -> (JMenuItem)c).findFirst().orElseThrow();
            main.doClick(0);
            Assert.assertFalse(source.getLabelFor(PhaseType.MAIN2).getEnabled());
            timeline.setSize(1150, 20);
            timeline.doLayout();
            Assert.assertTrue(button(timeline, PhaseType.MAIN2).isVisible());
        });
    }
}
