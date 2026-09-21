package lang3.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CloneFailedExceptionTest {

    @Test
    void testConstructorWithMessageAndCause() {
        String expectedMessage = "Test message";
        Throwable cause = new RuntimeException();
        CloneFailedException exception = new CloneFailedException(expectedMessage, cause);
        assertAll(
            () -> assertEquals(expectedMessage, exception.getMessage()),
            () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    void testConstructorWithNullMessageAndCause() {
        Throwable cause = new IllegalArgumentException();
        CloneFailedException exception = new CloneFailedException(null, cause);
        assertAll(
            () -> assertNull(exception.getMessage()),
            () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    void testConstructorWithMessageAndNullCause() {
        String expectedMessage = "Another message";
        CloneFailedException exception = new CloneFailedException(expectedMessage, null);
        assertAll(
            () -> assertEquals(expectedMessage, exception.getMessage()),
            () -> assertNull(exception.getCause())
        );
    }

    @Test
    void testConstructorWithNullMessageAndNullCause() {
        CloneFailedException exception = new CloneFailedException(null, null);
        assertAll(
            () -> assertNull(exception.getMessage()),
            () -> assertNull(exception.getCause())
        );
    }

    static Stream<Arguments> causeProvider() {
        return Stream.of(
            Arguments.of(new NullPointerException()),
            Arguments.of(new IllegalStateException("Error state")),
            Arguments.of((Throwable) null)
        );
    }

   /* @ParameterizedTest
    @MethodSource("causeProvider")
    void testConstructorWithCause(Throwable cause) {
        CloneFailedException exception = new CloneFailedException(null, cause);
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }*/
}