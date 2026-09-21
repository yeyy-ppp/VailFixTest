package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class CommandLineParserTest {
    private CommandLineParser parser;
    private Options options;

    @BeforeEach
    void setUp() {
        parser = new DefaultParser();
        options = new Options();
    }

    @Test
    void parse_WithRequiredOptionPresent() throws Exception {
        options.addOption(Option.builder("f").required().hasArg().build());
        String[] args = {"-f", "file.txt"};
        CommandLine cmd = parser.parse(options, args);
        assertTrue(cmd.hasOption("f"));
        assertEquals("file.txt", cmd.getOptionValue("f"));
    }

    @Test
    void parse_ThrowsWhenRequiredOptionMissing() {
        options.addOption(Option.builder("f").required().hasArg().build());
        String[] args = {};
        assertThrows(ParseException.class, () -> parser.parse(options, args));
    }

    @Test
    void parse_WithOptionAndArgument() throws Exception {
        options.addOption(Option.builder("d").hasArg().build());
        String[] args = {"-d", "debug"};
        CommandLine cmd = parser.parse(options, args);
        assertEquals("debug", cmd.getOptionValue("d"));
    }

    @Test
    void parse_WithUnrecognizedOption() {
        options.addOption(Option.builder("v").build());
        String[] args = {"-x"};
        assertThrows(ParseException.class, () -> parser.parse(options, args));
    }

    @Test
    void parse_WithStopAtNonOptionTrue() throws Exception {
        options.addOption(Option.builder("a").build());
        String[] args = {"-a", "nonOption", "-b"};
        CommandLine cmd = parser.parse(options, args, true);
        assertTrue(cmd.hasOption("a"));
        assertArrayEquals(new String[]{"nonOption", "-b"}, cmd.getArgs());
    }

    @Test
    void parse_WithStopAtNonOptionFalse() throws Exception {
        options.addOption(Option.builder("a").build());
        String[] args = {"-a", "nonOption", "-b"};
        assertThrows(ParseException.class, () -> parser.parse(options, args, false));
    }

    @Test
    void parse_WithEmptyArguments() throws Exception {
        options.addOption(Option.builder("v").build());
        CommandLine cmd = parser.parse(options, new String[]{});
        assertFalse(cmd.hasOption("v"));
    }

    @Test
    void parse_WithMultipleOptions() throws Exception {
        options.addOption(Option.builder("a").build());
        options.addOption(Option.builder("b").hasArg().build());
        String[] args = {"-a", "-b", "value"};
        CommandLine cmd = parser.parse(options, args);
        assertTrue(cmd.hasOption("a"));
        assertEquals("value", cmd.getOptionValue("b"));
    }

    @Test
    void parse_WithLongOption() throws Exception {
        options.addOption(Option.builder().longOpt("file").hasArg().build());
        String[] args = {"--file", "data.txt"};
        CommandLine cmd = parser.parse(options, args);
        assertEquals("data.txt", cmd.getOptionValue("file"));
    }
}