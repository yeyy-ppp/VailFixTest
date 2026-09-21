package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnrecognizedOptionExceptionTest {

    @Test
    void testConstructorWithMessageOnly() {
        final String message = "test message";
        UnrecognizedOptionException ex = new UnrecognizedOptionException(message);
        assertNull(ex.getOption());
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testConstructorWithMessageAndOption() {
        final String message = "invalid option";
        final String option = "--invalid";
        UnrecognizedOptionException ex = new UnrecognizedOptionException(message, option);
        assertEquals(option, ex.getOption());
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testConstructorWithEmptyOption() {
        final String message = "empty option";
        final String option = "";
        UnrecognizedOptionException ex = new UnrecognizedOptionException(message, option);
        assertEquals(option, ex.getOption());
        assertEquals(message, ex.getMessage());
    }
}