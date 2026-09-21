package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonArrayTest {

    @Test
    void testConstructorDefaultCapacity() {
        JsonArray array = new JsonArray();
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test
    void testConstructorWithCapacity() {
        JsonArray array = new JsonArray(10);
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test
    void testDeepCopy() {
        JsonArray original = new JsonArray();
        original.add("test");
        JsonArray copy = original.deepCopy();
        original.set(0, new JsonPrimitive("changed"));
        assertEquals("test", copy.get(0).getAsString());
    }

    @Test
    void testAddBoolean() {
        JsonArray array = new JsonArray();
        array.add(true);
        array.add((Boolean) null);
        assertEquals(2, array.size());
        assertTrue(array.get(0).getAsBoolean());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testAddCharacter() {
        JsonArray array = new JsonArray();
        array.add('a');
        array.add((Character) null);
        assertEquals(2, array.size());
        assertEquals('a', array.get(0).getAsCharacter());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testAddNumber() {
        JsonArray array = new JsonArray();
        array.add(123);
        array.add((Number) null);
        assertEquals(2, array.size());
        assertEquals(123, array.get(0).getAsInt());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testAddString() {
        JsonArray array = new JsonArray();
        array.add("test");
        array.add((String) null);
        assertEquals(2, array.size());
        assertEquals("test", array.get(0).getAsString());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testAddJsonElement() {
        JsonArray array = new JsonArray();
        JsonArray temp = new JsonArray();
        temp.add("element");
        array.add(temp.get(0));
        array.add((JsonElement) null);
        assertEquals(2, array.size());
        assertEquals("element", array.get(0).getAsString());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testAddAll() {
        JsonArray source = new JsonArray();
        source.add("a");
        source.add("b");
        
        JsonArray target = new JsonArray();
        target.addAll(source);
        
        assertEquals(2, target.size());
        assertEquals("a", target.get(0).getAsString());
        assertEquals("b", target.get(1).getAsString());
    }

    @Test
    void testSet() {
        JsonArray array = new JsonArray();
        array.add("original");
        JsonArray temp = new JsonArray();
        temp.add("new");
        JsonElement oldElement = array.set(0, temp.get(0));
        assertEquals("original", oldElement.getAsString());
        assertEquals("new", array.get(0).getAsString());
        
        JsonElement nullElement = array.set(0, null);
        assertTrue(array.get(0).isJsonNull());
    }

    @Test
    void testRemoveByElement() {
        JsonArray array = new JsonArray();
        JsonArray temp = new JsonArray();
        temp.add("test");
        JsonElement element = temp.get(0);
        array.add(element);
        boolean removed = array.remove(element);
        assertTrue(removed);
        assertTrue(array.isEmpty());
        
        boolean notRemoved = array.remove(element);
        assertFalse(notRemoved);
    }

    @Test
    void testRemoveByIndex() {
        JsonArray array = new JsonArray();
        array.add("first");
        array.add("second");
        JsonElement removed = array.remove(0);
        assertEquals("first", removed.getAsString());
        assertEquals(1, array.size());
        assertEquals("second", array.get(0).getAsString());
    }

    @Test
    void testContains() {
        JsonArray array = new JsonArray();
        JsonArray temp1 = new JsonArray();
        temp1.add("value");
        JsonElement element = temp1.get(0);
        array.add(element);
        assertTrue(array.contains(element));
        
        JsonArray temp2 = new JsonArray();
        temp2.add("other");
        assertFalse(array.contains(temp2.get(0)));
    }

    @Test
    void testSize() {
        JsonArray array = new JsonArray();
        assertEquals(0, array.size());
        array.add("test");
        assertEquals(1, array.size());
        array.remove(0);
        assertEquals(0, array.size());
    }

    @Test
    void testIsEmpty() {
        JsonArray array = new JsonArray();
        assertTrue(array.isEmpty());
        array.add("test");
        assertFalse(array.isEmpty());
        array.remove(0);
        assertTrue(array.isEmpty());
    }

    @Test
    void testIterator() {
        JsonArray array = new JsonArray();
        array.add("a");
        array.add("b");
        Iterator<JsonElement> it = array.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next().getAsString());
        assertTrue(it.hasNext());
        assertEquals("b", it.next().getAsString());
        assertFalse(it.hasNext());
    }

    @Test
    void testGet() {
        JsonArray array = new JsonArray();
        array.add("value");
        assertEquals("value", array.get(0).getAsString());
    }

    @Test
    void testGetAsNumber() {
        JsonArray array = new JsonArray();
        array.add(123);
        assertEquals(123, array.getAsNumber().intValue());
    }

    @Test
    void testGetAsString() {
        JsonArray array = new JsonArray();
        array.add("text");
        assertEquals("text", array.getAsString());
    }

    @Test
    void testGetAsDouble() {
        JsonArray array = new JsonArray();
        array.add(12.34);
        assertEquals(12.34, array.getAsDouble(), 0.001);
    }

    @Test
    void testGetAsBigDecimal() {
        JsonArray array = new JsonArray();
        array.add(new BigDecimal("123.456"));
        assertEquals(new BigDecimal("123.456"), array.getAsBigDecimal());
    }

    @Test
    void testGetAsBigInteger() {
        JsonArray array = new JsonArray();
        array.add(new BigInteger("123456"));
        assertEquals(new BigInteger("123456"), array.getAsBigInteger());
    }

    @Test
    void testGetAsFloat() {
        JsonArray array = new JsonArray();
        array.add(12.34f);
        assertEquals(12.34f, array.getAsFloat(), 0.001);
    }

    @Test
    void testGetAsLong() {
        JsonArray array = new JsonArray();
        array.add(123456789L);
        assertEquals(123456789L, array.getAsLong());
    }

    @Test
    void testGetAsInt() {
        JsonArray array = new JsonArray();
        array.add(123);
        assertEquals(123, array.getAsInt());
    }

    @Test
    void testGetAsByte() {
        JsonArray array = new JsonArray();
        array.add(127);
        assertEquals(127, array.getAsByte());
    }

    @Test
    void testGetAsCharacter() {
        JsonArray array = new JsonArray();
        array.add('a');
        assertEquals('a', array.getAsCharacter());
    }

    @Test
    void testGetAsShort() {
        JsonArray array = new JsonArray();
        array.add(32767);
        assertEquals(32767, array.getAsShort());
    }

    @Test
    void testGetAsBoolean() {
        JsonArray array = new JsonArray();
        array.add(true);
        assertTrue(array.getAsBoolean());
    }

    @Test
    void testAsList() {
        JsonArray array = new JsonArray();
        array.add("a");
        array.add("b");
        List<JsonElement> list = array.asList();
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).getAsString());
        assertEquals("b", list.get(1).getAsString());
    }

    @Test
    void testEquals() {
        JsonArray array1 = new JsonArray();
        array1.add("test");
        JsonArray array2 = new JsonArray();
        array2.add("test");
        JsonArray array3 = new JsonArray();
        array3.add("different");
        
        assertTrue(array1.equals(array2));
        assertFalse(array1.equals(array3));
        assertFalse(array1.equals(null));
    }

    @Test
    void testHashCode() {
        JsonArray array1 = new JsonArray();
        array1.add("test");
        JsonArray array2 = new JsonArray();
        array2.add("test");
        assertEquals(array1.hashCode(), array2.hashCode());
    }
}