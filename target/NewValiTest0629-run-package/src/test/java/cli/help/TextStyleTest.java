package cli.help;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextStyleTest {

    @Test
    void testBuilderInitialization() {
        TextStyle.Builder builder = TextStyle.builder();
        assertNotNull(builder);
        assertEquals(0, builder.getIndent());
        assertEquals(0, builder.getLeftPad());
        assertTrue(builder.isScalable());
        assertEquals(0, builder.getMinWidth());
        assertEquals(TextStyle.UNSET_MAX_WIDTH, builder.getMaxWidth());
    }

    @Test
    void testTextStyleConstruction() {
        TextStyle.Builder builder = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.CENTER)
                .setIndent(4)
                .setLeftPad(2)
                .setScalable(false)
                .setMinWidth(10)
                .setMaxWidth(50);

        TextStyle style = builder.get();
        assertEquals(TextStyle.Alignment.CENTER, style.getAlignment());
        assertEquals(4, style.getIndent());
        assertEquals(2, style.getLeftPad());
        assertFalse(style.isScalable());
        assertEquals(10, style.getMinWidth());
        assertEquals(50, style.getMaxWidth());
    }

    @Test
    void testSetTextStyle() {
        TextStyle.Builder builder1 = TextStyle.builder()
                .setIndent(3)
                .setLeftPad(1)
                .setScalable(true);

        TextStyle.Builder builder2 = TextStyle.builder()
                .setTextStyle(builder1.get());

        assertEquals(3, builder2.getIndent());
        assertEquals(1, builder2.getLeftPad());
        assertTrue(builder2.isScalable());
    }

    @Test
    void testPadLeftAlignment() {
        TextStyle style = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.LEFT)
                .setIndent(2)
                .setMaxWidth(10)
                .get();

        CharSequence result1 = style.pad(true, "text");
        assertEquals("  text    ", result1.toString());

        CharSequence result2 = style.pad(true, "longtext");
        assertEquals("longtext  ", result2.toString());

        TextStyle unsetStyle = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.LEFT)
                .setIndent(2)
                .get();
        CharSequence result3 = unsetStyle.pad(true, "txt");
        assertEquals("  txt", result3.toString());
    }

    @Test
    void testPadRightAlignment() {
        TextStyle style = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.RIGHT)
                .setIndent(2)
                .setMaxWidth(10)
                .get();

        CharSequence result1 = style.pad(true, "text");
        assertEquals("      text", result1.toString());

        CharSequence result2 = style.pad(false, "text");
        assertEquals("      text", result2.toString());
    }

    @Test
    void testPadCenterAlignment() {
        TextStyle style = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.CENTER)
                .setMaxWidth(10)
                .get();

        CharSequence result1 = style.pad(true, "text");
        assertEquals("   text   ", result1.toString());

        TextStyle unsetStyle = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.CENTER)
                .setIndent(3)
                .get();
        CharSequence result2 = unsetStyle.pad(true, "txt");
        assertEquals(" txt  ", result2.toString());
    }

    @Test
    void testPadNoChangeWhenExceedsMaxWidth() {
        TextStyle style = TextStyle.builder()
                .setMaxWidth(5)
                .get();

        CharSequence result = style.pad(true, "longtext");
        assertEquals("longtext", result.toString());
    }

    @Test
    void testToString() {
        TextStyle style = TextStyle.builder()
                .setAlignment(TextStyle.Alignment.RIGHT)
                .setLeftPad(3)
                .setIndent(1)
                .setScalable(false)
                .setMinWidth(5)
                .setMaxWidth(20)
                .get();

        String result = style.toString();
        assertTrue(result.contains("TextStyle{RIGHT, l:3, i:1, false, min:5, max:20}"));
    }
}