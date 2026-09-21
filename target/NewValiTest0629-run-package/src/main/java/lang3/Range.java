package lang3;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
public class Range<T> implements Serializable {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private enum ComparableComparator implements Comparator {
        INSTANCE;
        @Override
        public int compare(final Object obj1, final Object obj2) {
            return ((Comparable) obj1).compareTo(obj2);
        }
    }
    private static final long serialVersionUID = 2L;
    @Deprecated
    public static <T extends Comparable<? super T>> Range<T> between(final T fromInclusive, final T toInclusive) {
        return of(fromInclusive, toInclusive, null);
    }
    @Deprecated
    public static <T> Range<T> between(final T fromInclusive, final T toInclusive, final Comparator<T> comparator) {
        return new Range<>(fromInclusive, toInclusive, comparator);
    }
    public static <T extends Comparable<? super T>> Range<T> is(final T element) {
        return of(element, element, null);
    }
    public static <T> Range<T> is(final T element, final Comparator<T> comparator) {
        return of(element, element, comparator);
    }
    public static <T extends Comparable<? super T>> Range<T> of(final T fromInclusive, final T toInclusive) {
        return of(fromInclusive, toInclusive, null);
    }
    public static <T> Range<T> of(final T fromInclusive, final T toInclusive, final Comparator<T> comparator) {
        return new Range<>(fromInclusive, toInclusive, comparator);
    }
    private final Comparator<T> comparator;
    private final int hashCode;
    private final T maximum;
    private final T minimum;
    private transient String toString;
    @SuppressWarnings("unchecked")
    Range(final T element1, final T element2, final Comparator<T> comp) {
        Objects.requireNonNull(element1, "element1");
        Objects.requireNonNull(element2, "element2");
        if (comp == null) {
            this.comparator = ComparableComparator.INSTANCE;
        } else {
            this.comparator = comp;
        }
        if (this.comparator.compare(element1, element2) < 1) {
            this.minimum = element1;
            this.maximum = element2;
        } else {
            this.minimum = element2;
            this.maximum = element1;
        }
        this.hashCode = Objects.hash(minimum, maximum);
    }
    public boolean contains(final T element) {
        if (element == null) {
            return false;
        }
        return comparator.compare(element, minimum) > -1 && comparator.compare(element, maximum) < 1;
    }
    public boolean containsRange(final Range<T> otherRange) {
        if (otherRange == null) {
            return false;
        }
        return contains(otherRange.minimum)
            && contains(otherRange.maximum);
    }
    public int elementCompareTo(final T element) {
        Objects.requireNonNull(element, "element");
        if (isAfter(element)) {
            return -1;
        }
        if (isBefore(element)) {
            return 1;
        }
        return 0;
    }
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final
        Range<T> range = (Range<T>) obj;
        return minimum.equals(range.minimum) &&
               maximum.equals(range.maximum);
    }
    public T fit(final T element) {
        Objects.requireNonNull(element, "element");
        if (isAfter(element)) {
            return minimum;
        }
        if (isBefore(element)) {
            return maximum;
        }
        return element;
    }
    public Comparator<T> getComparator() {
        return comparator;
    }
    public T getMaximum() {
        return maximum;
    }
    public T getMinimum() {
        return minimum;
    }
    @Override
    public int hashCode() {
        return hashCode;
    }
    public Range<T> intersectionWith(final Range<T> other) {
        if (!this.isOverlappedBy(other)) {
            throw new IllegalArgumentException(String.format(
                "Cannot calculate intersection with non-overlapping range %s", other));
        }
        if (this.equals(other)) {
            return this;
        }
        final T min = getComparator().compare(minimum, other.minimum) < 0 ? other.minimum : minimum;
        final T max = getComparator().compare(maximum, other.maximum) < 0 ? maximum : other.maximum;
        return of(min, max, getComparator());
    }
    public boolean isAfter(final T element) {
        if (element == null) {
            return false;
        }
        return comparator.compare(element, minimum) < 0;
    }
    public boolean isAfterRange(final Range<T> otherRange) {
        if (otherRange == null) {
            return false;
        }
        return isAfter(otherRange.maximum);
    }
    public boolean isBefore(final T element) {
        if (element == null) {
            return false;
        }
        return comparator.compare(element, maximum) > 0;
    }
    public boolean isBeforeRange(final Range<T> otherRange) {
        if (otherRange == null) {
            return false;
        }
        return isBefore(otherRange.minimum);
    }
    public boolean isEndedBy(final T element) {
        if (element == null) {
            return false;
        }
        return comparator.compare(element, maximum) == 0;
    }
    public boolean isNaturalOrdering() {
        return comparator == ComparableComparator.INSTANCE;
    }
    public boolean isOverlappedBy(final Range<T> otherRange) {
        if (otherRange == null) {
            return false;
        }
        return otherRange.contains(minimum)
            || otherRange.contains(maximum)
            || contains(otherRange.minimum);
    }
    public boolean isStartedBy(final T element) {
        if (element == null) {
            return false;
        }
        return comparator.compare(element, minimum) == 0;
    }
    @Override
    public String toString() {
        if (toString == null) {
            toString = "[" + minimum + ".." + maximum + "]";
        }
        return toString;
    }
    public String toString(final String format) {
        return String.format(format, minimum, maximum, comparator);
    }
}