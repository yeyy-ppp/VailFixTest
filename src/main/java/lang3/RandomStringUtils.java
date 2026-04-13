package lang3;
import org.apache.maven.surefire.shared.lang3.StringUtils;

import java.util.Random;
import java.util.function.Supplier;
public class RandomStringUtils {
    private static final Supplier<lang3.RandomUtils> SECURE_SUPPLIER = lang3.RandomUtils::secure;
    private static final RandomStringUtils INSECURE = new RandomStringUtils(lang3.RandomUtils::insecure);
    private static final RandomStringUtils SECURE = new RandomStringUtils(SECURE_SUPPLIER);
    private static final RandomStringUtils SECURE_STRONG = new RandomStringUtils(lang3.RandomUtils::secureStrong);
    private static final char[] ALPHANUMERICAL_CHARS = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1',
            '2', '3', '4', '5', '6', '7', '8', '9' };
    private static final int ASCII_0 = '0';
    private static final int ASCII_9 = '9';
    private static final int ASCII_A = 'A';
    private static final int ASCII_z = 'z';
    private static final int CACHE_PADDING_BITS = 3;
    private static final int BITS_TO_BYTES_DIVISOR = 5;
    private static final int BASE_CACHE_SIZE_PADDING = 10;
    public static RandomStringUtils insecure() {
        return INSECURE;
    }
    @Deprecated
    public static String random(final int count) {
        return secure().next(count);
    }
    @Deprecated
    public static String random(final int count, final boolean letters, final boolean numbers) {
        return secure().next(count, letters, numbers);
    }
    @Deprecated
    public static String random(final int count, final char... chars) {
        return secure().next(count, chars);
    }
    @Deprecated
    public static String random(final int count, final int start, final int end, final boolean letters,
            final boolean numbers) {
        return secure().next(count, start, end, letters, numbers);
    }
    @Deprecated
    public static String random(final int count, final int start, final int end, final boolean letters,
            final boolean numbers, final char... chars) {
        return secure().next(count, start, end, letters, numbers, chars);
    }
    public static String random(int count, int start, int end, final boolean letters, final boolean digits,
            final char[] chars, final Random random) {
        if (count == 0) {
            return StringUtils.EMPTY;
        }
        if (count < 0) {
            throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
        }
        if (chars != null && chars.length == 0) {
            throw new IllegalArgumentException("The chars array must not be empty");
        }
        if (start == 0 && end == 0) {
            if (chars != null) {
                end = chars.length;
            } else if (!letters && !digits) {
                end = Character.MAX_CODE_POINT;
            } else {
                end = 'z' + 1;
                start = ' ';
            }
        } else if (end <= start) {
            throw new IllegalArgumentException("Parameter end (" + end + ") must be greater than start (" + start + ")");
        } else if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Character positions MUST be >= 0");
        }
        if (end > Character.MAX_CODE_POINT) {
            end = Character.MAX_CODE_POINT;
        }
        if (chars == null && end <= 0x7f) {
            if (letters && digits && start <= ASCII_0 && end >= ASCII_z + 1) {
                return random(count, 0, 0, false, false, ALPHANUMERICAL_CHARS, random);
            }
            if (digits && end <= ASCII_0 || letters && end <= ASCII_A) {
                throw new IllegalArgumentException("Parameter end (" + end + ") must be greater than (" + ASCII_0 + ") for generating digits "
                        + "or greater than (" + ASCII_A + ") for generating letters.");
            }
            if (letters && digits) {
                start = Math.max(ASCII_0, start);
                end = Math.min(ASCII_z + 1, end);
            } else if (digits) {
                start = Math.max(ASCII_0, start);
                end = Math.min(ASCII_9 + 1, end);
            } else if (letters) {
                start = Math.max(ASCII_A, start);
                end = Math.min(ASCII_z + 1, end);
            }
        }
        if (letters && !digits) {
            for (int i = start; i < end; i++) {
                if (Character.isLetter(i)) {
                    break;
                }
                if (i == end - 1) {
                    throw new IllegalArgumentException(String.format("No letters exist between start %,d and end %,d.", start, end));
                }
            }
        }
        if (!letters && digits) {
            for (int i = start; i < end; i++) {
                if (Character.isDigit(i)) {
                    break;
                }
                if (i == end - 1) {
                    throw new IllegalArgumentException(String.format("No digits exist between start %,d and end %,d.", start, end));
                }
            }
        }
        final StringBuilder builder = new StringBuilder(count);
        final int gap = end - start;
        final int gapBits = Integer.SIZE - Integer.numberOfLeadingZeros(gap);
        final long desiredCacheSize = ((long) count * gapBits + CACHE_PADDING_BITS) / BITS_TO_BYTES_DIVISOR + BASE_CACHE_SIZE_PADDING;
        final int cacheSize = (int) Math.min(desiredCacheSize, Integer.MAX_VALUE / BITS_TO_BYTES_DIVISOR + BASE_CACHE_SIZE_PADDING);
        final CachedRandomBits arb = new CachedRandomBits(cacheSize, random);
        while (count-- != 0) {
            final int randomValue = arb.nextBits(gapBits) + start;
            if (randomValue >= end) {
                count++;
                continue;
            }
            final int codePoint;
            if (chars == null) {
                codePoint = randomValue;
                switch (Character.getType(codePoint)) {
                case Character.UNASSIGNED:
                case Character.PRIVATE_USE:
                case Character.SURROGATE:
                    count++;
                    continue;
                }
            } else {
                codePoint = chars[randomValue];
            }
            final int numberOfChars = Character.charCount(codePoint);
            if (count == 0 && numberOfChars > 1) {
                count++;
                continue;
            }
            if (letters && Character.isLetter(codePoint) || digits && Character.isDigit(codePoint) || !letters && !digits) {
                builder.appendCodePoint(codePoint);
                if (numberOfChars == 2) {
                    count--;
                }
            } else {
                count++;
            }
        }
        return builder.toString();
    }
    @Deprecated
    public static String random(final int count, final String chars) {
        return secure().next(count, chars);
    }
    @Deprecated
    public static String randomAlphabetic(final int count) {
        return secure().nextAlphabetic(count);
    }
    @Deprecated
    public static String randomAlphabetic(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextAlphabetic(minLengthInclusive, maxLengthExclusive);
    }
    @Deprecated
    public static String randomAlphanumeric(final int count) {
        return secure().nextAlphanumeric(count);
    }
    @Deprecated
    public static String randomAlphanumeric(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextAlphanumeric(minLengthInclusive, maxLengthExclusive);
    }
    @Deprecated
    public static String randomAscii(final int count) {
        return secure().nextAscii(count);
    }
    @Deprecated
    public static String randomAscii(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextAscii(minLengthInclusive, maxLengthExclusive);
    }
    @Deprecated
    public static String randomGraph(final int count) {
        return secure().nextGraph(count);
    }
    @Deprecated
    public static String randomGraph(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextGraph(minLengthInclusive, maxLengthExclusive);
    }
    @Deprecated
    public static String randomNumeric(final int count) {
        return secure().nextNumeric(count);
    }
    @Deprecated
    public static String randomNumeric(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextNumeric(minLengthInclusive, maxLengthExclusive);
    }
    @Deprecated
    public static String randomPrint(final int count) {
        return secure().nextPrint(count);
    }
    @Deprecated
    public static String randomPrint(final int minLengthInclusive, final int maxLengthExclusive) {
        return secure().nextPrint(minLengthInclusive, maxLengthExclusive);
    }
    public static RandomStringUtils secure() {
        return SECURE;
    }
    public static RandomStringUtils secureStrong() {
        return SECURE_STRONG;
    }
    private final Supplier<lang3.RandomUtils> random;
    @Deprecated
    public RandomStringUtils() {
        this(SECURE_SUPPLIER);
    }
    private RandomStringUtils(final Supplier<lang3.RandomUtils> random) {
        this.random = random;
    }
    public String next(final int count) {
        return next(count, false, false);
    }
    public String next(final int count, final boolean letters, final boolean numbers) {
        return next(count, 0, 0, letters, numbers);
    }
    public String next(final int count, final char... chars) {
        if (chars == null) {
            return random(count, 0, 0, false, false, null, random());
        }
        return random(count, 0, chars.length, false, false, chars, random());
    }
    public String next(final int count, final int start, final int end, final boolean letters, final boolean numbers) {
        return random(count, start, end, letters, numbers, null, random());
    }
    public String next(final int count, final int start, final int end, final boolean letters, final boolean numbers,
            final char... chars) {
        return random(count, start, end, letters, numbers, chars, random());
    }
    public String next(final int count, final String chars) {
        if (chars == null) {
            return random(count, 0, 0, false, false, null, random());
        }
        return next(count, chars.toCharArray());
    }
    public String nextAlphabetic(final int count) {
        return next(count, true, false);
    }
    public String nextAlphabetic(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextAlphabetic(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    public String nextAlphanumeric(final int count) {
        return next(count, true, true);
    }
    public String nextAlphanumeric(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextAlphanumeric(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    public String nextAscii(final int count) {
        return next(count, 32, 127, false, false);
    }
    public String nextAscii(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextAscii(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    public String nextGraph(final int count) {
        return next(count, 33, 126, false, false);
    }
    public String nextGraph(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextGraph(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    public String nextNumeric(final int count) {
        return next(count, false, true);
    }
    public String nextNumeric(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextNumeric(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    public String nextPrint(final int count) {
        return next(count, 32, 126, false, false);
    }
    public String nextPrint(final int minLengthInclusive, final int maxLengthExclusive) {
        return nextPrint(randomUtils().randomInt(minLengthInclusive, maxLengthExclusive));
    }
    private Random random() {
        return randomUtils().random();
    }
    private RandomUtils randomUtils() {
        return random.get();
    }
    @Override
    public String toString() {
        return "RandomStringUtils [random=" + random() + "]";
    }
}