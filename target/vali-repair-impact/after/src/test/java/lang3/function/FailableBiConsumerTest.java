package lang3.function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FailableBiConsumerTest {

    @Test
    void testNop() throws Exception {
        final boolean[] called = {false};
        FailableBiConsumer<Object, Object, Exception> nop = FailableBiConsumer.nop();
        
        nop.accept(new Object(), new Object());
        called[0] = true;
        
        assertTrue(called[0], "NOP should allow execution to continue");
    }

    @Test
    void testAccept() throws Exception {
        final boolean[] accepted = {false};
        FailableBiConsumer<String, Integer, Exception> consumer = (s, i) -> accepted[0] = true;
        
        consumer.accept("test", 42);
        
        assertTrue(accepted[0], "Consumer should set accepted flag");
    }

    @Test
    void testAndThenSequence() throws Exception {
        final StringBuilder sequence = new StringBuilder();
        FailableBiConsumer<String, String, Exception> first = (s1, s2) -> sequence.append("first");
        FailableBiConsumer<String, String, Exception> second = (s1, s2) -> sequence.append("second");
        
        first.andThen(second).accept("a", "b");
        
        assertTrue(sequence.toString().equals("firstsecond"), "Consumers should execute in sequence");
    }

    @Test
    void testAndThenWithNull() {
        FailableBiConsumer<Object, Object, Exception> consumer = (t, u) -> {};
        
        assertThrows(NullPointerException.class, () -> consumer.andThen(null));
    }

    @Test
    void testAndThenFirstConsumerThrows() throws Exception {
        final boolean[] secondCalled = {false};
        FailableBiConsumer<String, Integer, Exception> first = (s, i) -> { throw new RuntimeException(); };
        FailableBiConsumer<String, Integer, Exception> second = (s, i) -> secondCalled[0] = true;
        
        FailableBiConsumer<String, Integer, Exception> combined = first.andThen(second);
        assertThrows(RuntimeException.class, () -> combined.accept("x", 1));
        assertTrue(!secondCalled[0], "Second consumer should not be called after exception");
    }
}