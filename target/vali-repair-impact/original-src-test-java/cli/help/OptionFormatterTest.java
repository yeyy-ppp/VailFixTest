package cli.help;

import cli.Option;
import org.junit.jupiter.api.Test;
import java.util.function.BiFunction;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class OptionFormatterTest {

    @Test
    void testOptionFormatterConstructor() {
        Option option = Option.builder("t").longOpt("test").build();
        OptionFormatter.Builder builder = OptionFormatter.builder();
        OptionFormatter formatter = builder.build(option);
        assertNotNull(formatter);
    }

    @Test
    void testGetArgName() {
        Option option = Option.builder("t").longOpt("test").build();
        option.setArgs(Option.UNLIMITED_VALUES);
        option.setArgName("file");
        OptionFormatter formatter = OptionFormatter.from(option);
        assertEquals("<file>", formatter.getArgName());

        option.setArgs(0);
        assertEquals("", formatter.getArgName());

        option.setArgs(Option.UNLIMITED_VALUES);
        option.setArgName(null);
        assertEquals("<arg>", formatter.getArgName());
    }

    @Test
    void testGetBothOpt() {
        Option option1 = Option.builder("t").longOpt("test").build();
        OptionFormatter formatter1 = OptionFormatter.from(option1);
        assertEquals("-t, --test", formatter1.getBothOpt());

        Option option2 = Option.builder().longOpt("test").build();
        OptionFormatter formatter2 = OptionFormatter.from(option2);
        assertEquals("--test", formatter2.getBothOpt());

        Option option3 = Option.builder("t").build();
        OptionFormatter formatter3 = OptionFormatter.from(option3);
        assertEquals("-t", formatter3.getBothOpt());
    }

    @Test
    void testGetDescription() {
        Option option = Option.builder("t")
                .longOpt("test")
                .desc("description")
                .deprecated()
                .build();
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setDeprecatedFormatFunction(OptionFormatter.SIMPLE_DEPRECATED_FORMAT);
        OptionFormatter formatter = builder.build(option);
        assertEquals("[Deprecated] description", formatter.getDescription());
    }

    @Test
    void testGetLongOpt() {
        Option option1 = Option.builder("t").longOpt("test").build();
        OptionFormatter formatter1 = OptionFormatter.from(option1);
        assertEquals("--test", formatter1.getLongOpt());

        Option option2 = Option.builder("t").build();
        OptionFormatter formatter2 = OptionFormatter.from(option2);
        assertEquals("", formatter2.getLongOpt());
    }

    @Test
    void testGetOpt() {
        Option option1 = Option.builder("t").longOpt("test").build();
        OptionFormatter formatter1 = OptionFormatter.from(option1);
        assertEquals("-t", formatter1.getOpt());

        Option option2 = Option.builder().longOpt("test").build();
        OptionFormatter formatter2 = OptionFormatter.from(option2);
        assertEquals("", formatter2.getOpt());
    }

    /*@Test
    void testGetSince() {
        Option option = Option.builder("t").longOpt("test").build();
        option.setSince("1.0");
        OptionFormatter formatter = OptionFormatter.from(option);
        assertEquals("1.0", formatter.getSince());

        option.setSince(null);
        assertEquals("--", formatter.getSince());
    }*/

    @Test
    void testIsRequired() {
        Option option = Option.builder("t").longOpt("test").build();
        option.setRequired(true);
        OptionFormatter formatter = OptionFormatter.from(option);
        assertTrue(formatter.isRequired());

        option.setRequired(false);
        assertFalse(formatter.isRequired());
    }

    @Test
    void testToOptional() {
        OptionFormatter formatter = OptionFormatter.builder().build(Option.builder("t").longOpt("test").build());
        assertEquals("[text]", formatter.toOptional("text"));

        assertEquals("", formatter.toOptional(""));
        assertEquals("", formatter.toOptional(null));
    }

   /* @Test
    void testToSyntaxOptionDefault() {
        Option option = Option.builder("t").longOpt("test").build();
        option.setRequired(true);
        OptionFormatter formatter = OptionFormatter.from(option);
        assertEquals("-t", formatter.toSyntaxOption());

        option.setRequired(false);
        assertEquals("[-t]", formatter.toSyntaxOption());

        option.setArgs(Option.UNLIMITED_VALUES);
        option.setRequired(true);
        assertEquals("-t<arg>", formatter.toSyntaxOption());
    }*/

    @Test
    void testToSyntaxOptionWithParam() {
        Option option = Option.builder("t").longOpt("test").build();
        OptionFormatter formatter = OptionFormatter.from(option);
        assertEquals("[-t]", formatter.toSyntaxOption(false));
        assertEquals("-t", formatter.toSyntaxOption(true));
    }
}

