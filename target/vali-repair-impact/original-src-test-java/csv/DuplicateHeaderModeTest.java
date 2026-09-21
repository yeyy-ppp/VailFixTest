package csv;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

public class DuplicateHeaderModeTest {

    @Test
    void testValues() {
        DuplicateHeaderMode[] values = DuplicateHeaderMode.values();
        assertEquals(3, values.length);
        EnumSet<DuplicateHeaderMode> set = EnumSet.allOf(DuplicateHeaderMode.class);
        assertTrue(set.contains(DuplicateHeaderMode.ALLOW_ALL));
        assertTrue(set.contains(DuplicateHeaderMode.ALLOW_EMPTY));
        assertTrue(set.contains(DuplicateHeaderMode.DISALLOW));
    }

    @Test
    void testValueOf() {
        assertEquals(DuplicateHeaderMode.ALLOW_ALL, DuplicateHeaderMode.valueOf("ALLOW_ALL"));
        assertEquals(DuplicateHeaderMode.ALLOW_EMPTY, DuplicateHeaderMode.valueOf("ALLOW_EMPTY"));
        assertEquals(DuplicateHeaderMode.DISALLOW, DuplicateHeaderMode.valueOf("DISALLOW"));
    }

    @Test
    void testValueOfCaseSensitivity() {
        assertThrows(IllegalArgumentException.class, () -> DuplicateHeaderMode.valueOf("allow_all"));
        assertThrows(IllegalArgumentException.class, () -> DuplicateHeaderMode.valueOf("DISalLow"));
    }

    @Test
    void testValueOfNonExistent() {
        assertThrows(IllegalArgumentException.class, () -> DuplicateHeaderMode.valueOf("NON_EXISTENT"));
    }
}