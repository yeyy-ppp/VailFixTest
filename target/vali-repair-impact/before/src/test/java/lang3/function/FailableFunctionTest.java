package lang3.function;

import lang3.function.FailableFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FailableFunctionTest {

    @Test
    void testFunction() {
        FailableFunction<Object, Object, Exception> func = input -> input;
        FailableFunction<Object, Object, Exception> result = FailableFunction.function(func);
        assertSame(func, result);
    }

    @Test
    void testIdentity() throws Exception {
        FailableFunction<String, String, Exception> identity = FailableFunction.identity();
        String input = "test";
        String result = identity.apply(input);
        assertEquals(input, result);
    }

    @Test
    void testNop() throws Exception {
        FailableFunction<Object, Object, Exception> nop = FailableFunction.nop();
        Object input = new Object();
        Object result = nop.apply(input);
        assertNull(result);
    }

    @Test
    void testAndThen() throws Exception {
        FailableFunction<String, Integer, Exception> parseInt = Integer::valueOf;
        FailableFunction<Integer, String, Exception> toString = Object::toString;
        
        FailableFunction<String, String, Exception> composed = parseInt.andThen(toString);
        String result = composed.apply("10");
        assertEquals("10", result);
    }

    @Test
    void testAndThenWithNull() {
        FailableFunction<String, Integer, Exception> func = Integer::valueOf;
        assertThrows(NullPointerException.class, () -> func.andThen(null));
    }

    @Test
    void testApplyThrowsException() {
        FailableFunction<String, Object, Exception> func = input -> {
            throw new Exception("test exception");
        };
        assertThrows(Exception.class, () -> func.apply("input"));
    }
}