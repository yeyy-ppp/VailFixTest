package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class OptionsTest {

    private Options options;
    private Option option1;
    private Option option2;
    private OptionGroup group;

    @BeforeEach
    void setUp() {
        options = new Options();
        option1 = new Option("a", "alpha", false, "Description A");
        option2 = new Option("b", "beta", true, "Description B");
        group = new OptionGroup();
        group.addOption(new Option("g1", "group1", false, "Group option 1"));
        group.addOption(new Option("g2", "group2", false, "Group option 2"));
    }

    @Test
    void testConstructor() {
        Options opts = new Options();
        assertNotNull(opts);
        assertEquals(0, opts.getOptions().size());
    }

    @Test
    void testAddOptionWithObject() {
        Options result = options.addOption(option1);
        assertSame(options, result);
        assertTrue(options.hasOption("a"));
        assertTrue(options.hasLongOption("alpha"));
    }

    @Test
    void testAddOptionWithShortOpt() {
        Options result = options.addOption("c", "Description C");
        assertSame(options, result);
        Option opt = options.getOption("c");
        assertNotNull(opt);
        assertFalse(opt.hasArg());
        assertEquals("Description C", opt.getDescription());
    }

    @Test
    void testAddOptionWithShortOptAndArgs() {
        Options result = options.addOption("d", true, "Description D");
        assertSame(options, result);
        Option opt = options.getOption("d");
        assertNotNull(opt);
        assertTrue(opt.hasArg());
    }

    @Test
    void testAddFullOption() {
        Options result = options.addOption("e", "epsilon", true, "Description E");
        assertSame(options, result);
        assertTrue(options.hasOption("e"));
        assertTrue(options.hasLongOption("epsilon"));
    }

    @Test
    void testAddOptionGroup() {
        group.setRequired(true);
        Options result = options.addOptionGroup(group);
        assertSame(options, result);
        Collection<OptionGroup> groups = options.getOptionGroups();
        assertEquals(1, groups.size());
        assertTrue(options.getRequiredOptions().contains(group));
    }

    @Test
    void testAddOptions() {
        Options other = new Options();
        other.addOption("x", "Description X");
        Options result = options.addOptions(other);
        assertSame(options, result);
        assertTrue(options.hasOption("x"));
    }

    @Test
    void testAddOptionsWithDuplicateThrowsException() {
        options.addOption("y", "Description Y");
        Options other = new Options();
        other.addOption("y", "Description Y");
        assertThrows(IllegalArgumentException.class, () -> options.addOptions(other));
    }

    @Test
    void testAddRequiredOption() {
        Options result = options.addRequiredOption("r", "required", true, "Required option");
        assertSame(options, result);
        assertTrue(options.getOption("r").isRequired());
        assertTrue(options.getRequiredOptions().contains("r"));
    }

    @Test
    void testGetMatchingOptions() {
        options.addOption("f", "first", false, "First");
        options.addOption("s", "second", false, "Second");
        List<String> matches = options.getMatchingOptions("fir");
        assertEquals(1, matches.size());
        assertEquals("first", matches.get(0));
    }

    @Test
    void testGetMatchingOptionsWithExactMatch() {
        options.addOption("f", "first", false, "First");
        List<String> matches = options.getMatchingOptions("first");
        assertEquals(1, matches.size());
        assertEquals("first", matches.get(0));
    }

    @Test
    void testGetMatchingOptionsWithNoMatch() {
        options.addOption("f", "first", false, "First");
        List<String> matches = options.getMatchingOptions("xyz");
        assertTrue(matches.isEmpty());
    }

    @Test
    void testGetOption() {
        options.addOption(option1);
        Option retrieved = options.getOption("a");
        assertSame(option1, retrieved);
    }

    @Test
    void testGetOptionByLongOpt() {
        options.addOption(option1);
        Option retrieved = options.getOption("alpha");
        assertSame(option1, retrieved);
    }

    @Test
    void testGetOptionReturnsNull() {
        assertNull(options.getOption("nonexistent"));
    }

    @Test
    void testGetOptionGroup() {
        options.addOptionGroup(group);
        Option groupOption = group.getOptions().iterator().next();
        OptionGroup retrievedGroup = options.getOptionGroup(groupOption);
        assertSame(group, retrievedGroup);
    }

    @Test
    void testGetOptionGroupReturnsNull() {
        options.addOption(option1);
        assertNull(options.getOptionGroup(option1));
    }

    @Test
    void testGetOptionGroups() {
        options.addOptionGroup(group);
        Collection<OptionGroup> groups = options.getOptionGroups();
        assertEquals(1, groups.size());
        assertTrue(groups.contains(group));
    }

    @Test
    void testGetOptions() {
        options.addOption(option1);
        options.addOption(option2);
        Collection<Option> opts = options.getOptions();
        assertEquals(2, opts.size());
        assertThrows(UnsupportedOperationException.class, () -> opts.add(new Option("z", "zeta", false, "Description Z")));
    }

    @Test
    void testGetRequiredOptions() {
        options.addRequiredOption("r", "required", true, "Required option");
        List<?> required = options.getRequiredOptions();
        assertEquals(1, required.size());
        assertTrue(required.contains("r"));
        assertThrows(UnsupportedOperationException.class, () -> ((List) required).add("new"));
    }

    @Test
    void testHasLongOption() {
        options.addOption(option1);
        assertTrue(options.hasLongOption("alpha"));
        assertFalse(options.hasLongOption("nonexistent"));
    }

    @Test
    void testHasOption() {
        options.addOption(option1);
        assertTrue(options.hasOption("a"));
        assertTrue(options.hasOption("alpha"));
        assertFalse(options.hasOption("nonexistent"));
    }

    @Test
    void testHasShortOption() {
        options.addOption(option1);
        assertTrue(options.hasShortOption("a"));
        assertFalse(options.hasShortOption("alpha"));
    }

    @Test
    void testHelpOptions() {
        options.addOption(option1);
        options.addOption(option2);
        List<Option> helpOpts = options.helpOptions();
        assertEquals(2, helpOpts.size());
        assertTrue(helpOpts.contains(option1));
    }

    @Test
    void testToString() {
        options.addOption(option1);
        String str = options.toString();
        assertTrue(str.contains("a"));
        assertTrue(str.contains("alpha"));
    }
}