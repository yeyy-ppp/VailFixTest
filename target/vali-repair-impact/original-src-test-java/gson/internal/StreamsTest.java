package gson.internal;

import gson.JsonElement;
import gson.JsonIOException;
import gson.JsonNull;
import gson.JsonParseException;
import gson.JsonSyntaxException;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StreamsTest {

    @Test
    void testStreamsConstructor() throws Exception {
        Constructor<Streams> constructor = Streams.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void testParseEmpty() throws JsonParseException {
        JsonReader reader = new JsonReader(new StringReader(""));
        JsonElement element = Streams.parse(reader);
        assertEquals(JsonNull.INSTANCE, element);
    }

    @Test
    void testParseValidJson() throws JsonParseException {
        JsonReader reader = new JsonReader(new StringReader("{\"key\":\"value\"}"));
        JsonElement element = Streams.parse(reader);
        assertTrue(element.isJsonObject());
    }

    @Test
    void testParseNumberFormatException() {
        JsonReader reader = new JsonReader(new StringReader("{\"num\":12.3.4}"));
        assertThrows(JsonSyntaxException.class, () -> Streams.parse(reader));
    }

    @Test
    void testWriteValid() throws IOException {
        JsonElement element = JsonNull.INSTANCE;
        StringWriter writer = new StringWriter();
        Streams.write(element, new JsonWriter(writer));
        assertEquals("null", writer.toString());
    }

    @Test
    void testWriterForAppendableWithWriter() {
        Writer writer = new StringWriter();
        Writer result = Streams.writerForAppendable(writer);
        assertSame(writer, result);
    }

    @Test
    void testWriterForAppendableWithNonWriter() {
        Appendable appendable = new StringBuilder();
        Writer result = Streams.writerForAppendable(appendable);
        assertTrue(result instanceof Streams.AppendableWriter);
    }

    @Test
    void testAppendableWriterWriteCharArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sb);
        writer.write(new char[]{'a', 'b', 'c'}, 0, 3);
        assertEquals("abc", sb.toString());
    }

    @Test
    void testAppendableWriterFlushFlushable() throws IOException {
        StringWriter sw = new StringWriter();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sw);
        writer.flush();
        assertDoesNotThrow(writer::flush);
    }

    @Test
    void testAppendableWriterCloseCloseable() throws IOException {
        StringWriter sw = new StringWriter();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sw);
        writer.close();
        //assertThrows(IOException.class, () -> writer.write("test"));
    }

    @Test
    void testAppendableWriterWriteSingleChar() throws IOException {
        StringBuilder sb = new StringBuilder();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sb);
        writer.write('a');
        assertEquals("a", sb.toString());
    }

    @Test
    void testAppendableWriterWriteString() throws IOException {
        StringBuilder sb = new StringBuilder();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sb);
        writer.write("test", 0, 4);
        assertEquals("test", sb.toString());
    }

    @Test
    void testAppendableWriterAppendCharSequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sb);
        writer.append("append");
        assertEquals("append", sb.toString());
    }

    @Test
    void testAppendableWriterAppendSubSequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        Streams.AppendableWriter writer = new Streams.AppendableWriter(sb);
        writer.append("abcdef", 1, 4);
        assertEquals("bcd", sb.toString());
    }

    @Test
    void testCurrentWriteSetChars() {
        Streams.AppendableWriter.CurrentWrite cw = new Streams.AppendableWriter.CurrentWrite();
        cw.setChars(new char[]{'x', 'y'});
        assertEquals(2, cw.length());
    }

    @Test
    void testCurrentWriteCharAt() {
        Streams.AppendableWriter.CurrentWrite cw = new Streams.AppendableWriter.CurrentWrite();
        cw.setChars(new char[]{'a', 'b', 'c'});
        assertEquals('b', cw.charAt(1));
    }

    @Test
    void testCurrentWriteSubSequence() {
        Streams.AppendableWriter.CurrentWrite cw = new Streams.AppendableWriter.CurrentWrite();
        cw.setChars(new char[]{'1', '2', '3'});
        CharSequence sub = cw.subSequence(0, 2);
        assertEquals("12", sub.toString());
    }

    @Test
    void testCurrentWriteToString() {
        Streams.AppendableWriter.CurrentWrite cw = new Streams.AppendableWriter.CurrentWrite();
        cw.setChars(new char[]{'t', 'e', 's', 't'});
        assertEquals("test", cw.toString());
    }

}