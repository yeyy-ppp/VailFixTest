package lang3;

import org.junit.jupiter.api.Test;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;

public class LongRangeTest {

    @Test
    void testOfWithPrimitives() {
        LongRange range = LongRange.of(10L, 20L);
        assertEquals(10L, range.getMinimum());
        assertEquals(20L, range.getMaximum());
    }

    @Test
    void testFit() {
        LongRange range = LongRange.of(10L, 20L);
        assertEquals(10L, range.fit(5L));
        assertEquals(15L, range.fit(15L));
        assertEquals(20L, range.fit(25L));
    }

    @Test
    void testToLongStream() {
        LongRange range = LongRange.of(10L, 15L);
        LongStream stream = range.toLongStream();
        assertArrayEquals(new long[]{10L, 11L, 12L, 13L, 14L, 15L}, stream.toArray());
    }

    @Test
    void testGetMinimumAndMaximum() {
        LongRange range = LongRange.of(5L, 15L);
        assertEquals(5L, range.getMinimum());
        assertEquals(15L, range.getMaximum());
    }

    @Test
    void testContains() {
        LongRange range = LongRange.of(10L, 20L);
        assertTrue(range.contains(10L));
        assertTrue(range.contains(15L));
        assertTrue(range.contains(20L));
        assertFalse(range.contains(9L));
        assertFalse(range.contains(21L));
    }
}
