package lang3.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UncheckedInterruptedExceptionTest {

    @Test
    void testConstructorWithNonNullCause() {
        Throwable cause = new InterruptedException("Test interruption");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithNullCause() {
        UncheckedInterruptedException exception = new UncheckedInterruptedException(null);
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithDifferentExceptionCause() {
        Throwable cause = new IllegalArgumentException("Invalid argument");
        UncheckedInterruptedException exception = new UncheckedInterruptedException(cause);
        assertSame(cause, exception.getCause());
    }
}