package gson.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Comparator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Iterator;
import java.util.ConcurrentModificationException;

public class LinkedTreeMapTest {

    @Test
    void testDefaultConstructor() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        assertEquals(0, map.size());
        map.put("A", null);
        assertNull(map.get("A"));
    }

    @Test
    void testConstructorWithAllowNullValues() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>(false);
        assertEquals(0, map.size());
        assertThrows(NullPointerException.class, () -> map.put("A", null));
    }

    @Test
    void testConstructorWithComparatorAndAllowNullValues() {
        Comparator<String> comparator = Comparator.reverseOrder();
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>(comparator, true);
        assertEquals(0, map.size());
        map.put("A", null);
        assertNull(map.get("A"));
    }

    @Test
    void testSize() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        assertEquals(1, map.size());
        map.put("B", 2);
        assertEquals(2, map.size());
    }

    @Test
    void testGet() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);
        assertEquals(1, map.get("A"));
        assertEquals(2, map.get("B"));
        assertNull(map.get("C"));
    }

    @Test
    void testContainsKey() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        assertTrue(map.containsKey("A"));
        assertFalse(map.containsKey("B"));
    }

    @Test
    void testPut() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        assertNull(map.put("A", 1));
        assertEquals(1, map.put("A", 2));
        assertEquals(2, map.get("A"));
    }

    @Test
    void testPutNullKey() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    void testPutNullValueWhenNotAllowed() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>(false);
        assertThrows(NullPointerException.class, () -> map.put("A", null));
    }

    @Test
    void testPutNullValueWhenAllowed() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", null);
        assertNull(map.get("A"));
    }

    @Test
    void testClear() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        map.clear();
        assertEquals(0, map.size());
        assertFalse(map.containsKey("A"));
    }

    @Test
    void testRemove() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        assertEquals(1, map.remove("A"));
        assertNull(map.remove("A"));
        assertEquals(0, map.size());
    }

    @Test
    void testFindCreateNodeWhenRootIsNull() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        LinkedTreeMap.Node<String, Integer> node = map.find("A", true);
        assertNotNull(node);
        assertEquals("A", node.key);
    }

    @Test
    void testFindDoNotCreateNodeWhenKeyNotFound() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("B", 1);
        assertNull(map.find("A", false));
    }

    @Test
    void testFindByObject() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        assertNotNull(map.findByObject("A"));
        assertNull(map.findByObject("B"));
    }

    @Test
    void testFindByObjectWithIncompatibleType() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        assertNull(map.findByObject(123));
    }

    @Test
    void testFindByEntry() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("A", 1);
        assertNotNull(map.findByEntry(entry));
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("A", 2);
        assertNull(map.findByEntry(entry2));
        Map.Entry<String, Integer> entry3 = new AbstractMap.SimpleEntry<>("B", 1);
        assertNull(map.findByEntry(entry3));
    }

    @Test
    void testRemoveInternal() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        LinkedTreeMap.Node<String, Integer> node = map.findByObject("B");
        map.removeInternal(node, true);
        assertFalse(map.containsKey("B"));
        assertEquals(2, map.size());
    }

    @Test
    void testRemoveInternalByKey() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        LinkedTreeMap.Node<String, Integer> node = map.removeInternalByKey("A");
        assertNotNull(node);
        assertEquals(0, map.size());
    }

    @Test
    void testRebalance() {
        LinkedTreeMap<Integer, Integer> map = new LinkedTreeMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(i, i);
        }
        assertEquals(1000, map.size());
    }

    @Test
    void testEntrySet() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());
        Iterator<Map.Entry<String, Integer>> iterator = entrySet.iterator();
        assertTrue(iterator.hasNext());
        Map.Entry<String, Integer> entry = iterator.next();
        assertEquals("A", entry.getKey());
        iterator.remove();
        assertEquals(1, map.size());
    }

    @Test
    void testKeySet() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);
        Set<String> keySet = map.keySet();
        assertTrue(keySet.contains("A"));
        keySet.remove("B");
        assertFalse(map.containsKey("B"));
    }

    @Test
    void testKeySetIterator() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        Iterator<String> iterator = map.keySet().iterator();
        assertTrue(iterator.hasNext());
        assertEquals("A", iterator.next());
        iterator.remove();
        assertEquals(0, map.size());
    }

    @Test
    void testEntrySetIteratorConcurrentModification() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        map.put("B", 2);
        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void testKeySetIteratorRemoveWithoutNext() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        Iterator<String> iterator = map.keySet().iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    void testEntrySetIteratorRemove() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        map.put("A", 1);
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        iterator.next();
        iterator.remove();
        assertTrue(map.isEmpty());
    }

    @Test
    void testKeySetIteratorNoSuchElement() {
        LinkedTreeMap<String, Integer> map = new LinkedTreeMap<>();
        Iterator<String> iterator = map.keySet().iterator();
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}