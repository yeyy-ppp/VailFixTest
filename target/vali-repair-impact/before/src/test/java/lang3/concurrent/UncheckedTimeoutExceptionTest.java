package lang3.concurrent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UncheckedTimeoutExceptionTest {

    @Test
    void testConstructorWithNonNullCause() {
        final Throwable expectedCause = new RuntimeException("Test cause");
        final UncheckedTimeoutException exception = new UncheckedTimeoutException(expectedCause);
        assertSame(expectedCause, exception.getCause());
    }

    @Test
    void testConstructorWithNullCause() {
        final UncheckedTimeoutException exception = new UncheckedTimeoutException(null);
        assertNull(exception.getCause());
    }
}