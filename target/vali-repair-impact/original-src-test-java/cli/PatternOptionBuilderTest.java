package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PatternOptionBuilderTest {

    @Test
    public void testConstructor() {
        new PatternOptionBuilder();
    }

    @Test
    public void testGetValueClass() {
        assertEquals(PatternOptionBuilder.STRING_VALUE, PatternOptionBuilder.getValueClass(':'));
        assertEquals(PatternOptionBuilder.NUMBER_VALUE, PatternOptionBuilder.getValueClass('%'));
        assertNull(PatternOptionBuilder.getValueClass('x'));
    }

    @Test
    public void testGetValueType() {
        assertEquals(PatternOptionBuilder.OBJECT_VALUE, PatternOptionBuilder.getValueType('@'));
        assertEquals(PatternOptionBuilder.STRING_VALUE, PatternOptionBuilder.getValueType(':'));
        assertEquals(PatternOptionBuilder.NUMBER_VALUE, PatternOptionBuilder.getValueType('%'));
        assertEquals(PatternOptionBuilder.CLASS_VALUE, PatternOptionBuilder.getValueType('+'));
        assertEquals(PatternOptionBuilder.DATE_VALUE, PatternOptionBuilder.getValueType('#'));
        assertEquals(PatternOptionBuilder.EXISTING_FILE_VALUE, PatternOptionBuilder.getValueType('<'));
        assertEquals(PatternOptionBuilder.FILE_VALUE, PatternOptionBuilder.getValueType('>'));
        assertEquals(PatternOptionBuilder.FILES_VALUE, PatternOptionBuilder.getValueType('*'));
        assertEquals(PatternOptionBuilder.URL_VALUE, PatternOptionBuilder.getValueType('/'));
        assertNull(PatternOptionBuilder.getValueType('?'));
    }

    @Test
    public void testIsValueCode() {
        assertTrue(PatternOptionBuilder.isValueCode('@'));
        assertTrue(PatternOptionBuilder.isValueCode(':'));
        assertTrue(PatternOptionBuilder.isValueCode('%'));
        assertTrue(PatternOptionBuilder.isValueCode('+'));
        assertTrue(PatternOptionBuilder.isValueCode('#'));
        assertTrue(PatternOptionBuilder.isValueCode('<'));
        assertTrue(PatternOptionBuilder.isValueCode('>'));
        assertTrue(PatternOptionBuilder.isValueCode('*'));
        assertTrue(PatternOptionBuilder.isValueCode('/'));
        assertTrue(PatternOptionBuilder.isValueCode('!'));
        assertFalse(PatternOptionBuilder.isValueCode('a'));
        assertFalse(PatternOptionBuilder.isValueCode('-'));
        assertFalse(PatternOptionBuilder.isValueCode(' '));
    }

    @Test
    public void testParsePattern_EmptyString() {
        Options options = PatternOptionBuilder.parsePattern("");
        assertTrue(options.getOptions().isEmpty());
    }

    @Test
    public void testParsePattern_SingleOptionNoArg() {
        Options options = PatternOptionBuilder.parsePattern("a");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertFalse(option.hasArg());
        assertFalse(option.isRequired());
       // assertNull(option.getType());
    }

    @Test
    public void testParsePattern_SingleOptionWithArg() {
        Options options = PatternOptionBuilder.parsePattern("a:");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertTrue(option.hasArg());
        assertEquals(PatternOptionBuilder.STRING_VALUE, option.getType());
    }

    @Test
    public void testParsePattern_RequiredOption() {
        Options options = PatternOptionBuilder.parsePattern("a!");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertTrue(option.isRequired());
    }

    @Test
    public void testParsePattern_OptionWithMultipleValueCodes() {
        Options options = PatternOptionBuilder.parsePattern("a%!*");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertTrue(option.hasArg());
        assertTrue(option.isRequired());
        assertEquals(PatternOptionBuilder.FILES_VALUE, option.getType());
    }

    @Test
    public void testParsePattern_MultipleOptions() {
        Options options = PatternOptionBuilder.parsePattern("ab:c");
        assertEquals(3, options.getOptions().size());
        assertTrue(options.hasOption("a"));
        assertTrue(options.hasOption("b"));
        assertTrue(options.hasOption("c"));
        
        Option optionB = options.getOption("b");
        assertTrue(optionB.hasArg());
        assertEquals(PatternOptionBuilder.STRING_VALUE, optionB.getType());
    }

    @Test
    public void testParsePattern_ValueCodeAtStart() {
        Options options = PatternOptionBuilder.parsePattern(":a");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertTrue(option.hasArg());
    }

    @Test
    public void testParsePattern_ValueCodeAtEnd() {
        Options options = PatternOptionBuilder.parsePattern("a:");
        Option option = options.getOption("a");
        assertNotNull(option);
        assertTrue(option.hasArg());
    }

    @Test
    public void testParsePattern_AllValueCodes() {
        String pattern = "@:%+#<>*/!";
        Options options = PatternOptionBuilder.parsePattern(pattern);
        assertTrue(options.getOptions().isEmpty());
    }

    @Test
    public void testUnsupported() {
        Converter<?, ?> converter = PatternOptionBuilder.unsupported();
        assertThrows(UnsupportedOperationException.class, () -> {
            converter.apply("test");
        });
    }
}