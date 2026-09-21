package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

class OptionTest {

    @Test
    void testBuilderWithShortOption() {
        Option option = Option.builder("a").desc("Description").build();
        assertEquals("a", option.getOpt());
        assertEquals("Description", option.getDescription());
    }

    @Test
    void testBuilderWithLongOption() {
        Option option = Option.builder().longOpt("long").desc("Long option").build();
        assertEquals("long", option.getLongOpt());
    }

    @Test
    void testBuilderThrowsExceptionWhenNoOption() {
        assertThrows(IllegalArgumentException.class, () -> Option.builder().build());
    }

    @Test
    void testArgName() {
        Option option = Option.builder("a").argName("arg").build();
        assertEquals("arg", option.getArgName());
    }

    @Test
    void testHasArg() {
        Option option = Option.builder("a").hasArg().build();
        assertTrue(option.hasArg());
        assertEquals(1, option.getArgs());
    }

    @Test
    void testHasArgWithFalse() {
        Option option = Option.builder("a").hasArg(false).build();
        assertFalse(option.hasArg());
    }

    @Test
    void testHasArgs() {
        Option option = Option.builder("a").hasArgs().build();
        assertTrue(option.hasArgs());
        assertEquals(Option.UNLIMITED_VALUES, option.getArgs());
    }

    @Test
    void testNumberOfArgs() {
        Option option = Option.builder("a").numberOfArgs(3).build();
        assertEquals(3, option.getArgs());
    }

    @Test
    void testOptionalArg() {
        Option option = Option.builder("a").optionalArg(true).build();
        assertTrue(option.hasOptionalArg());
    }

    @Test
    void testRequired() {
        Option option = Option.builder("a").required().build();
        assertTrue(option.isRequired());
    }

    @Test
    void testSince() {
        Option option = Option.builder("a").since("1.0").build();
        assertEquals("1.0", option.getSince());
    }

    @Test
    void testType() {
        Option option = Option.builder("a").type(Integer.class).build();
        assertEquals(Integer.class, option.getType());
    }

    @Test
    void testValueSeparator() {
        Option option = Option.builder("a").valueSeparator(',').build();
        assertEquals(',', option.getValueSeparator());
    }

    @Test
    void testDeprecated() {
        Option option = Option.builder("a").deprecated().build();
        assertTrue(option.isDeprecated());
    }

    @Test
    void testAddProcessValue() {
        Option option = Option.builder("a").hasArg().build();
        option.processValue("val1");
        assertEquals("val1", option.getValue());
    }

    @Test
    void testProcessValueWithSeparator() {
        Option option = Option.builder("a").hasArgs().valueSeparator(',').build();
        option.processValue("v1,v2,v3");
        List<String> values = option.getValuesList();
        assertEquals(Arrays.asList("v1", "v2", "v3"), values);
    }

    @Test
    void testProcessValueThrowsWhenUninitialized() {
        Option option = Option.builder("a").build();
        assertThrows(IllegalArgumentException.class, () -> option.processValue("val"));
    }

    @Test
    void testClearValues() {
        Option option = Option.builder("a").hasArg().build();
        option.processValue("val");
        option.clearValues();
        assertTrue(option.getValuesList().isEmpty());
    }

 /*   @Test
    void testGetValue() {
        Option option = Option.builder("a").hasArg().build();
        option.processValue("value");
        assertEquals("value", option.getValue());
        assertEquals("value", option.getValue(0));
        assertEquals("default", option.getValue("default"));
    }*/

    /*@Test
    void testGetValueOutOfBounds() {
        Option option = Option.builder("a").hasArg().build();
        assertThrows(IndexOutOfBoundsException.class, () -> option.getValue(1));
    }*/

    @Test
    void testGetValues() {
        Option option = Option.builder("a").numberOfArgs(2).build();
        option.processValue("v1");
        option.processValue("v2");
        assertArrayEquals(new String[]{"v1", "v2"}, option.getValues());
    }

    @Test
    void testRequiresArg() {
        Option option = Option.builder("a").hasArg().build();
        assertTrue(option.requiresArg());
    }

    @Test
    void testAcceptsArg() {
        Option option = Option.builder("a").numberOfArgs(1).build();
        assertTrue(option.acceptsArg());
        option.processValue("val");
        assertFalse(option.acceptsArg());
    }

    @Test
    void testEqualsAndHashCode() {
        Option option1 = Option.builder("a").build();
        Option option2 = Option.builder("a").build();
        Option option3 = Option.builder("b").build();
        
        assertEquals(option1, option2);
        assertNotEquals(option1, option3);
        assertEquals(option1.hashCode(), option2.hashCode());
    }

