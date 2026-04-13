package lang3;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.surefire.shared.lang3.Streams;

public class CharSetUtils {
    public static boolean containsAny(final String str, final String... set) {
        if (isEmpty(str, set)) {
            return false;
        }
        final lang3.CharSet chars = lang3.CharSet.getInstance(set);
        for (final char c : str.toCharArray()) {
            if (chars.contains(c)) {
                return true;
            }
        }
        return false;
    }
    public static int count(final String str, final String... set) {
        if (isEmpty(str, set)) {
            return 0;
        }
        final lang3.CharSet chars = lang3.CharSet.getInstance(set);
        int count = 0;
        for (final char c : str.toCharArray()) {
            if (chars.contains(c)) {
                count++;
            }
        }
        return count;
    }
    public static String delete(final String str, final String... set) {
        if (isEmpty(str, set)) {
            return str;
        }
        return modify(str, set, false);
    }
    private static boolean isEmpty(final String str, final String... set) {
        return StringUtils.isEmpty(str);
    }


    public static String keep(final String str, final String... set) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return modify(str, set, true);
    }
    private static String modify(final String str, final String[] set, final boolean expect) {
        final lang3.CharSet chars = lang3.CharSet.getInstance(set);
        final StringBuilder buffer = new StringBuilder(str.length());
        final char[] chrs = str.toCharArray();
        for (final char chr : chrs) {
            if (chars.contains(chr) == expect) {
                buffer.append(chr);
            }
        }
        return buffer.toString();
    }
    public static String squeeze(final String str, final String... set) {
        if (isEmpty(str, set)) {
            return str;
        }
        final CharSet chars = CharSet.getInstance(set);
        final StringBuilder buffer = new StringBuilder(str.length());
        final char[] chrs = str.toCharArray();
        final int sz = chrs.length;
        char lastChar = chrs[0];
        char ch;
        Character inChars = null;
        Character notInChars = null;
        buffer.append(lastChar);
        for (int i = 1; i < sz; i++) {
            ch = chrs[i];
            if (ch == lastChar) {
                if (inChars != null && ch == inChars) {
                    continue;
                }
                if (notInChars == null || ch != notInChars) {
                    if (chars.contains(ch)) {
                        inChars = ch;
                        continue;
                    }
                    notInChars = ch;
                }
            }
            buffer.append(ch);
            lastChar = ch;
        }
        return buffer.toString();
    }
    @Deprecated
    public CharSetUtils() {
    }
}