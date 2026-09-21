package lang3;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassPathUtilsTest {

    @Test
    void testPackageToPath_NullInput() {
        assertThrows(NullPointerException.class, () -> ClassPathUtils.packageToPath(null));
    }

    @Test
    void testPackageToPath_EmptyString() {
        assertEquals("", ClassPathUtils.packageToPath(""));
    }

    @Test
    void testPackageToPath_SingleSegment() {
        assertEquals("abc", ClassPathUtils.packageToPath("abc"));
    }

    @Test
    void testPackageToPath_MultipleSegments() {
        assertEquals("a/b/c", ClassPathUtils.packageToPath("a.b.c"));
    }

    @Test
    void testPackageToPath_AlreadyContainingSlashes() {
        assertEquals("a/b/c", ClassPathUtils.packageToPath("a/b.c"));
    }

    @Test
    void testPathToPackage_NullInput() {
        assertThrows(NullPointerException.class, () -> ClassPathUtils.pathToPackage(null));
    }

    @Test
    void testPathToPackage_EmptyString() {
        assertEquals("", ClassPathUtils.pathToPackage(""));
    }

    @Test
    void testPathToPackage_SingleSegment() {
        assertEquals("abc", ClassPathUtils.pathToPackage("abc"));
    }

    @Test
    void testPathToPackage_MultipleSegments() {
        assertEquals("a.b.c", ClassPathUtils.pathToPackage("a/b/c"));
    }
}