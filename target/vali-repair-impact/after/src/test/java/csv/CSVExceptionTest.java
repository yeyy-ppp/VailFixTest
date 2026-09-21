package csv;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.IllegalFormatException;

class CSVExceptionTest {

    @Test
    void testCSVExceptionWithValidFormatArgs() {
        CSVException exception = new CSVException("Error: %s", "file not found");
        assertEquals("Error: file not found", exception.getMessage());
    }

    @Test
    void testCSVExceptionWithEmptyArgs() {
        CSVException exception = new CSVException("Empty message");
        assertEquals("Empty message", exception.getMessage());
    }

    @Test
    void testCSVExceptionWithNullFormat() {
        assertThrows(NullPointerException.class, () -> 
            new CSVException(null, "arg")
        );
    }

    @Test
    void testCSVExceptionWithInvalidFormat() {
        assertThrows(IllegalFormatException.class, () ->
            new CSVException("Invalid format %d", "text")
        );
    }

    @Test
    void testCSVExceptionWithInsufficientArguments() {
        assertThrows(IllegalFormatException.class, () ->
            new CSVException("Missing %d %s")
        );
    }
}