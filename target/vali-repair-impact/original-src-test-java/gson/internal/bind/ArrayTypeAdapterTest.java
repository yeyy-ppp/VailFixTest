package gson.internal.bind;

import gson.Gson;
import gson.TypeAdapter;
import gson.internal.GsonTypes;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayTypeAdapterTest {

    @Test
    void create_WithNonArrayType_ReturnsNull() {
        TypeAdapter<?> adapter = ArrayTypeAdapter.FACTORY.create(new Gson(), TypeToken.get(String.class));
        assertNull(adapter);
    }

    @Test
    void create_WithObjectArrayType_ReturnsAdapter() {
        TypeToken<String[]> typeToken = TypeToken.get(String[].class);
        TypeAdapter<?> adapter = ArrayTypeAdapter.FACTORY.create(new Gson(), typeToken);
        assertNotNull(adapter);
    }

    @Test
    void create_WithGenericArrayType_ReturnsAdapter() {
        Type componentType = String.class;
        GenericArrayType arrayType = GsonTypes.arrayOf(componentType);
        TypeToken<?> typeToken = TypeToken.get(arrayType);
        TypeAdapter<?> adapter = ArrayTypeAdapter.FACTORY.create(new Gson(), typeToken);
        assertNotNull(adapter);
    }

    @Test
    void read_WithNullValue_ReturnsNull() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("null"));
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        assertNull(adapter.read(reader));
    }

    @Test
    void read_WithEmptyArray_ReturnsEmptyArray() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[]"));
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        String[] result = adapter.read(reader);
        assertEquals(0, result.length);
    }

    @Test
    void read_WithObjectArray_ReturnsCorrectArray() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[\"a\",\"b\"]"));
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        String[] result = adapter.read(reader);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    void read_WithPrimitiveArray_ReturnsCorrectArray() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[1,2]"));
        TypeAdapter<int[]> adapter = createIntArrayAdapter();
        int[] result = adapter.read(reader);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void write_WithNullArray_WritesNull() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        adapter.write(writer, null);
        writer.flush();
        assertEquals("null", stringWriter.toString());
    }

    @Test
    void write_WithEmptyArray_WritesEmptyArray() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        adapter.write(writer, new String[0]);
        writer.flush();
        assertEquals("[]", stringWriter.toString());
    }

    @Test
    void write_WithObjectArray_WritesCorrectJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        TypeAdapter<String[]> adapter = createStringArrayAdapter();
        adapter.write(writer, new String[]{"a", "b"});
        writer.flush();
        assertEquals("[\"a\",\"b\"]", stringWriter.toString());
    }

    @Test
    void write_WithPrimitiveArray_WritesCorrectJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        TypeAdapter<int[]> adapter = createIntArrayAdapter();
        adapter.write(writer, new int[]{1, 2});
        writer.flush();
        assertEquals("[1,2]", stringWriter.toString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TypeAdapter<String[]> createStringArrayAdapter() {
        Gson gson = new Gson();
        TypeToken<String[]> typeToken = TypeToken.get(String[].class);
        Type componentType = GsonTypes.getArrayComponentType(typeToken.getType());
        TypeAdapter<?> componentAdapter = gson.getAdapter(TypeToken.get(componentType));
        return (TypeAdapter<String[]>) new ArrayTypeAdapter(gson, componentAdapter, String.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TypeAdapter<int[]> createIntArrayAdapter() {
        Gson gson = new Gson();
        TypeToken<int[]> typeToken = TypeToken.get(int[].class);
        Type componentType = GsonTypes.getArrayComponentType(typeToken.getType());
        TypeAdapter<?> componentAdapter = gson.getAdapter(TypeToken.get(componentType));
        return (TypeAdapter<int[]>) new ArrayTypeAdapter(gson, componentAdapter, int.class);
    }
}