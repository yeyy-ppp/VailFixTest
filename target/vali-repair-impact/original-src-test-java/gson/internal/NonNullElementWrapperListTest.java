package gson.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NonNullElementWrapperListTest {

    @Test
    void testConstructorWithNullDelegate() {
        assertThrows(NullPointerException.class, () -> {
            new NonNullElementWrapperList<>(null);
        });
    }

    @Test
    void testConstructorWithEmptyDelegate() {
        ArrayList<String> delegate = new ArrayList<>();
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(0, list.size());
    }

    @Test
    void testConstructorWithNonEmptyDelegate() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void testGetValidIndex() {
        ArrayList<Integer> delegate = new ArrayList<>(Arrays.asList(1, 2));
        NonNullElementWrapperList<Integer> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    void testGetNegativeIndex() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void testGetIndexBeyondSize() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    void testSize() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(2, list.size());
        delegate.add("c");
        assertEquals(3, list.size());
    }

    @Test
    void testSetValidElement() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        String result = list.set(1, "c");
        assertEquals("b", result);
        assertEquals("c", list.get(1));
    }

    @Test
    void testSetNullElement() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertThrows(NullPointerException.class, () -> list.set(0, null));
    }

    @Test
    void testSetIndexOutOfBounds() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, "a"));
    }

    @Test
    void testAddAtIndexValidElement() {
        ArrayList<String> delegate = new ArrayList<>();
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        list.add(0, "a");
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void testAddAtIndexNullElement() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertThrows(NullPointerException.class, () -> list.add(0, null));
    }

    @Test
    void testAddAtIndexOutOfBounds() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, "a"));
    }

    @Test
    void testRemoveByIndex() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        String removed = list.remove(0);
        assertEquals("a", removed);
        assertEquals(1, list.size());
        assertEquals("b", list.get(0));
    }

    @Test
    void testRemoveByIndexEmptyList() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    }

    @Test
    void testClear() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(delegate.isEmpty());
    }

    @Test
    void testRemoveByObjectExisting() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.remove("a"));
        assertEquals(1, list.size());
    }

    @Test
    void testRemoveByObjectNonExisting() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertFalse(list.remove("b"));
    }

    @Test
    void testRemoveByObjectNullWhenPresent() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList(null, "a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.remove(null));
        assertEquals(1, list.size());
    }

    @Test
    void testRemoveByObjectNullWhenAbsent() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertFalse(list.remove(null));
    }

    @Test
    void testRemoveAll() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b", "c"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.removeAll(Arrays.asList("a", "c")));
        assertEquals(1, list.size());
        assertEquals("b", list.get(0));
    }

    @Test
    void testRemoveAllNoChange() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertFalse(list.removeAll(Collections.singletonList("b")));
    }

    @Test
    void testRemoveAllWithNulls() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList(null, "a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.removeAll(Collections.singletonList(null)));
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void testRetainAll() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b", "c"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.retainAll(Arrays.asList("a", "c")));
        assertEquals(2, list.size());
        assertTrue(list.contains("a") && list.contains("c"));
    }

    @Test
    void testRetainAllNoChange() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Arrays.asList("a", "b")));
        assertFalse(list.retainAll(Arrays.asList("a", "b")));
    }

    @Test
    void testContainsExisting() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Collections.singletonList("a")));
        assertTrue(list.contains("a"));
    }

    @Test
    void testContainsNonExisting() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertFalse(list.contains("a"));
    }

    @Test
    void testContainsNullWhenPresent() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList(null, "a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.contains(null));
    }

    @Test
    void testIndexOfExisting() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b", "a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(0, list.indexOf("a"));
    }

    @Test
    void testIndexOfNonExisting() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertEquals(-1, list.indexOf("a"));
    }

    @Test
    void testIndexOfNullWhenPresent() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", null));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(1, list.indexOf(null));
    }

    @Test
    void testLastIndexOf() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b", "a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(2, list.lastIndexOf("a"));
    }

    @Test
    void testLastIndexOfNonExisting() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertEquals(-1, list.lastIndexOf("a"));
    }

    @Test
    void testLastIndexOfNullWhenPresent() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList(null, "a", null));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(2, list.lastIndexOf(null));
    }

    @Test
    void testToArray() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[]{"a", "b"}, array);
    }

    @Test
    void testToArrayWithType() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        String[] array = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, array);
    }

    @Test
    void testToArrayWithLargerTypeArray() {
        ArrayList<String> delegate = new ArrayList<>(Collections.singletonList("a"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        String[] array = new String[2];
        array = list.toArray(array);
        assertEquals("a", array[0]);
        assertNull(array[1]);
    }

    @Test
    void testEqualsSameList() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertTrue(list.equals(delegate));
    }

    @Test
    void testEqualsDifferentList() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>(Arrays.asList("a", "b")));
        List<String> other = Arrays.asList("a", "c");
        assertFalse(list.equals(other));
    }

    @Test
    void testEqualsWithNull() {
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(new ArrayList<>());
        assertFalse(list.equals(null));
    }

    @Test
    void testHashCode() {
        ArrayList<String> delegate = new ArrayList<>(Arrays.asList("a", "b"));
        NonNullElementWrapperList<String> list = new NonNullElementWrapperList<>(delegate);
        assertEquals(delegate.hashCode(), list.hashCode());
    }

}