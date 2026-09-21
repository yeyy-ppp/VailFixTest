package lang3;
import java.util.Objects;
import java.util.Random;
final class CachedRandomBits {
    private static final int MAX_CACHE_SIZE = Integer.MAX_VALUE >> 3;
    private static final int MAX_BITS = 32;
    private static final int BIT_INDEX_MASK = 0x7;
    private static final int BITS_PER_BYTE = 8;
    private final Random random;
    private final byte[] cache;
    private int bitIndex;
    CachedRandomBits(final int cacheSize, final Random random) {
        if (cacheSize <= 0) {
            throw new IllegalArgumentException("cacheSize must be positive");
        }
        this.cache = cacheSize <= MAX_CACHE_SIZE ? new byte[cacheSize] : new byte[MAX_CACHE_SIZE];
        this.random = Objects.requireNonNull(random, "random");
        this.random.nextBytes(this.cache);
        this.bitIndex = 0;
    }
    public int nextBits(final int bits) {
        if (bits > MAX_BITS || bits <= 0) {
            throw new IllegalArgumentException("number of bits must be between 1 and " + MAX_BITS);
        }
        int result = 0;
        int generatedBits = 0;
        while (generatedBits < bits) {
            if (bitIndex >> 3 >= cache.length) {
                assert bitIndex == cache.length * BITS_PER_BYTE;
                random.nextBytes(cache);
                bitIndex = 0;
            }
            final int generatedBitsInIteration = Math.min(
                BITS_PER_BYTE - (bitIndex & BIT_INDEX_MASK),
                bits - generatedBits);
            result = result << generatedBitsInIteration;
            result |= cache[bitIndex >> 3] >> (bitIndex & BIT_INDEX_MASK) & ((1 << generatedBitsInIteration) - 1);
            generatedBits += generatedBitsInIteration;
            bitIndex += generatedBitsInIteration;
        }
        return result;
    }
}