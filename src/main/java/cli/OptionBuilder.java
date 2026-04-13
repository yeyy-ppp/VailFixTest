package cli;
@Deprecated
public final class OptionBuilder {
    private static String longOption;
    private static String description;
    private static String argName;
    private static boolean required;
    private static int argCount = Option.UNINITIALIZED;
    private static Class<?> type;
    private static boolean optionalArg;
    private static char valueSeparator;
    private static final OptionBuilder INSTANCE = new OptionBuilder();
    static {
        reset();
    }
    public static Option create() throws IllegalArgumentException {
        if (longOption == null) {
            reset();
            throw new IllegalArgumentException("must specify longopt");
        }

        return create(null);
    }
    public static Option create(final char opt) throws IllegalArgumentException {
        return create(String.valueOf(opt));
    }
    public static Option create(final String opt) throws IllegalArgumentException {
        Option option;
        try {
            option = new Option(opt, description);
            option.setLongOpt(longOption);
            option.setRequired(required);
            option.setOptionalArg(optionalArg);
            option.setArgs(argCount);
            option.setType(type);
            option.setConverter(TypeHandler.getDefault().getConverter(type));
            option.setValueSeparator(valueSeparator);
            option.setArgName(argName);
        } finally {
            reset();
        }
        return option;
    }
    public static OptionBuilder hasArg() {
        argCount = 1;
        return INSTANCE;
    }
    public static OptionBuilder hasArg(final boolean hasArg) {
        argCount = hasArg ? 1 : Option.UNINITIALIZED;
        return INSTANCE;
    }
    public static OptionBuilder hasArgs() {
        argCount = Option.UNLIMITED_VALUES;
        return INSTANCE;
    }
    public static OptionBuilder hasArgs(final int num) {
        argCount = num;
        return INSTANCE;
    }
    public static OptionBuilder hasOptionalArg() {
        argCount = 1;
        optionalArg = true;
        return INSTANCE;
    }
    public static OptionBuilder hasOptionalArgs() {
        argCount = Option.UNLIMITED_VALUES;
        optionalArg = true;
        return INSTANCE;
    }
    public static OptionBuilder hasOptionalArgs(final int numArgs) {
        argCount = numArgs;
        optionalArg = true;
        return INSTANCE;
    }
    public static OptionBuilder isRequired() {
        required = true;
        return INSTANCE;
    }
    public static OptionBuilder isRequired(final boolean newRequired) {
        required = newRequired;
        return INSTANCE;
    }
    private static void reset() {
        description = null;
        argName = null;
        longOption = null;
        type = String.class;
        required = false;
        argCount = Option.UNINITIALIZED;
        optionalArg = false;
        valueSeparator = (char) 0;
    }
    public static OptionBuilder withArgName(final String name) {
        argName = name;
        return INSTANCE;
    }
    public static OptionBuilder withDescription(final String newDescription) {
        description = newDescription;
        return INSTANCE;
    }
    public static OptionBuilder withLongOpt(final String newLongopt) {
        longOption = newLongopt;
        return INSTANCE;
    }
    public static OptionBuilder withType(final Class<?> newType) {
        type = newType;
        return INSTANCE;
    }
    @Deprecated
    public static OptionBuilder withType(final Object newType) {
        return withType((Class<?>) newType);
    }
    public static OptionBuilder withValueSeparator() {
        valueSeparator = Char.EQUAL;
        return INSTANCE;
    }
    public static OptionBuilder withValueSeparator(final char sep) {
        valueSeparator = sep;
        return INSTANCE;
    }
    private OptionBuilder() {
    }
}
