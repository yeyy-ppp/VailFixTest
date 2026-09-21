package lang3.function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FailableConsumerTest {

    @Test
    void testNopDoesNotThrowException() throws Throwable {
        final Object input = new Object();
        FailableConsumer.nop().accept(input);
    }

    @Test
    void testNopInstanceInvariance() {
        assertSame(FailableConsumer.nop(), FailableConsumer.nop());
    }

    @Test
    void testAcceptExecutesBehavior() throws Throwable {
        final StringBuilder result = new StringBuilder();
        FailableConsumer<String, RuntimeException> consumer = s -> result.append(s);
        consumer.accept("TEST");
        assertEquals("TEST", result.toString());
    }

    @Test
    void testAndThenSequenceExecution() throws Throwable {
        final StringBuilder buffer = new StringBuilder();
        FailableConsumer<String, RuntimeException> first = s -> buffer.append("A");
        FailableConsumer<String, RuntimeException> second = s -> buffer.append("B");

        first.andThen(second).accept("IGNORED");
        assertEquals("AB", buffer.toString());
    }

    @Test
    void testAndThenWithNullParamThrowsNPE() {
        final FailableConsumer<Object, RuntimeException> consumer = o -> {};
        assertThrows(NullPointerException.class, () -> consumer.andThen(null));
    }

    @Test
    void testChainedConsumersExecutionOrder() throws Throwable {
        final int[] counter = {0};
        FailableConsumer<Integer, RuntimeException> increment = i -> counter[0]++;
        FailableConsumer<Integer, RuntimeException> doubler = i -> counter[0] *= 2;

        increment.andThen(doubler).accept(0);
        assertEquals(2, counter[0]);
    }
}
