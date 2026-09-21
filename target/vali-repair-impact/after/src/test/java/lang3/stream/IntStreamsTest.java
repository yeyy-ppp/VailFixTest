package lang3.stream;

import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import lang3.stream.IntStreams;

public class IntStreamsTest {

    @Test
    void testOfWithNull() {
        IntStream stream = IntStreams.of(null);
        assertEquals(0L, stream.count());
    }

    @Test
    void testOfWithEmptyArray() {
        IntStream stream = IntStreams.of();
        assertArrayEquals(new int[]{}, stream.toArray());
    }

    @Test
    void testOfWithSingleValue() {
        IntStream stream = IntStreams.of(5);
        assertArrayEquals(new int[]{5}, stream.toArray());
    }

    @Test
    void testOfWithMultipleValues() {
        IntStream stream = IntStreams.of(1, 2, 3);
        assertArrayEquals(new int[]{1, 2, 3}, stream.toArray());
    }

    @Test
    void testRangeWithNegative() {
        IntStream stream = IntStreams.range(-5);
        assertEquals(0L, stream.count());
    }

    @Test
    void testRangeWithZero() {
        IntStream stream = IntStreams.range(0);
        assertEquals(0L, stream.count());
    }

    @Test
    void testRangeWithPositive() {
        IntStream stream = IntStreams.range(3);
        assertArrayEquals(new int[]{0, 1, 2}, stream.toArray());
    }

    @Test
    void testRangeClosedWithNegative() {
        IntStream stream = IntStreams.rangeClosed(-5);
        assertEquals(0L, stream.count());
    }

    @Test
    void testRangeClosedWithZero() {
        IntStream stream = IntStreams.rangeClosed(0);
        assertArrayEquals(new int[]{0}, stream.toArray());
    }

    @Test
    void testRangeClosedWithPositive() {
        IntStream stream = IntStreams.rangeClosed(3);
        assertArrayEquals(new int[]{0, 1, 2, 3}, stream.toArray());
    }

    @Test
    void testIntStreamsConstructor() {
        new IntStreams();
    }
}