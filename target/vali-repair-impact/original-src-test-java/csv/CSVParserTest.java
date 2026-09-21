package csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class CSVParserTest {

    @Test
    void testBuilderInitialization() throws IOException {
        CSVParser parser = CSVParser.builder()
            .setReader(new StringReader(""))
            .setFormat(CSVFormat.DEFAULT)
            .setCharacterOffset(0)
            .setRecordNumber(1)
            .get();
        assertNotNull(parser);
        parser.close();
    }

    @Test
    void testParseString() throws IOException {
        CSVParser parser = CSVParser.parse("a,b,c", CSVFormat.DEFAULT);
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"a","b","c"}, record.values());
        parser.close();
    }

    @Test
    void testParseReader() throws IOException {
        Reader reader = new StringReader("x,y,z");
        CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT);
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"x","y","z"}, record.values());
        parser.close();
    }

    @Test
    void testParseInputStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("1,2,3".getBytes());
        CSVParser parser = CSVParser.parse(stream, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"1","2","3"}, record.values());
        parser.close();
    }

    @Test
    void testParseFile(@TempDir Path dir) throws IOException {
        File file = dir.resolve("test.csv").toFile();
        java.nio.file.Files.write(file.toPath(), "alpha,beta".getBytes());
        CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"alpha","beta"}, record.values());
        parser.close();
    }

    @Test
    void testNextRecordWithComments() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setCommentMarker('#').build();
        CSVParser parser = CSVParser.parse("#comment\na,b", format);
        assertFalse(parser.hasHeaderComment());
       // assertEquals("null", parser.getHeaderComment());
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"a","b"}, record.values());
        parser.close();
    }

    @Test
    void testEmptyInput() throws IOException {
        CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT);
        assertNull(parser.nextRecord());
        parser.close();
    }

    @Test
    void testMultipleRecords() throws IOException {
        CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT);
        Iterator<CSVRecord> it = parser.iterator();
        assertEquals("a", it.next().get(0));
        assertEquals("b", it.next().get(0));
        assertEquals("c", it.next().get(0));
        assertFalse(it.hasNext());
        parser.close();
    }

    @Test
    void testHeaderHandling() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("H1","H2").build();
        CSVParser parser = CSVParser.parse("v1,v2", format);
        Map<String,Integer> headerMap = parser.getHeaderMap();
        assertEquals(0, headerMap.get("H1"));
        assertEquals(1, headerMap.get("H2"));
        parser.close();
    }

    @Test
    void testStrictQuoteMode() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setQuoteMode(QuoteMode.ALL_NON_NULL)
            .setNullString("NULL")
            .build();
        CSVParser parser = CSVParser.parse("\"NULL\",\"\"", format);
        CSVRecord record = parser.nextRecord();
       // assertNull("NULL");
        assertEquals("", record.get(1));
        parser.close();
    }

    @Test
    void testCloseParser() throws IOException {
        CSVParser parser = CSVParser.parse("data", CSVFormat.DEFAULT);
        assertFalse(parser.isClosed());
        parser.close();
        assertTrue(parser.isClosed());
    }

    @Test
    void testStreamRecords() throws IOException {
        CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT);
        assertEquals(3, parser.stream().count());
        parser.close();
    }

    @Test
    void testIteratorBehavior() throws IOException {
        CSVParser parser = CSVParser.parse("first\nsecond", CSVFormat.DEFAULT);
        Iterator<CSVRecord> it = parser.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
        parser.close();
    }

    @Test
    void testRecordNumberTracking() throws IOException {
        CSVParser parser = CSVParser.parse("1\n2\n3", CSVFormat.DEFAULT);
        parser.nextRecord();
        assertEquals(1, parser.getRecordNumber());
        parser.nextRecord();
        assertEquals(2, parser.getRecordNumber());
        parser.close();
    }

    @Test
    void testTrailingComment() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setCommentMarker('#').build();
        CSVParser parser = CSVParser.parse("value\n#trailer", format);
        parser.nextRecord();
        assertFalse(parser.hasTrailerComment());
//        assertEquals("null", parser.getTrailerComment());
        parser.close();
    }

    @Test
    void testDeprecatedConstructor1() throws IOException {
        Reader reader = new StringReader("a,b");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"a","b"}, record.values());
        parser.close();
    }

    @Test
    void testDeprecatedConstructor2() throws IOException {
        Reader reader = new StringReader("x,y");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT, 0, 1);
        assertEquals(0, parser.getRecordNumber());
        CSVRecord record = parser.nextRecord();
        assertArrayEquals(new String[]{"x","y"}, record.values());
        parser.close();
    }

    @Test
    void testGetRecords() throws IOException {
        CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT);
        List<CSVRecord> records = parser.getRecords();
        assertEquals(3, records.size());
        assertEquals("a", records.get(0).get(0));
        assertEquals("b", records.get(1).get(0));
        assertEquals("c", records.get(2).get(0));
        parser.close();
    }

    @Test
    void testGetHeaderMapRaw() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("Name", "Age").build();
        CSVParser parser = CSVParser.parse("John,30", format);
        Map<String, Integer> headerMapRaw = parser.getHeaderMapRaw();
        assertEquals(0, headerMapRaw.get("Name"));
        assertEquals(1, headerMapRaw.get("Age"));
        parser.close();
    }

    @Test
    void testGetHeaderNames() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("H1", "H2").build();
        CSVParser parser = CSVParser.parse("v1,v2", format);
        List<String> headerNames = parser.getHeaderNames();
        assertEquals(Arrays.asList("H1", "H2"), headerNames);
        parser.close();
    }

    @Test
    void testGetCurrentLineNumber() throws IOException {
        CSVParser parser = CSVParser.parse("line1\nline2", CSVFormat.DEFAULT);
        parser.nextRecord();
        assertEquals(1, parser.getCurrentLineNumber());
        parser.nextRecord();
        assertEquals(2, parser.getCurrentLineNumber());
        parser.close();
    }

    @Test
    void testGetFirstEndOfLine() throws IOException {
        CSVParser parser = CSVParser.parse("a\r\nb", CSVFormat.DEFAULT);
        parser.nextRecord();
        assertEquals("\r\n", parser.getFirstEndOfLine());
        parser.close();
    }

}