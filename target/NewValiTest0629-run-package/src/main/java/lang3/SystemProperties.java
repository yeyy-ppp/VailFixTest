package lang3;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lang3.function.Suppliers;
import org.apache.maven.surefire.shared.lang3.StringUtils;

public final class SystemProperties {
    @Deprecated
    public static final String AWT_TOOLKIT = "awt.toolkit";
    public static final String FILE_SEPARATOR = "file.separator";
    @Deprecated
    public static final String JAVA_AWT_FONTS = "java.awt.fonts";
    @Deprecated
    public static final String JAVA_AWT_GRAPHICSENV = "java.awt.graphicsenv";
    @Deprecated
    public static final String JAVA_AWT_HEADLESS = "java.awt.headless";
    @Deprecated
    public static final String JAVA_AWT_PRINTERJOB = "java.awt.printerjob";
    @Deprecated
    public static final String JAVA_COMPILER = "java.compiler";
    @Deprecated
    public static final String JAVA_ENDORSED_DIRS = "java.endorsed.dirs";
    @Deprecated
    public static final String JAVA_EXT_DIRS = "java.ext.dirs";
    public static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";
    public static final String LINE_SEPARATOR = "line.separator";
    public static final String OS_ARCH = "os.arch";
    public static final String PATH_SEPARATOR = "path.separator";
    public static final String USER_NAME = "user.name";
    @Deprecated
    public static String getAwtToolkit() {
        return getProperty(AWT_TOOLKIT);
    }
    public static boolean getBoolean(final String key, final BooleanSupplier defaultIfAbsent) {
        final String str = getProperty(key);
        return str == null ? defaultIfAbsent != null && defaultIfAbsent.getAsBoolean() : Boolean.parseBoolean(str);
    }
    public static String getFileSeparator() {
        return getProperty(FILE_SEPARATOR);
    }
    @Deprecated
    public static String getJavaAwtFonts() {
        return getProperty(JAVA_AWT_FONTS);
    }
    @Deprecated
    public static String getJavaAwtGraphicsenv() {
        return getProperty(JAVA_AWT_GRAPHICSENV);
    }
    @Deprecated
    public static String getJavaAwtHeadless() {
        return getProperty(JAVA_AWT_HEADLESS);
    }
    @Deprecated
    public static String getJavaAwtPrinterjob() {
        return getProperty(JAVA_AWT_PRINTERJOB);
    }
    @Deprecated
    public static String getJavaCompiler() {
        return getProperty(JAVA_COMPILER);
    }
    @Deprecated
    public static String getJavaEndorsedDirs() {
        return getProperty(JAVA_ENDORSED_DIRS);
    }
    @Deprecated
    public static String getJavaExtDirs() {
        return getProperty(JAVA_EXT_DIRS);
    }
    public static String getJavaSpecificationVersion() {
        return getProperty(JAVA_SPECIFICATION_VERSION);
    }
    public static String getJavaSpecificationVersion(final String defaultValue) {
        return getProperty(JAVA_SPECIFICATION_VERSION, defaultValue);
    }
    public static String getLineSeparator() {
        return getProperty(LINE_SEPARATOR);
    }
    public static String getOsArch() {
        return getProperty(OS_ARCH);
    }
    public static String getPathSeparator() {
        return getProperty(PATH_SEPARATOR);
    }
    public static String getProperty(final String property) {
        return getProperty(property, Suppliers.nul());
    }
    static String getProperty(final String property, final String defaultIfAbsent) {
        return getProperty(property, () -> defaultIfAbsent);
    }
    static String getProperty(final String property, final Supplier<String> defaultIfAbsent) {
        try {
            if (StringUtils.isEmpty(property)) {
                return Suppliers.get(defaultIfAbsent);
            }
            return StringUtils.getIfEmpty(System.getProperty(property), defaultIfAbsent);
        } catch (final SecurityException ignore) {
            return defaultIfAbsent.get();
        }
    }
    public static String getUserName() {
        return getProperty(USER_NAME);
    }
    public static String getUserName(final String defaultValue) {
        return getProperty(USER_NAME, defaultValue);
    }
    @Deprecated
    public SystemProperties() {
    }
}