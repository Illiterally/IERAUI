package forge.toolbox.special;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ResourceChangesTest {
    @Test public void initialTotalsAndRepeatedRefreshesNeverLookLikeGains() {
        final ResourceChanges changes = new ResourceChanges();
        Assert.assertEquals(changes.observe("life", 40, 100), 0);
        Assert.assertEquals(changes.observe("life", 40, 200), 0);
        Assert.assertFalse(changes.isActive(200));
    }

    @Test public void changesAccumulateExpireAndRestartWithoutOldHistory() {
        final ResourceChanges changes = new ResourceChanges();
        changes.observe("life", 40, 0);
        Assert.assertEquals(changes.observe("life", 36, 100), -4);
        Assert.assertEquals(changes.observe("life", 34, 200), -6);
        Assert.assertEquals(changes.observe("life", 34, 200 + ResourceChanges.DURATION_MILLIS), 0);
        Assert.assertFalse(changes.isActive(4000));
        Assert.assertEquals(changes.observe("life", 38, 4100), 4);
        Assert.assertEquals(ResourceChanges.signed(-6), "\u22126");
    }

    @Test public void refundsCancelTheNetChangeAndHiddenPanelsReset() {
        final ResourceChanges changes = new ResourceChanges();
        changes.observe("pool", 2, 0);
        changes.observe("pool", 0, 100);
        Assert.assertEquals(changes.observe("pool", 2, 200), 0);
        Assert.assertFalse(changes.isActive(200));
        changes.clear();
        Assert.assertEquals(changes.observe("pool", 10, 300), 0);
    }
}
