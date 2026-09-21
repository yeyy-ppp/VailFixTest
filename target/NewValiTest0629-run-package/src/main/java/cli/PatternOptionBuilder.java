package cli;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;
public class PatternOptionBuilder {
    @Deprecated
    public PatternOptionBuilder() {
    }
    public static final Class<String> STRING_VALUE = String.class;
    public static final Class<Object> OBJECT_VALUE = Object.class;
    public static final Class<Number> NUMBER_VALUE = Number.class;
    public static final Class<Date> DATE_VALUE = Date.class;
    public static final Class<?> CLASS_VALUE = Class.class;
    public static final Class<FileInputStream> EXISTING_FILE_VALUE = FileInputStream.class;
    public static final Class<File> FILE_VALUE = File.class;
    public static final Class<File[]> FILES_VALUE = File[].class;
    public static final Class<URL> URL_VALUE = URL.class;
    private static final Converter<?, UnsupportedOperationException> UNSUPPORTED =
            s -> {
                throw new UnsupportedOperationException("Not yet implemented");
            };
    @Deprecated
    public static Object getValueClass(final char ch) {
        return getValueType(ch);
    }
    public static Class<?> getValueType(final char ch) {
        switch (ch) {
            case '@':
                return OBJECT_VALUE;
            case ':':
                return STRING_VALUE;
            case '%':
                return NUMBER_VALUE;
            case '+':
                return CLASS_VALUE;
            case '#':
                return DATE_VALUE;
            case '<':
                return EXISTING_FILE_VALUE;
            case '>':
                return FILE_VALUE;
            case '*':
                return FILES_VALUE;
            case '/':
                return URL_VALUE;
        }
        return null;
    }
    public static boolean isValueCode(final char ch) {
        return ch == '@' || ch == ':' || ch == '%' || ch == '+' || ch == '#' || ch == '<' || ch == '>'
                || ch == '*' || ch == '/' || ch == '!';
    }
    public static Options parsePattern(final String pattern) {
        char opt = Char.SP;
        boolean required = false;
        Class<?> type = null;
        Converter<?, ?> converter = Converter.DEFAULT;
        final Options options = new Options();
        for (int i = 0; i < pattern.length(); i++) {
            final char ch = pattern.charAt(i);
            if (!isValueCode(ch)) {
                if (opt != Char.SP) {
                    final Option option =
                            Option.builder(String.valueOf(opt))
                                    .hasArg(type != null)
                                    .required(required)
                                    .type(type)
                                    .converter(converter)
                                    .build();
                    options.addOption(option);
                    required = false;
                    type = null;
                    converter = Converter.DEFAULT;
                }
                opt = ch;
            } else if (ch == '!') {
                required = true;
            } else {
                type = getValueType(ch);
                final Map<Class<?>, Converter<?, ? extends Throwable>> map = TypeHandler.createDefaultMap();
                map.put(FILES_VALUE, unsupported());
                converter = new TypeHandler(map).getConverter(getValueType(ch));
            }
        }
        if (opt != Char.SP) {
            final Option option =
                    Option.builder(String.valueOf(opt))
                            .hasArg(type != null)
                            .required(required)
                            .type(type)
                            .build();
            options.addOption(option);
        }
        return options;
    }
    @SuppressWarnings("unchecked")
    static <T> T unsupported() {
        return (T) UNSUPPORTED;
    }
}
