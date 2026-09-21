package lang3.concurrent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UncheckedExecutionExceptionTest {

    @Test
    void testConstructorWithNullCause() {
        UncheckedExecutionException exception = new UncheckedExecutionException(null);
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNonNullCause() {
        Throwable cause = new Exception("Test exception");
        UncheckedExecutionException exception = new UncheckedExecutionException(cause);
        assertSame(cause, exception.getCause());
    }
}