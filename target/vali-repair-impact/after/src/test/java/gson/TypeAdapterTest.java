package gson;

import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import gson.Gson;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TypeAdapterTest {
    private Gson gson;
    private TypeAdapter<String> stringAdapter;

    @BeforeEach
    void setUp() {
        gson = new Gson();
        stringAdapter = gson.getAdapter(String.class);
    }

    @Test
    void testConstructor() {
        assertNotNull(stringAdapter);
    }

    @Test
    void testWrite() throws IOException {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        stringAdapter.write(writer, "test");
        writer.close();
        assertEquals("\"test\"", out.toString());
    }

    @Test
    void testWriteNull() throws IOException {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        stringAdapter.write(writer, null);
        writer.close();
        assertEquals("null", out.toString());
    }

    @Test
    void testToJsonWriterValue() throws IOException {
        StringWriter writer = new StringWriter();
        stringAdapter.toJson(writer, "test");
        assertEquals("\"test\"", writer.toString());
    }

    @Test
    void testToJsonWriterNullValue() throws IOException {
        StringWriter writer = new StringWriter();
        stringAdapter.toJson(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    void testToJsonStringValue() {
        assertEquals("\"test\"", stringAdapter.toJson("test"));
    }

    @Test
    void testToJsonNullValue() {
        assertEquals("null", stringAdapter.toJson(null));
    }

    @Test
    void testToJsonTreeStringValue() {
        JsonElement element = stringAdapter.toJsonTree("test");
        assertTrue(element instanceof JsonPrimitive);
        assertEquals("test", element.getAsString());
    }

    @Test
    void testToJsonTreeNullValue() {
        JsonElement element = stringAdapter.toJsonTree(null);
        assertTrue(element instanceof JsonNull);
    }

    @Test
    void testRead() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("\"test\""));
        assertEquals("test", stringAdapter.read(reader));
    }

    @Test
    void testReadNull() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("null"));
        assertNull(stringAdapter.read(reader));
    }

    @Test
    void testFromJsonReaderValue() throws IOException {
        StringReader reader = new StringReader("\"test\"");
        assertEquals("test", stringAdapter.fromJson(reader));
    }

    @Test
    void testFromJsonReaderNullValue() throws IOException {
        StringReader reader = new StringReader("null");
        assertNull(stringAdapter.fromJson(reader));
    }

    @Test
    void testFromJsonStringValue() throws IOException {
        assertEquals("test", stringAdapter.fromJson("\"test\""));
    }

    @Test
    void testFromJsonStringNullValue() throws IOException {
        assertNull(stringAdapter.fromJson("null"));
    }

    @Test
    void testFromJsonTreePrimitiveValue() {
        JsonPrimitive primitive = new JsonPrimitive("test");
        assertEquals("test", stringAdapter.fromJsonTree(primitive));
    }

    @Test
    void testFromJsonTreeNullValue() {
        JsonNull jsonNull = JsonNull.INSTANCE;
        assertNull(stringAdapter.fromJsonTree(jsonNull));
    }

    @Test
    void testNullSafeWriteNonNull() throws IOException {
        TypeAdapter<String> nullSafeAdapter = stringAdapter.nullSafe();
        StringWriter writer = new StringWriter();
        nullSafeAdapter.write(new JsonWriter(writer), "test");
        assertEquals("\"test\"", writer.toString());
    }

    @Test
    void testNullSafeWriteNull() throws IOException {
        TypeAdapter<String> nullSafeAdapter = stringAdapter.nullSafe();
        StringWriter writer = new StringWriter();
        nullSafeAdapter.write(new JsonWriter(writer), null);
        assertEquals("null", writer.toString());
    }

    @Test
    void testNullSafeReadNonNull() throws IOException {
        TypeAdapter<String> nullSafeAdapter = stringAdapter.nullSafe();
        JsonReader reader = new JsonReader(new StringReader("\"test\""));
        assertEquals("test", nullSafeAdapter.read(reader));
    }

    @Test
    void testNullSafeReadNull() throws IOException {
        TypeAdapter<String> nullSafeAdapter = stringAdapter.nullSafe();
        JsonReader reader = new JsonReader(new StringReader("null"));
        assertNull(nullSafeAdapter.read(reader));
    }

    @Test
    void testNullSafeReturnsSameInstanceWhenAlreadyNullSafe() {
        TypeAdapter<String> original = stringAdapter.nullSafe();
        TypeAdapter<String> wrapped = original.nullSafe();
        assertSame(original, wrapped);
    }

}
