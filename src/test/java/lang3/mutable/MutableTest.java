package lang3.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class MutableTest {

    @Test
    void testGetMethodCallsGetValue() {
        Mutable<Number> mutable = new MutableInt(42);
        assertEquals(42, mutable.get().intValue());
    }

    @Test
    void testGetValueImplementation() {
        MutableInt mutable = new MutableInt(42);
        assertEquals(42, mutable.getValue().intValue());
    }

    @Test
    void testSetValueUpdatesState() {
        MutableInt mutable = new MutableInt(10);
        mutable.setValue(20);
        assertEquals(20, mutable.get().intValue());
    }
}