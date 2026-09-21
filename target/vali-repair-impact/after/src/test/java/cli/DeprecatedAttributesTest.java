package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import cli.DeprecatedAttributes;

@SuppressWarnings("deprecation")
public class DeprecatedAttributesTest {

    @Test
    void testBuilder() {
        DeprecatedAttributes.Builder builder = DeprecatedAttributes.builder();
        assertNotNull(builder);
    }

    @Test
    void testBuilderGet() {
        DeprecatedAttributes da = DeprecatedAttributes.builder().get();
        assertNotNull(da);
        assertEquals("", da.getDescription());
        assertEquals("", da.getSince());
        assertFalse(da.isForRemoval());
    }

    @Test
    void testBuilderSetDescription() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setDescription("test")
            .get();
        assertEquals("test", da.getDescription());
    }

    @Test
    void testBuilderSetDescriptionNull() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setDescription(null)
            .get();
        assertEquals("", da.getDescription());
    }

    @Test
    void testBuilderSetForRemoval() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .get();
        assertTrue(da.isForRemoval());
    }

    @Test
    void testBuilderSetSince() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setSince("1.0")
            .get();
        assertEquals("1.0", da.getSince());
    }

    @Test
    void testBuilderSetSinceNull() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setSince(null)
            .get();
        assertEquals("", da.getSince());
    }

    @Test
    void testGetDescription() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setDescription("desc")
            .get();
        assertEquals("desc", da.getDescription());
    }

    @Test
    void testGetSince() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setSince("2.0")
            .get();
        assertEquals("2.0", da.getSince());
    }

    @Test
    void testIsForRemoval() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .get();
        assertTrue(da.isForRemoval());
    }

    @Test
    void testToStringDefault() {
        DeprecatedAttributes da = DeprecatedAttributes.builder().get();
        assertEquals("Deprecated", da.toString());
    }

    @Test
    void testToStringForRemovalOnly() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .get();
        assertEquals("Deprecated for removal", da.toString());
    }

    @Test
    void testToStringSinceOnly() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setSince("3.0")
            .get();
        assertEquals("Deprecated since 3.0", da.toString());
    }

    @Test
    void testToStringDescriptionOnly() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setDescription("warning")
            .get();
        assertEquals("Deprecated: warning", da.toString());
    }

    @Test
    void testToStringFull() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .setSince("4.0")
            .setDescription("deprecated item")
            .get();
        assertEquals("Deprecated for removal since 4.0: deprecated item", da.toString());
    }

    @Test
    void testToStringForRemovalAndSince() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .setSince("5.0")
            .get();
        assertEquals("Deprecated for removal since 5.0", da.toString());
    }

    @Test
    void testToStringForRemovalAndDescription() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setForRemoval(true)
            .setDescription("not safe")
            .get();
        assertEquals("Deprecated for removal: not safe", da.toString());
    }

    @Test
    void testToStringSinceAndDescription() {
        DeprecatedAttributes da = DeprecatedAttributes.builder()
            .setSince("6.0")
            .setDescription("obsolete")
            .get();
        assertEquals("Deprecated since 6.0: obsolete", da.toString());
    }

}
