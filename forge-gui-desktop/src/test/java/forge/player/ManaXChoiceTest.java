package forge.player;

import forge.gui.interfaces.IGuiGame;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ManaXChoiceTest {
    @BeforeClass public void initializeLanguage() {
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
    }

    @Test public void choosingASmallNumberNeverRunsTheCalculation() {
        IGuiGame gui = mock(IGuiGame.class);
        when(gui.oneOrNone(anyString(), anyList())).thenReturn(4);
        assertEquals(ManaXChoice.choose(gui, "X", 0, Integer.MAX_VALUE,
                () -> { fail("Do not calculate before Other"); return 0; }), Integer.valueOf(4));
        verify(gui, never()).getManaXInput(anyString(), anyInt(), anyInt(), any());
    }

    @Test public void otherCalculatesOnceAndStillAllowsManualChoice() {
        IGuiGame gui = mock(IGuiGame.class);
        when(gui.oneOrNone(anyString(), anyList())).thenReturn("Other...");
        when(gui.getManaXInput("X", 0, Integer.MAX_VALUE, 23)).thenReturn(17);
        AtomicInteger calls = new AtomicInteger();
        assertEquals(ManaXChoice.choose(gui, "X", 0, Integer.MAX_VALUE,
                () -> { calls.incrementAndGet(); return 23; }), Integer.valueOf(17));
        assertEquals(calls.get(), 1);
    }

    @Test public void cancelDoesNotCalculateOrCast() {
        IGuiGame gui = mock(IGuiGame.class);
        assertNull(ManaXChoice.choose(gui, "X", 0, Integer.MAX_VALUE,
                () -> { fail("Cancelled"); return 0; }));
        when(gui.oneOrNone(anyString(), anyList())).thenReturn("Other...");
        when(gui.getManaXInput("X", 0, Integer.MAX_VALUE, 23)).thenReturn(null);
        assertNull(ManaXChoice.choose(gui, "X", 0, Integer.MAX_VALUE, () -> 23));
    }

    @Test public void smallBoundedChoicesUseTheExistingPrompt() {
        IGuiGame gui = mock(IGuiGame.class);
        when(gui.getInteger("X", 0, 5, false)).thenReturn(5);
        assertEquals(ManaXChoice.choose(gui, "X", 0, 5,
                () -> { fail("No Other option"); return 0; }), Integer.valueOf(5));
    }
}
