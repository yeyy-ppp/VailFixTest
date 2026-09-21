package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MissingArgumentExceptionTest {

    @Test
    void testConstructorWithOption() {
        Option option = new Option("f", "file option");
        MissingArgumentException exception = new MissingArgumentException(option);
        assertEquals(option, exception.getOption());
        assertTrue(exception.getMessage().contains(option.getKey()));
    }

    @Test
    void testConstructorWithMessage() {
        String message = "test message";
        MissingArgumentException exception = new MissingArgumentException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getOption());
    }

    @Test
    void testGetOption() {
        Option option = new Option("f", "file option");
        MissingArgumentException exception = new MissingArgumentException(option);
        assertEquals(option, exception.getOption());
    }
}