package lang3.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FailableBiFunctionTest {

    @Test
    void testNop() throws Exception {
        FailableBiFunction<Object, Object, Object, Exception> nop = FailableBiFunction.nop();
        assertNull(nop.apply("input1", "input2"));
    }

    @Test
    void testAndThen_Success() throws Exception {
        FailableBiFunction<Integer, Integer, Integer, Exception> adder = (a, b) -> a + b;
        FailableFunction<Integer, Integer, Exception> multiplier = (x) -> x * 2;

        FailableBiFunction<Integer, Integer, Integer, Exception> combined = adder.andThen(multiplier);
        int result = combined.apply(3, 4);
        assertEquals(14, result);
    }

    @Test
    void testAndThen_WhenFirstFunctionReturnsNull() throws Exception {
        FailableBiFunction<String, String, String, Exception> nullProducer = (a, b) -> null;
        FailableFunction<String, Integer, Exception> lengthGetter = (s) -> s.length();

        FailableBiFunction<String, String, Integer, Exception> combined = nullProducer.andThen(lengthGetter);
        assertThrows(NullPointerException.class, () -> combined.apply("test", "input"));
    }

    @Test
    void testAndThen_WithNullAfter() {
        FailableBiFunction<String, String, String, Exception> func = (a, b) -> a + b;
        assertThrows(NullPointerException.class, () -> func.andThen(null));
    }

    @Test
    void testApply_Success() throws Exception {
        FailableBiFunction<String, String, String, Exception> concat = (a, b) -> a + b;
        String result = concat.apply("Hello", "World");
        assertEquals("HelloWorld", result);
    }

    @Test
    void testApply_ThrowsException() {
        FailableBiFunction<Object, Object, Object, Exception> exceptionThrower = (a, b) -> {
            throw new Exception("Test exception");
        };
        assertThrows(Exception.class, () -> exceptionThrower.apply(null, null));
    }
}
