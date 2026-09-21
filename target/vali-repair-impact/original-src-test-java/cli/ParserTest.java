package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Properties;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Collections;

class TestParser extends Parser {
    public TestParser() {
        cmd = CommandLine.builder().get();
    }
    
    @Override
    protected String[] flatten(Options opts, String[] arguments, boolean stopAtNonOption) {
        return arguments;
    }
    
    CommandLine getCommandLine() {
        return cmd;
    }
}

public class ParserTest {

    @Test
    void testCheckRequiredOptionsThrowsWhenRequiredMissing() {
        Parser parser = new TestParser();
        Options options = new Options();
        options.addRequiredOption("a", "aaa", false, "desc");
        parser.setOptions(options);
        assertThrows(MissingOptionException.class, () -> parser.checkRequiredOptions());
    }

    @Test
    void testCheckRequiredOptionsNoThrowWhenRequiredMet() throws ParseException {
        Parser parser = new TestParser();
        Options options = new Options();
        Option opt = new Option("a", "aaa", false, "desc");
        opt.setRequired(true);
        options.addOption(opt);
        parser.setOptions(options);
        parser.parse(options, new String[]{"-a"});
        assertDoesNotThrow(() -> parser.checkRequiredOptions());
    }

    @Test
    void testParseWithMissingRequiredOptionThrowsException() {
        Parser parser = new TestParser();
        Options options = new Options();
        options.addRequiredOption("a", "aaa", false, "desc");
        assertThrows(MissingOptionException.class, () -> parser.parse(options, new String[0]));
    }

    @Test
    void testParseWithValidRequiredOptionSucceeds() throws Exception {
        Parser parser = new TestParser();
        Options options = new Options();
        options.addRequiredOption("a", "aaa", false, "desc");
        CommandLine cmd = parser.parse(options, new String[]{"-a"});
        assertTrue(cmd.hasOption("a"));
    }

    @Test
    void testProcessOptionUnrecognizedThrowsException() throws ParseException {
        Parser parser = new TestParser();
        parser.setOptions(new Options());
        assertThrows(UnrecognizedOptionException.class, () -> parser.processOption("-b", Collections.<String>emptyList().listIterator()));
    }

    @Test
    void testProcessOptionAddsValidOption() throws ParseException {
        TestParser parser = new TestParser();
        Options options = new Options();
        options.addOption("a", false, "desc");
        parser.setOptions(options);
        parser.processOption("-a", Collections.<String>emptyList().listIterator());
        assertTrue(parser.getCommandLine().hasOption("a"));
    }

    @Test
    void testProcessPropertiesAddsDefaultOption() throws ParseException {
        TestParser parser = new TestParser();
        Options options = new Options();
        options.addOption("a", false, "desc");
        parser.setOptions(options);
        Properties props = new Properties();
        props.setProperty("a", "true");
        parser.processProperties(props);
        assertTrue(parser.getCommandLine().hasOption("a"));
    }

 /*   @Test
    void testProcessArgsHandlesValuesCorrectly() throws ParseException {
        TestParser parser = new TestParser();
        Option opt = new Option("a", true, "desc");
        ListIterator<String> iter = Arrays.asList("value1", "value2").listIterator();
        parser.processArgs(opt, iter);
        assertEquals("value1", opt.getValue());
    }*/

    @Test
    void testParseWithStopAtNonOptionStopsProcessing() throws Exception {
        TestParser parser = new TestParser();
        Options options = new Options();
        options.addOption("a", false, "desc");
        CommandLine cmd = parser.parse(options, new String[]{"-a", "nonOption"}, true);
        assertTrue(cmd.hasOption("a"));
        assertEquals(1, cmd.getArgList().size());
    }

    @Test
    void testParseWithPropertiesAppliesDefaults() throws Exception {
        TestParser parser = new TestParser();
        Options options = new Options();
        options.addOption("a", false, "desc");
        Properties props = new Properties();
        props.setProperty("a", "true");
        CommandLine cmd = parser.parse(options, new String[]{}, props);
        assertTrue(cmd.hasOption("a"));
    }

    @Test
    void testGetOptions() {
        Parser parser = new TestParser();
        Options options = new Options();
        parser.setOptions(options);
        assertEquals(options, parser.getOptions());
    }

    @Test
    void testGetRequiredOptions() {
        Parser parser = new TestParser();
        Options options = new Options();
        Option opt = new Option("a", "aaa", false, "desc");
        opt.setRequired(true);
        options.addOption(opt);
        parser.setOptions(options);
        List requiredOptions = parser.getRequiredOptions();
        assertTrue(requiredOptions.contains(opt.getKey()));
    }
}