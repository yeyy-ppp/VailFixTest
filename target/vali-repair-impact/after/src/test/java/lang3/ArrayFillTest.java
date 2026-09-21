package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import lang3.function.FailableIntFunction;
import java.lang.reflect.Constructor;

class ArrayFillTest {

    @Test
    void testFillBooleanArray() {
        boolean[] arr = new boolean[3];
        boolean[] result = ArrayFill.fill(arr, true);
        assertSame(arr, result);
        assertArrayEquals(new boolean[]{true, true, true}, arr);

        assertNull(ArrayFill.fill(null, true));
    }

    @Test
    void testFillByteArray() {
        byte[] arr = new byte[3];
        byte[] result = ArrayFill.fill(arr, (byte)5);
        assertSame(arr, result);
        assertArrayEquals(new byte[]{5, 5, 5}, arr);

        assertNull(ArrayFill.fill((byte[]) null, (byte)5));
    }

    @Test
    void testFillCharArray() {
        char[] arr = new char[3];
        char[] result = ArrayFill.fill(arr, 'a');
        assertSame(arr, result);
        assertArrayEquals(new char[]{'a', 'a', 'a'}, arr);

        assertNull(ArrayFill.fill((char[]) null, 'a'));
    }

    @Test
    void testFillDoubleArray() {
        double[] arr = new double[3];
        double[] result = ArrayFill.fill(arr, 1.5);
        assertSame(arr, result);
        assertArrayEquals(new double[]{1.5, 1.5, 1.5}, arr, 0.0);

        assertNull(ArrayFill.fill(null, 1.5));
    }

    @Test
    void testFillFloatArray() {
        float[] arr = new float[3];
        float[] result = ArrayFill.fill(arr, 2.5f);
        assertSame(arr, result);
        assertArrayEquals(new float[]{2.5f, 2.5f, 2.5f}, arr, 0.0f);

        assertNull(ArrayFill.fill((double[]) null, 2.5f));
    }

    @Test
    void testFillIntArray() {
        int[] arr = new int[3];
        int[] result = ArrayFill.fill(arr, 10);
        assertSame(arr, result);
        assertArrayEquals(new int[]{10, 10, 10}, arr);

        assertNull(ArrayFill.fill((double[]) null, 10));
    }

    @Test
    void testFillLongArray() {
        long[] arr = new long[3];
        long[] result = ArrayFill.fill(arr, 100L);
        assertSame(arr, result);
        assertArrayEquals(new long[]{100L, 100L, 100L}, arr);

        assertNull(ArrayFill.fill((double[]) null, 100L));
    }

    @Test
    void testFillShortArray() {
        short[] arr = new short[3];
        short[] result = ArrayFill.fill(arr, (short)20);
        assertSame(arr, result);
        assertArrayEquals(new short[]{20, 20, 20}, arr);

        assertNull(ArrayFill.fill((double[]) null, (short)20));
    }

    @Test
    void testFillObjectArray() {
        String[] arr = new String[3];
        String[] result = ArrayFill.fill(arr, "test");
        assertSame(arr, result);
        assertArrayEquals(new String[]{"test", "test", "test"}, arr);

        assertNull(ArrayFill.fill(null, "test"));

        Integer[] intArr = new Integer[2];
        ArrayFill.fill(intArr, 42);
        assertArrayEquals(new Integer[]{42, 42}, intArr);
    }

    @Test
    void testFillWithGenerator() {
        String[] arr = new String[3];
        FailableIntFunction<String, RuntimeException> generator = idx -> "item" + idx;
        String[] result = ArrayFill.fill(arr, generator);
        assertSame(arr, result);
        assertArrayEquals(new String[]{"item0", "item1", "item2"}, arr);

        assertNull(ArrayFill.fill(null, generator));

        arr = new String[]{"a", "b", "c"};
        result = ArrayFill.fill(arr, (FailableIntFunction<String, RuntimeException>) null);
        assertSame(arr, result);
        assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    void testFillWithGeneratorThrows() {
        String[] arr = new String[2];
        FailableIntFunction<String, RuntimeException> generator = idx -> {
            if (idx == 1) throw new RuntimeException("Test exception");
            return "value";
        };

        assertThrows(RuntimeException.class, () -> ArrayFill.fill(arr, generator));
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Class<?> cls = Class.forName("lang3.ArrayFill");
        Constructor<?> constructor = cls.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}