package gson.internal;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class NumberLimitsTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        Constructor<NumberLimits> constructor = NumberLimits.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testCheckNumberStringLengthWithinLimit() throws Exception {
        Method method = NumberLimits.class.getDeclaredMethod("checkNumberStringLength", String.class);
        method.setAccessible(true);
        method.invoke(null, new String(new char[10000]).replace('\0', '0'));
    }

    @Test
    void testCheckNumberStringLengthExceedsLimit() throws Exception {
        Method method = NumberLimits.class.getDeclaredMethod("checkNumberStringLength", String.class);
        method.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, 
            () -> method.invoke(null, new String(new char[10001]).replace('\0', '0')));
        assertTrue(ex.getCause().getMessage().startsWith("Number string too large:"));
    }

    @Test
    void testParseBigDecimalValid() {
        assertNotNull(NumberLimits.parseBigDecimal("1e-9999"));
    }

    @Test
    void testParseBigDecimalExceedsLength() {
        NumberFormatException ex = assertThrows(NumberFormatException.class,
            () -> NumberLimits.parseBigDecimal(new String(new char[10001]).replace('\0', '0')));
        assertTrue(ex.getMessage().startsWith("Number string too large:"));
    }

    @Test
    void testParseBigDecimalScaleExceedsLimit() {
        NumberFormatException ex = assertThrows(NumberFormatException.class,
            () -> NumberLimits.parseBigDecimal("1e-10000"));
        assertTrue(ex.getMessage().startsWith("Number has unsupported scale:"));
    }

    @Test
    void testParseBigIntegerValid() {
        assertNotNull(NumberLimits.parseBigInteger("1234567890"));
    }

    @Test
    void testParseBigIntegerExceedsLength() {
        NumberFormatException ex = assertThrows(NumberFormatException.class,
            () -> NumberLimits.parseBigInteger(new String(new char[10001]).replace('\0', '0')));
        assertTrue(ex.getMessage().startsWith("Number string too large:"));
    }

}
