package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StrictnessTest {

    @Test
    void testValues() {
        Strictness[] values = Strictness.values();
        assertEquals(3, values.length);
        assertEquals(Strictness.LENIENT, values[0]);
        assertEquals(Strictness.LEGACY_STRICT, values[1]);
        assertEquals(Strictness.STRICT, values[2]);
    }

    @Test
    void testValueOf_ValidNames() {
        assertEquals(Strictness.LENIENT, Strictness.valueOf("LENIENT"));
        assertEquals(Strictness.LEGACY_STRICT, Strictness.valueOf("LEGACY_STRICT"));
        assertEquals(Strictness.STRICT, Strictness.valueOf("STRICT"));
    }

    @Test
    void testValueOf_InvalidName() {
        assertThrows(IllegalArgumentException.class, () -> Strictness.valueOf("NON_EXISTENT"));
    }

    @Test
    void testValueOf_NullName() {
        assertThrows(NullPointerException.class, () -> Strictness.valueOf(null));
    }

    @Test
    void testName() {
        assertEquals("LENIENT", Strictness.LENIENT.name());
        assertEquals("LEGACY_STRICT", Strictness.LEGACY_STRICT.name());
        assertEquals("STRICT", Strictness.STRICT.name());
    }

    @Test
    void testOrdinal() {
        assertEquals(0, Strictness.LENIENT.ordinal());
        assertEquals(1, Strictness.LEGACY_STRICT.ordinal());
        assertEquals(2, Strictness.STRICT.ordinal());
    }

    @Test
    void testToString() {
        assertEquals("LENIENT", Strictness.LENIENT.toString());
        assertEquals("LEGACY_STRICT", Strictness.LEGACY_STRICT.toString());
        assertEquals("STRICT", Strictness.STRICT.toString());
    }

    @Test
    void testEquals() {
        assertTrue(Strictness.LENIENT.equals(Strictness.LENIENT));
        assertFalse(Strictness.LENIENT.equals(Strictness.STRICT));
        assertFalse(Strictness.LENIENT.equals(null));
        assertFalse(Strictness.LENIENT.equals("Not an enum"));
    }

    @Test
    void testHashCode() {
        assertEquals(Strictness.LENIENT.hashCode(), Strictness.LENIENT.hashCode());
        assertEquals(Strictness.LEGACY_STRICT.hashCode(), Strictness.LEGACY_STRICT.hashCode());
        assertNotEquals(Strictness.LENIENT.hashCode(), Strictness.STRICT.hashCode());
    }

    @Test
    void testCompareTo() {
        assertTrue(Strictness.LENIENT.compareTo(Strictness.LEGACY_STRICT) < 0);
        assertEquals(0, Strictness.STRICT.compareTo(Strictness.STRICT));
        assertTrue(Strictness.STRICT.compareTo(Strictness.LEGACY_STRICT) > 0);
    }

    @Test
    void testGetDeclaringClass() {
        assertEquals(Strictness.class, Strictness.LENIENT.getDeclaringClass());
        assertEquals(Strictness.class, Strictness.STRICT.getDeclaringClass());
    }

    @Test
    void testGetClass() {
        assertEquals(Strictness.class, Strictness.LENIENT.getClass());
        assertEquals(Strictness.class, Strictness.STRICT.getClass());
    }

    @Test
    void testWait_ZeroTimeout() throws InterruptedException {
        Object lock = new Object();
        synchronized (lock) {
            lock.wait(1000);
        }
    }

    @Test
    void testWait_WithTimeout() throws InterruptedException {
        Object lock = new Object();
        synchronized (lock) {
            lock.wait(10);
        }
    }

    @Test
    void testWait_WithNanos() throws InterruptedException {
        Object lock = new Object();
        synchronized (lock) {
            lock.wait(10, 5000);
        }
    }

    @Test
    void testNotify() {
        Object lock = new Object();
        synchronized (lock) {
            lock.notify();
        }
    }

    @Test
    void testNotifyAll() {
        Object lock = new Object();
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}