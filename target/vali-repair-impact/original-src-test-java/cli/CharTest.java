package cli;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

public class CharTest {

    @Test
    void testStaticFieldValues() {
        assertEquals('\'', Char.APOS);
        assertEquals('\r', Char.CR);
        assertEquals('=', Char.EQUAL);
        assertEquals('\n', Char.LF);
        assertEquals(' ', Char.SP);
        assertEquals('\t', Char.TAB);
    }
    
    @Test
    void testConstructorViaReflection() throws Exception {
        Constructor<Char> constructor = Char.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Char instance = constructor.newInstance();
        assertNotNull(instance);
    }
    
    @Test
    void testConstructorVisibility() throws Exception {
        Constructor<Char> constructor = Char.class.getDeclaredConstructor();
      //  assertFalse(constructor.canAccess(null));
    }
    
    @Test
    void testConstructorAccessibility() throws Exception {
        Constructor<Char> constructor = Char.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        assertTrue(constructor.isAccessible());
    }
}