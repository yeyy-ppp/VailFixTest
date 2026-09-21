package codeing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ExampleTest {

    @Test
    public void testAddPositiveNumbers() {
        Example example = new Example();
        assertEquals(3, example.add(1, 2));
    }

    @Test
    public void testAddNegativeNumbers() {
        Example example = new Example();
        assertEquals(-3, example.add(-1, -2));
    }

    @Test
    public void testAddPositiveAndNegative() {
        Example example = new Example();
        assertEquals(2, example.add(5, -3));
    }

    @Test
    public void testAddZeroValues() {
        Example example = new Example();
        assertEquals(0, example.add(0, 0));
    }

    @Test
    public void testAddMaxIntBoundary() {
        Example example = new Example();
        assertEquals(Integer.MAX_VALUE, example.add(Integer.MAX_VALUE, 0));
    }

    @Test
    public void testAddMinIntBoundary() {
        Example example = new Example();
        assertEquals(Integer.MIN_VALUE, example.add(Integer.MIN_VALUE, 0));
    }

    @Test
    public void testAddOverflowResult() {
        Example example = new Example();
        assertEquals(-2147483648, example.add(Integer.MAX_VALUE, 1));
    }

    @Test
    public void testAddUnderflowResult() {
        Example example = new Example();
        assertEquals(Integer.MAX_VALUE, example.add(Integer.MIN_VALUE, -1));
    }
}