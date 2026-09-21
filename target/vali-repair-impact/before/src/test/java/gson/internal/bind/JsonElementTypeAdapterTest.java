package gson.internal.bind;

import gson.JsonArray;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonElementTypeAdapterTest {

    @Test
    void testReadString() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("\"test\""));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonPrimitive);
        assertEquals("test", element.getAsString());
    }

    @Test
    void testReadNumber() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonPrimitive);
        assertEquals(123.45, element.getAsDouble());
    }

    @Test
    void testReadBoolean() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("true"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonPrimitive);
        assertTrue(element.getAsBoolean());
    }

    @Test
    void testReadNull() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("null"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertEquals(JsonNull.INSTANCE, element);
    }

    @Test
    void testReadEmptyArray() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[]"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonArray);
        assertEquals(0, element.getAsJsonArray().size());
    }

    @Test
    void testReadNestedArray() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[[1,2],3]"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonArray);
        JsonArray array = element.getAsJsonArray();
        assertEquals(2, array.size());
        assertTrue(array.get(0).isJsonArray());
        assertTrue(array.get(1).isJsonPrimitive());
    }

    @Test
    void testReadEmptyObject() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{}"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonObject);
        assertEquals(0, element.getAsJsonObject().size());
    }

    @Test
    void testReadObjectWithNesting() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"a\":{\"b\":2}}"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonObject);
        JsonObject obj = element.getAsJsonObject();
        assertTrue(obj.get("a").isJsonObject());
        assertEquals(2, obj.get("a").getAsJsonObject().get("b").getAsInt());
    }

    @Test
    void testReadComplexStructure() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"arr\":[1,null,true],\"obj\":{\"k\":\"v\"}}"));
        JsonElement element = JsonElementTypeAdapter.ADAPTER.read(reader);
        assertTrue(element instanceof JsonObject);
        JsonObject obj = element.getAsJsonObject();
        JsonArray arr = obj.get("arr").getAsJsonArray();
        assertEquals(3, arr.size());
        assertEquals(1, arr.get(0).getAsInt());
        assertEquals(JsonNull.INSTANCE, arr.get(1));
        assertTrue(arr.get(2).getAsBoolean());
        JsonObject nestedObj = obj.get("obj").getAsJsonObject();
        assertEquals("v", nestedObj.get("k").getAsString());
    }

    @Test
    void testWriteNull() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, JsonNull.INSTANCE);
        writer.flush();
        assertEquals("null", stringWriter.toString());
    }

    @Test
    void testWriteString() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, new JsonPrimitive("test"));
        writer.flush();
        assertEquals("\"test\"", stringWriter.toString());
    }

    @Test
    void testWriteNumber() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, new JsonPrimitive(123));
        writer.flush();
        assertEquals("123", stringWriter.toString());
    }

    @Test
    void testWriteBoolean() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, new JsonPrimitive(true));
        writer.flush();
        assertEquals("true", stringWriter.toString());
    }

    @Test
    void testWriteArray() throws IOException {
        JsonArray array = new JsonArray();
        array.add(1);
        array.add("two");
        array.add(JsonNull.INSTANCE);

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, array);
        writer.flush();
        assertEquals("[1,\"two\",null]", stringWriter.toString());
    }

    @Test
    void testWriteObject() throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("k1", 1);
        obj.add("k2", JsonNull.INSTANCE);
        obj.addProperty("k3", "val");

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, obj);
        writer.flush();
        assertEquals("{\"k1\":1,\"k2\":null,\"k3\":\"val\"}", stringWriter.toString());
    }

    @Test
    void testWriteNestedStructures() throws IOException {
        JsonArray innerArray = new JsonArray();
        innerArray.add(true);
        innerArray.add(false);

        JsonObject innerObj = new JsonObject();
        innerObj.addProperty("num", 123);

        JsonObject outerObj = new JsonObject();
        outerObj.add("arr", innerArray);
        outerObj.add("obj", innerObj);

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        JsonElementTypeAdapter.ADAPTER.write(writer, outerObj);
        writer.flush();
        assertEquals("{\"arr\":[true,false],\"obj\":{\"num\":123}}", stringWriter.toString());
    }

    @Test
    void testTryBeginNesting() throws Exception {
        Method method = JsonElementTypeAdapter.class.getDeclaredMethod("tryBeginNesting", JsonReader.class, JsonToken.class);
        method.setAccessible(true);
        JsonElementTypeAdapter adapter = JsonElementTypeAdapter.ADAPTER;

        JsonReader reader1 = new JsonReader(new StringReader("[]"));
        JsonToken token1 = reader1.peek();
        JsonElement result1 = (JsonElement) method.invoke(adapter, reader1, token1);
        assertNotNull(result1);
        assertTrue(result1 instanceof JsonArray);

        JsonReader reader2 = new JsonReader(new StringReader("{}"));
        JsonToken token2 = reader2.peek();
        JsonElement result2 = (JsonElement) method.invoke(adapter, reader2, token2);
        assertNotNull(result2);
        assertTrue(result2 instanceof JsonObject);

        JsonReader reader3 = new JsonReader(new StringReader("\"hello\""));
        JsonToken token3 = reader3.peek();
        JsonElement result3 = (JsonElement) method.invoke(adapter, reader3, token3);
        assertNull(result3);
    }

    @Test
    void testReadTerminal() throws Exception {
        Method method = JsonElementTypeAdapter.class.getDeclaredMethod("readTerminal", JsonReader.class, JsonToken.class);
        method.setAccessible(true);
        JsonElementTypeAdapter adapter = JsonElementTypeAdapter.ADAPTER;

        JsonReader reader1 = new JsonReader(new StringReader("\"hello\""));
        JsonToken token1 = reader1.peek();
        JsonElement result1 = (JsonElement) method.invoke(adapter, reader1, token1);
        assertTrue(result1 instanceof JsonPrimitive);
        assertEquals("hello", result1.getAsString());

        JsonReader reader2 = new JsonReader(new StringReader("123"));
        JsonToken token2 = reader2.peek();
        JsonElement result2 = (JsonElement) method.invoke(adapter, reader2, token2);
        assertTrue(result2 instanceof JsonPrimitive);
        assertEquals(123, result2.getAsInt());

        JsonReader reader3 = new JsonReader(new StringReader("true"));
        JsonToken token3 = reader3.peek();
        JsonElement result3 = (JsonElement) method.invoke(adapter, reader3, token3);
        assertTrue(result3 instanceof JsonPrimitive);
        assertTrue(result3.getAsBoolean());

        JsonReader reader4 = new JsonReader(new StringReader("false"));
        JsonToken token4 = reader4.peek();
        JsonElement result4 = (JsonElement) method.invoke(adapter, reader4, token4);
        assertTrue(result4 instanceof JsonPrimitive);
        assertFalse(result4.getAsBoolean());

        JsonReader reader5 = new JsonReader(new StringReader("null"));
        JsonToken token5 = reader5.peek();
        JsonElement result5 = (JsonElement) method.invoke(adapter, reader5, token5);
        assertEquals(JsonNull.INSTANCE, result5);

        JsonReader reader6 = new JsonReader(new StringReader("{\"key\":123}"));
        reader6.beginObject();
        JsonToken token6 = reader6.peek();
        try {
            method.invoke(adapter, reader6, token6);
            fail("Expected exception not thrown");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
        }
    }
}
