package lang3;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class ClassLoaderUtilsTest {

    @Test
    void testGetSystemURLs() throws Exception {
        URL[] systemURLs = ClassLoaderUtils.getSystemURLs();
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        Method getURLsMethod = ClassLoaderUtils.class.getDeclaredMethod("getURLs", ClassLoader.class);
        getURLsMethod.setAccessible(true);
        URL[] expected = (URL[]) getURLsMethod.invoke(null, systemLoader);
        assertArrayEquals(expected, systemURLs);
    }

    @Test
    void testGetThreadURLsWithURLClassLoader() throws Exception {
        URL[] urls = new URL[]{new URL("file://test1.jar"), new URL("file://test2.jar")};
        URLClassLoader customLoader = new URLClassLoader(urls);
        Thread.currentThread().setContextClassLoader(customLoader);

        URL[] result = ClassLoaderUtils.getThreadURLs();
        assertArrayEquals(urls, result);
    }

    @Test
    void testGetThreadURLsWithNonURLClassLoader() {
        Thread.currentThread().setContextClassLoader(null);
        URL[] result = ClassLoaderUtils.getThreadURLs();
        assertEquals(0, result.length);
    }

    @Test
    void testGetURLsWithURLClassLoader() throws Exception {
        URL[] urls = new URL[]{new URL("file://test.jar")};
        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        Method getURLsMethod = ClassLoaderUtils.class.getDeclaredMethod("getURLs", ClassLoader.class);
        getURLsMethod.setAccessible(true);
        URL[] result = (URL[]) getURLsMethod.invoke(null, urlClassLoader);
        assertArrayEquals(urls, result);
    }

    @Test
    void testGetURLsWithNonURLClassLoader() throws Exception {
        Method getURLsMethod = ClassLoaderUtils.class.getDeclaredMethod("getURLs", ClassLoader.class);
        getURLsMethod.setAccessible(true);
        URL[] result = (URL[]) getURLsMethod.invoke(null, (Object) null);
        assertEquals(0, result.length);
    }

    @Test
    void testToStringWithURLClassLoader() throws Exception {
        URLClassLoader customLoader = new URLClassLoader(new URL[0]);
        String result = ClassLoaderUtils.toString(customLoader);
        assertTrue(result.contains(customLoader.toString()));
        assertTrue(result.contains(Arrays.toString(customLoader.getURLs())));
    }

    @Test
    void testToStringWithNonURLClassLoader() {
        ClassLoader nonURLLoader = null;
        String result = ClassLoaderUtils.toString(nonURLLoader);
        assertEquals("null", result);
    }

    @Test
    void testToStringWithURLClassLoaderSpecific() throws Exception {
        URLClassLoader customLoader = new URLClassLoader(new URL[]{new URL("file://test.jar")});
        String result = ClassLoaderUtils.toString(customLoader);
        assertTrue(result.contains(customLoader.toString()));
        assertTrue(result.contains("file://test.jar"));
    }

    @Test
    void testToStringWithURLClassLoaderNull() {
        URLClassLoader nullLoader = null;
        String result = ClassLoaderUtils.toString(nullLoader);
        assertEquals("null", result);
    }

    @Test
    void testConstructor() throws Exception {
        Constructor<ClassLoaderUtils> constructor = ClassLoaderUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

}
