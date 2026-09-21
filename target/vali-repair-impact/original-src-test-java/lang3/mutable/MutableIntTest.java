package lang3.mutable;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MutableIntTest {

    @Test
    void testDefaultConstructor() {
        MutableInt mutableInt = new MutableInt();
        assertEquals(0, mutableInt.intValue());
    }

    @Test
    void testIntConstructor() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5, mutableInt.intValue());
    }

    @Test
    void testNumberConstructor() {
        MutableInt mutableInt = new MutableInt(Integer.valueOf(10));
        assertEquals(10, mutableInt.intValue());
    }

    @Test
    void testStringConstructor() {
        MutableInt mutableInt = new MutableInt("15");
        assertEquals(15, mutableInt.intValue());
    }

    @Test
    void testStringConstructorThrowsException() {
        assertThrows(NumberFormatException.class, () -> new MutableInt("invalid"));
    }

    @Test
    void testAddInt() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.add(3);
        assertEquals(8, mutableInt.intValue());
    }

    @Test
    void testAddNumber() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.add(Integer.valueOf(4));
        assertEquals(9, mutableInt.intValue());
    }

    @Test
    void testAddAndGetInt() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.addAndGet(3);
        assertEquals(8, result);
        assertEquals(8, mutableInt.intValue());
    }

    @Test
    void testAddAndGetNumber() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.addAndGet(Integer.valueOf(4));
        assertEquals(9, result);
        assertEquals(9, mutableInt.intValue());
    }

    @Test
    void testCompareTo() {
        MutableInt mutable1 = new MutableInt(5);
        MutableInt mutable2 = new MutableInt(10);
        MutableInt mutable3 = new MutableInt(5);
        
        assertTrue(mutable1.compareTo(mutable2) < 0);
        assertTrue(mutable2.compareTo(mutable1) > 0);
        assertEquals(0, mutable1.compareTo(mutable3));
    }

    @Test
    void testDecrement() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.decrement();
        assertEquals(4, mutableInt.intValue());
    }

    @Test
    void testDecrementAndGet() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.decrementAndGet();
        assertEquals(4, result);
        assertEquals(4, mutableInt.intValue());
    }

    @Test
    void testDoubleValue() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5.0, mutableInt.doubleValue(), 0.0001);
    }

    @Test
    void testEquals() {
        MutableInt mutable1 = new MutableInt(5);
        MutableInt mutable2 = new MutableInt(5);
        MutableInt mutable3 = new MutableInt(10);
        String notMutable = "test";
        
        assertTrue(mutable1.equals(mutable2));
        assertFalse(mutable1.equals(mutable3));
        assertFalse(mutable1.equals(notMutable));
        assertTrue(mutable1.equals(mutable1));
    }

    @Test
    void testFloatValue() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5.0f, mutableInt.floatValue(), 0.0001f);
    }

    @Test
    void testGetAndAddInt() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.getAndAdd(3);
        assertEquals(5, result);
        assertEquals(8, mutableInt.intValue());
    }

    @Test
    void testGetAndAddNumber() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.getAndAdd(Integer.valueOf(4));
        assertEquals(5, result);
        assertEquals(9, mutableInt.intValue());
    }

    @Test
    void testGetAndDecrement() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.getAndDecrement();
        assertEquals(5, result);
        assertEquals(4, mutableInt.intValue());
    }

    @Test
    void testGetAndIncrement() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.getAndIncrement();
        assertEquals(5, result);
        assertEquals(6, mutableInt.intValue());
    }

    @Test
    void testGetValue() {
        MutableInt mutableInt = new MutableInt(5);
        Integer result = mutableInt.getValue();
        assertEquals(Integer.valueOf(5), result);
    }

    @Test
    void testHashCode() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5, mutableInt.hashCode());
    }

    @Test
    void testIncrement() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.increment();
        assertEquals(6, mutableInt.intValue());
    }

    @Test
    void testIncrementAndGet() {
        MutableInt mutableInt = new MutableInt(5);
        int result = mutableInt.incrementAndGet();
        assertEquals(6, result);
        assertEquals(6, mutableInt.intValue());
    }

    @Test
    void testIntValue() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5, mutableInt.intValue());
    }

    @Test
    void testLongValue() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals(5L, mutableInt.longValue());
    }

    @Test
    void testSetValueInt() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.setValue(10);
        assertEquals(10, mutableInt.intValue());
    }

    @Test
    void testSetValueNumber() {
        MutableInt mutableInt = new MutableInt(5);
        mutableInt.setValue(Integer.valueOf(15));
        assertEquals(15, mutableInt.intValue());
    }

    @Test
    void testSubtractInt() {
        MutableInt mutableInt = new MutableInt(10);
        mutableInt.subtract(3);
        assertEquals(7, mutableInt.intValue());
    }

    @Test
    void testSubtractNumber() {
        MutableInt mutableInt = new MutableInt(10);
        mutableInt.subtract(Integer.valueOf(4));
        assertEquals(6, mutableInt.intValue());
    }

    @Test
    void testToInteger() {
        MutableInt mutableInt = new MutableInt(5);
        Integer result = mutableInt.toInteger();
        assertEquals(Integer.valueOf(5), result);
    }

    @Test
    void testToString() {
        MutableInt mutableInt = new MutableInt(5);
        assertEquals("5", mutableInt.toString());
    }
}