package lang3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SystemPropertiesTest {

    private static final String TEST_PROPERTY = "test.lang3.property";
    private String originalPropertyValue;

    @BeforeEach
    void backupProperty() throws SecurityException {
        originalPropertyValue = System.getProperty(TEST_PROPERTY);
    }

    @AfterEach
    void restoreProperty() throws SecurityException {
        if (originalPropertyValue != null) {
            System.setProperty(TEST_PROPERTY, originalPropertyValue);
        } else {
            System.clearProperty(TEST_PROPERTY);
        }
    }

    @Test
    void testGetProperty_KeyExists() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "testValue");
        assertEquals("testValue", SystemProperties.getProperty(TEST_PROPERTY));
    }

    @Test
    void testGetProperty_KeyNotExists() throws SecurityException {
        System.clearProperty(TEST_PROPERTY);
        assertNull(SystemProperties.getProperty(TEST_PROPERTY));
    }

    @Test
    void testGetProperty_WithDefaultString_KeyExists() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "testValue");
        assertEquals("testValue", SystemProperties.getProperty(TEST_PROPERTY, "default"));
    }

    @Test
    void testGetProperty_WithDefaultString_KeyNotExists() throws SecurityException {
        System.clearProperty(TEST_PROPERTY);
        assertEquals("default", SystemProperties.getProperty(TEST_PROPERTY, "default"));
    }

    @Test
    void testGetProperty_WithSupplier_KeyExists() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "testValue");
        assertEquals("testValue", SystemProperties.getProperty(TEST_PROPERTY, () -> "default"));
    }

    @Test
    void testGetProperty_WithSupplier_KeyNotExists() throws SecurityException {
        System.clearProperty(TEST_PROPERTY);
        assertEquals("default", SystemProperties.getProperty(TEST_PROPERTY, () -> "default"));
    }

    @Test
    void testGetProperty_EmptyKey() throws SecurityException {
        assertNull(SystemProperties.getProperty(""));
        assertNull(SystemProperties.getProperty(null));
        assertEquals("default", SystemProperties.getProperty("", "default"));
        assertEquals("default", SystemProperties.getProperty(null, "default"));
        assertEquals("default", SystemProperties.getProperty("", () -> "default"));
        assertEquals("default", SystemProperties.getProperty(null, () -> "default"));
    }

    @Test
    void testGetBoolean_True() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "true");
        assertTrue(SystemProperties.getBoolean(TEST_PROPERTY, () -> false));
    }

    @Test
    void testGetBoolean_False() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "false");
        assertFalse(SystemProperties.getBoolean(TEST_PROPERTY, () -> true));
    }

    @Test
    void testGetBoolean_Invalid() throws SecurityException {
        System.setProperty(TEST_PROPERTY, "invalid");
        assertFalse(SystemProperties.getBoolean(TEST_PROPERTY, () -> true));
    }

    @Test
    void testGetBoolean_KeyNotExists() throws SecurityException {
        System.clearProperty(TEST_PROPERTY);
        assertTrue(SystemProperties.getBoolean(TEST_PROPERTY, () -> true));
        assertFalse(SystemProperties.getBoolean(TEST_PROPERTY, () -> false));
    }

    @Test
    void testGetFileSeparator() throws SecurityException {
        assertEquals(System.getProperty("file.separator"), SystemProperties.getFileSeparator());
    }

    @Test
    void testGetJavaSpecificationVersion() throws SecurityException {
        assertEquals(System.getProperty("java.specification.version"), SystemProperties.getJavaSpecificationVersion());
    }

    @Test
    void testGetJavaSpecificationVersion_WithDefault() throws SecurityException {
        assertEquals(System.getProperty("java.specification.version", "default"), SystemProperties.getJavaSpecificationVersion("default"));
    }

    @Test
    void testGetLineSeparator() throws SecurityException {
        assertEquals(System.getProperty("line.separator"), SystemProperties.getLineSeparator());
    }

    @Test
    void testGetOsArch() throws SecurityException {
        assertEquals(System.getProperty("os.arch"), SystemProperties.getOsArch());
    }

    @Test
    void testGetPathSeparator() throws SecurityException {
        assertEquals(System.getProperty("path.separator"), SystemProperties.getPathSeparator());
    }

    @Test
    void testGetUserName() throws SecurityException {
        assertEquals(System.getProperty("user.name"), SystemProperties.getUserName());
    }

    @Test
    void testGetUserName_WithDefault() throws SecurityException {
        assertEquals(System.getProperty("user.name", "default"), SystemProperties.getUserName("default"));
    }
}