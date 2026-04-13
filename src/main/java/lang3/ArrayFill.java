package lang3;
import java.util.Arrays;
import lang3.function.FailableIntFunction;
public final class ArrayFill {
    public static boolean[] fill(final boolean[] a, final boolean val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static byte[] fill(final byte[] a, final byte val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static char[] fill(final char[] a, final char val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static double[] fill(final double[] a, final double val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static float[] fill(final float[] a, final float val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static int[] fill(final int[] a, final int val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static long[] fill(final long[] a, final long val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static short[] fill(final short[] a, final short val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    public static <T, E extends Throwable> T[] fill(final T[] array, final FailableIntFunction<? extends T, E> generator) throws E {
        if (array != null && generator != null) {
            for (int i = 0; i < array.length; i++) {
                array[i] = generator.apply(i);
            }
        }
        return array;
    }
    public static <T> T[] fill(final T[] a, final T val) {
        if (a != null) {
            Arrays.fill(a, val);
        }
        return a;
    }
    private ArrayFill() {
    }
}