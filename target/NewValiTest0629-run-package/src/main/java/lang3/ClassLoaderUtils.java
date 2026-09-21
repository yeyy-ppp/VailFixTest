package lang3;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;
public class ClassLoaderUtils {
    private static final URL[] EMPTY_URL_ARRAY = {};
    public static URL[] getSystemURLs() {
        return getURLs(ClassLoader.getSystemClassLoader());
    }
    public static URL[] getThreadURLs() {
        return getURLs(Thread.currentThread().getContextClassLoader());
    }
    private static URL[] getURLs(final ClassLoader cl) {
        return cl instanceof URLClassLoader ? ((URLClassLoader) cl).getURLs() : EMPTY_URL_ARRAY;
    }
    public static String toString(final ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return toString((URLClassLoader) classLoader);
        }
        return Objects.toString(classLoader);
    }
    public static String toString(final URLClassLoader classLoader) {
        return classLoader != null ? classLoader + Arrays.toString(classLoader.getURLs()) : "null";
    }
    @Deprecated
    public ClassLoaderUtils() {
    }
}