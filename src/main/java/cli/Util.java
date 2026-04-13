package cli;
public final class Util {
    static final String[] EMPTY_STRING_ARRAY = {};
    static boolean isEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }
    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }
    static String stripLeadingAndTrailingQuotes(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final int length = str.length();
        if (length > 1
                && str.startsWith("\"")
                && str.endsWith("\"")
                && str.substring(1, length - 1).indexOf('"') == -1) {
            return str.substring(1, length - 1);
        }
        return str;
    }
    static String stripLeadingHyphens(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        if (str.startsWith("--")) {
            return str.substring(2);
        }
        if (str.startsWith("-")) {
            return str.substring(1);
        }
        return str;
    }
    private Util() {
    }
}

