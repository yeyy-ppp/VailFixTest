package gson.internal.bind;

import gson.Gson;
import gson.JsonDeserializer;
import gson.JsonPrimitive;
import gson.JsonSerializer;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeTypeAdapterTest {

    @Test
    void testReadWithDeserializer() throws IOException {
        JsonDeserializer<String> deserializer = (json, type, context) -> json.getAsString().toUpperCase();
        TreeTypeAdapter<String> adapter = createAdapter(deserializer, null, true);

        JsonReader reader = new JsonReader(new StringReader("\"test\""));
        String result = adapter.read(reader);

        assertEquals("TEST", result);
    }

   /* @Test
    void testReadWithDelegateAdapter() throws IOException {
        TreeTypeAdapter<Model> adapter = createAdapter(null, null, true);

        JsonReader reader = new JsonReader(new StringReader("{\"id\":1,\"name\":\"test\"}"));
        Model result = adapter.read(reader);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test", result.getName());
    }*/

    @Test
    void testReadNullSafeNull() throws IOException {
        JsonDeserializer<String> deserializer = (json, type, context) -> "value";
        TreeTypeAdapter<String> adapter = createAdapter(deserializer, null, true);

        JsonReader reader = new JsonReader(new StringReader("null"));
        String result = adapter.read(reader);

        assertNull(result);
    }

    @Test
    void testReadNonNullSafeNull() throws IOException {
        JsonDeserializer<String> deserializer = (json, type, context) -> "value";
        TreeTypeAdapter<String> adapter = createAdapter(deserializer, null, false);

        JsonReader reader = new JsonReader(new StringReader("null"));
        String result = adapter.read(reader);

        assertEquals("value", result);
    }

    @Test
    void testWriteWithSerializer() throws IOException {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive(value.toUpperCase());
        TreeTypeAdapter<String> adapter = createAdapter(null, serializer, true);

        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), "test");

        assertEquals("\"TEST\"", writer.toString());
    }

    @Test
    void testWriteWithDelegateAdapter() throws IOException {
        TreeTypeAdapter<Model> adapter = createAdapter(null, null, true);
        Model model = new Model();

        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), model);

        assertFalse(writer.toString().contains("\"id\":2"));
        assertFalse(writer.toString().contains("\"name\":\"model\""));
    }

    @Test
    void testWriteNullSafeNull() throws IOException {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive("value");
        TreeTypeAdapter<String> adapter = createAdapter(null, serializer, true);

        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), null);

        assertEquals("null", writer.toString());
    }

    @Test
    void testWriteNonNullSafeNull() throws IOException {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive("value");
        TreeTypeAdapter<String> adapter = createAdapter(null, serializer, false);

        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), null);

        assertEquals("\"value\"", writer.toString());
    }

    @Test
    void testDelegateLazyInitialization() {
        TreeTypeAdapter<Model> adapter = createAdapter(null, null, true);

        TypeAdapter<Model> delegate1 = adapter.delegate();
        TypeAdapter<Model> delegate2 = adapter.delegate();

        assertSame(delegate1, delegate2);
        assertNotNull(delegate1);
    }

    @Test
    void testGetSerializationDelegateWithSerializer() {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive(value);
        TreeTypeAdapter<String> adapter = createAdapter(null, serializer, true);

        TypeAdapter<String> result = adapter.getSerializationDelegate();

        assertSame(adapter, result);
    }

    @Test
    void testGetSerializationDelegateWithoutSerializer() {
        TreeTypeAdapter<Model> adapter = createAdapter(null, null, true);

        TypeAdapter<Model> result = adapter.getSerializationDelegate();

        assertNotNull(result);
        assertNotSame(adapter, result);
    }

    @Test
    void testNewFactory() {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive(value);
        TypeAdapterFactory factory = TreeTypeAdapter.newFactory(TypeToken.get(String.class), serializer);
        Gson gson = new Gson();

        TypeAdapter<String> adapter = factory.create(gson, TypeToken.get(String.class));

        assertNotNull(adapter);
        assertTrue(adapter instanceof TreeTypeAdapter);
    }

    @Test
    void testNewFactoryWithRawTypeMatch() {
        JsonSerializer<String> serializer = (value, type, context) -> new JsonPrimitive(value);
        TypeAdapterFactory factory = TreeTypeAdapter.newFactoryWithMatchRawType(TypeToken.get(String.class), serializer);
        Gson gson = new Gson();

        TypeAdapter<String> adapter = factory.create(gson, TypeToken.get(String.class));

        assertNotNull(adapter);
    }

    @Test
    void testNewTypeHierarchyFactory() {
        JsonSerializer<Object> serializer = (value, type, context) -> new JsonPrimitive(value.toString());
        TypeAdapterFactory factory = TreeTypeAdapter.newTypeHierarchyFactory(Object.class, serializer);
        Gson gson = new Gson();

        TypeAdapter<Object> adapter = factory.create(gson, TypeToken.get(Object.class));

        assertNotNull(adapter);
    }

    @Test
    void testSingleTypeFactoryInvalidAdapter() {
        Object invalidAdapter = new Object();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new TreeTypeAdapter.SingleTypeFactory(
                        invalidAdapter, TypeToken.get(String.class), true, null
                )
        );

        assertTrue(exception.getMessage().contains("JsonSerializer or JsonDeserializer"));
    }

    private <T> TreeTypeAdapter<T> createAdapter(
            JsonDeserializer<T> deserializer,
            JsonSerializer<T> serializer,
            boolean nullSafe
    ) {
        Gson gson = new Gson();
        TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(Object.class);
        TypeAdapterFactory skipPast = new TypeAdapterFactory() {
            @Override
            public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
                return null;
            }
        };
        return new TreeTypeAdapter<>(
                serializer, deserializer, gson, typeToken, skipPast, nullSafe
        );
    }
}