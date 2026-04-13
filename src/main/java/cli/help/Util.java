package cli.help;
import java.util.Arrays;

public final class Util {
    private static final int NOT_FOUND = -1;
    public static <T extends CharSequence> T defaultValue(final T str, final T defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    public static int indexOfNonWhitespace(final CharSequence text, final int startPos) {
        if (isEmpty(text)) {
            return NOT_FOUND;
        }
        int idx = startPos;
        while (idx < text.length() && isWhitespace(text.charAt(idx))) {
            idx++;
        }
        return idx < text.length() ? idx : NOT_FOUND;
    }
    public static boolean isEmpty(final CharSequence str) {
        return str == null || str.length() == 0;
    }
    public static boolean isWhitespace(final char c) {
        return Character.isWhitespace(c) || Character.PARAGRAPH_SEPARATOR == c;
    }
    public static String ltrim(final String s) {
        final int pos = indexOfNonWhitespace(s, 0);
        return pos == NOT_FOUND ? "" : s.substring(pos);
    }
    public static String repeat(final int len, final char fillChar) {
        if (len < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + len);
        }
        if (len == 0) {
            return "";
        }
        // 防止超大数组导致 OutOfMemoryError
        if (len > 100000) {
            throw new IllegalArgumentException("Length too large (max 100000): " + len);
        }
        final char[] padding = new char[len];
        Arrays.fill(padding, fillChar);
        return new String(padding);
    }
    public static String repeatSpace(final int len) {
        return repeat(len, ' ');
    }
    public static String rtrim(final String s) {
        if (isEmpty(s)) {
            return s;
        }
        int pos = s.length();
        while (pos > 0 && isWhitespace(s.charAt(pos - 1))) {
            --pos;
        }
        return s.substring(0, pos);
    }
    private Util() {
    }
}