package csv;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtendedBufferedReaderTest {

    private ExtendedBufferedReader reader;
    private final String testString = "test\nline2";
    private final Charset utf8 = StandardCharsets.UTF_8;

    @BeforeEach
    void setUp() {
        reader = new ExtendedBufferedReader(new StringReader(testString));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    @Test
    void testConstructorWithReader() {
        assertNotNull(reader);
        assertFalse(reader.isClosed());
    }

    @Test
    void testConstructorWithCharsetAndTracking() {
        ExtendedBufferedReader trackedReader = new ExtendedBufferedReader(
            new StringReader(testString), utf8, true);
        assertNotNull(trackedReader);
        assertFalse(trackedReader.isClosed());
    }

    @Test
    void testClose() throws IOException {
        reader.close();
        assertTrue(reader.isClosed());
    }

    @Test
    void testIsClosed() throws IOException {
        assertFalse(reader.isClosed());
        reader.close();
        assertTrue(reader.isClosed());
    }

    @Test
    void testGetBytesRead() throws IOException {
        ExtendedBufferedReader trackedReader = new ExtendedBufferedReader(
            new StringReader("ä"), StandardCharsets.UTF_8, true);
        trackedReader.read();
        assertEquals(2, trackedReader.getBytesRead());
    }

    @Test
    void testGetLastChar() throws IOException {
        reader.read();
        assertEquals('t', reader.getLastChar());
    }

    @Test
    void testGetLineNumber() throws IOException {
        reader.readLine();
        assertEquals(1, reader.getLineNumber());
    }

    @Test
    void testGetPosition() throws IOException {
        reader.read();
        assertEquals(1, reader.getPosition());
    }

    @Test
    void testPeek() throws IOException {
        assertEquals('t', reader.peek());
    }

    @Test
    void testPeekCharArray() throws IOException {
        char[] buf = new char[3];
        int read = reader.peek(buf);
        assertEquals(3, read);
        assertArrayEquals(new char[]{'t','e','s'}, buf);
    }

    @Test
    void testMarkAndReset() throws IOException {
        reader.mark(10);
        reader.read();
        reader.reset();
        assertEquals('t', reader.read());
    }

    @Test
    void testRead() throws IOException {
        assertEquals('t', reader.read());
        assertEquals('e', reader.read());
    }

    @Test
    void testReadCharArray() throws IOException {
        char[] buf = new char[4];
        assertEquals(4, reader.read(buf, 0, 4));
        assertArrayEquals("test".toCharArray(), buf);
    }

    @Test
    void testReadLine() throws IOException {
        assertEquals("test", reader.readLine());
        assertEquals("line2", reader.readLine());
    }
}