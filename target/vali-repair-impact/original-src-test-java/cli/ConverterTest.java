package cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConverterTest {

    @Test
    void testDefaultConverter() {
        String input = "test";
        Object result = Converter.DEFAULT.apply(input);
        assertEquals(input, result);
    }

    @Test
    void testClassConverter_Success() throws ClassNotFoundException {
        Class<?> result = Converter.CLASS.apply("java.lang.String");
        assertEquals(String.class, result);
    }

    @Test
    void testClassConverter_Failure() {
        assertThrows(ClassNotFoundException.class, () -> 
            Converter.CLASS.apply("invalid.ClassName")
        );
    }

    @Test
    void testFileConverter_Success() {
        File result = Converter.FILE.apply("test.txt");
        assertEquals(new File("test.txt"), result);
    }

    @Test
    void testFileConverter_Failure() {
        assertThrows(NullPointerException.class, () -> 
            Converter.FILE.apply(null)
        );
    }

    @Test
    void testPathConverter_Success() {
        Path result = Converter.PATH.apply("testDir");
        assertEquals(Paths.get("testDir"), result);
    }

    @Test
    void testPathConverter_Failure() {
        assertThrows(InvalidPathException.class, () -> 
            Converter.PATH.apply("invalid\0path")
        );
    }

    @Test
    void testNumberConverter_Integer() {
        Number result = Converter.NUMBER.apply("123");
        assertEquals(123L, result);
    }

    @Test
    void testNumberConverter_Float() {
        Number result = Converter.NUMBER.apply("12.34");
        assertEquals(12.34, result.doubleValue(), 0.001);
    }

    @Test
    void testNumberConverter_Failure() {
        assertThrows(NumberFormatException.class, () -> 
            Converter.NUMBER.apply("abc")
        );
    }

    @Test
    void testObjectConverter_Success() throws ReflectiveOperationException {
        Object result = Converter.OBJECT.apply("java.lang.String");
        assertEquals("", result);
    }

    @Test
    void testObjectConverter_Failure() {
        assertThrows(ReflectiveOperationException.class, () -> 
            Converter.OBJECT.apply("non.existent.Class")
        );
    }

    @Test
    void testUrlConverter_Success() throws MalformedURLException {
        URL result = Converter.URL.apply("http://example.com");
        assertEquals(new URL("http://example.com"), result);
    }

    @Test
    void testUrlConverter_Failure() {
        assertThrows(MalformedURLException.class, () -> 
            Converter.URL.apply("invalid_url")
        );
    }

   /* @Test
    void testDateConverter_Success() throws java.text.ParseException {
        Date result = Converter.DATE.apply("Mon Jan 01 12:00:00 GMT 2024");
        assertNotNull(result);
    }*/

    @Test
    void testDateConverter_Failure() {
        assertThrows(java.text.ParseException.class, () -> 
            Converter.DATE.apply("invalid date")
        );
    }
}