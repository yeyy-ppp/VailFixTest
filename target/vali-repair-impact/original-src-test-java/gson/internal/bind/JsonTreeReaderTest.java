package gson.internal.bind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import gson.JsonArray;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import gson.stream.JsonToken;
import gson.stream.MalformedJsonException;
import java.io.IOException;

class JsonTreeReaderTest {

    private JsonTreeReader reader;

    @BeforeEach
    void setup() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", "value");
        obj.add("nested", new JsonObject());
        
        JsonArray arr = new JsonArray();
        arr.add(10);
        arr.add(true);
        
        JsonObject root = new JsonObject();
        root.add("obj", obj);
        root.add("arr", arr);
        root.add("null", JsonNull.INSTANCE);
        
        reader = new JsonTreeReader(root);
    }

    @Test
    void testConstructor() throws IOException {
        JsonTreeReader r = new JsonTreeReader(JsonNull.INSTANCE);
        assertEquals(JsonToken.NULL, r.peek());
    }
/*
    @Test
    void testBeginEndArray() throws IOException {
        reader.beginObject();
        reader.nextName();
        reader.nextName();
        reader.beginArray();
        assertEquals(JsonToken.NUMBER, reader.peek());
        reader.nextInt();
        reader.nextBoolean();
        reader.endArray();
        assertEquals(JsonToken.NAME, reader.peek());
    }*/

    @Test
    void testBeginEndObject() throws IOException {
        reader.beginObject();
        reader.nextName();
        reader.beginObject();
        reader.nextName();
        reader.nextString();
        reader.nextName();
        reader.beginObject();
        reader.endObject();
        reader.endObject();
        assertEquals(JsonToken.NAME, reader.peek());
    }

    @Test
    void testHasNext() throws IOException {
        reader.beginObject();
        assertTrue(reader.hasNext());
        reader.nextName();
        reader.beginObject();
        assertTrue(reader.hasNext());
        reader.nextName();
        reader.nextString();
        assertTrue(reader.hasNext());
    }

    @Test
    void testPeek() throws IOException {
        assertEquals(JsonToken.BEGIN_OBJECT, reader.peek());
        reader.beginObject();
        assertEquals(JsonToken.NAME, reader.peek());
    }

    @Test
    void testNextName() throws IOException {
        reader.beginObject();
        assertEquals("obj", reader.nextName());
    }

    @Test
    void testNextString() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive("text"));
        assertEquals("text", r.nextString());
    }

    @Test
    void testNextBoolean() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive(true));
        assertTrue(r.nextBoolean());
    }

    @Test
    void testNextNull() throws IOException {
        JsonTreeReader r = new JsonTreeReader(JsonNull.INSTANCE);
        r.nextNull();
        assertEquals(JsonToken.END_DOCUMENT, r.peek());
    }

    @Test
    void testNextDouble() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive(3.14));
        assertEquals(3.14, r.nextDouble());
    }

    @Test
    void testNextDoubleNaN() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive(Double.NaN));
        r.setLenient(false);
        assertThrows(MalformedJsonException.class, r::nextDouble);
    }

    @Test
    void testNextLong() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive(1000L));
        assertEquals(1000L, r.nextLong());
    }

    @Test
    void testNextInt() throws IOException {
        JsonTreeReader r = new JsonTreeReader(new JsonPrimitive(42));
        assertEquals(42, r.nextInt());
    }

    @Test
    void testNextJsonElement() throws IOException {
        JsonPrimitive element = new JsonPrimitive("test");
        JsonTreeReader r = new JsonTreeReader(element);
        assertEquals(element, r.nextJsonElement());
    }

    @Test
    void testClose() throws IOException {
        reader.close();
        assertThrows(IllegalStateException.class, () -> reader.peek());
    }

    @Test
    void testSkipValue() throws IOException {
        reader.beginObject();
        reader.nextName();
        reader.skipValue();
        assertEquals(JsonToken.NAME, reader.peek());
    }

    @Test
    void testToString() {
        assertTrue(reader.toString().contains("JsonTreeReader"));
    }

    @Test
    void testPromoteNameToValue() throws IOException {
        reader.beginObject();
        reader.promoteNameToValue();
        assertEquals(JsonToken.STRING, reader.peek());
        assertEquals("obj", reader.nextString());
    }

    @Test
    void testPushExpansion() throws Exception {
        JsonTreeReader r = new JsonTreeReader(JsonNull.INSTANCE);
        java.lang.reflect.Method pushMethod = JsonTreeReader.class.getDeclaredMethod("push", Object.class);
        pushMethod.setAccessible(true);
        for (int i = 0; i < 40; i++) {
            pushMethod.invoke(r, new JsonPrimitive(i));
        }
        assertEquals(JsonToken.NUMBER, r.peek());
    }

    @Test
    void testGetPath() throws IOException {
        reader.beginObject();
        reader.nextName();
        reader.beginObject();
        assertTrue(reader.getPath().contains(".obj"));
    }

    @Test
    void testGetPreviousPath() throws IOException {
        reader.beginObject();
        reader.nextName();
        reader.beginObject();
        reader.nextName();
        reader.nextString();
        assertTrue(reader.getPreviousPath().contains(".obj.name"));
    }


    @Test
    void testPeekStack() throws Exception {
        java.lang.reflect.Method method = JsonTreeReader.class.getDeclaredMethod("peekStack");
        method.setAccessible(true);
        assertTrue(method.invoke(reader) instanceof JsonObject);
    }

    @Test
    void testPopStack() throws Exception {
        java.lang.reflect.Method method = JsonTreeReader.class.getDeclaredMethod("popStack");
        method.setAccessible(true);
        method.invoke(reader);

    }

    @Test
    void testEmptyDocument() throws IOException {
        JsonTreeReader r = new JsonTreeReader(JsonNull.INSTANCE);
        assertEquals(JsonToken.NULL, r.peek());
    }

    @Test
    void testNestedArrays() throws IOException {
        JsonArray inner = new JsonArray();
        inner.add(1);
        
        JsonArray outer = new JsonArray();
        outer.add(inner);
        
        JsonTreeReader r = new JsonTreeReader(outer);
        r.beginArray();
        r.beginArray();
        r.nextInt();
        r.endArray();
        r.endArray();
        assertEquals(JsonToken.END_DOCUMENT, r.peek());
    }


    @Test
    void testPathIndicesIncrement() throws IOException {
        JsonArray arr = new JsonArray();
        arr.add(1);
        arr.add(2);
        JsonTreeReader r = new JsonTreeReader(arr);
        r.beginArray();
        r.nextInt();
        r.nextInt();
        r.endArray();
        assertEquals(JsonToken.END_DOCUMENT, r.peek());
    }
}