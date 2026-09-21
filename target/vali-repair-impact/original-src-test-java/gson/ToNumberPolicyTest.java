package gson;

import gson.stream.JsonReader;
import gson.stream.MalformedJsonException;
import gson.internal.LazilyParsedNumber;
import gson.JsonParseException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToNumberPolicyTest {

    @Test
    void testDOUBLE_readNumber() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        Number result = ToNumberPolicy.DOUBLE.readNumber(reader);
        assertEquals(123.45, result.doubleValue());
    }

    @Test
    void testDOUBLE_readNumber_invalid() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("abc"));
        assertThrows(MalformedJsonException.class, () -> ToNumberPolicy.DOUBLE.readNumber(reader));
    }

    @Test
    void testLAZILY_PARSED_NUMBER_readNumber() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        Number result = ToNumberPolicy.LAZILY_PARSED_NUMBER.readNumber(reader);
        assertTrue(result instanceof LazilyParsedNumber);
        assertEquals("123.45", result.toString());
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_integer() throws IOException, JsonParseException {
        JsonReader reader = new JsonReader(new StringReader("123"));
        Number result = ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader);
        assertTrue(result instanceof Long);
        assertEquals(123L, result);
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_decimal() throws IOException, JsonParseException {
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        Number result = ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader);
        assertTrue(result instanceof Double);
        assertEquals(123.45, result);
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_longOverflow() throws IOException, JsonParseException {
        JsonReader reader = new JsonReader(new StringReader("9223372036854775808"));
        Number result = ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader);
        assertTrue(result instanceof Double);
        assertEquals(9.223372036854776E18, result);
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_invalid() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("abc"));
        assertThrows(MalformedJsonException.class, () -> ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader));
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_infinityStrict() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("Infinity"));
        reader.setLenient(false);
        assertThrows(MalformedJsonException.class, () -> ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader));
    }

    @Test
    void testLONG_OR_DOUBLE_readNumber_infinityLenient() throws IOException, JsonParseException {
        JsonReader reader = new JsonReader(new StringReader("Infinity"));
        reader.setLenient(true);
        Number result = ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void testBIG_DECIMAL_readNumber_valid() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        Number result = ToNumberPolicy.BIG_DECIMAL.readNumber(reader);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void testBIG_DECIMAL_readNumber_invalid() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("abc"));
        assertThrows(MalformedJsonException.class, () -> ToNumberPolicy.BIG_DECIMAL.readNumber(reader));
    }
}