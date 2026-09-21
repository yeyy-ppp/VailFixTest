package gson.internal;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;

class LazilyParsedNumberTest {

    @Test
    void testConstructorAndToString() {
        LazilyParsedNumber number = new LazilyParsedNumber("123");
        assertEquals("123", number.toString());
    }

    @Test
    void testIntValueValidInt() {
        LazilyParsedNumber number = new LazilyParsedNumber("2147483647");
        assertEquals(2147483647, number.intValue());
    }

    @Test
    void testIntValueIntOverflowFallsToLong() {
        LazilyParsedNumber number = new LazilyParsedNumber("2147483648");
        assertEquals(-2147483648, number.intValue());
    }

    @Test
    void testIntValueLongOverflowFallsToBigDecimal() {
        LazilyParsedNumber number = new LazilyParsedNumber("9223372036854775808");
        assertEquals(0, number.intValue());
    }

    @Test
    void testIntValueDecimalFallsToBigDecimal() {
        LazilyParsedNumber number = new LazilyParsedNumber("123.45");
        assertEquals(123, number.intValue());
    }

    @Test
    void testIntValueInvalidNumber() {
        LazilyParsedNumber number = new LazilyParsedNumber("invalid");
        assertThrows(NumberFormatException.class, number::intValue);
    }

    @Test
    void testLongValueValidLong() {
        LazilyParsedNumber number = new LazilyParsedNumber("9223372036854775807");
        assertEquals(9223372036854775807L, number.longValue());
    }

    @Test
    void testLongValueOverflowFallsToBigDecimal() {
        LazilyParsedNumber number = new LazilyParsedNumber("9223372036854775808");
        assertEquals(-9223372036854775808L, number.longValue());
    }

    @Test
    void testLongValueDecimalFallsToBigDecimal() {
        LazilyParsedNumber number = new LazilyParsedNumber("123.45");
        assertEquals(123L, number.longValue());
    }

    @Test
    void testLongValueInvalidNumber() {
        LazilyParsedNumber number = new LazilyParsedNumber("invalid");
        assertThrows(NumberFormatException.class, number::longValue);
    }

    @Test
    void testFloatValue() {
        LazilyParsedNumber number = new LazilyParsedNumber("123.45");
        assertEquals(123.45f, number.floatValue(), 0.001);
    }

    @Test
    void testFloatValueInfinity() {
        LazilyParsedNumber number = new LazilyParsedNumber("1e39");
        assertEquals(Float.POSITIVE_INFINITY, number.floatValue());
    }

    @Test
    void testFloatValueInvalid() {
        LazilyParsedNumber number = new LazilyParsedNumber("invalid");
        assertThrows(NumberFormatException.class, number::floatValue);
    }

    @Test
    void testDoubleValue() {
        LazilyParsedNumber number = new LazilyParsedNumber("123.45");
        assertEquals(123.45, number.doubleValue(), 0.001);
    }

    @Test
    void testDoubleValueInfinity() {
        LazilyParsedNumber number = new LazilyParsedNumber("1e309");
        assertEquals(Double.POSITIVE_INFINITY, number.doubleValue());
    }

    @Test
    void testDoubleValueInvalid() {
        LazilyParsedNumber number = new LazilyParsedNumber("invalid");
        assertThrows(NumberFormatException.class, number::doubleValue);
    }

    @Test
    void testWriteReplace() throws Exception {
        LazilyParsedNumber number = new LazilyParsedNumber("123.45");
        Method method = LazilyParsedNumber.class.getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        Object result = method.invoke(number);
        assertTrue(result instanceof BigDecimal);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void testHashCode() {
        LazilyParsedNumber number1 = new LazilyParsedNumber("123");
        LazilyParsedNumber number2 = new LazilyParsedNumber("123");
        assertEquals(number1.hashCode(), number2.hashCode());
        assertEquals("123".hashCode(), number1.hashCode());
    }

    @Test
    void testEqualsSameInstance() {
        LazilyParsedNumber number = new LazilyParsedNumber("123");
        assertTrue(number.equals(number));
    }

    @Test
    void testEqualsSameValue() {
        LazilyParsedNumber number1 = new LazilyParsedNumber("123");
        LazilyParsedNumber number2 = new LazilyParsedNumber("123");
        assertTrue(number1.equals(number2));
    }

    @Test
    void testEqualsDifferentValue() {
        LazilyParsedNumber number1 = new LazilyParsedNumber("123");
        LazilyParsedNumber number2 = new LazilyParsedNumber("456");
        assertFalse(number1.equals(number2));
    }

    @Test
    void testEqualsDifferentType() {
        LazilyParsedNumber number = new LazilyParsedNumber("123");
        Object other = new Object();
        assertFalse(number.equals(other));
    }

    @Test
    void testEqualsNull() {
        LazilyParsedNumber number = new LazilyParsedNumber("123");
        assertFalse(number.equals(null));
    }
}