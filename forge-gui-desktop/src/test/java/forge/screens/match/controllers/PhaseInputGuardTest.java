package forge.screens.match.controllers;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PhaseInputGuardTest {
    @Test
    public void resolvingLastItemRequiresFreshClickBeforeLeavingSameMainPhase() {
        final PhaseInputGuard guard = new PhaseInputGuard();
        guard.enter("main1", true, 1000);
        Assert.assertTrue(guard.accepts(2000, 2000));
        guard.keyPressed(32);
        guard.protect(3000);
        guard.enter("main1", true, 3100);
        Assert.assertFalse(guard.accepts(2990, 3700));
        Assert.assertFalse(guard.accepts(3200, 3200));
        Assert.assertFalse(guard.accepts(3700, 3700));
        guard.keyReleased(32);
        Assert.assertTrue(guard.accepts(3800, 3800));
    }

    @Test
    public void newMainPhaseRejectsOldAndRapidInputsButAcceptsFreshInput() {
        final PhaseInputGuard guard = new PhaseInputGuard();
        guard.enter("upkeep", false, 1000);
        guard.enter("main1", true, 2000);
        Assert.assertFalse(guard.accepts(1900, 2700));
        Assert.assertFalse(guard.accepts(2100, 2100));
        Assert.assertTrue(guard.accepts(2600, 2600));
        guard.enter("main1", true, 3000);
        Assert.assertTrue(guard.accepts(3001, 3001));
    }

    @Test
    public void keyHeldAcrossPhaseChangeRequiresRelease() {
        final PhaseInputGuard guard = new PhaseInputGuard();
        guard.enter("draw", false, 1000);
        guard.keyPressed(32);
        guard.enter("main1", true, 2000);
        Assert.assertFalse(guard.accepts(3000, 3000));
        guard.keyReleased(32);
        Assert.assertTrue(guard.accepts(3100, 3100));
        guard.enter("main2", true, 4000);
        Assert.assertFalse(guard.accepts(4100, 4100));
    }
}