    @Test
    void testClone() {
        Option option = Option.builder("a").hasArg().build();
        option.processValue("val");
        Option clone = (Option) option.clone();
        assertEquals(option.getValuesList(), clone.getValuesList());
    }

    @Test
    void testToString() {
        Option option = Option.builder("a").desc("Desc").type(Integer.class).build();
        assertNotNull(option.toString());
    }

    @Test
    void testToDeprecatedString() {
        Option option = Option.builder("a").deprecated().build();
        assertFalse(option.toDeprecatedString().isEmpty());
    }

    @Test
    void testConstructorWithShortAndDesc() {
        Option option = new Option("a", "Description");
        assertEquals("a", option.getOpt());
        assertEquals("Description", option.getDescription());
    }

    @Test
    void testConstructorWithShortHasArgDesc() {
        Option option = new Option("a", true, "Desc");
        assertTrue(option.hasArg());
    }

    @Test
    void testConstructorWithFullParams() {
        Option option = new Option("a", "arg", true, "Desc");
        assertEquals("arg", option.getLongOpt());
    }

    @Test
    void testSetArgName() {
        Option option = Option.builder("a").build();
        option.setArgName("newArg");
        assertEquals("newArg", option.getArgName());
    }

    @Test
    void testSetArgs() {
        Option option = Option.builder("a").build();
        option.setArgs(2);
        assertEquals(2, option.getArgs());
    }

    @Test
    void testSetConverter() {
        Option option = Option.builder("a").build();
        Converter<String, RuntimeException> converter = value -> value;
        option.setConverter(converter);
        assertSame(converter, option.getConverter());
    }

    @Test
    void testSetType() {
        Option option = Option.builder("a").build();
        option.setType(Integer.class);
        assertEquals(Integer.class, option.getType());
    }

    @Test
    void testGetDefaultConverter() {
        Option option = Option.builder("a").type(Integer.class).build();
        assertNotNull(option.getConverter());
    }

    @Test
    void testHasArgName() {
        Option option = Option.builder("a").argName("arg").build();
        assertTrue(option.hasArgName());
    }

    @Test
    void testHasArgsMethod() {
        Option option = Option.builder("a").numberOfArgs(3).build();
        assertTrue(option.hasArgs());
    }

    @Test
    void testHasLongOpt() {
        Option option = Option.builder("a").longOpt("long").build();
        assertTrue(option.hasLongOpt());
    }

    @Test
    void testValueSeparatorCheck() {
        Option option = Option.builder("a").valueSeparator(',').build();
        assertTrue(option.hasValueSeparator());
    }

    @Test
    void testAddValueDeprecatedThrows() {
        Option option = Option.builder("a").build();
        assertThrows(UnsupportedOperationException.class, () -> option.addValue("value"));
    }

    @Test
    void testSetDeprecatedTypeMethod() {
        Option option = Option.builder("a").build();
        option.setType((Class) String.class);
        assertEquals(String.class, option.getType());
    }

    @Test
    void testGetDeprecated() {
        Option option = Option.builder("a").deprecated().build();
        assertNotNull(option.getDeprecated());
    }

    @Test
    void testGetId() {
        Option option1 = Option.builder("a").build();
        Option option2 = Option.builder("b").build();
        assertNotEquals(option1.getId(), option2.getId());
    }

    @Test
    void testGetKey() {
        Option optionShort = Option.builder("a").build();
        assertEquals("a", optionShort.getKey());

        Option optionLong = Option.builder().longOpt("long").build();
        assertEquals("long", optionLong.getKey());
    }

    @Test
    void testSetDescription() {
        Option option = Option.builder("a").desc("Initial").build();
        option.setDescription("Updated");
        assertEquals("Updated", option.getDescription());
    }

    @Test
    void testSetLongOpt() {
        Option option = Option.builder("a").build();
        option.setLongOpt("newLong");
        assertEquals("newLong", option.getLongOpt());
    }

    @Test
    void testSetOptionalArg() {
        Option option = Option.builder("a").build();
        option.setOptionalArg(true);
        assertTrue(option.hasOptionalArg());
    }

    @Test
    void testSetRequired() {
        Option option = Option.builder("a").build();
        option.setRequired(true);
        assertTrue(option.isRequired());
    }

    @Test
    void testSetValueSeparator() {
        Option option = Option.builder("a").build();
        option.setValueSeparator(';');
        assertEquals(';', option.getValueSeparator());
    }
}