package gson.internal.bind;

import gson.*;
import gson.internal.*;
import gson.reflect.TypeToken;
import gson.stream.*;
import gson.internal.bind.MapTypeAdapterFactory.Adapter;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MapTypeAdapterFactoryTest {

    private ConstructorConstructor constructorConstructor;
    private Gson gson;
    private MapTypeAdapterFactory factory;
    private TypeAdapter<Map<String, Integer>> adapter;

    @BeforeEach
    void setUp() {
        constructorConstructor = new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList());
        gson = new Gson();
        factory = new MapTypeAdapterFactory(constructorConstructor, true);
        TypeToken<Map<String, Integer>> mapType = new TypeToken<Map<String, Integer>>() {};
        adapter = factory.create(gson, mapType);
    }

    @Test
    void testConstructor() {
        assertNotNull(factory);
        assertEquals(constructorConstructor, getField(factory, "constructorConstructor"));
        assertTrue((boolean) getField(factory, "complexMapKeySerialization"));
    }

    @Test
    void testCreate_NonMapType() {
        TypeAdapter<?> result = factory.create(gson, TypeToken.get(String.class));
        assertNull(result);
    }

    @Test
    void testCreate_MapType() {
        assertNotNull(adapter);
    }

    @Test
    void testRead_Null() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("null"));
        reader.peek();
        assertNull(adapter.read(reader));
    }

    @Test
    void testRead_ObjectFormat() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"a\":1,\"b\":2}"));
        Map<String, Integer> map = adapter.read(reader);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    void testRead_ArrayFormat() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[[\"a\",1],[\"b\",2]]"));
        Map<String, Integer> map = adapter.read(reader);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    void testRead_ArrayFormat_DuplicateKey() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("[[\"a\",1],[\"a\",2]]"));
        reader.beginArray();
        reader.beginArray();
        reader.nextString();
        reader.nextInt();
        reader.endArray();
        reader.beginArray();
        reader.nextString();
        assertThrows(IllegalStateException.class, () -> adapter.read(reader));
    }

    @Test
    void testRead_ObjectFormat_DuplicateKey() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"a\":1,\"a\":2}"));
        reader.beginObject();
        reader.nextName();
        reader.nextInt();
        assertThrows(IllegalStateException.class, () -> adapter.read(reader));
    }

    @Test
    void testWrite_Null() throws IOException {
        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), null);
        assertEquals("null", writer.toString());
    }

    @Test
    void testWrite_WithoutComplexKeySerialization() throws IOException {
        MapTypeAdapterFactory factory = new MapTypeAdapterFactory(constructorConstructor, false);
        TypeAdapter<Map<String, Integer>> simpleAdapter = factory.create(gson, new TypeToken<Map<String, Integer>>() {});
        
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        StringWriter writer = new StringWriter();
        simpleAdapter.write(new JsonWriter(writer), map);
        assertEquals("{\"a\":1}", writer.toString());
    }

    @Test
    void testWrite_WithComplexKeySerialization_NoComplexKey() throws IOException {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), map);
        assertEquals("{\"a\":1}", writer.toString());
    }

    @Test
    void testWrite_WithComplexKeySerialization_WithComplexKey() throws IOException {
        Map<List<String>, Integer> map = new HashMap<>();
        map.put(Collections.singletonList("a"), 1);
        
        TypeAdapter<Map<List<String>, Integer>> complexAdapter = factory.create(
            gson, new TypeToken<Map<List<String>, Integer>>() {});
        
        StringWriter writer = new StringWriter();
        complexAdapter.write(new JsonWriter(writer), map);
        assertEquals("[[[\"a\"],1]]", writer.toString());
    }

    @Test
    void testKeyToString_Number() throws Exception {
        Adapter<?, ?> adapterInstance = getAdapterInstance();
        Method method = getKeyToStringMethod(adapterInstance);
        JsonElement element = new JsonPrimitive(123);
        assertEquals("123", method.invoke(adapterInstance, element));
    }

    @Test
    void testKeyToString_Boolean() throws Exception {
        Adapter<?, ?> adapterInstance = getAdapterInstance();
        Method method = getKeyToStringMethod(adapterInstance);
        JsonElement element = new JsonPrimitive(true);
        assertEquals("true", method.invoke(adapterInstance, element));
    }

    @Test
    void testKeyToString_String() throws Exception {
        Adapter<?, ?> adapterInstance = getAdapterInstance();
        Method method = getKeyToStringMethod(adapterInstance);
        JsonElement element = new JsonPrimitive("test");
        assertEquals("test", method.invoke(adapterInstance, element));
    }

    @Test
    void testKeyToString_Null() throws Exception {
        Adapter<?, ?> adapterInstance = getAdapterInstance();
        Method method = getKeyToStringMethod(adapterInstance);
        JsonElement element = JsonNull.INSTANCE;
        assertEquals("null", method.invoke(adapterInstance, element));
    }

    @Test
    void testKeyToString_InvalidType() throws Exception {
        Adapter<?, ?> adapterInstance = getAdapterInstance();
        Method method = getKeyToStringMethod(adapterInstance);
        JsonElement element = new JsonObject();
        assertThrows(InvocationTargetException.class, () -> method.invoke(adapterInstance, element));
    }

    @SuppressWarnings("unchecked")
    private Adapter<?, ?> getAdapterInstance() {
        return (Adapter<?, ?>) adapter;
    }

    private Method getKeyToStringMethod(Adapter<?, ?> adapterInstance) throws NoSuchMethodException {
        Method method = adapterInstance.getClass().getDeclaredMethod("keyToString", JsonElement.class);
        method.setAccessible(true);
        return method;
    }

    private Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}