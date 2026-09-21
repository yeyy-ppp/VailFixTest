package gson.internal;

import gson.ReflectionAccessFilter;
import gson.ReflectionAccessFilter.FilterResult;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionAccessFilterHelperTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        Constructor<ReflectionAccessFilterHelper> constructor = ReflectionAccessFilterHelper.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testIsJavaTypeClass() {
        assertTrue(ReflectionAccessFilterHelper.isJavaType(String.class));
        assertFalse(ReflectionAccessFilterHelper.isJavaType(getClass()));
    }

    @Test
    void testIsJavaTypeString() {
        assertTrue(ReflectionAccessFilterHelper.isJavaType("java.lang.String"));
        assertTrue(ReflectionAccessFilterHelper.isJavaType("javax.sql.DataSource"));
        assertFalse(ReflectionAccessFilterHelper.isJavaType("com.example.Foo"));
    }

    @Test
    void testIsAndroidTypeClass() {
        assertTrue(ReflectionAccessFilterHelper.isAndroidType(String.class));
        assertFalse(ReflectionAccessFilterHelper.isAndroidType(getClass()));
    }

    @Test
    void testIsAndroidTypeString() {
        assertTrue(ReflectionAccessFilterHelper.isAndroidType("android.os.Build"));
        assertTrue(ReflectionAccessFilterHelper.isAndroidType("androidx.core.app.ComponentActivity"));
        assertTrue(ReflectionAccessFilterHelper.isAndroidType("java.lang.String"));
        assertFalse(ReflectionAccessFilterHelper.isAndroidType("com.example.Foo"));
    }

    @Test
    void testIsAnyPlatformType() {
        assertTrue(ReflectionAccessFilterHelper.isAnyPlatformType(String.class));
        assertFalse(ReflectionAccessFilterHelper.isAnyPlatformType(ReflectionAccessFilterHelperTest.class));
    }

    @Test
    void testGetFilterResult() {
        class TestClass {}
        ReflectionAccessFilter allowFilter = c -> FilterResult.ALLOW;
        ReflectionAccessFilter blockFilter = c -> FilterResult.BLOCK_INACCESSIBLE;
        ReflectionAccessFilter indecisiveFilter = c -> FilterResult.INDECISIVE;

        assertEquals(FilterResult.ALLOW, ReflectionAccessFilterHelper.getFilterResult(Collections.emptyList(), TestClass.class));
        assertEquals(FilterResult.BLOCK_INACCESSIBLE, ReflectionAccessFilterHelper.getFilterResult(Arrays.asList(blockFilter), TestClass.class));
        assertEquals(FilterResult.ALLOW, ReflectionAccessFilterHelper.getFilterResult(Arrays.asList(indecisiveFilter), TestClass.class));
        assertEquals(FilterResult.BLOCK_INACCESSIBLE, ReflectionAccessFilterHelper.getFilterResult(Arrays.asList(indecisiveFilter, blockFilter), TestClass.class));
    }

    @Test
    void testCanAccess() throws Exception {
        Field publicField = String.class.getField("CASE_INSENSITIVE_ORDER");
        assertTrue(ReflectionAccessFilterHelper.canAccess(publicField, null));

        Constructor<?> privateConstructor = ReflectionAccessFilterHelper.class.getDeclaredConstructor();
        privateConstructor.setAccessible(true);
        assertTrue(ReflectionAccessFilterHelper.canAccess(privateConstructor, null));
    }
}