package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BasicParserTest {

    @Test
    public void testConstructor() {
        BasicParser parser = new BasicParser();
        assertNotNull(parser);
    }

    @Test
    public void testFlattenWithEmptyArray() {
        BasicParser parser = new BasicParser();
        Options options = new Options();
        String[] args = {};
        String[] result = parser.flatten(options, args, true);
        assertArrayEquals(args, result);
    }

    @Test
    public void testFlattenWithSingleElement() {
        BasicParser parser = new BasicParser();
        Options options = new Options();
        String[] args = {"arg1"};
        String[] result = parser.flatten(options, args, false);
        assertArrayEquals(args, result);
    }

    @Test
    public void testFlattenWithMultipleElements() {
        BasicParser parser = new BasicParser();
        Options options = new Options();
        String[] args = {"arg1", "arg2", "arg3"};
        String[] result = parser.flatten(options, args, true);
        assertArrayEquals(args, result);
    }

    @Test
    public void testFlattenWithNullArguments() {
        BasicParser parser = new BasicParser();
        Options options = new Options();
        String[] result = parser.flatten(options, null, false);
        assertNull(result);
    }

    @Test
    public void testFlattenIgnoresOptionsAndStopFlag() {
        BasicParser parser = new BasicParser();
        Options options = null;
        String[] args = {"test"};
        String[] result = parser.flatten(options, args, true);
        assertArrayEquals(args, result);
    }
}