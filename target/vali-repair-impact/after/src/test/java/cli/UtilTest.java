package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class UtilTest {
    // [兜底] 保留了 23 个正确的测试方法

    @Test
    void testIsEmpty_ObjectArray_Null() {
        assertTrue(Util.isEmpty((Object[]) null));
    }

    @Test
    void testIsEmpty_ObjectArray_Empty() {
        assertTrue(Util.isEmpty(new Object[0]));
    }

    @Test
    void testIsEmpty_ObjectArray_NonEmpty() {
        assertFalse(Util.isEmpty(new Object[]{"test"}));
    }

    @Test
    void testIsEmpty_String_Null() {
        assertTrue(Util.isEmpty((String) null));
    }

    @Test
    void testIsEmpty_String_Empty() {
        assertTrue(Util.isEmpty(""));
    }

    @Test
    void testIsEmpty_String_NonEmpty() {
        assertFalse(Util.isEmpty("test"));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_Null() {
        assertNull(Util.stripLeadingAndTrailingQuotes(null));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_Empty() {
        assertEquals("", Util.stripLeadingAndTrailingQuotes(""));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_SingleChar() {
        assertEquals("a", Util.stripLeadingAndTrailingQuotes("a"));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_NoQuotes() {
        assertEquals("test", Util.stripLeadingAndTrailingQuotes("test"));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_LeadingQuoteOnly() {
        assertEquals("\"test", Util.stripLeadingAndTrailingQuotes("\"test"));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_TrailingQuoteOnly() {
        assertEquals("test\"", Util.stripLeadingAndTrailingQuotes("test\""));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_ValidQuotes() {
        assertEquals("test", Util.stripLeadingAndTrailingQuotes("\"test\""));
    }

    @Test
    void testStripLeadingAndTrailingQuotes_InternalQuotes() {
        assertEquals("\"te\"st\"", Util.stripLeadingAndTrailingQuotes("\"te\"st\""));
    }

    @Test
    void testStripLeadingHyphens_Null() {
        assertNull(Util.stripLeadingHyphens(null));
    }

    @Test
    void testStripLeadingHyphens_Empty() {
        assertEquals("", Util.stripLeadingHyphens(""));
    }

    @Test
    void testStripLeadingHyphens_NoHyphen() {
        assertEquals("test", Util.stripLeadingHyphens("test"));
    }

    @Test
    void testStripLeadingHyphens_SingleHyphen() {
        assertEquals("test", Util.stripLeadingHyphens("-test"));
    }

    @Test
    void testStripLeadingHyphens_DoubleHyphen() {
        assertEquals("test", Util.stripLeadingHyphens("--test"));
    }

    @Test
    void testStripLeadingHyphens_TripleHyphen() {
        assertEquals("-test", Util.stripLeadingHyphens("---test"));
    }

    @Test
    void testStripLeadingHyphens_SingleHyphenOnly() {
        assertEquals("", Util.stripLeadingHyphens("-"));
    }

    @Test
    void testStripLeadingHyphens_DoubleHyphenOnly() {
        assertEquals("", Util.stripLeadingHyphens("--"));
    }

    @Test
    void testUtilConstructor() throws NoSuchMethodException {
        Constructor<Util> constructor = Util.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

}
