package cli;
public final class OptionValidator {
    static final char[] ADDITIONAL_OPTION_CHARS = {'?', '@'};
    static final char[] ADDITIONAL_LONG_CHARS = {'-'};
    private static boolean isValidChar(final char c) {
        return Character.isJavaIdentifierPart(c) || search(ADDITIONAL_LONG_CHARS, c);
    }
    private static boolean isValidOpt(final char c) {
        return Character.isJavaIdentifierPart(c) || search(ADDITIONAL_OPTION_CHARS, c);
    }
    private static boolean search(final char[] chars, final char c) {
        for (final char a : chars) {
            if (a == c) {
                return true;
            }
        }
        return false;
    }
    public static String validate(final String option) throws IllegalArgumentException {
        if (option == null) {
            return null;
        }
        if (option.isEmpty()) {
            throw new IllegalArgumentException("Empty option name.");
        }
        final char[] chars = option.toCharArray();
        final char ch0 = chars[0];
        if (!isValidOpt(ch0)) {
            throw new IllegalArgumentException(String.format("Illegal option name '%s'.", ch0));
        }
        if (option.length() > 1) {
            for (int i = 1; i < chars.length; i++) {
                final char ch = chars[i];
                if (!isValidChar(ch)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "The option '%s' contains an illegal " + "character : '%s'.", option, ch));
                }
            }
        }
        return option;
    }
}

