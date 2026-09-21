package codeing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
public class AddExampleTest {
@Test
    public void testAddTwoPositiveNumbers() {
        AddExample example = new AddExample();
        int result = example.add(5, 3);
        assertEquals(8, result);
    }
    @Test
    public void testAddTwoNegativeNumbers() {
        AddExample example = new AddExample();
        int result = example.add(-5, -3);
        assertEquals(-8, result);
    }
    @Test
    public void testAddPositiveAndNegative() {
        AddExample example = new AddExample();
        int result = example.add(-5, 3);
        assertEquals(-2, result);
    }
    @Test
    public void testAddWithZero() {
        AddExample example = new AddExample();
        int result = example.add(0, 0);
        assertEquals(0, result);
    }
    @Test
    public void testAddPositiveWithZero() {
        AddExample example = new AddExample();
        int result = example.add(7, 0);
        assertEquals(7, result);
    }
    @Test
    public void testAddNegativeWithZero() {
        AddExample example = new AddExample();
        int result = example.add(-7, 0);
        assertEquals(-7, result);
    }
    @Test
    public void testAddToMaxValue() {
        AddExample example = new AddExample();
        int result = example.add(Integer.MAX_VALUE, 0);
        assertEquals(Integer.MAX_VALUE, result);
    }
    @Test
    public void testAddToMinValue() {
        AddExample example = new AddExample();
        int result = example.add(Integer.MIN_VALUE, 0);
        assertEquals(Integer.MIN_VALUE, result);
    }
}