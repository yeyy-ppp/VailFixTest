package gson;

import gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;
import gson.FieldNamingPolicy;
import gson.Strictness;
import gson.FormattingStyle;
import gson.ExclusionStrategy;
import gson.FieldNamingStrategy;
import gson.JsonSerializer;
import gson.JsonDeserializer;
import gson.InstanceCreator;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.ReflectionAccessFilter;
import gson.Gson;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Date;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.text.DateFormat;

public final class GsonBuilderTest {

    @Test
    void testDefaultConstructor() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        assertNotNull(gson);
    }

    @Test
    void testCopyConstructor() {
        Gson originalGson = new GsonBuilder().serializeNulls().create();
        Map<String, Object> map = new HashMap<>();
        map.put("field", null);
        String json = originalGson.toJson(map);
        assertTrue(json.contains("null"));
        
        GsonBuilder builder = new GsonBuilder(originalGson);
        Gson copiedGson = builder.create();
        json = copiedGson.toJson(map);
        assertTrue(json.contains("null"));
    }

    @Test
    void testSetVersion_Valid() {
        GsonBuilder builder = new GsonBuilder().setVersion(1.0);
        assertNotNull(builder.create());
    }

    @Test
    void testSetVersion_Negative() {
        assertThrows(IllegalArgumentException.class, () -> new GsonBuilder().setVersion(-1.0));
    }

    @Test
    void testSetVersion_NaN() {
        assertThrows(IllegalArgumentException.class, () -> new GsonBuilder().setVersion(Double.NaN));
    }

    @Test
    void testExcludeFieldsWithModifiers() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
        assertNotNull(builder.create());
    }

    @Test
    void testGenerateNonExecutableJson() {
        Gson gson = new GsonBuilder().generateNonExecutableJson().create();
        String json = gson.toJson("test");
        assertTrue(json.startsWith(")]}'\n"));
    }

    @Test
    void testExcludeFieldsWithoutExposeAnnotation() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
        assertNotNull(builder.create());
    }

    @Test
    void testSerializeNulls() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Map<String, Object> map = new HashMap<>();
        map.put("field", null);
        String json = gson.toJson(map);
        assertTrue(json.contains("\"field\":null"));
    }

    @Test
    void testEnableComplexMapKeySerialization() {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        Map<Object, String> map = new HashMap<>();
        map.put(42, "value");
        String json = gson.toJson(map);
        assertNotNull(json);
    }

    @Test
    void testDisableInnerClassSerialization() {
        GsonBuilder builder = new GsonBuilder().disableInnerClassSerialization();
        assertNotNull(builder.create());
    }

    @Test
    void testSetFieldNamingPolicy() {
        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();
        assertNotNull(gson);
    }

    @Test
    void testSetFieldNamingStrategy() {
        FieldNamingStrategy strategy = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
        Gson gson = new GsonBuilder().setFieldNamingStrategy(strategy).create();
        assertNotNull(gson);
    }

    @Test
    void testSetExclusionStrategies() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes f) { return false; }
            @Override public boolean shouldSkipClass(Class<?> clazz) { return false; }
        };
        GsonBuilder builder = new GsonBuilder().setExclusionStrategies(strategy);
        assertNotNull(builder.create());
    }

    @Test
    void testAddSerializationExclusionStrategy() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes f) { return false; }
            @Override public boolean shouldSkipClass(Class<?> clazz) { return false; }
        };
        GsonBuilder builder = new GsonBuilder().addSerializationExclusionStrategy(strategy);
        assertNotNull(builder.create());
    }

    @Test
    void testAddDeserializationExclusionStrategy() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes f) { return false; }
            @Override public boolean shouldSkipClass(Class<?> clazz) { return false; }
        };
        GsonBuilder builder = new GsonBuilder().addDeserializationExclusionStrategy(strategy);
        assertNotNull(builder.create());
    }

    @Test
    void testSetPrettyPrinting() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(Collections.singletonMap("key", "value"));
        assertTrue(json.contains("\n"));
    }

    @Test
    void testSetFormattingStyle() {
        Gson gson = new GsonBuilder().setFormattingStyle(FormattingStyle.COMPACT).create();
        String json = gson.toJson(Collections.singletonMap("key", "value"));
        assertFalse(json.contains("\n"));
    }

    @Test
    void testSetStrictness() {
        Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();
        String invalidJson = "{key: 'value'}";
        assertThrows(RuntimeException.class, () -> gson.fromJson(invalidJson, Object.class));
    }

    @Test
    void testDisableHtmlEscaping() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String json = gson.toJson("<html>");
        assertEquals("\"<html>\"", json);
    }

    @Test
    void testSetDateFormat_ValidPattern() {
        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyy-MM-dd");
        Gson gson = builder.create();
        assertNotNull(gson.toJson(new Date()));
    }

    @Test
    void testSetDateFormat_InvalidPattern() {
        assertThrows(IllegalArgumentException.class, () -> new GsonBuilder().setDateFormat("invalid"));
    }

    @Test
    void testRegisterTypeAdapter_ValidAdapter() {
        JsonSerializer<String> serializer = (src, typeOfSrc, context) -> null;
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(String.class, serializer);
        assertNotNull(builder.create());
    }

    @Test
    void testRegisterTypeAdapter_ObjectType() {
        JsonSerializer<Object> serializer = (src, typeOfSrc, context) -> null;
        Executable action = () -> new GsonBuilder().registerTypeAdapter(Object.class, serializer);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void testRegisterTypeAdapter_InvalidAdapter() {
        Object invalidAdapter = new Object();
        Executable action = () -> new GsonBuilder().registerTypeAdapter(String.class, invalidAdapter);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void testRegisterTypeAdapterFactory() {
        TypeAdapterFactory factory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return null;
            }
        };
        GsonBuilder builder = new GsonBuilder().registerTypeAdapterFactory(factory);
        assertNotNull(builder.create());
    }

    @Test
    void testRegisterTypeHierarchyAdapter() {
        JsonSerializer<Number> serializer = (src, typeOfSrc, context) -> null;
        GsonBuilder builder = new GsonBuilder().registerTypeHierarchyAdapter(Number.class, serializer);
        assertNotNull(builder.create());
    }

    @Test
    void testSerializeSpecialFloatingPointValues() {
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        String json = gson.toJson(Double.NaN);
        assertEquals("NaN", json);
    }

    @Test
    void testDisableJdkUnsafe() {
        GsonBuilder builder = new GsonBuilder().disableJdkUnsafe();
        assertNotNull(builder.create());
    }

    @Test
    void testAddReflectionAccessFilter() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_PLATFORM;
        GsonBuilder builder = new GsonBuilder().addReflectionAccessFilter(filter);
        assertNotNull(builder.create());
    }

    @Test
    void testSetLenient() {
        Gson gson = new GsonBuilder().setLenient().create();
        String json = "/* comment */{\"name\":\"value\"}";
        Object obj = gson.fromJson(json, Object.class);
        assertNotNull(obj);
    }

    @Test
    void testSetDateFormat_ValidStyle() {
        assertDoesNotThrow(() -> new GsonBuilder().setDateFormat(DateFormat.SHORT));
    }

    @Test
    void testSetDateFormat_InvalidStyle() {
        assertThrows(IllegalArgumentException.class, () -> new GsonBuilder().setDateFormat(-1));
    }

    @Test
    void testAddTypeAdaptersForDate() {
        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyy-MM-dd");
        Gson gson = builder.create();
        String json = gson.toJson(new Date(0));
        assertTrue(json.contains("1970") && json.contains("01") && json.contains("01"));
    }
}