package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class MissingOptionExceptionTest {

    @Test
    void constructorWithList_EmptyList_GeneratesPluralMessage() {
        MissingOptionException exception = new MissingOptionException(Collections.emptyList());
        assertEquals("Missing required options: ", exception.getMessage());
        assertTrue(exception.getMissingOptions().isEmpty());
    }

    @Test
    void constructorWithList_SingleOption_GeneratesSingularMessage() {
        List<String> options = Collections.singletonList("--file");
        MissingOptionException exception = new MissingOptionException(options);
        assertEquals("Missing required option: --file", exception.getMessage());
        assertEquals(options, exception.getMissingOptions());
    }

    @Test
    void constructorWithList_MultipleOptions_GeneratesCommaSeparatedMessage() {
        List<String> options = Arrays.asList("--input", "--output", "--verbose");
        MissingOptionException exception = new MissingOptionException(options);
        assertEquals("Missing required options: --input, --output, --verbose", exception.getMessage());
        assertEquals(options, exception.getMissingOptions());
    }

    @Test
    void constructorWithString_SetsMessageAndNullMissingOptions() {
        String message = "Custom error message";
        MissingOptionException exception = new MissingOptionException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getMissingOptions());
    }

    @Test
    void getMissingOptions_AfterListConstructor_ReturnsOriginalList() {
        List<String> options = Arrays.asList("--host", "--port");
        MissingOptionException exception = new MissingOptionException(options);
        assertEquals(options, exception.getMissingOptions());
    }

    @Test
    void getMissingOptions_AfterStringConstructor_ReturnsNull() {
        MissingOptionException exception = new MissingOptionException("Error occurred");
        assertNull(exception.getMissingOptions());
    }
}