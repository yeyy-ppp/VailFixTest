package lang3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Comparator;

public class NumberRangeTest {

    @Test
    void testCreateWithValidIntegers() {
        Integer num1 = 5;
        Integer num2 = 10;
        Comparator<Integer> comparator = Comparator.naturalOrder();
        NumberRange<Integer> range = new NumberRange<>(num1, num2, comparator);
        assertNotNull(range);
    }

    @Test
    void testCreateWithNullComparator() {
        Integer num1 = 5;
        Integer num2 = 10;
        NumberRange<Integer> range = new NumberRange<>(num1, num2, null);
        assertNotNull(range);
    }

    @Test
    void testCreateWithEqualNumbers() {
        Integer num1 = 5;
        Integer num2 = 5;
        Comparator<Integer> comparator = Comparator.reverseOrder();
        NumberRange<Integer> range = new NumberRange<>(num1, num2, comparator);
        assertNotNull(range);
    }

    @Test
    void testCreateWithFirstNumberNull() {
        Integer num2 = 10;
        Comparator<Integer> comparator = Comparator.naturalOrder();
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(null, num2, comparator));
    }

    @Test
    void testCreateWithSecondNumberNull() {
        Integer num1 = 5;
        Comparator<Integer> comparator = Comparator.naturalOrder();
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(num1, null, comparator));
    }

    @Test
    void testCreateWithBothNumbersNull() {
        Comparator<Integer> comparator = Comparator.naturalOrder();
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(null, null, comparator));
    }

    @Test
    void testCreateWithFirstNullAndNullComparator() {
        Integer num2 = 10;
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(null, num2, null));
    }

    @Test
    void testCreateWithSecondNullAndNullComparator() {
        Integer num1 = 5;
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(num1, null, null));
    }

    @Test
    void testCreateWithBothNullAndNullComparator() {
        assertThrows(NullPointerException.class, () -> 
            new NumberRange<>(null, null, null));
    }

    @Test
    void testCreateWithDouble() {
        Double num1 = 3.14;
        Double num2 = 2.71;
        Comparator<Double> comparator = Comparator.naturalOrder();
        NumberRange<Double> range = new NumberRange<>(num1, num2, comparator);
        assertNotNull(range);
    }

}