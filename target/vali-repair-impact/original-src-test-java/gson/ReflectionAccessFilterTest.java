package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionAccessFilterTest {

    @Test
    void testBlockInaccessibleJava_JavaType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA;
        ReflectionAccessFilter.FilterResult result = filter.check(String.class);
        assertEquals(ReflectionAccessFilter.FilterResult.BLOCK_INACCESSIBLE, result);
    }

    @Test
    void testBlockInaccessibleJava_NonJavaType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA;
        ReflectionAccessFilter.FilterResult result = filter.check(ReflectionAccessFilterTest.class);
        assertEquals(ReflectionAccessFilter.FilterResult.INDECISIVE, result);
    }

    @Test
    void testBlockInaccessibleJava_toString() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA;
        assertEquals("ReflectionAccessFilter#BLOCK_INACCESSIBLE_JAVA", filter.toString());
    }

    @Test
    void testBlockAllJava_JavaType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_JAVA;
        ReflectionAccessFilter.FilterResult result = filter.check(String.class);
        assertEquals(ReflectionAccessFilter.FilterResult.BLOCK_ALL, result);
    }

    @Test
    void testBlockAllJava_NonJavaType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_JAVA;
        ReflectionAccessFilter.FilterResult result = filter.check(ReflectionAccessFilterTest.class);
        assertEquals(ReflectionAccessFilter.FilterResult.INDECISIVE, result);
    }

    @Test
    void testBlockAllJava_toString() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_JAVA;
        assertEquals("ReflectionAccessFilter#BLOCK_ALL_JAVA", filter.toString());
    }

    @Test
    void testBlockAllAndroid_NonAndroidType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_ANDROID;
        ReflectionAccessFilter.FilterResult result = filter.check(String.class);
        assertEquals(ReflectionAccessFilter.FilterResult.BLOCK_ALL, result);
    }

    @Test
    void testBlockAllAndroid_AnotherNonAndroidType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_ANDROID;
        ReflectionAccessFilter.FilterResult result = filter.check(ReflectionAccessFilterTest.class);
        assertEquals(ReflectionAccessFilter.FilterResult.INDECISIVE, result);
    }

    @Test
    void testBlockAllAndroid_toString() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_ANDROID;
        assertEquals("ReflectionAccessFilter#BLOCK_ALL_ANDROID", filter.toString());
    }

    @Test
    void testBlockAllPlatform_JavaType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_PLATFORM;
        ReflectionAccessFilter.FilterResult result = filter.check(String.class);
        assertEquals(ReflectionAccessFilter.FilterResult.BLOCK_ALL, result);
    }

    @Test
    void testBlockAllPlatform_NonPlatformType() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_PLATFORM;
        ReflectionAccessFilter.FilterResult result = filter.check(ReflectionAccessFilterTest.class);
        assertEquals(ReflectionAccessFilter.FilterResult.INDECISIVE, result);
    }

    @Test
    void testBlockAllPlatform_toString() {
        ReflectionAccessFilter filter = ReflectionAccessFilter.BLOCK_ALL_PLATFORM;
        assertEquals("ReflectionAccessFilter#BLOCK_ALL_PLATFORM", filter.toString());
    }
}