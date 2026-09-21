package lang3.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ComputableTest {

    @Test
    void testCompute_ReturnsExpectedResult() throws InterruptedException {
        Computable<Integer, String> computable = arg -> String.valueOf(arg * 2);
        assertEquals("20", computable.compute(10));
    }

    @Test
    void testCompute_ThrowsInterruptedException() {
        Computable<Integer, String> computable = arg -> {
            throw new InterruptedException("Interrupted");
        };
        assertThrows(InterruptedException.class, () -> {
            computable.compute(5);
        });
    }

    @Test
    void testCompute_BoundaryZeroValue() throws InterruptedException {
        Computable<Integer, Integer> computable = arg -> arg * arg;
        assertEquals(0, computable.compute(0));
    }

    @Test
    void testCompute_NegativeValueHandling() throws InterruptedException {
        Computable<Integer, Integer> computable = arg -> Math.abs(arg);
        assertEquals(15, computable.compute(-15));
    }
}