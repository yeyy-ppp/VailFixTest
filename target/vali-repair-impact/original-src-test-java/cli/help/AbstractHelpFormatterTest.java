package cli.help;

import cli.Option;
import cli.OptionGroup;
import cli.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractHelpFormatterTest {

    @Test
    void testBuilderConstructor() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        assertNotNull(builder);
    }

    @Test
    void testBuilderAsThis() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        assertSame(builder, builder.asThis());
    }

    @Test
    void testBuilderSetComparator() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        Comparator<Option> customComparator = (o1, o2) -> 0;
        builder.setComparator(customComparator);
        assertSame(customComparator, builder.getComparator());
    }

    @Test
    void testBuilderSetHelpAppendable() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        HelpAppendable customAppendable = new TextHelpAppendable(new StringBuilder());
        builder.setHelpAppendable(customAppendable);
        assertSame(customAppendable, builder.getHelpAppendable());
    }

    @Test
    void testBuilderSetHelpAppendableNull() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        builder.setHelpAppendable(null);
        assertSame(TextHelpAppendable.systemOut().getClass(), builder.getHelpAppendable().getClass());
    }

    @Test
    void testBuilderSetOptionFormatBuilder() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        OptionFormatter.Builder customBuilder = OptionFormatter.builder();
        builder.setOptionFormatBuilder(customBuilder);
        assertSame(customBuilder, builder.getOptionFormatBuilder());
    }

    @Test
    void testBuilderSetOptionFormatBuilderNull() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        builder.setOptionFormatBuilder(null);
        assertNotNull(builder.getOptionFormatBuilder());
    }

    @Test
    void testBuilderSetOptionGroupSeparator() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        String separator = "##";
        builder.setOptionGroupSeparator(separator);
        assertEquals(separator, builder.getOptionGroupSeparator());
    }

    @Test
    void testBuilderSetOptionGroupSeparatorNull() {
        AbstractHelpFormatter.Builder<?, ?> builder = createDefaultBuilder();
        builder.setOptionGroupSeparator(null);
        assertEquals("", builder.getOptionGroupSeparator());
    }

    @Test
    void testAbstractHelpFormatterConstructor() {
        AbstractHelpFormatter formatter = createFormatter();
        assertNotNull(formatter);
    }

    @Test
    void testGetComparator() {
        AbstractHelpFormatter formatter = createFormatter();
        assertNotNull(formatter.getComparator());
    }

    @Test
    void testGetHelpAppendable() {
        AbstractHelpFormatter formatter = createFormatter();
        assertNotNull(formatter.getHelpAppendable());
    }

    @Test
    void testGetOptionFormatBuilder() {
        AbstractHelpFormatter formatter = createFormatter();
        assertNotNull(formatter.getOptionFormatBuilder());
    }

    @Test
    void testGetOptionFormatter() {
        AbstractHelpFormatter formatter = createFormatter();
        Option option = createOption("a");
        assertNotNull(formatter.getOptionFormatter(option));
    }

  /*  @Test
    void testGetOptionGroupSeparator() {
        AbstractHelpFormatter formatter = createFormatter();
        assertEquals("", formatter.getOptionGroupSeparator());
    }*/

    @Test
    void testGetSerializer() {
        AbstractHelpFormatter formatter = createFormatter();
        assertSame(formatter.getHelpAppendable(), formatter.getSerializer());
    }

    @Test
    void testGetSetSyntaxPrefix() {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.setSyntaxPrefix("test:");
        assertEquals("test:", formatter.getSyntaxPrefix());
    }

   /* @Test
    void testSetSyntaxPrefixNull() {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.setSyntaxPrefix(null);
        assertEquals("", formatter.getSyntaxPrefix());
    }*/

    @Test
    void testSortIterable() {
        AbstractHelpFormatter formatter = createDefaultBuilder()
                .setComparator((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                .get();
        List<Option> options = Arrays.asList(createOption("b"), createOption("a"));
        List<Option> sorted = formatter.sort(options);
        assertEquals("a", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
    }

    @Test
    void testSortIterableNull() {
        AbstractHelpFormatter formatter = createFormatter();
        List<Option> result = formatter.sort((Iterable<Option>) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSortOptions() {
        AbstractHelpFormatter formatter = createFormatter();
        Options options = new Options();
        options.addOption(createOption("b"));
        options.addOption(createOption("a"));
        List<Option> sorted = formatter.sort(options);
        assertEquals(2, sorted.size());
    }

    @Test
    void testSortOptionsNull() {
        AbstractHelpFormatter formatter = createFormatter();
        List<Option> result = formatter.sort((Options) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testToArgName() {
        AbstractHelpFormatter formatter = createFormatter();
        String result = formatter.toArgName("arg");
        assertNotNull(result);
    }

    @Test
    void testToSyntaxOptionsIterableEmpty() {
        AbstractHelpFormatter formatter = createFormatter();
        String result = formatter.toSyntaxOptions(new ArrayList<>());
        assertEquals("", result);
    }

    @Test
    void testToSyntaxOptionsWithLookup() {
        AbstractHelpFormatter formatter = createFormatter();
        Option option = createOption("a");
        List<Option> options = Arrays.asList(option);
        String result = formatter.toSyntaxOptions(options, o -> null);
        assertFalse(result.isEmpty());
    }

    @Test
    void testToSyntaxOptionsOptionGroupEmpty() {
        AbstractHelpFormatter formatter = createFormatter();
        OptionGroup group = createOptionGroup();
        String result = formatter.toSyntaxOptions(group);
        assertEquals("", result);
    }

    @Test
    void testToSyntaxOptionsOptionGroupSingle() {
        AbstractHelpFormatter formatter = createFormatter();
        OptionGroup group = createOptionGroup(createOption("a"));
        group.setRequired(true);
        String result = formatter.toSyntaxOptions(group);
        assertFalse(result.isEmpty());
    }

    @Test
    void testToSyntaxOptionsOptionGroupMultiple() {
        AbstractHelpFormatter formatter = createFormatter();
        OptionGroup group = createOptionGroup(createOption("a"), createOption("b"));
        String result = formatter.toSyntaxOptions(group);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    void testToSyntaxOptionsOptions() {
        AbstractHelpFormatter formatter = createFormatter();
        Options options = new Options();
        options.addOption(createOption("a"));
        String result = formatter.toSyntaxOptions(options);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPrintHelpCmdLineSyntaxEmpty() {
        AbstractHelpFormatter formatter = createFormatter();
        Executable action = () -> formatter.printHelp("", "header", new ArrayList<>(), "footer", true);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void testPrintHelpAutoUsage() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.printHelp("cmd", "header", new ArrayList<>(), "footer", true);
    }

    @Test
    void testPrintHelpNoAutoUsage() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.printHelp("cmd", "header", new ArrayList<>(), "footer", false);
    }

    @Test
    void testPrintHelpWithOptions() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        Options options = new Options();
        formatter.printHelp("cmd", "header", options, "footer", true);
    }

    @Test
    void testPrintOptionsIterable() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.printOptions(new ArrayList<>());
    }

    @Test
    void testPrintOptionsOptions() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.printOptions(new Options());
    }

  /*  @Test
    void testPrintOptionsTableDefinition() throws IOException {
        AbstractHelpFormatter formatter = createFormatter();
        formatter.printOptions((TableDefinition) null);
    }*/

    private AbstractHelpFormatter.Builder<?, ?> createDefaultBuilder() {
        return HelpFormatter.builder();
    }

    private AbstractHelpFormatter createFormatter() {
        return createDefaultBuilder().get();
    }

    private Option createOption(String opt) {
        return Option.builder(opt).build();
    }

    private OptionGroup createOptionGroup(Option... options) {
        OptionGroup group = new OptionGroup();
        for (Option option : options) {
            group.addOption(option);
        }
        return group;
    }
}