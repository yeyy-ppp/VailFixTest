package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SerializationExceptionTest {

    @Test
    void testDefaultConstructor() {
        SerializationException exception = new SerializationException();
        assertAll(
                () -> assertNull(exception.getMessage(), "Message should be null"),
                () -> assertNull(exception.getCause(), "Cause should be null")
        );
    }

    @Test
    void testMessageConstructor() {
        String testMessage = "Test exception message";
        SerializationException exception = new SerializationException(testMessage);
        assertAll(
                () -> assertEquals(testMessage, exception.getMessage(), "Message mismatch"),
                () -> assertNull(exception.getCause(), "Cause should be null")
        );
    }

    @Test
    void testMessageWithCauseConstructor() {
        String testMessage = "Test exception message";
        Throwable testCause = new RuntimeException("Test cause");
        SerializationException exception = new SerializationException(testMessage, testCause);
        assertAll(
                () -> assertEquals(testMessage, exception.getMessage(), "Message mismatch"),
                () -> assertEquals(testCause, exception.getCause(), "Cause mismatch")
        );
    }

    @Test
    void testCauseConstructor() {
        Throwable testCause = new RuntimeException("Test cause");
        SerializationException exception = new SerializationException(testCause);
        assertAll(
                () -> assertTrue(exception.getMessage().contains("Test cause"), "Message should contain cause info"),
                () -> assertEquals(testCause, exception.getCause(), "Cause mismatch")
        );
    }

    @Test
    void testNullCauseConstructor() {
        SerializationException exception = new SerializationException((Throwable) null);
        assertAll(
                () -> assertNull(exception.getMessage(), "Message should be null"),
                () -> assertNull(exception.getCause(), "Cause should be null")
        );
    }

    @Test
    void testNullMessageConstructor() {
        SerializationException exception = new SerializationException((String) null);
        assertAll(
                () -> assertNull(exception.getMessage(), "Message should be null"),
                () -> assertNull(exception.getCause(), "Cause should be null")
        );
    }

    @Test
    void testNullMessageWithCauseConstructor() {
        Throwable testCause = new RuntimeException("Test cause");
        SerializationException exception = new SerializationException(null, testCause);
        assertAll(
                () -> assertNull(exception.getMessage(), "Message should be null"),
                () -> assertEquals(testCause, exception.getCause(), "Cause mismatch")
        );
    }

    @Test
    void testEmptyMessageConstructor() {
        SerializationException exception = new SerializationException("");
        assertAll(
                () -> assertEquals("", exception.getMessage(), "Empty message should be preserved"),
                () -> assertNull(exception.getCause(), "Cause should be null")
        );
    }

    @Test
    void testConstructor1() {
        SerializationException exception = new SerializationException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor2() {
        SerializationException exception = new SerializationException("msg");
        assertEquals("msg", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor3() {
        Throwable cause = new RuntimeException();
        SerializationException exception = new SerializationException("msg", cause);
        assertEquals("msg", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructor4() {
        Throwable cause = new RuntimeException();
        SerializationException exception = new SerializationException(cause);
        assertTrue(exception.getMessage().contains(cause.toString()));
        assertEquals(cause, exception.getCause());
    }

}