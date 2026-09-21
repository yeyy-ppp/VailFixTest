package lang3;

import org.junit.jupiter.api.Test;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

public class ArraySorterTest {

    @Test
    void testArraySorterConstructor() {
        new ArraySorter();
    }
    
    @Test
    void testSortIntArray() {
        int[] array = {3, 2, 1};
        int[] sorted = ArraySorter.sort(array);
        assertArrayEquals(new int[]{1, 2, 3}, sorted);
    }
    
    @Test
    void testSortStringArray() {
        String[] array = {"c", "b", "a"};
        String[] sorted = ArraySorter.sort(array);
        assertArrayEquals(new String[]{"a", "b", "c"}, sorted);
    }
    
    @Test
    void testSortWithComparator() {
        String[] array = {"a", "b", "c"};
        Comparator<String> comp = Comparator.reverseOrder();
        String[] sorted = ArraySorter.sort(array, comp);
        assertArrayEquals(new String[]{"c", "b", "a"}, sorted);
    }
}
