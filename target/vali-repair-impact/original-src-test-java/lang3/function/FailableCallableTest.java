package lang3.function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FailableCallableTest {

    @Test
    void testCall_SuccessfulExecution() throws Exception {
        FailableCallable<String, Exception> callable = () -> "test success";
        String result = callable.call();
        assertEquals("test success", result);
    }

    @Test
    void testCall_ThrowsException() {
        FailableCallable<String, Exception> callable = () -> {
            throw new Exception("test failure");
        };
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                callable.call();
            } catch (Exception e) {
                throw e;
            }
        });
        assertEquals("test failure", exception.getMessage());
    }
}