package cli;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
public class TypeHandler {
    private static final TypeHandler DEFAULT = new TypeHandler();
    private static final int HEX_RADIX = 16;
    public static Class<?> createClass(final String className) throws ParseException {
        return createValue(className, Class.class);
    }
    public static Date createDate(final String string) {
        return createValueUnchecked(string, Date.class);
    }
    public static Map<Class<?>, Converter<?, ? extends Throwable>> createDefaultMap() {
        return putDefaultMap(new HashMap<>());
    }
    public static File createFile(final String string) {
        return createValueUnchecked(string, File.class);
    }
    @Deprecated
    public static File[] createFiles(final String string) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Deprecated
    public static Number createNumber(final String string) throws ParseException {
        return createValue(string, Number.class);
    }
    @Deprecated
    public static Object createObject(final String className) throws ParseException {
        return createValue(className, Object.class);
    }
    public static URL createURL(final String string) throws ParseException {
        return createValue(string, URL.class);
    }
    public static <T> T createValue(final String string, final Class<T> clazz) throws ParseException {
        try {
            return getDefault().getConverter(clazz).apply(string);
        } catch (final Exception e) {
            throw ParseException.wrap(e);
        }
    }
    @Deprecated
    public static Object createValue(final String string, final Object obj) throws ParseException {
        return createValue(string, (Class<?>) obj);
    }
    private static <T> T createValueUnchecked(final String string, final Class<T> clazz) {
        try {
            return createValue(string, clazz);
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
    public static TypeHandler getDefault() {
        return DEFAULT;
    }
    @Deprecated
    public static FileInputStream openFile(final String string) throws ParseException {
        return createValue(string, FileInputStream.class);
    }
    private static Map<Class<?>, Converter<?, ? extends Throwable>> putDefaultMap(
            final Map<Class<?>, Converter<?, ? extends Throwable>> map) {
        map.put(Object.class, Converter.OBJECT);
        map.put(Class.class, Converter.CLASS);
        map.put(Date.class, Converter.DATE);
        map.put(File.class, Converter.FILE);
        map.put(Path.class, Converter.PATH);
        map.put(Number.class, Converter.NUMBER);
        map.put(URL.class, Converter.URL);
        map.put(FileInputStream.class, FileInputStream::new);
        map.put(Long.class, Long::parseLong);
        map.put(Integer.class, Integer::parseInt);
        map.put(Short.class, Short::parseShort);
        map.put(Byte.class, Byte::parseByte);
        map.put(
                Character.class,
                s ->
                        s.startsWith("\\u")
                                ? Character.toChars(Integer.parseInt(s.substring(2), HEX_RADIX))[0]
                                : s.charAt(0));
        map.put(Double.class, Double::parseDouble);
        map.put(Float.class, Float::parseFloat);
        map.put(BigInteger.class, BigInteger::new);
        map.put(BigDecimal.class, BigDecimal::new);
        return map;
    }
    private final Map<Class<?>, Converter<?, ? extends Throwable>> converterMap;
    public TypeHandler() {
        this(createDefaultMap());
    }
    public TypeHandler(final Map<Class<?>, Converter<?, ? extends Throwable>> converterMap) {
        this.converterMap = Objects.requireNonNull(converterMap, "converterMap");
    }
    @SuppressWarnings("unchecked") // returned value will have type T because it is fixed by clazz
    public <T> Converter<T, ?> getConverter(final Class<T> clazz) {
        return (Converter<T, ?>) converterMap.getOrDefault(clazz, Converter.DEFAULT);
    }
}
