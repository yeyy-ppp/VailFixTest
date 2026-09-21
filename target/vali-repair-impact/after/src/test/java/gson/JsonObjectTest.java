package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import gson.JsonArray;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import java.util.Map;
import java.util.Set;

class JsonObjectTest {

    @Test
    void testConstructor() {
        JsonObject obj = new JsonObject();
        assertTrue(obj.isEmpty());
        assertEquals(0, obj.size());
    }

    @Test
    void testDeepCopy() {
        JsonObject original = new JsonObject();
        original.addProperty("key", "value");
        JsonObject copy = original.deepCopy();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        
        original.addProperty("newKey", 123);
        assertFalse(copy.has("newKey"));
    }

    @Test
    void testAdd() {
        JsonObject obj = new JsonObject();
        obj.add("key", null);
        assertEquals(JsonNull.INSTANCE, obj.get("key"));
        
        JsonPrimitive primitive = new JsonPrimitive("test");
        obj.add("key2", primitive);
        assertSame(primitive, obj.get("key2"));
    }

    @Test
    void testRemove() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", "value");
        JsonElement removed = obj.remove("key");
        assertEquals(new JsonPrimitive("value"), removed);
        assertNull(obj.remove("nonExistent"));
    }

    @Test
    void testAddPropertyString() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", "value");
        assertEquals(new JsonPrimitive("value"), obj.get("key"));
        
        obj.addProperty("nullKey", (String) null);
        assertEquals(JsonNull.INSTANCE, obj.get("nullKey"));
    }

    @Test
    void testAddPropertyNumber() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", 123);
        assertEquals(new JsonPrimitive(123), obj.get("key"));
        
        obj.addProperty("nullKey", (Number) null);
        assertEquals(JsonNull.INSTANCE, obj.get("nullKey"));
    }

    @Test
    void testAddPropertyBoolean() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", true);
        assertEquals(new JsonPrimitive(true), obj.get("key"));
        
        obj.addProperty("nullKey", (Boolean) null);
        assertEquals(JsonNull.INSTANCE, obj.get("nullKey"));
    }

    @Test
    void testAddPropertyCharacter() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", 'a');
        assertEquals(new JsonPrimitive("a"), obj.get("key"));
        
        obj.addProperty("nullKey", (Character) null);
        assertEquals(JsonNull.INSTANCE, obj.get("nullKey"));
    }

    @Test
    void testEntrySet() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", "value");
        Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, JsonElement> entry = entries.iterator().next();
        assertEquals("key", entry.getKey());
        assertEquals(new JsonPrimitive("value"), entry.getValue());
    }

    @Test
    void testKeySet() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key1", "value1");
        obj.addProperty("key2", "value2");
        Set<String> keys = obj.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    void testSize() {
        JsonObject obj = new JsonObject();
        assertEquals(0, obj.size());
        obj.addProperty("key", "value");
        assertEquals(1, obj.size());
    }

    @Test
    void testIsEmpty() {
        JsonObject obj = new JsonObject();
        assertTrue(obj.isEmpty());
        obj.addProperty("key", "value");
        assertFalse(obj.isEmpty());
    }

    @Test
    void testHas() {
        JsonObject obj = new JsonObject();
        assertFalse(obj.has("key"));
        obj.addProperty("key", "value");
        assertTrue(obj.has("key"));
    }

    @Test
    void testGet() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", "value");
        assertEquals(new JsonPrimitive("value"), obj.get("key"));
        assertNull(obj.get("nonExistent"));
    }

    @Test
    void testGetAsJsonPrimitive() {
        JsonObject obj = new JsonObject();
        obj.add("primitive", new JsonPrimitive("test"));
        assertEquals(new JsonPrimitive("test"), obj.getAsJsonPrimitive("primitive"));
        
        obj.add("array", new JsonArray());
        assertThrows(ClassCastException.class, () -> obj.getAsJsonPrimitive("array"));
    }

    @Test
    void testGetAsJsonArray() {
        JsonObject obj = new JsonObject();
        JsonArray array = new JsonArray();
        obj.add("array", array);
        assertSame(array, obj.getAsJsonArray("array"));
        
        obj.add("primitive", new JsonPrimitive("test"));
        assertThrows(ClassCastException.class, () -> obj.getAsJsonArray("primitive"));
    }

    @Test
    void testGetAsJsonObject() {
        JsonObject obj = new JsonObject();
        JsonObject nested = new JsonObject();
        obj.add("nested", nested);
        assertSame(nested, obj.getAsJsonObject("nested"));
        
        obj.add("primitive", new JsonPrimitive("test"));
        assertThrows(ClassCastException.class, () -> obj.getAsJsonObject("primitive"));
    }

    @Test
    void testAsMap() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", "value");
        Map<String, JsonElement> map = obj.asMap();
        assertEquals(1, map.size());
        assertEquals(new JsonPrimitive("value"), map.get("key"));
    }

    @Test
    void testEquals() {
        JsonObject obj1 = new JsonObject();
        obj1.addProperty("key", "value");
        
        JsonObject obj2 = new JsonObject();
        obj2.addProperty("key", "value");
        
        JsonObject obj3 = new JsonObject();
        obj3.addProperty("different", "value");
        
        assertEquals(obj1, obj1);
        assertEquals(obj1, obj2);
        assertNotEquals(obj1, obj3);
        assertNotEquals(obj1, null);
        assertNotEquals(obj1, new Object());
    }

    @Test
    void testHashCode() {
        JsonObject obj1 = new JsonObject();
        obj1.addProperty("key", "value");
        
        JsonObject obj2 = new JsonObject();
        obj2.addProperty("key", "value");
        
        assertEquals(obj1.hashCode(), obj2.hashCode());
    }
}