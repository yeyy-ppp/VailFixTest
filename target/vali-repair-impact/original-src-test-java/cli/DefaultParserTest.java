package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class DefaultParserTest {

    private cli.Options options;
    private DefaultParser parser;

    @BeforeEach
    void setUp() {
        options = new cli.Options();
        parser = new DefaultParser();
    }

    @Test
    void testBuilder() {
        DefaultParser.Builder builder = DefaultParser.builder();
        assertNotNull(builder);
    }

    @Test
    void testBuild() {
        DefaultParser.Builder builder = DefaultParser.builder();
        DefaultParser parser = builder.build();
        assertNotNull(parser);
    }

    @Test
    void testGet() {
        DefaultParser.Builder builder = DefaultParser.builder();
        DefaultParser parser = builder.get();
        assertNotNull(parser);
    }

    @Test
    void testSetAllowPartialMatching() {
        DefaultParser.Builder builder = DefaultParser.builder();
        builder.setAllowPartialMatching(false);
        DefaultParser parser = builder.get();
    }

    @Test
    void testSetDeprecatedHandler() {
        DefaultParser.Builder builder = DefaultParser.builder();
        Consumer<Option> handler = option -> {};
        builder.setDeprecatedHandler(handler);
        DefaultParser parser = builder.get();
    }

    @Test
    void testSetStripLeadingAndTrailingQuotes() {
        DefaultParser.Builder builder = DefaultParser.builder();
        builder.setStripLeadingAndTrailingQuotes(true);
        DefaultParser parser = builder.get();
    }

    @Test
    void testIndexOfEqual() {
        int index = DefaultParser.indexOfEqual("key=value");
        assertEquals(3, index);
    }

    @Test
    void testDefaultParserConstructor() {
        DefaultParser parser = new DefaultParser();
    }

    @Test
    void testCheckRequiredArgsNoException() throws ParseException {
        Option option = Option.builder("a").build();
        parser.currentOption = option;
        parser.checkRequiredArgs();
    }

    /*@Test
    void testCheckRequiredArgsThrowsException() throws ParseException {
        Option option = Option.builder("a").hasArg(true).build();
        parser.currentOption = option;
        assertThrows(MissingArgumentException.class, () -> parser.checkRequiredArgs());
    }*/

    @Test
    void testGetMatchingLongOptionsPartialDisabled() {
        parser = new DefaultParser(false);
        options.addOption(Option.builder().longOpt("long").build());
        parser.options = options;
        List<String> matches = parser.getMatchingLongOptions("long");
        assertEquals(1, matches.size());
    }

    /*@Test
    void testHandleOption() throws ParseException {
        Option option = Option.builder("a").build();
        parser.handleOption(option);
        assertNull(parser.currentOption);
    }*/

   /* @Test
    void testHandleProperties() throws ParseException {
        options.addOption(Option.builder("prop").build());
        parser.options = options;
        Properties props = new Properties();
        props.setProperty("prop", "true");
        parser.handleProperties(props);
        assertNull(parser.currentOption);
    }
*/
   /* @Test
    void testHandleShortAndLongOptionSingleChar() throws ParseException {
        options.addOption(Option.builder("a").build());
        parser.options = options;
        parser.handleShortAndLongOption("-a");
        assertNull(parser.currentOption);
    }*/

    /*@Test
    void testHandleTokenShortOption() throws ParseException {
        options.addOption(Option.builder("a").build());
        parser.options = options;
        parser.handleToken("-a");
        assertNull(parser.currentOption);
    }
*/
  /*  @Test
    void testIsJavaProperty() {
        Option option = Option.builder("p").numberOfArgs(2).build();
        options.addOption(option);
        parser.options = options;
        assertTrue(parser.isJavaProperty("-pkey=value"));
    }*/

    @Test
    void testIsLongOption() {
        options.addOption(Option.builder().longOpt("long").build());
        parser.options = options;
        assertTrue(parser.isLongOption("--long"));
    }

    @Test
    void testIsNegativeNumber() {
        assertTrue(parser.isNegativeNumber("-123"));
    }

    @Test
    void testIsShortOption() {
        options.addOption(Option.builder("a").build());
        parser.options = options;
        assertTrue(parser.isShortOption("-a"));
    }

    @Test
    void testParseBasic() throws ParseException {
        options.addOption(Option.builder("a").build());
        String[] args = {"-a"};
        CommandLine cmd = parser.parse(options, args);
        assertTrue(cmd.hasOption("a"));
    }

    @Test
    void testParseWithStopAtNonOption() throws ParseException {
        options.addOption(Option.builder("a").build());
        String[] args = {"-a", "nonOption"};
        CommandLine cmd = parser.parse(options, args, true);
        assertTrue(cmd.hasOption("a"));
        assertEquals(1, cmd.getArgList().size());
    }

    @Test
    void testUpdateRequiredOptions() throws ParseException {
        Option option = Option.builder("a").required(true).build();
        options.addOption(option);
        parser.options = options;
        parser.expectedOpts = new ArrayList<>();
        parser.expectedOpts.add("a");
        parser.updateRequiredOptions(option);
        assertTrue(parser.expectedOpts.isEmpty());
    }
}