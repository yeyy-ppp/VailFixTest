package cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ParseExceptionTest {

    @Test
    void testWrap_UnsupportedOperationException() {
        UnsupportedOperationException input = new UnsupportedOperationException();
        assertThrows(UnsupportedOperationException.class, () -> ParseException.wrap(input));
    }

    @Test
    void testWrap_ParseException() {
        ParseException input = new ParseException("Test");
        try {
            ParseException result = ParseException.wrap(input);
            assertSame(input, result);
        } catch (UnsupportedOperationException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    void testWrap_OtherException() {
        RuntimeException input = new RuntimeException("Test");
        try {
            ParseException result = ParseException.wrap(input);
            assertNotNull(result);
            assertSame(input, result.getCause());
        } catch (UnsupportedOperationException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    void testConstructorWithMessage() {
        String message = "Test message";
        ParseException exception = new ParseException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithThrowable() {
        Throwable cause = new RuntimeException("Cause");
        ParseException exception = new ParseException(cause);
        assertSame(cause, exception.getCause());
        assertEquals(cause.toString(), exception.getMessage());
    }
}