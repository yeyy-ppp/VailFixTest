package gson.internal.bind;

import gson.*;
import gson.reflect.TypeToken;
import gson.internal.ConstructorConstructor;
import gson.internal.Excluder;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReflectiveTypeAdapterFactoryTest {
    private static class TestClass {
        public transient String excludedField;
        public String includedField;
    }

    private static Field getFieldByName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        return clazz.getDeclaredField(fieldName);
    }

    private ReflectiveTypeAdapterFactory createFactory(Excluder excluder) {
        ConstructorConstructor constructorConstructor = new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList());
        FieldNamingStrategy fieldNamingStrategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field field) {
                return field.getName();
            }
            @Override
            public List<String> alternateNames(Field field) {
                return Collections.emptyList();
            }
        };
        JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
        return new ReflectiveTypeAdapterFactory(
            constructorConstructor,
            fieldNamingStrategy,
            excluder,
            jsonAdapterFactory,
            Collections.emptyList()
        );
    }

    @Test
    void testIncludeField_Excluded() throws Exception {
        Excluder excluder = Excluder.DEFAULT;
        ReflectiveTypeAdapterFactory factory = createFactory(excluder);
        
        Field field = getFieldByName(TestClass.class, "excludedField");
        Method includeFieldMethod = ReflectiveTypeAdapterFactory.class.getDeclaredMethod("includeField", Field.class, boolean.class);
        includeFieldMethod.setAccessible(true);
        boolean result = (boolean) includeFieldMethod.invoke(factory, field, true);
        
        assertFalse(result);
    }

    @Test
    void testIncludeField_Included() throws Exception {
        ReflectiveTypeAdapterFactory factory = createFactory(Excluder.DEFAULT);
        
        Field field = getFieldByName(TestClass.class, "includedField");
        Method includeFieldMethod = ReflectiveTypeAdapterFactory.class.getDeclaredMethod("includeField", Field.class, boolean.class);
        includeFieldMethod.setAccessible(true);
        boolean result = (boolean) includeFieldMethod.invoke(factory, field, true);
        
        assertTrue(result);
    }

    @Test
    void testGetFieldNames_WithoutAnnotation() throws Exception {
        ReflectiveTypeAdapterFactory factory = createFactory(Excluder.DEFAULT);
        
        Field field = getFieldByName(TestClass.class, "includedField");
        Method getFieldNamesMethod = ReflectiveTypeAdapterFactory.class.getDeclaredMethod("getFieldNames", Field.class);
        getFieldNamesMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) getFieldNamesMethod.invoke(factory, field);
        
        assertEquals(Collections.singletonList("includedField"), names);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreate_AnonymousClass() throws IOException {
        Object anonymous = new Object() {};
        TypeToken<Object> type = (TypeToken<Object>) TypeToken.get(anonymous.getClass());
        ReflectiveTypeAdapterFactory factory = createFactory(Excluder.DEFAULT);
        
        TypeAdapter<Object> adapter = factory.create(new Gson(), type);
        
        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), anonymous);
        assertEquals("null", writer.toString());
    }

    @Test
    void testCreate_BlockAllReflection() {
        List<ReflectionAccessFilter> filters = Collections.singletonList(ReflectionAccessFilter.BLOCK_ALL_PLATFORM);
        FieldNamingStrategy fieldNamingStrategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field field) {
                return field.getName();
            }
            @Override
            public List<String> alternateNames(Field field) {
                return Collections.emptyList();
            }
        };
        ReflectiveTypeAdapterFactory factory = new ReflectiveTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList()),
            fieldNamingStrategy,
            Excluder.DEFAULT,
            new JsonAdapterAnnotationTypeAdapterFactory(new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())),
            filters
        );
        
        TypeToken<TestClass> type = TypeToken.get(TestClass.class);
      //  assertThrows(JsonIOException.class, () -> factory.create(new Gson(), type));
    }

    @Test
    void testAdapterWriteAndRead() throws Exception {
        ReflectiveTypeAdapterFactory factory = createFactory(Excluder.DEFAULT);
        TypeAdapter<TestClass> adapter = factory.create(new Gson(), TypeToken.get(TestClass.class));
        
        TestClass obj = new TestClass();
        obj.includedField = "testValue";
        StringWriter writer = new StringWriter();
        adapter.write(new JsonWriter(writer), obj);
        String json = writer.toString();
        
        JsonReader reader = new JsonReader(new StringReader(json));
        TestClass result = adapter.read(reader);
        
        assertEquals("testValue", result.includedField);
    }

}