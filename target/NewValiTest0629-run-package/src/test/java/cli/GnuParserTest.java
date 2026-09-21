package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GnuParserTest {

    @Test
    public void testFlatten_EmptyArguments() {
        GnuParser parser = new GnuParser();
        Options options = new Options();
        String[] result = parser.flatten(options, new String[]{}, false);
        assertArrayEquals(new String[]{}, result);
    }

    @Test
    public void testFlatten_NullArgumentsSkipped() {
        GnuParser parser = new GnuParser();
        Options options = new Options();
        String[] arguments = {null, "arg1", null};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"arg1"}, result);
    }

    @Test
    public void testFlatten_DoubleDashSetsEatTheRest() {
        GnuParser parser = new GnuParser();
        Options options = new Options();
        String[] arguments = {"--", "-a", "b"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"--", "-a", "b"}, result);
    }

    @Test
    public void testFlatten_SingleDashAddedAsToken() {
        GnuParser parser = new GnuParser();
        Options options = new Options();
        String[] arguments = {"-"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"-"}, result);
    }

    @Test
    public void testFlatten_ExistingShortOption() {
        Options options = new Options();
        options.addOption(new Option("a", "a option"));
        GnuParser parser = new GnuParser();
        String[] arguments = {"-a"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"-a"}, result);
    }

    @Test
    public void testFlatten_ExistingLongOption() {
        Options options = new Options();
        options.addOption(new Option(null, "alpha", false, ""));
        GnuParser parser = new GnuParser();
        String[] arguments = {"--alpha"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"--alpha"}, result);
    }

    @Test
    public void testFlatten_OptionWithEqualSign_ValidOption() {
        Options options = new Options();
        options.addOption(new Option("k", "key", true, "key option"));
        GnuParser parser = new GnuParser();
        String[] arguments = {"--key=value"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"--key", "value"}, result);
    }

    @Test
    public void testFlatten_OptionWithEqualSign_InvalidOption() {
        Options options = new Options();
        GnuParser parser = new GnuParser();
        String[] arguments = {"--invalid=value"};
        String[] result = parser.flatten(options, arguments, true);
        assertArrayEquals(new String[]{"--invalid=value"}, result);
    }

    @Test
    public void testFlatten_ShortOptionCombinationExists() {
        Options options = new Options();
        options.addOption(new Option("a", "special option"));
        GnuParser parser = new GnuParser();
        String[] arguments = {"-abc"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"-a", "bc"}, result);
    }

    @Test
    public void testFlatten_OptionNotExists_StopAtNonOptionTrue() {
        Options options = new Options();
        GnuParser parser = new GnuParser();
        String[] arguments = {"-x", "arg1", "-y"};
        String[] result = parser.flatten(options, arguments, true);
        assertArrayEquals(new String[]{"-x", "arg1", "-y"}, result);
    }

    @Test
    public void testFlatten_OptionNotExists_StopAtNonOptionFalse() {
        Options options = new Options();
        GnuParser parser = new GnuParser();
        String[] arguments = {"-x", "arg1"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"-x", "arg1"}, result);
    }

    @Test
    public void testFlatten_NonOptionArgument() {
        GnuParser parser = new GnuParser();
        Options options = new Options();
        String[] arguments = {"filename"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"filename"}, result);
    }

    @Test
    public void testFlatten_StopAtNonOptionTriggeredByNonOption() {
        Options options = new Options();
        options.addOption(new Option("a", "a option"));
        GnuParser parser = new GnuParser();
        String[] arguments = {"non-option", "-a", "arg"};
        String[] result = parser.flatten(options, arguments, true);
        assertArrayEquals(new String[]{"non-option", "-a", "arg"}, result);
    }

    @Test
    public void testFlatten_CombinationWithEqualSignAndShortOption() {
        Options options = new Options();
        options.addOption(new Option("k", "key", true, "key option"));
        options.addOption(new Option("e", "special e"));
        GnuParser parser = new GnuParser();
        String[] arguments = {"--key=val", "-efg", "non-opt"};
        String[] result = parser.flatten(options, arguments, false);
        assertArrayEquals(new String[]{"--key", "val", "-e", "fg", "non-opt"}, result);
    }
}