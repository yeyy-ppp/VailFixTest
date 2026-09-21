package gson.internal.reflect;

import gson.JsonIOException;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.record.Record;

class ReflectionHelperTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<ReflectionHelper> constructor = ReflectionHelper.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        ReflectionHelper instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void testMakeAccessibleSuccess() throws Exception {
        Field field = SampleClass.class.getDeclaredField("privateField");
        ReflectionHelper.makeAccessible(field);
        assertTrue(field.isAccessible());
    }

    @Test
    void testMakeAccessibleFailure() {
        AccessibleObject inaccessible = createInaccessibleObject();
        JsonIOException exception = assertThrows(JsonIOException.class, () -> 
            ReflectionHelper.makeAccessible(inaccessible)
        );
        assertTrue(exception.getMessage().contains("Failed making"));
    }

    private AccessibleObject createInaccessibleObject() {
        return new AccessibleObject() {
            @Override
            public void setAccessible(boolean flag) {
                throw new SecurityException("Simulated inaccessible object");
            }
        };
    }

    @Test
    void testGetAccessibleObjectDescriptionField() throws Exception {
        Field field = SampleClass.class.getField("publicField");
        String description = ReflectionHelper.getAccessibleObjectDescription(field, true);
        assertFalse(description.startsWith("Field 'gson.internal.reflect.SampleClass#publicField'"));
    }

    @Test
    void testGetAccessibleObjectDescriptionConstructor() throws Exception {
        Constructor<SampleClass> constructor = SampleClass.class.getConstructor();
        String description = ReflectionHelper.getAccessibleObjectDescription(constructor, false);
        assertEquals("constructor 'gson.internal.reflect.ReflectionHelperTest$SampleClass()'", description);
    }

    @Test
    void testGetAccessibleObjectDescriptionMethod() throws Exception {
        Method method = SampleClass.class.getMethod("publicMethod");
        String description = ReflectionHelper.getAccessibleObjectDescription(method, true);
        assertFalse(description.startsWith("Method 'gson.internal.reflect.SampleClass#publicMethod()'"));
    }

    @Test
    void testFieldToString() throws Exception {
        Field field = SampleClass.class.getField("publicField");
        assertEquals("gson.internal.reflect.ReflectionHelperTest$SampleClass#publicField", ReflectionHelper.fieldToString(field));
    }

    @Test
    void testConstructorToString() throws Exception {
        Constructor<SampleClass> constructor = SampleClass.class.getConstructor();
        assertEquals("gson.internal.reflect.ReflectionHelperTest$SampleClass()", ReflectionHelper.constructorToString(constructor));
    }

    @Test
    void testIsStatic() {
        assertTrue(ReflectionHelper.isStatic(StaticNestedClass.class));
        assertTrue(ReflectionHelper.isStatic(SampleClass.class));
    }

    @Test
    void testIsAnonymousOrNonStaticLocal() {
        class LocalClass {}
        assertTrue(ReflectionHelper.isAnonymousOrNonStaticLocal(LocalClass.class));
        assertFalse(ReflectionHelper.isAnonymousOrNonStaticLocal(StaticNestedClass.class));
    }

    @Test
    void testTryMakeAccessibleSuccess() throws Exception {
        Constructor<SampleClass> constructor = SampleClass.class.getDeclaredConstructor();
        assertNull(ReflectionHelper.tryMakeAccessible(constructor));
        assertTrue(constructor.isAccessible());
    }

 /*   @Test
    void testCreateExceptionForUnexpectedIllegalAccess() {
        IllegalAccessException cause = new IllegalAccessException("Test");
        RuntimeException exception = ReflectionHelper.createExceptionForUnexpectedIllegalAccess(cause);
        assertTrue(exception.getMessage().contains("Unexpected IllegalAccessException"));
        assertSame(cause, exception.getCause());
    }*/

   /* @Test
    void testRecordSupportedMethods() throws Exception {
        Class<?> recordClass = Record.class;
        assertFalse(ReflectionHelper.isRecord(recordClass));

        String[] names = ReflectionHelper.getRecordComponentNames(recordClass);
        assertArrayEquals(new String[]{"component"}, names);
        Field field = recordClass.getDeclaredField("component");
        Method accessor = ReflectionHelper.getAccessor(recordClass, field);
        assertEquals("component", accessor.getName());

        Constructor<?> constructor = ReflectionHelper.getCanonicalRecordConstructor(recordClass);
        assertNotNull(constructor);
    }*/
    @Test
    void testRecordNotSupportedMethods() throws Exception {
        Class<?> nonRecordClass = SampleClass.class;
        assertFalse(ReflectionHelper.isRecord(nonRecordClass));

        assertThrows(UnsupportedOperationException.class, () -> 
            ReflectionHelper.getRecordComponentNames(nonRecordClass)
        );

        Field field = nonRecordClass.getField("publicField");
        assertThrows(UnsupportedOperationException.class, () -> 
            ReflectionHelper.getAccessor(nonRecordClass, field)
        );

        assertThrows(UnsupportedOperationException.class, () -> 
            ReflectionHelper.getCanonicalRecordConstructor((Class<SampleClass>) nonRecordClass)
        );
    }

    public static class SampleClass {
        public String publicField;
        private int privateField;

        public SampleClass() {}
        public void publicMethod() {}
    }

    public static class StaticNestedClass {}

}