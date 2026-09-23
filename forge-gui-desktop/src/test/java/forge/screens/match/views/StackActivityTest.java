package forge.screens.match.views;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Set;

public class StackActivityTest {
    @Test public void noticesNewObjectsEvenWhenTheStackCountStaysTheSame() {
        final StackActivity activity = new StackActivity();
        Assert.assertFalse(activity.update(1, Set.of()));
        Assert.assertTrue(activity.update(1, Set.of(10)));
        Assert.assertFalse(activity.update(1, Set.of(10)));
        Assert.assertTrue(activity.update(1, Set.of(11)));
        Assert.assertFalse(activity.update(1, Set.of()));
    }

    @Test public void resolvingAndLoadingANewMatchDoNotCreateFalseActivity() {
        final StackActivity activity = new StackActivity();
        Assert.assertFalse(activity.update(1, Set.of(1, 2)));
        Assert.assertFalse(activity.update(1, Set.of(1)));
        Assert.assertFalse(activity.update(2, Set.of(4, 5)));
        Assert.assertTrue(activity.update(2, Set.of(4, 5, 6)));
    }
}
