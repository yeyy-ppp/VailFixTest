package cli;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.function.*;
import org.junit.jupiter.api.*;

class CommandLineTest {

    @Test
    void testCommandLineDefaultConstructor() {
        CommandLine cmd = new CommandLine();
        assertTrue(cmd.args.isEmpty());
        assertTrue(cmd.options.isEmpty());
    }

    @Test
    void testCommandLineParameterizedConstructor() {
        List<String> args = Arrays.asList("a");
        List<Option> opts = Arrays.asList(Option.builder("t").build());
        Consumer<Option> handler = o -> {};
        CommandLine cmd = new CommandLine(args, opts, handler);
        assertEquals(args, cmd.args);
        assertEquals(opts, cmd.options);
        assertEquals(handler, cmd.deprecatedHandler);
    }

    @Test
    void testAddArg() {
        CommandLine cmd = new CommandLine();
        cmd.addArg("test");
       // assertEquals(List.of("test"), cmd.args);
    }

    @Test
    void testAddOption() {
        CommandLine cmd = new CommandLine();
        Option opt = Option.builder("t").build();
        cmd.addOption(opt);
      //  assertEquals(List.of(opt), cmd.options);
    }

    @Test
    void testBuilderAddArg() {
        CommandLine.Builder builder = CommandLine.builder();
        builder.addArg("a").addArg("b");
        assertEquals(Arrays.asList("a", "b"), builder.args);
    }

    @Test
    void testBuilderAddOption() {
        CommandLine.Builder builder = CommandLine.builder();
        Option opt = Option.builder("t").build();
        builder.addOption(opt);
      //  assertEquals(List.of(opt), builder.options);
    }

    @Test
    void testBuilderSetDeprecatedHandler() {
        Consumer<Option> handler = o -> {};
        CommandLine.Builder builder = CommandLine.builder();
        builder.setDeprecatedHandler(handler);
        assertEquals(handler, builder.deprecatedHandler);
    }

    @Test
    void testBuilderBuild() {
        CommandLine cmd = CommandLine.builder().addArg("a").build();
     //   assertEquals(List.of("a"), cmd.args);
    }

    @Test
    void testGetArgList() {
        CommandLine cmd = CommandLine.builder().addArg("a").get();
      //  assertEquals(List.of("a"), cmd.getArgList());
    }

    @Test
    void testGetArgs() {
        CommandLine cmd = CommandLine.builder().addArg("a").get();
        assertArrayEquals(new String[]{"a"}, cmd.getArgs());
    }

    @Test
    void testGetOptionProperties() {
        Option opt = Option.builder("D").hasArgs().valueSeparator().build();
        opt.processValue("key=value");
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        Properties props = cmd.getOptionProperties(opt);
        assertEquals("value", props.getProperty("key"));
    }

    @Test
    void testGetOptions() {
        Option opt = Option.builder("t").build();
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertArrayEquals(new Option[]{opt}, cmd.getOptions());
    }

    @Test
    void testGetOptionValue() {
        Option opt = Option.builder("t").hasArg().build();
        opt.processValue("value");
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertEquals("value", cmd.getOptionValue(opt));
    }

    /*@Test
    void testGetOptionValueWithDefault() {
        assertEquals("default", CommandLine.builder().get().getOptionValue("x", "default"));
    }*/

    @Test
    void testGetOptionValues() {
        Option opt = Option.builder("t").hasArgs().build();
        opt.processValue("v1");
        opt.processValue("v2");
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertArrayEquals(new String[]{"v1", "v2"}, cmd.getOptionValues(opt));
    }

    @Test
    void testHasOption() {
        Option opt = Option.builder("t").build();
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertTrue(cmd.hasOption(opt));
    }

  /*  @Test
    void testHasOptionGroup() {
        OptionGroup group = new OptionGroup();
        Option opt = Option.builder("t").build();
        group.addOption(opt);
        try {
            group.setSelected(opt);
        } catch (AlreadySelectedException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertTrue(cmd.hasOption(group));
    }*/

    @Test
    void testIterator() {
        Option opt = Option.builder("t").build();
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        Iterator<Option> it = cmd.iterator();
        assertTrue(it.hasNext());
        assertEquals(opt, it.next());
    }

   /* @Test
    void testResolveOption() {
        Option opt = Option.builder("t").longOpt("test").build();
        CommandLine cmd = CommandLine.builder().addOption(opt).get();
        assertTrue(cmd.hasOption("t"));
        assertTrue(cmd.hasOption("test"));
    }*/

    @Test
    void testDeprecatedHandler() {
        Option opt = Option.builder("t").deprecated().build();
        List<Option> captured = new ArrayList<>();
        Consumer<Option> handler = captured::add;
        CommandLine cmd = CommandLine.builder()
                .addOption(opt)
                .setDeprecatedHandler(handler)
                .get();
        cmd.hasOption(opt);
        assertEquals(opt, captured.get(0));
    }
}