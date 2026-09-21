package codeing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FixTest {

    @Test
    public void addPositiveNumbers() {
        Fix fix = new Fix();
        assertEquals(5, fix.add(2, 3));
    }

    @Test
    public void addNegativeAndPositive() {
        Fix fix = new Fix();
        assertEquals(-1, fix.add(2, -3));
    }

    @Test
    public void addNegativeNumbers() {
        Fix fix = new Fix();
        assertEquals(-5, fix.add(-2, -3));
    }

    @Test
    public void charAtValidIndex() {
        Fix fix = new Fix();
        assertEquals('e', fix.charAt("test", 1));
    }

    @Test
    public void charAtNegativeIndex() {
        Fix fix = new Fix();
        assertThrows(StringIndexOutOfBoundsException.class, () -> fix.charAt("test", -1));
    }

    @Test
    public void charAtOutOfBoundsIndex() {
        Fix fix = new Fix();
        assertThrows(StringIndexOutOfBoundsException.class, () -> fix.charAt("test", 4));
    }

    @Test
    public void divideValidResult() {
        Fix fix = new Fix();
        assertEquals(2, fix.divide(6, 3));
    }

    @Test
    public void divideByZeroThrowsException() {
        Fix fix = new Fix();
        assertThrows(ArithmeticException.class, () -> fix.divide(5, 0));
    }

    @Test
    public void divideNegativeResult() {
        Fix fix = new Fix();
        assertEquals(-4, fix.divide(-8, 2));
    }

    @Test
    public void divideNegativeDivisor() {
        Fix fix = new Fix();
        assertEquals(-3, fix.divide(9, -3));
    }
}