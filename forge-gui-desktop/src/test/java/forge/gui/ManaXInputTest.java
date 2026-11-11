package forge.gui;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ManaXInputTest {
    @Test public void invalidOrOverflowingInputCannotConfirm() {
        for (String value : new String[]{"", "abc", "2.5", "-1", "999999999999999999999999"}) {
            assertNull(ManaXInput.parse(value, 0, Integer.MAX_VALUE));
        }
        assertNull(ManaXInput.parse("5", 6, 10));
        assertNull(ManaXInput.parse("11", 6, 10));
        assertEquals(ManaXInput.parse(" 9001 ", 0, Integer.MAX_VALUE), Integer.valueOf(9001));
        assertEquals(ManaXInput.parse("0", 0, Integer.MAX_VALUE), Integer.valueOf(0));
    }
}
