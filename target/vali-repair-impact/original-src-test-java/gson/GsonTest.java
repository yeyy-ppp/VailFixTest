package gson;

import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import gson.stream.MalformedJsonException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import gson.reflect.TypeToken;
import gson.JsonSyntaxException;

public class GsonTest {

    @Test
    void testDefaultConstructor() {
        Gson gson = new Gson();
        assertNotNull(gson);
        assertFalse(gson.serializeNulls());
        assertTrue(gson.htmlSafe());
    }

    @Test
    void testNewBuilder() {
        Gson gson = new Gson();
        GsonBuilder builder = gson.newBuilder();
        assertNotNull(builder);
        Gson newGson = builder.create();
        assertNotNull(newGson);
    }

    @Test
    void testFieldNamingStrategy() {
        Gson gson = new Gson();
        assertNotNull(gson.fieldNamingStrategy());
    }

    @Test
    void testSerializeNulls() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        assertTrue(gson.serializeNulls());
    }

    @Test
    void testHtmlSafe() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        assertFalse(gson.htmlSafe());
    }

    @Test
    void testGetAdapter_Class() {
        Gson gson = new Gson();
        TypeAdapter<String> adapter = gson.getAdapter(String.class);
        assertNotNull(adapter);
    }

    @Test
    void testGetAdapter_TypeToken() {
        Gson gson = new Gson();
        TypeToken<String> typeToken = TypeToken.get(String.class);
        TypeAdapter<String> adapter = gson.getAdapter(typeToken);
        assertNotNull(adapter);
    }

    @Test
    void testToJsonTree_Object() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree("test");
        assertTrue(element.isJsonPrimitive());
        assertEquals("test", element.getAsString());
    }

    @Test
    void testToJsonTree_NullObject() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(null);
        assertTrue(element.isJsonNull());
    }

    @Test
    void testToJson_Object() {
        Gson gson = new Gson();
        String json = gson.toJson("test");
        assertEquals("\"test\"", json);
    }

    @Test
    void testToJson_JsonElement() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree("test");
        String json = gson.toJson(element);
        assertEquals("\"test\"", json);
    }

    @Test
    void testToJson_Appendable() throws IOException {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        gson.toJson("test", sb);
        assertEquals("\"test\"", sb.toString());
    }

    @Test
    void testToJson_JsonWriter() throws IOException {
        Gson gson = new Gson();
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = gson.newJsonWriter(writer);
        gson.toJson(new JsonPrimitive("test"), jsonWriter);
        jsonWriter.close();
        assertEquals("\"test\"", writer.toString());
    }

    @Test
    void testNewJsonWriter() throws IOException {
        Gson gson = new Gson();
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = gson.newJsonWriter(writer);
        assertNotNull(jsonWriter);
        jsonWriter.nullValue();
        jsonWriter.close();
    }

    @Test
    void testNewJsonReader() {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("{}"));
        assertNotNull(reader);
    }

    @Test
    void testFromJson_String_Class() {
        Gson gson = new Gson();
        String result = gson.fromJson("\"test\"", String.class);
        assertEquals("test", result);
    }

    @Test
    void testFromJson_String_Type() {
        Gson gson = new Gson();
        List<String> result = gson.fromJson("[\"test\"]", (Type) new TypeToken<List<String>>() {}.getType());
        assertEquals("test", result.get(0));
    }

    @Test
    void testFromJson_Reader_TypeToken() {
        Gson gson = new Gson();
        TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {};
        List<String> result = gson.fromJson(new StringReader("[\"test\"]"), typeToken);
        assertEquals("test", result.get(0));
    }

    @Test
    void testFromJson_JsonReader_Type() throws IOException {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("\"test\""));
        String result = gson.fromJson(reader, String.class);
        assertEquals("test", result);
    }

    @Test
    void testFromJson_JsonElement_Type() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree("test");
        String result = gson.fromJson(element, String.class);
        assertEquals("test", result);
    }

    @Test
    void testFromJson_NullInput() {
        Gson gson = new Gson();
        String result = gson.fromJson((String) null, String.class);
        assertNull(result);
    }

    @Test
    void testFromJson_EmptyInput() {
        Gson gson = new Gson();
        String result = gson.fromJson("", String.class);
        assertNull(result);
    }

    @Test
    void testGetDelegateAdapter() throws Exception {
        Gson gson = new Gson();
        Field field = Gson.class.getDeclaredField("jsonAdapterFactory");
        field.setAccessible(true);
        TypeAdapterFactory skipFactory = (TypeAdapterFactory) field.get(gson);
        TypeAdapter<String> adapter = gson.getDelegateAdapter(skipFactory, TypeToken.get(String.class));
        assertNotNull(adapter);
    }

    @Test
    void testToString() {
        Gson gson = new Gson();
        String str = gson.toString();
        assertTrue(str.contains("serializeNulls"));
    }

    @Test
    void testExcluder() {
        Gson gson = new Gson();
        assertNotNull(gson.excluder());
    }

    @Test
    void testFloatAdapter() throws Exception {
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        java.lang.reflect.Method method = gson.getClass().getDeclaredMethod("floatAdapter");
        method.setAccessible(true);
        TypeAdapter<Number> adapter = (TypeAdapter<Number>) method.invoke(gson);
        assertNotNull(adapter);
    }

    @Test
    void testDoubleAdapter() throws Exception {
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        java.lang.reflect.Method method = gson.getClass().getDeclaredMethod("doubleAdapter");
        method.setAccessible(true);
        TypeAdapter<Number> adapter = (TypeAdapter<Number>) method.invoke(gson);
        assertNotNull(adapter);
    }

    @Test
    void testToJson_Object_Type() {
        Gson gson = new Gson();
        String json = gson.toJson(123, int.class);
        assertEquals("123", json);
    }

    @Test
    void testToJson_Object_Type_Appendable() throws IOException {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        gson.toJson(123, int.class, sb);
        assertEquals("123", sb.toString());
    }

    @Test
    void testToJson_JsonElement_Appendable() throws IOException {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(123);
        StringBuilder sb = new StringBuilder();
        gson.toJson(element, sb);
        assertEquals("123", sb.toString());
    }

    @Test
    void testFromJson_Reader_Class() {
        Gson gson = new Gson();
        AtomicLong result = gson.fromJson(new StringReader("123"), AtomicLong.class);
        assertEquals(123, result.get());
    }

    @Test
    void testFromJson_JsonElement_TypeToken() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(123);
        Integer result = gson.fromJson(element, new TypeToken<Integer>() {});
        assertEquals(123, (int) result);
    }

    @Test
    void testAssertFullConsumption() throws Exception {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("123"));
        Object obj = gson.fromJson(reader, Integer.class);
        java.lang.reflect.Method method = gson.getClass().getDeclaredMethod("assertFullConsumption", Object.class, JsonReader.class);
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(gson, obj, reader));
    }

    @Test
    void testFromJson_JsonReader_TypeToken() throws IOException {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("\"test\""));
        String result = gson.fromJson(reader, new TypeToken<String>() {});
        assertEquals("test", result);
    }

 /*   @Test
    void testFromJson_JsonReader_TypeToken_Throws() {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("invalid"));
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson(reader, new TypeToken<String>() {}));
    }*/

  /*  @Test
    void testToJson_JsonWriter_Throws() {
        Gson gson = new Gson();
        Writer writer = new Writer() {
            @Override public void write(char[] cbuf, int off, int len) throws IOException { throw new IOException("Test"); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        assertThrows(JsonIOException.class, () -> {
            JsonWriter jsonWriter = gson.newJsonWriter(writer);
            gson.toJson("test", (Type) jsonWriter);
        });
    }*/

  /*  @Test
    void testFromJson_JsonReader_TypeToken_EOF() {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader(""));
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson(reader, new TypeToken<String>() {}));
    }*/

 /*   @Test
    void testGetAdapter_CyclicDependency() {
        Gson gson = new Gson();
        TypeToken<List> typeToken = new TypeToken<List>() {};
        assertThrows(IllegalArgumentException.class, () -> gson.getAdapter(typeToken));
    }*/

    @Test
    void testNewJsonWriter_NonExecutable() throws IOException {
        Gson gson = new GsonBuilder().generateNonExecutableJson().create();
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = gson.newJsonWriter(writer);
        jsonWriter.nullValue();
        jsonWriter.close();
        assertTrue(writer.toString().startsWith(")]}'\n"));
    }

    @Test
    void testToJsonTree_WithType() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(123, int.class);
        assertTrue(element.isJsonPrimitive());
        assertEquals(123, element.getAsInt());
    }

    @Test
    void testFromJson_JsonElement_Class() {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree("value");
        String result = gson.fromJson(element, String.class);
        assertEquals("value", result);
    }

    @Test
    void testFromJson_JsonReader_Type_Throws() {
        Gson gson = new Gson();
        JsonReader reader = gson.newJsonReader(new StringReader("{"));
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson(reader, (Type) Object.class));
    }
}