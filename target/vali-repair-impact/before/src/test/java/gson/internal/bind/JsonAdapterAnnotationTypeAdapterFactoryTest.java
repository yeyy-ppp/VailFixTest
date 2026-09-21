package gson.internal.bind;

import gson.Gson;
import gson.JsonIOException;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.annotations.JsonAdapter;
import gson.internal.ConstructorConstructor;
import gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;

public class JsonAdapterAnnotationTypeAdapterFactoryTest {

    @Test
    void create_noAnnotation_returnsNull() {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        Gson gson = new Gson();
        TypeToken<String> type = TypeToken.get(String.class);
        
        TypeAdapter<String> result = factory.create(gson, type);
        
        assertNull(result);
    }

    @Test
    void create_adapterClassIsInvalid_throwsException() throws NoSuchFieldException, IllegalAccessException {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        Gson gson = new Gson();
        TypeToken<String> type = TypeToken.get(String.class);
        JsonAdapter annotation = createAnnotation(String.class);
        Field constructorConstructorField = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredField("constructorConstructor");
        constructorConstructorField.setAccessible(true);
        ConstructorConstructor constructorConstructor = (ConstructorConstructor) constructorConstructorField.get(factory);
        
        Executable action = () -> factory.getTypeAdapter(
            constructorConstructor, gson, type, annotation, true
        );
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertTrue(exception.getMessage().contains("Invalid attempt to bind"));
    }

    @Test
    void isClassJsonAdapterFactory_dummyFactory_returnsTrue() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeToken<?> type = TypeToken.get(Object.class);
        Field field = JsonAdapterAnnotationTypeAdapterFactory.class
            .getDeclaredField("TREE_TYPE_CLASS_DUMMY_FACTORY");
        field.setAccessible(true);
        TypeAdapterFactory dummyFactory = (TypeAdapterFactory) field.get(null);
        
        boolean result = factory.isClassJsonAdapterFactory(type, dummyFactory);
        
        assertTrue(result);
    }

    @Test
    void isClassJsonAdapterFactory_existingFactoryMatch_returnsTrue() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeToken<?> type = TypeToken.get(String.class);
        TypeAdapterFactory testFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                return null;
            }
        };
        
        Field mapField = JsonAdapterAnnotationTypeAdapterFactory.class
            .getDeclaredField("adapterFactoryMap");
        mapField.setAccessible(true);
        ConcurrentMap<Class<?>, TypeAdapterFactory> map = 
            (ConcurrentMap<Class<?>, TypeAdapterFactory>) mapField.get(factory);
        map.put(String.class, testFactory);
        
        boolean result = factory.isClassJsonAdapterFactory(type, testFactory);
        
        assertTrue(result);
    }

    @Test
    void isClassJsonAdapterFactory_noAnnotation_returnsFalse() {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeToken<?> type = TypeToken.get(Integer.class);
        TypeAdapterFactory testFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                return null;
            }
        };
        
        boolean result = factory.isClassJsonAdapterFactory(type, testFactory);
        
        assertFalse(result);
    }

    @Test
    void isClassJsonAdapterFactory_annotationValueNotFactory_returnsFalse() {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeToken<?> type = TypeToken.get(TestClass.class);
        TypeAdapterFactory testFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                return null;
            }
        };
        
        boolean result = factory.isClassJsonAdapterFactory(type, testFactory);
        
        assertFalse(result);
    }

    @Test
    void getAnnotation_classWithAnnotation_returnsAnnotation() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        Method method = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredMethod("getAnnotation", Class.class);
        method.setAccessible(true);
        JsonAdapter result = (JsonAdapter) method.invoke(null, TestClass.class);
        assertNotNull(result);
        assertEquals(Object.class, result.value());
    }

    @Test
    void getAnnotation_classWithoutAnnotation_returnsNull() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        Method method = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredMethod("getAnnotation", Class.class);
        method.setAccessible(true);
        JsonAdapter result = (JsonAdapter) method.invoke(null, String.class);
        assertNull(result);
    }

    @Test
    void createAdapter_invalidClass_throwsException() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        Method method = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredMethod("createAdapter", ConstructorConstructor.class, Class.class);
        method.setAccessible(true);
        
        Field constructorField = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredField("constructorConstructor");
        constructorField.setAccessible(true);
        ConstructorConstructor constructorConstructor = (ConstructorConstructor) constructorField.get(factory);
        
        Executable action = () -> method.invoke(null, constructorConstructor, Runnable.class);
        
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, action);
        assertTrue(exception.getCause() instanceof JsonIOException);
        assertTrue(exception.getCause().getMessage().contains("Interfaces can't be instantiated"));
    }

    @Test
    void putFactoryAndGetCurrent_newClass_returnsCurrentFactory() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeAdapterFactory testFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return null;
            }
        };
        Method method = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredMethod("putFactoryAndGetCurrent", Class.class, TypeAdapterFactory.class);
        method.setAccessible(true);
        TypeAdapterFactory result = (TypeAdapterFactory) method.invoke(factory, String.class, testFactory);
        assertSame(testFactory, result);
        ConcurrentMap<Class<?>, TypeAdapterFactory> map = getAdapterFactoryMap(factory);
        assertSame(testFactory, map.get(String.class));
    }

    @Test
    void putFactoryAndGetCurrent_existingClass_returnsExistingFactory() throws Exception {
        JsonAdapterAnnotationTypeAdapterFactory factory = new JsonAdapterAnnotationTypeAdapterFactory(
            new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList())
        );
        TypeAdapterFactory existingFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return null;
            }
        };
        Method method = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredMethod("putFactoryAndGetCurrent", Class.class, TypeAdapterFactory.class);
        method.setAccessible(true);
        method.invoke(factory, Integer.class, existingFactory);
        TypeAdapterFactory newFactory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return null;
            }
        };
        TypeAdapterFactory result = (TypeAdapterFactory) method.invoke(factory, Integer.class, newFactory);
        assertSame(existingFactory, result);
        ConcurrentMap<Class<?>, TypeAdapterFactory> map = getAdapterFactoryMap(factory);
        assertSame(existingFactory, map.get(Integer.class));
    }

    private static JsonAdapter createAnnotation(Class<?> adapterClass) {
        return new JsonAdapter() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JsonAdapter.class;
            }
            @Override
            public Class<?> value() {
                return adapterClass;
            }
            @Override
            public boolean nullSafe() {
                return false;
            }
        };
    }

    private ConcurrentMap<Class<?>, TypeAdapterFactory> getAdapterFactoryMap(
        JsonAdapterAnnotationTypeAdapterFactory factory) throws Exception {
        Field mapField = JsonAdapterAnnotationTypeAdapterFactory.class.getDeclaredField("adapterFactoryMap");
        mapField.setAccessible(true);
        return (ConcurrentMap<Class<?>, TypeAdapterFactory>) mapField.get(factory);
    }

    @JsonAdapter(Object.class)
    private static class TestClass {}
}