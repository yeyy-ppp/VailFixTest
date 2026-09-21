package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonParseExceptionTest {

    @Test
    void testJsonParseExceptionWithMessage() {
        String errorMessage = "Test error message";
        JsonParseException exception = new JsonParseException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithNullMessage() {
        JsonParseException exception = new JsonParseException((String) null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithEmptyMessage() {
        String errorMessage = "";
        JsonParseException exception = new JsonParseException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithMessageAndCause() {
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        JsonParseException exception = new JsonParseException(errorMessage, cause);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithMessageAndNullCause() {
        String errorMessage = "Test error message";
        JsonParseException exception = new JsonParseException(errorMessage, null);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithNullMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        JsonParseException exception = new JsonParseException(null, cause);
        assertNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithNullMessageAndNullCause() {
        JsonParseException exception = new JsonParseException(null, null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        JsonParseException exception = new JsonParseException(cause);
        assertEquals(cause.toString(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testJsonParseExceptionWithNullCause() {
        JsonParseException exception = new JsonParseException((Throwable) null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }
}