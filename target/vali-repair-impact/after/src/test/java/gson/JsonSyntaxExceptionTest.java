package gson;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JsonSyntaxExceptionTest {

    @Test
    void testConstructorWithMessage() {
        final String expectedMessage = "Test error message";
        JsonSyntaxException exception = new JsonSyntaxException(expectedMessage);
        
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        final String expectedMessage = "Test error with cause";
        final Throwable expectedCause = new RuntimeException("Root cause");
        JsonSyntaxException exception = new JsonSyntaxException(expectedMessage, expectedCause);
        
        assertEquals(expectedMessage, exception.getMessage());
        assertSame(expectedCause, exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        final Throwable expectedCause = new IllegalStateException("Internal error");
        JsonSyntaxException exception = new JsonSyntaxException(expectedCause);
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(expectedCause.toString()));
        assertSame(expectedCause, exception.getCause());
    }

    @Test
    void testConstructorWithNullCause() {
        JsonSyntaxException exception = new JsonSyntaxException((Throwable) null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessageAndCause() {
        Throwable cause = new NullPointerException();
        JsonSyntaxException exception = new JsonSyntaxException(null, cause);
        
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}