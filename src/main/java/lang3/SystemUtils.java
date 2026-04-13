package lang3;
public class SystemUtils {
    @Deprecated
    public static final String FILE_SEPARATOR = SystemProperties.getFileSeparator();
    @Deprecated
    public static final String JAVA_AWT_FONTS = SystemProperties.getJavaAwtFonts();
    @Deprecated
    public static final String JAVA_AWT_GRAPHICSENV = SystemProperties.getJavaAwtGraphicsenv();
    @Deprecated
    public static final String JAVA_AWT_HEADLESS = SystemProperties.getJavaAwtHeadless();
    @Deprecated
    public static final String JAVA_AWT_PRINTERJOB = SystemProperties.getJavaAwtPrinterjob();
    @Deprecated
    public static final String JAVA_COMPILER = SystemProperties.getJavaCompiler();
    @Deprecated
    public static final String JAVA_ENDORSED_DIRS = SystemProperties.getJavaEndorsedDirs();
    @Deprecated
    public static final String JAVA_EXT_DIRS = SystemProperties.getJavaExtDirs();
    public static final String JAVA_SPECIFICATION_VERSION = SystemProperties.getJavaSpecificationVersion();
    @Deprecated
    public static final String LINE_SEPARATOR = SystemProperties.getLineSeparator();
    @Deprecated
    public static final String PATH_SEPARATOR = SystemProperties.getPathSeparator();
    @Deprecated
    public static final boolean IS_JAVA_1_9 = getJavaVersionMatches("9");
    @Deprecated
    public static final String USER_NAME_KEY = "user.name";
    @Deprecated
    public static final String USER_DIR_KEY = "user.dir";
    @Deprecated
    public static final String JAVA_IO_TMPDIR_KEY = "java.io.tmpdir";
    @Deprecated
    public static final String JAVA_HOME_KEY = "java.home";
    @Deprecated
    public static final String AWT_TOOLKIT = SystemProperties.getAwtToolkit();
    private static boolean getJavaVersionMatches(final String versionPrefix) {
        return isJavaVersionMatch(JAVA_SPECIFICATION_VERSION, versionPrefix);
    }
    @Deprecated
    public static String getUserName() {
        return SystemProperties.getUserName();
    }
    @Deprecated
    public static String getUserName(final String defaultValue) {
        return SystemProperties.getUserName(defaultValue);
    }
    @Deprecated
    public static boolean isJavaAwtHeadless() {
        return Boolean.TRUE.toString().equals(JAVA_AWT_HEADLESS);
    }
    static boolean isJavaVersionMatch(final String version, final String versionPrefix) {
        if (version == null) {
            return false;
        }
        return version.startsWith(versionPrefix);
    }
    public SystemUtils() {
    }
}