package csv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class CSVPrinterTest {

  /*  @Test
    void testConstructorWithHeaderComments() throws IOException {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.Builder.create()
                .setHeaderComments("Comment1", "Comment2")
                .setHeader("Header1", "Header2")
                .get();
        new CSVPrinter(writer, format);
        String result = writer.toString();
        assertTrue(result.contains("# Comment1"));
        assertTrue(result.contains("# Comment2"));
        assertTrue(result.contains("Header1,Header2"));
    }*/

    @Test
    void testConstructorWithoutHeader() throws IOException {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.Builder.create()
                .setSkipHeaderRecord(true)
                .setHeader("Header1", "Header2")
                .get();
        new CSVPrinter(writer, format);
        assertFalse(writer.toString().contains("Header1,Header2"));
    }

    @Test
    void testCloseWithoutFlush() throws IOException {
        Appendable appendable = Mockito.mock(Appendable.class, Mockito.withSettings().extraInterfaces(Closeable.class));
        Closeable closeable = (Closeable) appendable;
        CSVPrinter printer = new CSVPrinter(appendable, CSVFormat.DEFAULT);
        printer.close();
        Mockito.verify(closeable).close();
    }

    @Test
    void testCloseWithFlush() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.print("data");
        printer.close(true);
        assertEquals("data", writer.toString());
    }

    @Test
    void testEndOfRecord() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.print("value");
        printer.endOfRecord();
        assertEquals("value" + CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
        assertEquals(1, printer.getRecordCount());
    }

    @Test
    void testFlush() throws IOException {
        Writer writer = Mockito.mock(Writer.class);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.flush();
        Mockito.verify(writer).flush();
    }

    @Test
    void testGetOut() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        assertEquals(writer, printer.getOut());
    }

    @Test
    void testGetRecordCount() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecord("value");
        assertEquals(1, printer.getRecordCount());
    }

    @Test
    void testPrint() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.print("test");
        assertEquals("test", writer.toString());
    }

    @Test
    void testPrintCommentWithNewlines() throws IOException {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.Builder.create().setCommentMarker('#').get();
        CSVPrinter printer = new CSVPrinter(writer, format);
        printer.printComment("line1\nline2\rline3\r\nline4");
        String expected = "# line1" + format.getRecordSeparator() + 
                          "# line2" + format.getRecordSeparator() + 
                          "# line3" + format.getRecordSeparator() + 
                          "# line4" + format.getRecordSeparator();
        assertEquals(expected, writer.toString());
    }

    @Test
    void testPrintRecordIterable() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecord(Arrays.asList("a", "b", "c"));
        assertEquals("a,b,c" + CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
    }

    @Test
    void testPrintRecordVarargs() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecord("x", "y", "z");
        assertEquals("x,y,z" + CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
    }

    @Test
    void testPrintRecordStream() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecord(Stream.of("p", "q", "r"));
        assertEquals("p,q,r" + CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
    }

    @Test
    void testPrintRecordsIterable() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecords(Arrays.asList(
            Arrays.asList("a1", "a2"),
            Arrays.asList("b1", "b2")
        ));
        String expected = "a1,a2" + CSVFormat.DEFAULT.getRecordSeparator() + 
                          "b1,b2" + CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals(expected, writer.toString());
    }

    @Test
    void testPrintHeadersResultSet() throws SQLException, IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(metaData.getColumnLabel(1)).thenReturn("col1");
        Mockito.when(metaData.getColumnLabel(2)).thenReturn("col2");
        
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getMetaData()).thenReturn(metaData);
        
        printer.printHeaders(resultSet);
        assertEquals("col1,col2" + CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
    }

    @Test
    void testPrintRecordsResultSetWithBlobClob() throws SQLException, IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metaData.getColumnCount()).thenReturn(3);
        Mockito.when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        Mockito.when(metaData.getColumnType(2)).thenReturn(Types.CLOB);
        Mockito.when(metaData.getColumnType(3)).thenReturn(Types.BLOB);
        
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getMetaData()).thenReturn(metaData);
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.getObject(1)).thenReturn("text");
        
        Mockito.when(resultSet.getObject(2)).thenAnswer(new Answer<Clob>() {
            @Override
            public Clob answer(InvocationOnMock invocation) throws Throwable {
                Clob clob = Mockito.mock(Clob.class);
                Mockito.when(clob.getCharacterStream()).thenReturn(new StringReader(""));
                return clob;
            }
        });
        
        Mockito.when(resultSet.getObject(3)).thenAnswer(new Answer<Blob>() {
            @Override
            public Blob answer(InvocationOnMock invocation) throws Throwable {
                Blob blob = Mockito.mock(Blob.class);
                Mockito.when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                return blob;
            }
        });
        
        printer.printRecords(resultSet);
        assertTrue(writer.toString().startsWith("text"));
    }

    @Test
    void testPrintRecordsResultSetWithHeader() throws SQLException, IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metaData.getColumnCount()).thenReturn(2);
        Mockito.when(metaData.getColumnLabel(1)).thenReturn("h1");
        Mockito.when(metaData.getColumnLabel(2)).thenReturn("h2");
        
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getMetaData()).thenReturn(metaData);
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.getObject(1)).thenReturn("v1");
        Mockito.when(resultSet.getObject(2)).thenReturn("v2");
        
        printer.printRecords(resultSet, true);
        String expected = "h1,h2" + CSVFormat.DEFAULT.getRecordSeparator() + 
                          "v1,v2" + CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals(expected, writer.toString());
    }

    @Test
    void testPrintRecordsStream() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.printRecords(Stream.of("a", "b"));
        String expected = "a" + CSVFormat.DEFAULT.getRecordSeparator() + 
                          "b" + CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals(expected, writer.toString());
    }

    @Test
    void testPrintln() throws IOException {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        printer.println();
        assertEquals(CSVFormat.DEFAULT.getRecordSeparator(), writer.toString());
    }

    static class Base64OutputStream extends OutputStream {
        private final OutputStream out;
        Base64OutputStream(OutputStream out) { this.out = out; }
        @Override
        public void write(int b) throws IOException { out.write(b); }
        @Override
        public void close() throws IOException { out.close(); }
    }

    static class AppendableOutputStream<T extends Appendable> extends OutputStream {
        private final T appendable;
        AppendableOutputStream(T appendable) { this.appendable = appendable; }
        @Override
        public void write(int b) throws IOException { appendable.append((char) b); }
    }

    static class IOUtilsHelper {
        public static void copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
        }
        public static void copy(Reader input, Appendable output) throws IOException {
            char[] buffer = new char[1024];
            int n;
            while ((n = input.read(buffer)) != -1) {
                output.append(new String(buffer, 0, n));
            }
        }
        public static void copyLarge(Reader input, Writer output) throws IOException {
            copy(input, output);
        }
        public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    }

    static class ExtendedBufferedReader extends Reader {
        private final Reader reader;
        ExtendedBufferedReader(Reader reader) { this.reader = reader; }
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException { 
            return reader.read(cbuf, off, len); 
        }
        @Override
        public void close() throws IOException { reader.close(); }
        public int read() throws IOException { return reader.read(); }
        public int peek(char[] buffer) throws IOException { return 0; }
    }
}