package lang3;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lang3.exception.UncheckedException;
import org.apache.commons.lang3.Validate;

public class RandomUtils {
    private static final RandomUtils INSECURE = new RandomUtils(ThreadLocalRandom::current);
    private static final RandomUtils SECURE = new RandomUtils(SecureRandom::new);
    private static final Supplier<Random> SECURE_STRONG_SUPPLIER = () -> RandomUtils.SECURE_RANDOM_STRONG.get();
    private static final RandomUtils SECURE_STRONG = new RandomUtils(SECURE_STRONG_SUPPLIER);
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM_STRONG = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (final NoSuchAlgorithmException e) {
            throw new UncheckedException(e);
        }
    });
    public static RandomUtils insecure() {
        return INSECURE;
    }
    @Deprecated
    public static boolean nextBoolean() {
        return secure().randomBoolean();
    }
    @Deprecated
    public static byte[] nextBytes(final int count) {
        return secure().randomBytes(count);
    }
    @Deprecated
    public static double nextDouble() {
        return secure().randomDouble();
    }
    @Deprecated
    public static double nextDouble(final double startInclusive, final double endExclusive) {
        return secure().randomDouble(startInclusive, endExclusive);
    }
    @Deprecated
    public static float nextFloat() {
        return secure().randomFloat();
    }
    @Deprecated
    public static float nextFloat(final float startInclusive, final float endExclusive) {
        return secure().randomFloat(startInclusive, endExclusive);
    }
    @Deprecated
    public static int nextInt() {
        return secure().randomInt();
    }
    @Deprecated
    public static int nextInt(final int startInclusive, final int endExclusive) {
        return secure().randomInt(startInclusive, endExclusive);
    }
    @Deprecated
    public static long nextLong() {
        return secure().randomLong();
    }
    @Deprecated
    public static long nextLong(final long startInclusive, final long endExclusive) {
        return secure().randomLong(startInclusive, endExclusive);
    }
    public static RandomUtils secure() {
        return SECURE;
    }
    static SecureRandom secureRandom() {
        return SECURE_RANDOM_STRONG.get();
    }
    public static RandomUtils secureStrong() {
        return SECURE_STRONG;
    }
    private final Supplier<Random> random;
    @Deprecated
    public RandomUtils() {
        this(SECURE_STRONG_SUPPLIER);
    }
    private RandomUtils(final Supplier<Random> random) {
        this.random = random;
    }
    Random random() {
        return random.get();
    }
    public boolean randomBoolean() {
        return random().nextBoolean();
    }
    public byte[] randomBytes(final int count) {
        Validate.isTrue(count >= 0, "Count cannot be negative.");
        final byte[] result = new byte[count];
        random().nextBytes(result);
        return result;
    }
    public double randomDouble() {
        return randomDouble(0, Double.MAX_VALUE);
    }
    public double randomDouble(final double startInclusive, final double endExclusive) {
        Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
        Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");
        if (startInclusive == endExclusive) {
            return startInclusive;
        }
        return startInclusive + (endExclusive - startInclusive) * random().nextDouble();
    }
    public float randomFloat() {
        return randomFloat(0, Float.MAX_VALUE);
    }
    public float randomFloat(final float startInclusive, final float endExclusive) {
        Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
        Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");
        if (startInclusive == endExclusive) {
            return startInclusive;
        }
        return startInclusive + (endExclusive - startInclusive) * random().nextFloat();
    }
    public int randomInt() {
        return randomInt(0, Integer.MAX_VALUE);
    }
    public int randomInt(final int startInclusive, final int endExclusive) {
        Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
        Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");
        if (startInclusive == endExclusive) {
            return startInclusive;
        }
        return startInclusive + random().nextInt(endExclusive - startInclusive);
    }
    public long randomLong() {
        return randomLong(Long.MAX_VALUE);
    }
    private long randomLong(final long n) {
        long bits;
        long val;
        do {
            bits = random().nextLong() >>> 1;
            val = bits % n;
        } while (bits - val + n - 1 < 0);
        return val;
    }
    public long randomLong(final long startInclusive, final long endExclusive) {
       Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
        Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");
        if (startInclusive == endExclusive) {
            return startInclusive;
        }
        return startInclusive + randomLong(endExclusive - startInclusive);
    }
    @Override
    public String toString() {
        return "RandomUtils [random=" + random() + "]";
    }
}