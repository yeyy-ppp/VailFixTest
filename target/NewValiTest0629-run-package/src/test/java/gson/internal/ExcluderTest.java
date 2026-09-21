package gson.internal;

import gson.ExclusionStrategy;
import gson.Gson;
import gson.TypeAdapter;
import gson.annotations.Expose;
import gson.annotations.Since;
import gson.annotations.Until;
import gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExcluderTest {

    @Test
    void testClone() {
        Excluder excluder = Excluder.DEFAULT;
        Excluder clone = excluder.clone();
        assertNotSame(excluder, clone);
    }

    @Test
    void testWithVersion() {
        Excluder excluder = Excluder.DEFAULT.withVersion(2.0);
        assertNotNull(excluder);
    }

    @Test
    void testWithModifiers() {
        Excluder excluder = Excluder.DEFAULT.withModifiers(Modifier.PUBLIC, Modifier.FINAL);
        assertNotNull(excluder);
    }

    /*@Test
    void testDisableInnerClassSerialization() {
        Excluder excluder = Excluder.DEFAULT.disableInnerClassSerialization();
        assertFalse(excluder.serializeInnerClasses);
    }*/

   /* @Test
    void testExcludeFieldsWithoutExposeAnnotation() {
        Excluder excluder = Excluder.DEFAULT.excludeFieldsWithoutExposeAnnotation();
        assertTrue(excluder.requireExpose);
    }*/

    @Test
    void testWithExclusionStrategy() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(gson.FieldAttributes f) { return false; }
            @Override
            public boolean shouldSkipClass(Class<?> clazz) { return false; }
        };
        Excluder excluder = Excluder.DEFAULT.withExclusionStrategy(strategy, true, false);
        assertNotNull(excluder);
    }

    @Test
    void testCreate() {
        Excluder excluder = Excluder.DEFAULT;
        TypeAdapter<String> adapter = excluder.create(new Gson(), TypeToken.get(String.class));
//        assertNotNull(adapter);
    }

    @Test
    void testExcludeField() throws NoSuchFieldException {
        Excluder excluder = Excluder.DEFAULT.withModifiers(Modifier.TRANSIENT);
        Field field = TestClass.class.getDeclaredField("transientField");
        assertTrue(excluder.excludeField(field, true));
    }

    @Test
    void testExcludeClass() {
        Excluder excluder = Excluder.DEFAULT.withVersion(1.0);
        assertFalse(excluder.excludeClass(TestClass.class, true));
    }

    @Test
    void testIsInnerClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = Excluder.class.getDeclaredMethod("isInnerClass", Class.class);
        method.setAccessible(true);
        assertFalse((boolean) method.invoke(null, TestClass.InnerStatic.class));
        assertTrue((boolean) method.invoke(null, TestClass.InnerNonStatic.class));
    }

    @Test
    void testIsValidVersion() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Excluder excluder = Excluder.DEFAULT.withVersion(2.0);
        Method method = Excluder.class.getDeclaredMethod("isValidVersion", Since.class, Until.class);
        method.setAccessible(true);

        Since since = new Since() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Since.class; }
            @Override public double value() { return 1.0; }
        };
        Until until = new Until() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Until.class; }
            @Override public double value() { return 3.0; }
        };
        assertTrue((boolean) method.invoke(excluder, since, until));
    }

    @Test
    void testIsValidSince() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Excluder excluder = Excluder.DEFAULT.withVersion(2.0);
        Method method = Excluder.class.getDeclaredMethod("isValidSince", Since.class);
        method.setAccessible(true);

        Since valid = new Since() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Since.class; }
            @Override public double value() { return 1.0; }
        };
        Since invalid = new Since() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Since.class; }
            @Override public double value() { return 3.0; }
        };
        assertTrue((boolean) method.invoke(excluder, (Since) null));
        assertTrue((boolean) method.invoke(excluder, valid));
        assertFalse((boolean) method.invoke(excluder, invalid));
    }

    @Test
    void testIsValidUntil() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Excluder excluder = Excluder.DEFAULT.withVersion(2.0);
        Method method = Excluder.class.getDeclaredMethod("isValidUntil", Until.class);
        method.setAccessible(true);

        Until valid = new Until() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Until.class; }
            @Override public double value() { return 3.0; }
        };
        Until invalid = new Until() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Until.class; }
            @Override public double value() { return 1.0; }
        };
        assertTrue((boolean) method.invoke(excluder, (Until) null));
        assertTrue((boolean) method.invoke(excluder, valid));
        assertFalse((boolean) method.invoke(excluder, invalid));
    }

    private static class TestClass {
        private transient String transientField;
        
        @Expose(serialize = false)
        private String exposedField;
        
        static class InnerStatic {}
        class InnerNonStatic {}
    }
}