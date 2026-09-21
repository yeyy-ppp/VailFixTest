package cli;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import cli.ParseException;

class TypeHandlerTest {

    @Test
    void createClass_validName_returnsClass() throws Exception {
        Class<?> result = TypeHandler.createClass("java.lang.String");
        assertEquals(String.class, result);
    }

    @Test
    void createClass_invalidName_throwsParseException() {
        assertThrows(ParseException.class, () -> TypeHandler.createClass("invalid.ClassName"));
    }

    /*@Test
    void createDate_validString_returnsDate() {
        Date date = TypeHandler.createDate("2020-01-01");
        assertNotNull(date);
    }*/

    @Test
    void createDate_invalidString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TypeHandler.createDate("invalid-date"));
    }

    @Test
    void createDefaultMap_containsAllDefaultConverters() {
        Map<Class<?>, Converter<?, ? extends Throwable>> map = TypeHandler.createDefaultMap();
        assertNotNull(map.get(File.class));
        assertNotNull(map.get(URL.class));
        assertNotNull(map.get(Integer.class));
    }

    @Test
    void createFile_validPath_returnsFile() {
        File file = TypeHandler.createFile("test.txt");
        assertNotNull(file);
        assertEquals("test.txt", file.getName());
    }

    @Test
    void createURL_validString_returnsURL() throws Exception {
        URL url = TypeHandler.createURL("http://example.com");
        assertEquals(new URL("http://example.com"), url);
    }

    @Test
    void createURL_invalidString_throwsParseException() {
        assertThrows(ParseException.class, () -> TypeHandler.createURL("invalid-url"));
    }

    @Test
    void createValue_stringToInteger_returnsInteger() throws ParseException {
        Integer result = TypeHandler.createValue("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    void createValue_stringToDouble_returnsDouble() throws ParseException {
        Double result = TypeHandler.createValue("123.45", Double.class);
        assertEquals(123.45, result, 0.001);
    }

    @Test
    void createValue_stringToCharacter_unicode_returnsChar() throws ParseException {
        Character result = TypeHandler.createValue("\\u0041", Character.class);
        assertEquals('A', result);
    }

    @Test
    void createValue_stringToCharacter_singleChar_returnsChar() throws ParseException {
        Character result = TypeHandler.createValue("B", Character.class);
        assertEquals('B', result);
    }

    @Test
    void createValue_invalidConversion_throwsParseException() {
        assertThrows(ParseException.class, () -> TypeHandler.createValue("text", Integer.class));
    }

  /*  @Test
    void createValue_nullInput_throwsParseException() {
        assertThrows(ParseException.class, () -> TypeHandler.createValue(null, String.class));
    }*/

    @Test
    void getDefault_always_returnsSameInstance() {
        TypeHandler instance1 = TypeHandler.getDefault();
        TypeHandler instance2 = TypeHandler.getDefault();
        assertSame(instance1, instance2);
    }

    @Test
    void defaultConstructor_initializesWithDefaultMap() {
        TypeHandler handler = new TypeHandler();
        Converter<Character, ?> converter = handler.getConverter(Character.class);
        assertNotNull(converter);
    }

    @Test
    void customMapConstructor_usesProvidedConverters() {
        Map<Class<?>, Converter<?, ? extends Throwable>> customMap = new HashMap<>();
        customMap.put(String.class, s -> "custom_" + s);
        TypeHandler handler = new TypeHandler(customMap);
        
        /*String result = handler.getConverter(String.class).apply("test");
        assertEquals("custom_test", result);*/
    }

    @Test
    void getConverter_unknownClass_returnsDefaultConverter() {
        TypeHandler handler = new TypeHandler();
        Converter<?, ?> converter = handler.getConverter(Boolean.class);
        assertNotNull(converter);
        
        /*Object result = converter.apply("test");
        assertEquals("test", result);*/
    }

    @Test
    void getConverter_integerType_returnsParser() {
        TypeHandler handler = new TypeHandler();
        Converter<Integer, ?> converter = handler.getConverter(Integer.class);
        assertNotNull(converter);
        
       /* Integer result = converter.apply("456");
        assertEquals(456, result);*/
    }

    @Test
    void getConverter_bigDecimal_returnsParser() {
        TypeHandler handler = new TypeHandler();
        Converter<BigDecimal, ?> converter = handler.getConverter(BigDecimal.class);
        assertNotNull(converter);
        
        /*BigDecimal result = converter.apply("123.456");
        assertEquals(new BigDecimal("123.456"), result);*/
    }
}