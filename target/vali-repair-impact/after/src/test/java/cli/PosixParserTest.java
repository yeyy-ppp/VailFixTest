package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PosixParserTest {

    private PosixParser parser;
    private Options options;

    @BeforeEach
    public void setUp() {
        parser = new PosixParser();
        options = new Options();
    }

    @Test
    public void testConstructor() {
        assertNotNull(new PosixParser());
    }

    @Test
    public void testBurstToken_AllOptionsExist() throws Exception {
        options.addOption("a", false, "");
        options.addOption("b", false, "");
        options.addOption("c", true, "");
        setField(parser, "options", options);

        Method burstToken = PosixParser.class.getDeclaredMethod("burstToken", String.class, boolean.class);
        burstToken.setAccessible(true);
        burstToken.invoke(parser, "-abcde", false);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("-a", "-b", "-c", "de"), tokens);
    }

    @Test
    public void testBurstToken_NonExistingOptionStopAtNonOptionTrue() throws Exception {
        options.addOption("a", false, "");
        setField(parser, "options", options);

        Method burstToken = PosixParser.class.getDeclaredMethod("burstToken", String.class, boolean.class);
        burstToken.setAccessible(true);
        burstToken.invoke(parser, "-abc", true);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("-a", "--", "bc"), tokens);
    }

    @Test
    public void testBurstToken_NonExistingOptionStopAtNonOptionFalse() throws Exception {
        options.addOption("a", false, "");
        setField(parser, "options", options);

        Method burstToken = PosixParser.class.getDeclaredMethod("burstToken", String.class, boolean.class);
        burstToken.setAccessible(true);
        burstToken.invoke(parser, "-abc", false);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("-abc"), tokens);
    }

    @Test
    public void testFlatten_EmptyArguments() throws Exception {
        String[] result = parser.flatten(options, new String[]{}, false);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatten_SingleNonOptionToken() throws Exception {
        String[] result = parser.flatten(options, new String[]{"arg1"}, false);
        assertArrayEquals(new String[]{"arg1"}, result);
    }

    @Test
    public void testFlatten_ShortOptionExists() throws Exception {
        options.addOption("a", false, "");
        String[] result = parser.flatten(options, new String[]{"-a"}, false);
        assertArrayEquals(new String[]{"-a"}, result);
    }

    @Test
    public void testFlatten_ShortOptionsBursting() throws Exception {
        options.addOption("a", false, "");
        options.addOption("b", false, "");
        options.addOption("c", false, "");
        String[] result = parser.flatten(options, new String[]{"-abc"}, false);
        assertArrayEquals(new String[]{"-a", "-b", "-c"}, result);
    }

    @Test
    public void testFlatten_ShortOptionWithArgInBurst() throws Exception {
        options.addOption("a", false, "");
        options.addOption("b", true, "");
        String[] result = parser.flatten(options, new String[]{"-abcde"}, false);
        assertArrayEquals(new String[]{"-a", "-b", "cde"}, result);
    }

    @Test
    public void testFlatten_LongOption() throws Exception {
        options.addOption(Option.builder("a").longOpt("long").build());
        String[] result = parser.flatten(options, new String[]{"--long"}, false);
        assertArrayEquals(new String[]{"--long"}, result);
    }

    @Test
    public void testFlatten_LongOptionWithEqual() throws Exception {
        options.addOption(Option.builder("a").longOpt("long").hasArg().build());
        String[] result = parser.flatten(options, new String[]{"--long=value"}, false);
        assertArrayEquals(new String[]{"--long", "value"}, result);
    }

    @Test
    public void testFlatten_DoubleDash() throws Exception {
        String[] result = parser.flatten(options, new String[]{"--"}, false);
        assertArrayEquals(new String[]{"--"}, result);
    }

    @Test
    public void testFlatten_SingleDash() throws Exception {
        String[] result = parser.flatten(options, new String[]{"-"}, false);
        assertArrayEquals(new String[]{"-"}, result);
    }

    @Test
    public void testFlatten_StopAtNonOptionTrue() throws Exception {
        options.addOption("a", false, "");
        String[] result = parser.flatten(options, new String[]{"-a", "arg1", "-b"}, true);
        assertArrayEquals(new String[]{"-a", "--", "arg1", "-b"}, result);
    }

// [兜底]     @Test
// [兜底]     public void testFlatten_AmbiguousOption() {
// [兜底]         options.addOption(Option.builder("a").longOpt("apple").build());
// [兜底]         options.addOption(Option.builder("b").longOpt("app").build());
// [兜底]         
// [兜底]         assertThrows(AmbiguousOptionException.class, () -> {
// [兜底]             parser.flatten(options, new String[]{"--app"}, false);
// [兜底]         });
// [兜底]     }

    @Test
    public void testGobble() throws Exception {
        setField(parser, "tokens", Arrays.asList("token1", "token2"));
        setField(parser, "eatTheRest", true);

        Method gobble = PosixParser.class.getDeclaredMethod("gobble", Iterator.class);
        gobble.setAccessible(true);
        gobble.invoke(parser, Arrays.asList("arg1", "arg2").iterator());

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("token1", "token2", "arg1", "arg2"), tokens);
    }

    @Test
    public void testInit() throws Exception {
        setField(parser, "tokens", Arrays.asList("existing"));
        setField(parser, "eatTheRest", true);
        setField(parser, "options", options);

        Method init = PosixParser.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(parser);

        List<String> tokens = getTokens(parser);
        assertTrue(tokens.isEmpty());
        boolean eatTheRest = (boolean) getField(parser, "eatTheRest");
        assertFalse(eatTheRest);
    }

    @Test
    public void testProcessNonOptionToken() throws Exception {
        setField(parser, "tokens", new ArrayList<>());

        Method processNonOptionToken = PosixParser.class.getDeclaredMethod("processNonOptionToken", String.class, boolean.class);
        processNonOptionToken.setAccessible(true);
        processNonOptionToken.invoke(parser, "arg1", true);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("--", "arg1"), tokens);
    }

    @Test
    public void testProcessOptionToken_WithArg() throws Exception {
        Option option = Option.builder("a").hasArg(true).build();
        options.addOption(option);
        setField(parser, "options", options);
        setField(parser, "tokens", new ArrayList<>());

        Method processOptionToken = PosixParser.class.getDeclaredMethod("processOptionToken", String.class, boolean.class);
        processOptionToken.setAccessible(true);
        processOptionToken.invoke(parser, "-a", true);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("-a"), tokens);
    }

    @Test
    public void testProcessOptionToken_WithoutArg() throws Exception {
        Option option = Option.builder("a").build();
        options.addOption(option);
        setField(parser, "options", options);
        setField(parser, "tokens", new ArrayList<>());

        Method processOptionToken = PosixParser.class.getDeclaredMethod("processOptionToken", String.class, boolean.class);
        processOptionToken.setAccessible(true);
        processOptionToken.invoke(parser, "-a", true);

        List<String> tokens = getTokens(parser);
        assertEquals(Arrays.asList("-a"), tokens);
    }

    @SuppressWarnings("unchecked")
    private List<String> getTokens(PosixParser parser) throws Exception {
        Field tokensField = PosixParser.class.getDeclaredField("tokens");
        tokensField.setAccessible(true);
        return (List<String>) tokensField.get(parser);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
