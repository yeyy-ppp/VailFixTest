package csv;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void testConstructorAccessibility() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
    }

    @Test
    void testConstructorInvocation() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void testConstantsConstructor() throws Exception {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testConstantValues() {
        assertEquals('\\', Constants.BACKSLASH);
        assertEquals('\b', Constants.BACKSPACE);
        assertEquals(",", Constants.COMMA);
        assertEquals('#', Constants.COMMENT);
        assertEquals('\r', Constants.CR);
        assertEquals("\r\n", Constants.CRLF);
        assertEquals('"', Constants.DOUBLE_QUOTE_CHAR);
        assertEquals("", Constants.EMPTY);
        assertArrayEquals(new String[]{}, Constants.EMPTY_STRING_ARRAY);
        assertEquals(-1, Constants.EOF);
        assertEquals('\f', Constants.FF);
        assertEquals('\n', Constants.LF);
        assertEquals("\u2028", Constants.LINE_SEPARATOR);
        assertEquals("\u0085", Constants.NEXT_LINE);
        assertEquals("\u2029", Constants.PARAGRAPH_SEPARATOR);
        assertEquals('|', Constants.PIPE);
        assertEquals((char)30, Constants.RS);
        assertEquals(' ', Constants.SP);
        assertEquals("\\N", Constants.SQL_NULL_STRING);
        assertEquals('\t', Constants.TAB);
        assertEquals(-2, Constants.UNDEFINED);
        assertEquals((char)31, Constants.US);
    }
}
