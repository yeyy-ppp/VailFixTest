package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import gson.JsonStreamParser;
import gson.JsonElement;
import gson.JsonIOException;
import gson.JsonParseException;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.lang.reflect.Field;
import gson.Strictness;
import java.util.NoSuchElementException;

public class JsonStreamParserTest {

    @Test
    void testConstructorWithStringInitializesParser() throws Exception {
        JsonStreamParser parser = new JsonStreamParser("{}");
        Field parserField = JsonStreamParser.class.getDeclaredField("parser");
        parserField.setAccessible(true);
        assertNotNull(parserField.get(parser));
    }

    @Test
    void testConstructorWithReaderSetsLenientMode() throws Exception {
        JsonStreamParser parser = new JsonStreamParser(new StringReader("{}"));
        Field parserField = JsonStreamParser.class.getDeclaredField("parser");
        parserField.setAccessible(true);
        Object jsonReader = parserField.get(parser);
        Field strictnessField = jsonReader.getClass().getDeclaredField("strictness");
        strictnessField.setAccessible(true);
        assertEquals(Strictness.LENIENT, strictnessField.get(jsonReader));
    }

    @Test
    void testHasNextWithValidJsonReturnsTrue() {
        JsonStreamParser parser = new JsonStreamParser("[1,2]");
        assertTrue(parser.hasNext());
    }

    @Test
    void testHasNextWithEndDocumentReturnsFalse() {
        JsonStreamParser parser = new JsonStreamParser("{}");
        parser.next();
        assertFalse(parser.hasNext());
    }

    @Test
    void testHasNextWithClosedReaderThrowsJsonIOException() {
        Reader reader = new StringReader("{}");
        try {
            reader.close();
        } catch (IOException ignored) {}
        JsonStreamParser parser = new JsonStreamParser(reader);
        assertThrows(JsonIOException.class, parser::hasNext);
    }

    @Test
    void testNextParsesElementSuccessfully() {
        JsonStreamParser parser = new JsonStreamParser("\"test\"");
        assertEquals("test", parser.next().getAsString());
    }

    @Test
    void testNextWhenNoElementsThrowsNoSuchElementException() {
        JsonStreamParser parser = new JsonStreamParser("{}");
        parser.next();
        assertThrows(NoSuchElementException.class, parser::next);
    }

    @Test
    void testRemoveThrowsUnsupportedOperationException() {
        JsonStreamParser parser = new JsonStreamParser("{}");
        assertThrows(UnsupportedOperationException.class, parser::remove);
    }

}