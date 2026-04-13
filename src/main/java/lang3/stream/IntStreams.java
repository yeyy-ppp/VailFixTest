package lang3.stream;
import java.util.stream.IntStream;
public class IntStreams {
    @SafeVarargs
    public static IntStream of(final int... values) {
        return values == null ? IntStream.empty() : IntStream.of(values);
    }
    public static IntStream range(final int endExclusive) {
        return IntStream.range(0, endExclusive);
    }
    public static IntStream rangeClosed(final int endInclusive) {
        return IntStream.rangeClosed(0, endInclusive);
    }
    @Deprecated
    public IntStreams() {
    }
}