package lang3;
import java.util.stream.LongStream;
public final class LongRange extends NumberRange<Long> {
    private static final long serialVersionUID = 1L;
    public static LongRange of(final long fromInclusive, final long toInclusive) {
        return of(Long.valueOf(fromInclusive), Long.valueOf(toInclusive));
    }
    public static LongRange of(final Long fromInclusive, final Long toInclusive) {
        return new LongRange(fromInclusive, toInclusive);
    }
    private LongRange(final Long number1, final Long number2) {
        super(number1, number2, null);
    }
    public long fit(final long element) {
        return super.fit(element).longValue();
    }
    public LongStream toLongStream() {
        return LongStream.rangeClosed(getMinimum(), getMaximum());
    }
}