package lang3.function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FailableRunnableTest {

    @Test
    void testRunWithoutException() {
        FailableRunnable<Exception> runnable = () -> {};
        Failable.run(runnable);
    }

    @Test
    void testRunWithException() {
        FailableRunnable<Exception> runnable = () -> {
            throw new Exception("Test exception");
        };
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> Failable.run(runnable));
        assertEquals("Test exception", thrown.getCause().getMessage());
    }

    @Test
    void testRunWithRuntimeException() {
        FailableRunnable<RuntimeException> runnable = () -> {
            throw new RuntimeException("Test runtime exception");
        };
        assertThrows(RuntimeException.class, runnable::run);
    }
}