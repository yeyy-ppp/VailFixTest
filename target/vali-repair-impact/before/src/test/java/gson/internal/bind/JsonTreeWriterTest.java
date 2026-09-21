package gson.internal.bind;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import gson.JsonArray;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import java.io.IOException;

class JsonTreeWriterTest {

    @Test
    void testConstructor() {
        JsonTreeWriter writer = new JsonTreeWriter();
        assertSame(JsonNull.INSTANCE, writer.get());
    }

    @Test
    void testGetWithNonEmptyStackThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        assertThrows(IllegalStateException.class, writer::get);
    }

    @Test
    void testBeginEndArray() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        writer.value(1);
        writer.endArray();
        JsonArray array = (JsonArray) writer.get();
        assertEquals(1, array.get(0).getAsInt());
    }

    @Test
    void testBeginEndObject() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginObject();
        writer.name("key");
        writer.value("value");
        writer.endObject();
        JsonObject obj = (JsonObject) writer.get();
        assertEquals("value", obj.get("key").getAsString());
    }

    @Test
    void testNameWithoutObjectThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        assertThrows(IllegalStateException.class, () -> writer.name("test"));
    }

    @Test
    void testValueString() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        writer.value("text");
        writer.value((String) null);
        writer.endArray();
        JsonArray array = (JsonArray) writer.get();
        assertEquals("text", array.get(0).getAsString());
        assertTrue(array.get(1).isJsonNull());
    }

    @Test
    void testValueBoolean() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        writer.value(true);
        writer.value(false);
        writer.value((Boolean) null);
        writer.endArray();
        JsonArray array = (JsonArray) writer.get();
        assertTrue(array.get(0).getAsBoolean());
        assertFalse(array.get(1).getAsBoolean());
        assertTrue(array.get(2).isJsonNull());
    }

    @Test
    void testValueFloatNonLenientThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.setLenient(false);
        writer.beginArray();
        assertThrows(IllegalArgumentException.class, () -> writer.value(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> writer.value(Float.POSITIVE_INFINITY));
    }

    @Test
    void testValueDoubleNonLenientThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.setLenient(false);
        writer.beginArray();
        assertThrows(IllegalArgumentException.class, () -> writer.value(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> writer.value(Double.POSITIVE_INFINITY));
    }

    @Test
    void testValueNumber() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        writer.value(100);
        writer.value(200L);
        writer.value(3.14);
        writer.value((Number) null);
        writer.endArray();
        JsonArray array = (JsonArray) writer.get();
        assertEquals(100, array.get(0).getAsInt());
        assertEquals(200L, array.get(1).getAsLong());
        assertEquals(3.14, array.get(2).getAsDouble());
        assertTrue(array.get(3).isJsonNull());
    }

    @Test
    void testNullValue() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginObject();
        writer.name("key");
        writer.nullValue();
        writer.endObject();
        JsonObject obj = (JsonObject) writer.get();
        assertTrue(obj.has("key"));
        assertTrue(obj.get("key").isJsonNull());
    }

    @Test
    void testJsonValueThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        assertThrows(UnsupportedOperationException.class, () -> writer.jsonValue("test"));
    }

    @Test
    void testFlushDoesNothing() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.flush();
    }

    @Test
    void testCloseWithEmptyStack() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.close();
    }

    @Test
    void testCloseWithNonEmptyStackThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        assertThrows(IOException.class, writer::close);
    }

    @Test
    void testPutInObjectWithNullValueAndSerializeNulls() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.setSerializeNulls(true);
        writer.beginObject();
        writer.name("key");
        writer.nullValue();
        writer.endObject();
        JsonObject obj = (JsonObject) writer.get();
        assertTrue(obj.has("key"));
    }

    @Test
    void testPutInObjectWithNullValueWithoutSerializeNulls() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.setSerializeNulls(false);
        writer.beginObject();
        writer.name("key");
        writer.nullValue();
        writer.endObject();
        JsonObject obj = (JsonObject) writer.get();
        assertFalse(obj.has("key"));
    }

    @Test
    void testEndArrayWithPendingNameThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginObject();
        writer.name("test");
        assertThrows(IllegalStateException.class, writer::endArray);
    }

    @Test
    void testEndObjectWithPendingNameThrows() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginObject();
        writer.name("test");
        assertThrows(IllegalStateException.class, writer::endObject);
    }

    @Test
    void testValueLong() throws IOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        writer.beginArray();
        writer.value(123456789L);
        writer.endArray();
        JsonArray array = (JsonArray) writer.get();
        assertEquals(123456789L, array.get(0).getAsLong());
    }
}