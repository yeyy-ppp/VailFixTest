package lang3.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.lang.reflect.Type;

public class TypedTest {

    @Test
    public void testGetTypeWithNonNull() {
        Typed<?> typed = () -> String.class;
        Type result = typed.getType();
        Assertions.assertEquals(String.class, result);
    }

    @Test
    public void testGetTypeWithAnotherType() {
        Typed<?> typed = () -> Integer.class;
        Type result = typed.getType();
        Assertions.assertEquals(Integer.class, result);
    }

    @Test
    public void testGetTypeWithNull() {
        Typed<?> typed = () -> null;
        Type result = typed.getType();
        Assertions.assertNull(result);
    }
}