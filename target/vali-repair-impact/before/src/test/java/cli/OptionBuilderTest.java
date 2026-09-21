package cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class OptionBuilderTest {

    @BeforeEach
    void resetStaticState() throws Exception {
        Method reset = OptionBuilder.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    @Test
    void create_throwsExceptionWhenNoLongOpt() {
        assertThrows(IllegalArgumentException.class, () -> OptionBuilder.create());
    }

    @Test
    void create_successWithLongOpt() {
        Option option = OptionBuilder.withLongOpt("long").hasArg().create();
        assertEquals("long", option.getLongOpt());
        assertFalse(option.isRequired());
    }

    @Test
    void createCharOption_success() {
        Option option = OptionBuilder.create('a');
        assertEquals("a", option.getOpt());
        assertFalse(option.hasArg());
    }

    @Test
    void createStringOption_success() {
        Option option = OptionBuilder.create("b");
        assertEquals("b", option.getOpt());
        assertFalse(option.hasArg());
    }

    @Test
    void hasArg_setsArgCountToOne() throws Exception {
        OptionBuilder.hasArg();
        Field argCount = OptionBuilder.class.getDeclaredField("argCount");
        argCount.setAccessible(true);
        assertEquals(1, argCount.get(null));
    }

    @Test
    void hasArgBoolean_trueSetsArgCount() {
        Option option = OptionBuilder.hasArg(true).create('c');
        assertTrue(option.hasArg());
    }

    @Test
    void hasArgBoolean_falseResetsArgCount() {
        Option option = OptionBuilder.hasArg(false).create('d');
        assertFalse(option.hasArg());
    }

    @Test
    void hasArgs_setsUnlimitedArgs() {
        Option option = OptionBuilder.hasArgs().create('e');
        assertEquals(Option.UNLIMITED_VALUES, option.getArgs());
    }

    @Test
    void hasArgsInt_setsSpecificArgCount() {
        Option option = OptionBuilder.hasArgs(3).create('f');
        assertEquals(3, option.getArgs());
    }

    @Test
    void hasOptionalArg_setsArgAndOptional() {
        Option option = OptionBuilder.hasOptionalArg().create('g');
        assertTrue(option.hasOptionalArg());
        assertEquals(1, option.getArgs());
    }

    @Test
    void hasOptionalArgs_setsUnlimitedOptional() {
        Option option = OptionBuilder.hasOptionalArgs().create('h');
        assertTrue(option.hasOptionalArg());
        assertEquals(Option.UNLIMITED_VALUES, option.getArgs());
    }

    @Test
    void hasOptionalArgsInt_setsSpecificOptionalArgs() {
        Option option = OptionBuilder.hasOptionalArgs(2).create('i');
        assertTrue(option.hasOptionalArg());
        assertEquals(2, option.getArgs());
    }

    @Test
    void isRequired_setsRequiredFlag() {
        Option option = OptionBuilder.isRequired().create('j');
        assertTrue(option.isRequired());
    }

    @Test
    void isRequiredBoolean_setsRequiredAsTrue() {
        Option option = OptionBuilder.isRequired(true).create('k');
        assertTrue(option.isRequired());
    }

    @Test
    void isRequiredBoolean_setsRequiredAsFalse() {
        Option option = OptionBuilder.isRequired(false).create('l');
        assertFalse(option.isRequired());
    }

    @Test
    void withArgName_setsArgumentName() {
        Option option = OptionBuilder.withArgName("arg").create('m');
        assertEquals("arg", option.getArgName());
    }

    @Test
    void withDescription_setsDescription() {
        Option option = OptionBuilder.withDescription("desc").create('n');
        assertEquals("desc", option.getDescription());
    }

    @Test
    void withLongOpt_setsLongOption() {
        Option option = OptionBuilder.withLongOpt("longopt").create('o');
        assertEquals("longopt", option.getLongOpt());
    }

    @Test
    void withTypeClass_setsParameterType() {
        Option option = OptionBuilder.withType(Integer.class).create('p');
        assertEquals(Integer.class, option.getType());
    }

    @Test
    void withTypeObject_setsParameterType() {
        Option option = OptionBuilder.withType(Integer.class).create('q');
        assertEquals(Integer.class, option.getType());
    }

    @Test
    void withValueSeparator_setsDefaultSeparator() {
        Option option = OptionBuilder.withValueSeparator().create('r');
        assertEquals('=', option.getValueSeparator());
    }

    @Test
    void withValueSeparatorChar_setsCustomSeparator() {
        Option option = OptionBuilder.withValueSeparator(':').create('s');
        assertEquals(':', option.getValueSeparator());
    }

    @Test
    void create_resetsStateAfterCall() throws Exception {
        OptionBuilder.withLongOpt("long").hasArg().create();
        Field longOption = OptionBuilder.class.getDeclaredField("longOption");
        longOption.setAccessible(true);
        assertNull(longOption.get(null));
    }

    @Test
    void createChar_resetsStateAfterCall() {
        OptionBuilder.hasArg().create('a');
        Option option = OptionBuilder.create('b');
        assertFalse(option.hasArg());
    }

    @Test
    void createString_resetsStateAfterCall() {
        OptionBuilder.hasArg().create("test");
        Option option = OptionBuilder.create("test2");
        assertFalse(option.hasArg());
    }

    @Test
    void optionalArgWithZeroArgs_handlesCorrectly() {
        Option option = OptionBuilder.hasOptionalArgs(0).create('t');
        assertEquals(0, option.getArgs());
        assertTrue(option.hasOptionalArg());
    }

    @Test
    void resetMethod_clearsAllFields() throws Exception {
        OptionBuilder.withLongOpt("long").withDescription("desc").hasArgs().withType(Integer.class);
        Method reset = OptionBuilder.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);

        Field longOption = OptionBuilder.class.getDeclaredField("longOption");
        longOption.setAccessible(true);
        assertNull(longOption.get(null));

        Field description = OptionBuilder.class.getDeclaredField("description");
        description.setAccessible(true);
        assertNull(description.get(null));

        Field argCount = OptionBuilder.class.getDeclaredField("argCount");
        argCount.setAccessible(true);
        assertEquals(Option.UNINITIALIZED, argCount.get(null));
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<OptionBuilder> constructor = OptionBuilder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(IllegalStateException.class, constructor::newInstance);
    }
}
