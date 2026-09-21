package gson.stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

public class MalformedJsonExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String errorMessage = "Invalid JSON format";
        MalformedJsonException exception = new MalformedJsonException(errorMessage);
        
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String errorMessage = "Parsing error";
        Throwable cause = new RuntimeException("Root cause");
        MalformedJsonException exception = new MalformedJsonException(errorMessage, cause);
        
        assertEquals(errorMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new IOException("I/O failure");
        MalformedJsonException exception = new MalformedJsonException(cause);
        
        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}