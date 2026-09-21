package codeing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class FixTest2 {
    @Test
    void testAdd() {
        Example example = new Example();
        assertEquals(5, example.add(2, 3));
    }
    @Test
    void testCharAt() {
        Fix example = new Fix();
assertEquals('v', example.charAt("java", 2));
    }
    @Test
    void testDivide() {
        Fix example = new Fix();
        assertEquals(2, example.divide(6, 3));
        assertThrows(ArithmeticException.class, () -> example.divide(6, 0));
    }
}
