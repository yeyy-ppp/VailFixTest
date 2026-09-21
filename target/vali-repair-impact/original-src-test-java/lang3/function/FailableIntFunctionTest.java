package lang3.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class FailableIntFunctionTest {

    @Test
    void testNopApply() throws Throwable {
        FailableIntFunction<Object, Throwable> nopFunction = FailableIntFunction.nop();
        Object result = nopFunction.apply(0);
        assertNull(result);
    }

    @Test
    void testApplyBasic() throws Throwable {
        FailableIntFunction<String, RuntimeException> function = input -> "Value:" + input;
        String result = function.apply(42);
        assertEquals("Value:42", result);
    }

    @Test
    void testApplyBoundaryMin() throws Exception {
        FailableIntFunction<Integer, Exception> function = input -> input;
        int result = function.apply(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    void testApplyBoundaryMax() throws Throwable {
        FailableIntFunction<Integer, Error> function = input -> input;
        int result = function.apply(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void testApplyZero() throws Throwable {
        FailableIntFunction<String, Throwable> function = input -> input == 0 ? "Zero" : "NonZero";
        String result = function.apply(0);
        assertEquals("Zero", result);
    }
}