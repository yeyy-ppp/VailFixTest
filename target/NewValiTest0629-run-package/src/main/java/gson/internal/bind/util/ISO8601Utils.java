package gson.internal.bind.util;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
@SuppressWarnings("MemberName")
public class ISO8601Utils {
  private ISO8601Utils() {}
  private static final String UTC_ID = "UTC";
  private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);
  public static String format(Date date) {
    return format(date, false, TIMEZONE_UTC);
  }
  public static String format(Date date, boolean millis) {
    return format(date, millis, TIMEZONE_UTC);
  }
  public static String format(Date date, boolean millis, TimeZone tz) {
    Calendar calendar = new GregorianCalendar(tz, Locale.US);
    calendar.setTime(date);
    int capacity = "yyyy-MM-ddThh:mm:ss".length();
    capacity += millis ? ".sss".length() : 0;
    capacity += tz.getRawOffset() == 0 ? "Z".length() : "+hh:mm".length();
    StringBuilder formatted = new StringBuilder(capacity);
    padInt(formatted, calendar.get(Calendar.YEAR), "yyyy".length());
    formatted.append('-');
    padInt(formatted, calendar.get(Calendar.MONTH) + 1, "MM".length());
    formatted.append('-');
    padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), "dd".length());
    formatted.append('T');
    padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), "hh".length());
    formatted.append(':');
    padInt(formatted, calendar.get(Calendar.MINUTE), "mm".length());
    formatted.append(':');
    padInt(formatted, calendar.get(Calendar.SECOND), "ss".length());
    if (millis) {
      formatted.append('.');
      padInt(formatted, calendar.get(Calendar.MILLISECOND), "sss".length());
    }
    int offset = tz.getOffset(calendar.getTimeInMillis());
    if (offset != 0) {
      int hours = Math.abs((offset / (60 * 1000)) / 60);
      int minutes = Math.abs((offset / (60 * 1000)) % 60);
      formatted.append(offset < 0 ? '-' : '+');
      padInt(formatted, hours, "hh".length());
      formatted.append(':');
      padInt(formatted, minutes, "mm".length());
    } else {
      formatted.append('Z');
    }
    return formatted.toString();
  }
  public static Date parse(String date, ParsePosition pos) throws ParseException {
    Exception fail = null;
    try {
      int offset = pos.getIndex();
      int year = parseInt(date, offset, offset += 4);
      if (checkOffset(date, offset, '-')) {
        offset += 1;
      }
      int month = parseInt(date, offset, offset += 2);
      if (checkOffset(date, offset, '-')) {
        offset += 1;
      }
      int day = parseInt(date, offset, offset += 2);
      int hour = 0;
      int minutes = 0;
      int seconds = 0;
      int milliseconds = 0;
      boolean hasT = checkOffset(date, offset, 'T');
      if (!hasT && (date.length() <= offset)) {
        Calendar calendar = new GregorianCalendar(year, month - 1, day);
        calendar.setLenient(false);
        pos.setIndex(offset);
        return calendar.getTime();
      }
      if (hasT) {
        hour = parseInt(date, offset += 1, offset += 2);
        if (checkOffset(date, offset, ':')) {
          offset += 1;
        }
        minutes = parseInt(date, offset, offset += 2);
        if (checkOffset(date, offset, ':')) {
          offset += 1;
        }
        if (date.length() > offset) {
          char c = date.charAt(offset);
          if (c != 'Z' && c != '+' && c != '-') {
            seconds = parseInt(date, offset, offset += 2);
            if (seconds > 59 && seconds < 63) {
              seconds = 59;
            }
            if (checkOffset(date, offset, '.')) {
              offset += 1;
              int endOffset = indexOfNonDigit(date, offset + 1);
              int parseEndOffset = Math.min(endOffset, offset + 3);
              int fraction = parseInt(date, offset, parseEndOffset);
              switch (parseEndOffset - offset) {
                case 2:
                  milliseconds = fraction * 10;
                  break;
                case 1:
                  milliseconds = fraction * 100;
                  break;
                default:
                  milliseconds = fraction;
              }
              offset = endOffset;
            }
          }
        }
      }
      if (date.length() <= offset) {
        throw new IllegalArgumentException("No time zone indicator");
      }
      TimeZone timezone = null;
      char timezoneIndicator = date.charAt(offset);
      if (timezoneIndicator == 'Z') {
        timezone = TIMEZONE_UTC;
        offset += 1;
      } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
        String timezoneOffset = date.substring(offset);
        timezoneOffset = timezoneOffset.length() >= 5 ? timezoneOffset : timezoneOffset + "00";
        offset += timezoneOffset.length();
        if (timezoneOffset.equals("+0000") || timezoneOffset.equals("+00:00")) {
          timezone = TIMEZONE_UTC;
        } else {
          String timezoneId = "GMT" + timezoneOffset;
          timezone = TimeZone.getTimeZone(timezoneId);
          String act = timezone.getID();
          if (!act.equals(timezoneId)) {
            String cleaned = act.replace(":", "");
            if (!cleaned.equals(timezoneId)) {
              throw new IndexOutOfBoundsException(
                  "Mismatching time zone indicator: "
                      + timezoneId
                      + " given, resolves to "
                      + timezone.getID());
            }
          }
        }
      } else {
        throw new IndexOutOfBoundsException(
            "Invalid time zone indicator '" + timezoneIndicator + "'");
      }
      Calendar calendar = new GregorianCalendar(timezone);
      calendar.setLenient(false);
      calendar.set(Calendar.YEAR, year);
      calendar.set(Calendar.MONTH, month - 1);
      calendar.set(Calendar.DAY_OF_MONTH, day);
      calendar.set(Calendar.HOUR_OF_DAY, hour);
      calendar.set(Calendar.MINUTE, minutes);
      calendar.set(Calendar.SECOND, seconds);
      calendar.set(Calendar.MILLISECOND, milliseconds);
      pos.setIndex(offset);
      return calendar.getTime();
    } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
      fail = e;
    }
    String input = (date == null) ? null : ('"' + date + '"');
    String msg = fail.getMessage();
    if (msg == null || msg.isEmpty()) {
      msg = "(" + fail.getClass().getName() + ")";
    }
    ParseException ex =
        new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
    ex.initCause(fail);
    throw ex;
  }
  private static boolean checkOffset(String value, int offset, char expected) {
    return (offset < value.length()) && (value.charAt(offset) == expected);
  }
  private static int parseInt(String value, int beginIndex, int endIndex)
      throws NumberFormatException {
    if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
      throw new NumberFormatException(value);
    }
    int i = beginIndex;
    int result = 0;
    int digit;
    if (i < endIndex) {
      digit = Character.digit(value.charAt(i++), 10);
      if (digit < 0) {
        throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
      }
      result = -digit;
    }
    while (i < endIndex) {
      digit = Character.digit(value.charAt(i++), 10);
      if (digit < 0) {
        throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
      }
      result *= 10;
      result -= digit;
    }
    return -result;
  }
  private static void padInt(StringBuilder buffer, int value, int length) {
    String strValue = Integer.toString(value);
    for (int i = length - strValue.length(); i > 0; i--) {
      buffer.append('0');
    }
    buffer.append(strValue);
  }
  private static int indexOfNonDigit(String string, int offset) {
    for (int i = offset; i < string.length(); i++) {
      char c = string.charAt(i);
      if (c < '0' || c > '9') {
        return i;
      }
    }
    return string.length();
  }
}