class OptionFormatterBuilderTest {

    @Test
    void testBuilderConstructor() {
        OptionFormatter.Builder builder = OptionFormatter.builder();
        assertNotNull(builder);
    }

    @Test
    void testBuilderConstructorWithFormatter() {
        OptionFormatter formatter = OptionFormatter.from(Option.builder("t").longOpt("test").build());
        OptionFormatter.Builder builder = new OptionFormatter.Builder(formatter);
        assertNotNull(builder);
    }

    @Test
    void testBuild() {
        OptionFormatter.Builder builder = OptionFormatter.builder();
        Option option = Option.builder("t").longOpt("test").build();
        OptionFormatter formatter = builder.build(option);
        assertNotNull(formatter);
    }

    @Test
    void testGet() {
        OptionFormatter.Builder builder = OptionFormatter.builder();
        assertNull(builder.get());
    }

    @Test
    void testSetArgumentNameDelimiters() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setArgumentNameDelimiters("{", "}");
        Option option = Option.builder("t").longOpt("test").build();
        option.setArgs(Option.UNLIMITED_VALUES);
        OptionFormatter formatter = builder.build(option);
        assertEquals("{arg}", formatter.getArgName());
    }

    @Test
    void testSetDefaultArgName() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setDefaultArgName("value");
        Option option = Option.builder("t").longOpt("test").build();
        option.setArgs(Option.UNLIMITED_VALUES);
        OptionFormatter formatter = builder.build(option);
        assertEquals("<value>", formatter.getArgName());
    }

    @Test
    void testSetDeprecatedFormatFunction() {
        Function<Option, String> customDeprecated = o -> "Custom: " + o.getDescription();
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setDeprecatedFormatFunction(customDeprecated);
        Option option = Option.builder("t")
                .longOpt("test")
                .desc("desc")
                .deprecated()
                .build();
        OptionFormatter formatter = builder.build(option);
        assertEquals("Custom: desc", formatter.getDescription());
    }

    @Test
    void testSetLongOptPrefix() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setLongOptPrefix("++");
        OptionFormatter formatter = builder.build(Option.builder("t").longOpt("test").build());
        assertEquals("++test", formatter.getLongOpt());
    }

   /* @Test
    void testSetOptArgSeparator() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setOptArgSeparator("=");
        Option option = Option.builder("t").longOpt("test").build();
        option.setArgs(Option.UNLIMITED_VALUES);
        OptionFormatter formatter = builder.build(option);
        assertEquals("-t=<arg>", formatter.toSyntaxOption());
    }*/

    @Test
    void testSetOptionalDelimiters() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setOptionalDelimiters("(", ")");
        OptionFormatter formatter = builder.build(Option.builder("t").longOpt("test").build());
        assertEquals("(text)", formatter.toOptional("text"));
    }

    @Test
    void testSetOptPrefix() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setOptPrefix("+");
        OptionFormatter formatter = builder.build(Option.builder("t").longOpt("test").build());
        assertEquals("+t", formatter.getOpt());
    }

    @Test
    void testSetOptSeparator() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setOptSeparator(" | ");
        OptionFormatter formatter = builder.build(Option.builder("t").longOpt("test").build());
        assertEquals("-t | --test", formatter.getBothOpt());
    }

    @Test
    void testSetSyntaxFormatFunction() {
        BiFunction<OptionFormatter, Boolean, String> customSyntax = (o, req) -> "custom:" + o.getOpt();
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setSyntaxFormatFunction(customSyntax);
        OptionFormatter formatter = builder.build(Option.builder("t").longOpt("test").build());
        assertEquals("custom:-t", formatter.toSyntaxOption());
    }

    @Test
    void testToArgName() {
        OptionFormatter.Builder builder = OptionFormatter.builder()
                .setArgumentNameDelimiters("[", "]");
        assertEquals("[name]", builder.toArgName("name"));
        assertEquals("[]", builder.toArgName(null));
    }
}