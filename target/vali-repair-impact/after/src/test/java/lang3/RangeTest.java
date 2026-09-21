package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Comparator;

class RangeTest {

    @Test
    void testBetween() {
        Range<Integer> range = Range.between(5, 10);
        assertEquals(5, range.getMinimum());
        assertEquals(10, range.getMaximum());
    }

    @Test
    void testBetweenWithComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        Range<Integer> range = Range.between(10, 5, reverse);
        assertEquals(10, range.getMinimum());
        assertEquals(5, range.getMaximum());
    }

    @Test
    void testIs() {
        Range<Integer> range = Range.is(7);
        assertEquals(7, range.getMinimum());
        assertEquals(7, range.getMaximum());
        assertTrue(range.contains(7));
    }

    @Test
    void testIsWithComparator() {
        Comparator<Integer> comp = Integer::compare;
        Range<Integer> range = Range.is(7, comp);
        assertEquals(7, range.getMinimum());
        assertEquals(7, range.getMaximum());
    }

    @Test
    void testOf() {
        Range<Integer> range = Range.of(5, 10);
        assertEquals(5, range.getMinimum());
        assertEquals(10, range.getMaximum());
    }

    @Test
    void testOfWithComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        Range<Integer> range = Range.of(10, 5, reverse);
        assertEquals(10, range.getMinimum());
        assertEquals(5, range.getMaximum());
    }

    @Test
    void testRangeConstructor() {
        Range<Integer> range = Range.of(5, 10, Integer::compare);
        assertEquals(5, range.getMinimum());
        assertEquals(10, range.getMaximum());
    }

    @Test
    void testContains() {
        Range<Integer> range = Range.of(5, 10);
        assertTrue(range.contains(7));
        assertFalse(range.contains(3));
        assertFalse(range.contains(12));
        assertTrue(range.contains(5));
        assertTrue(range.contains(10));
    }

    @Test
    void testContainsRange() {
        Range<Integer> range = Range.of(5, 10);
        Range<Integer> inside = Range.of(6, 9);
        Range<Integer> outside = Range.of(3, 4);
        Range<Integer> overlap = Range.of(8, 12);

        assertTrue(range.containsRange(inside));
        assertFalse(range.containsRange(outside));
        assertFalse(range.containsRange(overlap));
    }

    @Test
    void testElementCompareTo() {
        Range<Integer> range = Range.of(5, 10);
        assertTrue(range.elementCompareTo(3) < 0);
        assertTrue(range.elementCompareTo(12) > 0);
        assertEquals(0, range.elementCompareTo(7));
        assertEquals(0, range.elementCompareTo(5));
        assertEquals(0, range.elementCompareTo(10));
    }

    @Test
    void testEquals() {
        Range<Integer> range1 = Range.of(5, 10);
        Range<Integer> range2 = Range.of(5, 10);
        Range<Integer> range3 = Range.of(3, 8);
        Range<String> range4 = Range.of("A", "Z");

        assertTrue(range1.equals(range2));
        assertFalse(range1.equals(range3));
        assertFalse(range1.equals(range4));
        assertFalse(range1.equals(null));
    }

    @Test
    void testFit() {
        Range<Integer> range = Range.of(5, 10);
        assertEquals(5, range.fit(3));
        assertEquals(10, range.fit(12));
        assertEquals(7, range.fit(7));
    }

    @Test
    void testGetComparator() {
        Comparator<Integer> comp = Integer::compare;
        Range<Integer> range = Range.of(5, 10, comp);
        assertEquals(comp, range.getComparator());
    }

    @Test
    void testGetMaximum() {
        Range<Integer> range = Range.of(5, 10);
        assertEquals(10, range.getMaximum());
    }

    @Test
    void testGetMinimum() {
        Range<Integer> range = Range.of(5, 10);
        assertEquals(5, range.getMinimum());
    }

    @Test
    void testHashCode() {
        Range<Integer> range1 = Range.of(5, 10);
        Range<Integer> range2 = Range.of(5, 10);
        assertEquals(range1.hashCode(), range2.hashCode());
    }

    @Test
    void testIntersectionWith() {
        Range<Integer> range = Range.of(5, 10);
        Range<Integer> other = Range.of(7, 12);
        Range<Integer> intersection = range.intersectionWith(other);
        assertEquals(7, intersection.getMinimum());
        assertEquals(10, intersection.getMaximum());
    }

    @Test
    void testIsAfter() {
        Range<Integer> range = Range.of(5, 10);
        assertFalse(range.isAfter(7));
        assertTrue(range.isAfter(3));
        assertFalse(range.isAfter(12));
    }

    @Test
    void testIsAfterRange() {
        Range<Integer> range = Range.of(5, 10);
        Range<Integer> before = Range.of(1, 4);
        Range<Integer> after = Range.of(11, 15);

        assertFalse(range.isAfterRange(before));
        assertTrue(range.isAfterRange(after));
    }

    @Test
    void testIsBefore() {
        Range<Integer> range = Range.of(5, 10);
        assertFalse(range.isBefore(7));
        assertFalse(range.isBefore(3));
        assertTrue(range.isBefore(12));
    }

    @Test
    void testIsBeforeRange() {
        Range<Integer> range = Range.of(5, 10);
        Range<Integer> before = Range.of(1, 4);
        Range<Integer> after = Range.of(11, 15);

        assertTrue(range.isBeforeRange(before));
        assertFalse(range.isBeforeRange(after));
    }

    @Test
    void testIsEndedBy() {
        Range<Integer> range = Range.of(5, 10);
        assertTrue(range.isEndedBy(10));
        assertFalse(range.isEndedBy(5));
        assertFalse(range.isEndedBy(7));
    }

    @Test
    void testIsNaturalOrdering() {
        Range<Integer> natural = Range.of(5, 10);
        assertTrue(natural.isNaturalOrdering());

        Comparator<Integer> comp = Integer::compare;
        Range<Integer> custom = Range.of(5, 10, comp);
        assertFalse(custom.isNaturalOrdering());
    }

    @Test
    void testIsOverlappedBy() {
        Range<Integer> range = Range.of(5, 10);
        Range<Integer> overlap = Range.of(7, 12);
        Range<Integer> noOverlap = Range.of(1, 4);

        assertTrue(range.isOverlappedBy(overlap));
        assertFalse(range.isOverlappedBy(noOverlap));
    }

    @Test
    void testIsStartedBy() {
        Range<Integer> range = Range.of(5, 10);
        assertTrue(range.isStartedBy(5));
        assertFalse(range.isStartedBy(10));
        assertFalse(range.isStartedBy(7));
    }

    @Test
    void testToString() {
        Range<Integer> range = Range.of(5, 10);
        assertEquals("[5..10]", range.toString());
    }
}
