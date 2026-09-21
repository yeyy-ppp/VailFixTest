package lang3.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UncheckedExceptionTest {

    @Test
    void testConstructorWithNullCause() {
        UncheckedException exception = new UncheckedException(null);
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNonNullCause() {
        Throwable cause = new RuntimeException("Test cause");
        UncheckedException exception = new UncheckedException(cause);
        assertSame(cause, exception.getCause());
    }
}