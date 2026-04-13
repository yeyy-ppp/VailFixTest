package gson.internal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
public class PreJava9DateFormatProvider {
  private PreJava9DateFormatProvider() {}
  public static DateFormat getUsDateTimeFormat(int dateStyle, int timeStyle) {
    String pattern =
        getDatePartOfDateTimePattern(dateStyle) + " " + getTimePartOfDateTimePattern(timeStyle);
    return new SimpleDateFormat(pattern, Locale.US);
  }
  private static String getDatePartOfDateTimePattern(int dateStyle) {
    switch (dateStyle) {
      case DateFormat.SHORT:
        return "M/d/yy";
      case DateFormat.MEDIUM:
        return "MMM d, yyyy";
      case DateFormat.LONG:
        return "MMMM d, yyyy";
      case DateFormat.FULL:
        return "EEEE, MMMM d, yyyy";
      default:
        throw new IllegalArgumentException("Unknown DateFormat style: " + dateStyle);
    }
  }
  private static String getTimePartOfDateTimePattern(int timeStyle) {
    switch (timeStyle) {
      case DateFormat.SHORT:
        return "h:mm a";
      case DateFormat.MEDIUM:
        return "h:mm:ss a";
      case DateFormat.FULL:
      case DateFormat.LONG:
        return "h:mm:ss a z";
      default:
        throw new IllegalArgumentException("Unknown DateFormat style: " + timeStyle);
    }
  }
}