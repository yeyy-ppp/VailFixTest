package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormattingStyleTest {

    @Test
    void withNewlineAndIndent_valid_createsInstance() {
        FormattingStyle style = FormattingStyle.PRETTY.withNewline("\n").withIndent("  ");
        assertEquals("\n", style.getNewline());
        assertEquals("  ", style.getIndent());
        assertTrue(style.usesSpaceAfterSeparators());
    }

    @Test
    void withNewline_null_throwsException() {
        assertThrows(NullPointerException.class, () -> 
            FormattingStyle.PRETTY.withNewline(null)
        );
    }

    @Test
    void withIndent_null_throwsException() {
        assertThrows(NullPointerException.class, () -> 
            FormattingStyle.PRETTY.withIndent(null)
        );
    }

    @Test
    void withNewline_invalidPattern_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            FormattingStyle.PRETTY.withNewline("invalid")
        );
    }

    @Test
    void withIndent_invalidPattern_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            FormattingStyle.PRETTY.withIndent("invalid")
        );
    }

    @Test
    void withNewline_validValue_returnsNewInstance() {
        FormattingStyle original = FormattingStyle.PRETTY;
        FormattingStyle modified = original.withNewline("\r\n");
        
        assertEquals("\r\n", modified.getNewline());
        assertEquals(original.getIndent(), modified.getIndent());
        assertEquals(original.usesSpaceAfterSeparators(), modified.usesSpaceAfterSeparators());
    }

    @Test
    void withIndent_validValue_returnsNewInstance() {
        FormattingStyle original = FormattingStyle.PRETTY;
        FormattingStyle modified = original.withIndent("\t");
        
        assertEquals("\t", modified.getIndent());
        assertEquals(original.getNewline(), modified.getNewline());
        assertEquals(original.usesSpaceAfterSeparators(), modified.usesSpaceAfterSeparators());
    }

    @Test
    void withSpaceAfterSeparators_returnsNewInstance() {
        FormattingStyle styleFalse = FormattingStyle.PRETTY.withSpaceAfterSeparators(false);
        FormattingStyle styleTrue = FormattingStyle.PRETTY.withSpaceAfterSeparators(true);
        
        assertFalse(styleFalse.usesSpaceAfterSeparators());
        assertTrue(styleTrue.usesSpaceAfterSeparators());
    }

    @Test
    void getNewline_returnsCorrectValue() {
        assertEquals("", FormattingStyle.COMPACT.getNewline());
        assertEquals("\n", FormattingStyle.PRETTY.getNewline());
    }

    @Test
    void getIndent_returnsCorrectValue() {
        assertEquals("", FormattingStyle.COMPACT.getIndent());
        assertEquals("  ", FormattingStyle.PRETTY.getIndent());
    }

    @Test
    void usesSpaceAfterSeparators_returnsCorrectValue() {
        assertFalse(FormattingStyle.COMPACT.usesSpaceAfterSeparators());
        assertTrue(FormattingStyle.PRETTY.usesSpaceAfterSeparators());
    }
}