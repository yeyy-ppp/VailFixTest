package lang3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class SystemUtilsTest {

    private String originalJavaSpecVersion;
    private String originalAwtHeadless;
    private String originalUserName;

    @BeforeEach
    void backupSystemProperties() {
        originalJavaSpecVersion = System.getProperty("java.specification.version");
        originalAwtHeadless = System.getProperty("java.awt.headless");
        originalUserName = System.getProperty("user.name");
    }

    @AfterEach
    void restoreSystemProperties() {
        setSystemProperty("java.specification.version", originalJavaSpecVersion);
        setSystemProperty("java.awt.headless", originalAwtHeadless);
        setSystemProperty("user.name", originalUserName);
    }

    private void setSystemProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }

    @Test
    void testGetJavaVersionMatches() {
        System.setProperty("java.specification.version", "1.8");
        String javaVersion = System.getProperty("java.specification.version");
        assertTrue(SystemUtils.isJavaVersionMatch(javaVersion, "1.8"));
        assertFalse(SystemUtils.isJavaVersionMatch(javaVersion, "9"));
    }

    @Test
    void testGetUserName() {
        System.setProperty("user.name", "testUser");
        assertEquals("testUser", SystemUtils.getUserName());
    }

    @Test
    void testGetUserNameWithDefault() {
        System.clearProperty("user.name");
        assertEquals("defaultUser", SystemUtils.getUserName("defaultUser"));
    }

    @Test
    void testIsJavaAwtHeadless() {
        System.setProperty("java.awt.headless", "true");
        assertTrue(SystemUtils.isJavaAwtHeadless());
        System.setProperty("java.awt.headless", "false");
        assertTrue(SystemUtils.isJavaAwtHeadless());
    }

    @Test
    void testIsJavaVersionMatch() {
        assertTrue(SystemUtils.isJavaVersionMatch("11.0.2", "11"));
        assertFalse(SystemUtils.isJavaVersionMatch("1.8.0", "9"));
        assertFalse(SystemUtils.isJavaVersionMatch(null, "1"));
    }

    @Test
    void testConstructor() {
        assertDoesNotThrow(() -> new SystemUtils());
    }
}