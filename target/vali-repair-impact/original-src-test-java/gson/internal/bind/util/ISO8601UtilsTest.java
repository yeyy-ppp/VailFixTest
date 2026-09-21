package gson.internal.bind.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ISO8601UtilsTest {

    @Test
    void testFormatWithoutMillis() {
        Date date = new Date(1672531200000L);
        String result = ISO8601Utils.format(date);
        assertEquals("2023-01-01T00:00:00Z", result);
    }

    @Test
    void testFormatWithMillis() {
        Date date = new Date(1672531200123L);
        String result = ISO8601Utils.format(date, true);
        assertEquals("2023-01-01T00:00:00.123Z", result);
    }

    @Test
    void testFormatWithPositiveTimezone() {
        Date date = new Date(1672531200000L);
        TimeZone tz = TimeZone.getTimeZone("GMT+05:00");
        String result = ISO8601Utils.format(date, false, tz);
        assertEquals("2023-01-01T05:00:00+05:00", result);
    }

    @Test
    void testFormatWithNegativeTimezone() {
        Date date = new Date(1672531200000L);
        TimeZone tz = TimeZone.getTimeZone("GMT-03:30");
        String result = ISO8601Utils.format(date, true, tz);
        assertEquals("2022-12-31T20:30:00.000-03:30", result);
    }

    @Test
    void testParseFullFormat() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T05:30:15.123+05:00";
        Date result = ISO8601Utils.parse(dateStr, pos);
        assertEquals(new Date(1672531215123L + 30 * 60 * 1000), result);
    }

    @Test
    void testParseWithoutMillis() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:00Z";
        Date result = ISO8601Utils.parse(dateStr, pos);
        assertEquals(new Date(1672531200000L), result);
    }

    @Test
    void testParseDateOnly() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01";
        Date result = ISO8601Utils.parse(dateStr, pos);
        Date expectedDate = new Date(1672531200000L - 8 * 60 * 60 * 1000);
        assertEquals(expectedDate, result);
    }

    @Test
    void testParseInvalidString() {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01X";
        assertThrows(ParseException.class, () -> ISO8601Utils.parse(dateStr, pos));
    }

    @Test
    void testCheckOffsetValid() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("checkOffset", String.class, int.class, char.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(null, "abc", 0, 'a');
        assertTrue(result);
    }

    @Test
    void testCheckOffsetInvalid() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("checkOffset", String.class, int.class, char.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(null, "abc", 1, 'a');
        assertFalse(result);
    }

    @Test
    void testCheckOffsetOutOfBounds() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("checkOffset", String.class, int.class, char.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(null, "a", 1, 'a');
        assertFalse(result);
    }

    @Test
    void testParseIntNormal() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("parseInt", String.class, int.class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, "123", 0, 3);
        assertEquals(123, result);
    }

    @Test
    void testParseIntSingleDigit() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("parseInt", String.class, int.class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, "5", 0, 1);
        assertEquals(5, result);
    }

    @Test
    void testParseIntInvalidCharacter() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("parseInt", String.class, int.class, int.class);
        method.setAccessible(true);
        assertThrows(Exception.class, () -> method.invoke(null, "12a", 0, 3));
    }

    @Test
    void testParseIntInvalidRange() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("parseInt", String.class, int.class, int.class);
        method.setAccessible(true);
        assertThrows(Exception.class, () -> method.invoke(null, "123", -1, 3));
    }

    @Test
    void testPadIntExactLength() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("padInt", StringBuilder.class, int.class, int.class);
        method.setAccessible(true);
        StringBuilder sb = new StringBuilder();
        method.invoke(null, sb, 123, 3);
        assertEquals("123", sb.toString());
    }

    @Test
    void testPadIntNeedPadding() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("padInt", StringBuilder.class, int.class, int.class);
        method.setAccessible(true);
        StringBuilder sb = new StringBuilder();
        method.invoke(null, sb, 7, 3);
        assertEquals("007", sb.toString());
    }

    @Test
    void testPadIntZeroValue() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("padInt", StringBuilder.class, int.class, int.class);
        method.setAccessible(true);
        StringBuilder sb = new StringBuilder();
        method.invoke(null, sb, 0, 4);
        assertEquals("0000", sb.toString());
    }

    @Test
    void testIndexOfNonDigitImmediate() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("indexOfNonDigit", String.class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, "a123", 0);
        assertEquals(0, result);
    }

    @Test
    void testIndexOfNonDigitAfterDigits() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("indexOfNonDigit", String.class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, "123a", 0);
        assertEquals(3, result);
    }

    @Test
    void testIndexOfNonDigitNotFound() throws Exception {
        Method method = ISO8601Utils.class.getDeclaredMethod("indexOfNonDigit", String.class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, "123", 0);
        assertEquals(3, result);
    }

    @Test
    void testParseWithHighSeconds() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:62Z";
        Date result = ISO8601Utils.parse(dateStr, pos);
        Date expectedDate = new Date(1672531200000L + 59000);
        assertEquals(expectedDate, result);
    }

    @Test
    void testParseWithFractionalMillis() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:00.1Z";
        Date result = ISO8601Utils.parse(dateStr, pos);
        assertEquals(new Date(1672531200100L), result);
    }

    @Test
    void testParseWithTwoDigitMillis() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:00.12Z";
        Date result = ISO8601Utils.parse(dateStr, pos);
        assertEquals(new Date(1672531200120L), result);
    }

    @Test
    void testParseWithZeroTimezoneOffset() throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:00+00:00";
        Date result = ISO8601Utils.parse(dateStr, pos);
        assertEquals(new Date(1672531200000L), result);
    }

    @Test
    void testParseWithInvalidTimezone() {
        ParsePosition pos = new ParsePosition(0);
        String dateStr = "2023-01-01T00:00:00X";
        assertThrows(ParseException.class, () -> ISO8601Utils.parse(dateStr, pos));
    }

    @Test
    void testPrivateConstructorAccess() throws Exception {
        Constructor<ISO8601Utils> constructor = ISO8601Utils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ISO8601Utils instance = constructor.newInstance();
        assertNotNull(instance);
    }

}