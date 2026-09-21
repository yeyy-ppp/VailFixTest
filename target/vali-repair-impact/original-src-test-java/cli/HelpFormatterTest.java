package cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class HelpFormatterTest {

    @Test
    void testHelpFormatterConstructor() {
        PrintWriter pw = new PrintWriter(System.out);
        Function<Option, String> func = o -> "Deprecated: " + o.getDescription();
        HelpFormatter formatter = HelpFormatter.builder()
                .setShowDeprecated(func)
                .setShowSince(true)
                .get();
        assertNotNull(formatter);
    }

    @Test
    void testCreatePadding() {
        HelpFormatter formatter = new HelpFormatter();
        String padding = formatter.createPadding(3);
        assertEquals("   ", padding);
    }

    /*@Test
    void testFindWrapPos() {
        HelpFormatter formatter = new HelpFormatter();
        int pos = formatter.findWrapPos("text with space", 10, 0);
        assertEquals(4, pos);
    }*/

    @Test
    void testFindWrapPosNoSpace() {
        HelpFormatter formatter = new HelpFormatter();
        int pos = formatter.findWrapPos("longtextwithoutspace", 5, 0);
        assertEquals(5, pos);
    }

    @Test
    void testGetSetProperties() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setArgName("argument");
        assertEquals("argument", formatter.getArgName());

        formatter.setDescPadding(5);
        assertEquals(5, formatter.getDescPadding());

        formatter.setLeftPadding(2);
        assertEquals(2, formatter.getLeftPadding());

        formatter.setLongOptPrefix("++");
        assertEquals("++", formatter.getLongOptPrefix());

        formatter.setLongOptSeparator("=");
        assertEquals("=", formatter.getLongOptSeparator());

        formatter.setNewLine("\r\n");
        assertEquals("\r\n", formatter.getNewLine());

        Comparator<Option> comp = (o1, o2) -> 0;
        formatter.setOptionComparator(comp);
        assertEquals(comp, formatter.getOptionComparator());

        formatter.setOptPrefix("+");
        assertEquals("+", formatter.getOptPrefix());

        formatter.setSyntaxPrefix("usage: ");
        assertEquals("usage: ", formatter.getSyntaxPrefix());

        formatter.setWidth(100);
        assertEquals(100, formatter.getWidth());
    }

    @Test
    void testPrintHelpBasic() {
        Options options = new Options();
        options.addOption(Option.builder("a").desc("Option A").build());
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, 80, "app", "header", options, 1, 3, "footer", true);
        assertTrue(sw.toString().contains("header"));
    }

    @Test
    void testPrintHelpWithAutoUsage() {
        Options options = new Options();
        options.addOption(Option.builder("a").build());
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, 80, "app", "header", options, 1, 3, "footer", true);
        assertTrue(sw.toString().contains("usage: app"));
    }

    @Test
    void testPrintOptions() {
        Options options = new Options();
        options.addOption(Option.builder("a").desc("description").build());
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printOptions(pw, 80, options, 1, 3);
        assertTrue(sw.toString().contains("-a"));
    }

    @Test
    void testPrintUsageWithoutOptions() {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printUsage(pw, 80, "app -f file");
        assertTrue(sw.toString().contains("usage: app -f file"));
    }

    @Test
    void testPrintUsageWithOptions() {
        Options options = new Options();
        options.addOption(Option.builder("f").build());
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printUsage(pw, 80, "app", options);
        assertTrue(sw.toString().contains("[-f]"));
    }

    @Test
    void testPrintWrappedText() {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printWrapped(pw, 10, 0, "This is a long text");
        assertTrue(sw.toString().contains("This is a"));
    }

    @Test
    void testRtrim() {
        HelpFormatter formatter = new HelpFormatter();
        String result = formatter.rtrim("text  ");
        assertEquals("text", result);
    }

    @Test
    void testRtrimEmpty() {
        HelpFormatter formatter = new HelpFormatter();
        String result = formatter.rtrim("");
        assertEquals("", result);
    }
}

class BuilderTest {

    @Test
    void testBuilderConstructor() {
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        assertNotNull(builder);
    }

    @Test
    void testBuilderGet() {
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        HelpFormatter formatter = builder.get();
        assertNotNull(formatter);
    }

    @Test
    void testSetPrintWriter() {
        PrintWriter pw = new PrintWriter(System.out);
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        builder.setPrintWriter(pw);
        HelpFormatter formatter = builder.get();
        assertNotNull(formatter);
    }

    @Test
    void testSetShowDeprecatedBoolean() {
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        builder.setShowDeprecated(true);
        HelpFormatter formatter = builder.get();
        assertNotNull(formatter);
    }

    @Test
    void testSetShowDeprecatedFunction() {
        Function<Option, String> func = o -> "Custom: " + o.getDescription();
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        builder.setShowDeprecated(func);
        HelpFormatter formatter = builder.get();
        assertNotNull(formatter);
    }

    @Test
    void testSetShowSince() {
        HelpFormatter.Builder builder = new HelpFormatter.Builder();
        builder.setShowSince(true);
        HelpFormatter formatter = builder.get();
        assertNotNull(formatter);
    }
}