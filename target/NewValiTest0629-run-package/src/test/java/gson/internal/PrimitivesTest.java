package gson.internal;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class PrimitivesTest {

    @Test
    void testConstructorAccessibility() throws Exception {
        Constructor<Primitives> constructor = Primitives.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void testIsPrimitiveWithPrimitiveTypes() {
        assertTrue(Primitives.isPrimitive(int.class));
        assertTrue(Primitives.isPrimitive(float.class));
        assertTrue(Primitives.isPrimitive(byte.class));
        assertTrue(Primitives.isPrimitive(double.class));
        assertTrue(Primitives.isPrimitive(long.class));
        assertTrue(Primitives.isPrimitive(char.class));
        assertTrue(Primitives.isPrimitive(boolean.class));
        assertTrue(Primitives.isPrimitive(short.class));
        assertTrue(Primitives.isPrimitive(void.class));
    }

    @Test
    void testIsPrimitiveWithNonPrimitiveTypes() {
        assertFalse(Primitives.isPrimitive(Integer.class));
        assertFalse(Primitives.isPrimitive(String.class));
        assertFalse(Primitives.isPrimitive(Object.class));
        assertFalse(Primitives.isPrimitive(null));
    }

    @Test
    void testIsWrapperTypeWithWrapperTypes() {
        assertTrue(Primitives.isWrapperType(Integer.class));
        assertTrue(Primitives.isWrapperType(Float.class));
        assertTrue(Primitives.isWrapperType(Byte.class));
        assertTrue(Primitives.isWrapperType(Double.class));
        assertTrue(Primitives.isWrapperType(Long.class));
        assertTrue(Primitives.isWrapperType(Character.class));
        assertTrue(Primitives.isWrapperType(Boolean.class));
        assertTrue(Primitives.isWrapperType(Short.class));
        assertTrue(Primitives.isWrapperType(Void.class));
    }

    @Test
    void testIsWrapperTypeWithNonWrapperTypes() {
        assertFalse(Primitives.isWrapperType(int.class));
        assertFalse(Primitives.isWrapperType(String.class));
        assertFalse(Primitives.isWrapperType(Object.class));
        assertFalse(Primitives.isWrapperType(null));
    }

    @Test
    void testWrapPrimitiveTypes() {
        assertEquals(Integer.class, Primitives.wrap(int.class));
        assertEquals(Float.class, Primitives.wrap(float.class));
        assertEquals(Byte.class, Primitives.wrap(byte.class));
        assertEquals(Double.class, Primitives.wrap(double.class));
        assertEquals(Long.class, Primitives.wrap(long.class));
        assertEquals(Character.class, Primitives.wrap(char.class));
        assertEquals(Boolean.class, Primitives.wrap(boolean.class));
        assertEquals(Short.class, Primitives.wrap(short.class));
        assertEquals(Void.class, Primitives.wrap(void.class));
    }
}