package cli;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
public class CommandLine implements Serializable {
    public static final class Builder implements Supplier<CommandLine> {
        static final Consumer<Option> DEPRECATED_HANDLER =
                o -> System.out.println(o.toDeprecatedString());
        public final List<String> args = new LinkedList<>();
        public final List<Option> options = new ArrayList<>();
        public Consumer<Option> deprecatedHandler = DEPRECATED_HANDLER;
        @Deprecated
        public Builder() {
        }
        public Builder addArg(final String arg) {
            if (arg != null) {
                args.add(arg);
            }
            return this;
        }
        public Builder addOption(final Option option) {
            if (option != null) {
                options.add(option);
            }
            return this;
        }
        @Deprecated
        public CommandLine build() {
            return get();
        }
        @Override
        public CommandLine get() {
            return new CommandLine(args, options, deprecatedHandler);
        }
        public Builder setDeprecatedHandler(final Consumer<Option> deprecatedHandler) {
            this.deprecatedHandler = deprecatedHandler;
            return this;
        }
    }
    private static final long serialVersionUID = 1L;
    public static Builder builder() {
        return new Builder();
    }
    public final List<String> args;
    public final List<Option> options;
    public final transient Consumer<Option> deprecatedHandler;
    protected CommandLine() {
        this(new LinkedList<>(), new ArrayList<>(), Builder.DEPRECATED_HANDLER);
    }
    public CommandLine(
            final List<String> args,
            final List<Option> options,
            final Consumer<Option> deprecatedHandler) {
        this.args = Objects.requireNonNull(args, "args");
        this.options = Objects.requireNonNull(options, "options");
        this.deprecatedHandler = deprecatedHandler;
    }
    protected void addArg(final String arg) {
        if (arg != null) {
            args.add(arg);
        }
    }
    protected void addOption(final Option option) {
        if (option != null) {
            options.add(option);
        }
    }

    private <T> T get(final Supplier<T> supplier) {
        return supplier == null ? null : supplier.get();
    }
    public List<String> getArgList() {
        return args;
    }
    public String[] getArgs() {
        return args.toArray(Util.EMPTY_STRING_ARRAY);
    }
    @Deprecated
    public Object getOptionObject(final char optionChar) {
        return getOptionObject(String.valueOf(optionChar));
    }
    @Deprecated
    public Object getOptionObject(final String optionName) {
        try {
            return getParsedOptionValue(optionName);
        } catch (final ParseException pe) {
            System.err.println(
                    "Exception found converting " + optionName + " to desired type: " + pe.getMessage());
            return null;
        }
    }
    public Properties getOptionProperties(final Option option) {
        final Properties props = new Properties();
        for (final Option processedOption : options) {
            if (processedOption.equals(option)) {
                processPropertiesFromValues(props, processedOption.getValuesList());
            }
        }
        return props;
    }
    public Properties getOptionProperties(final String optionName) {
        final Properties props = new Properties();
        for (final Option option : options) {
            if (optionName.equals(option.getOpt()) || optionName.equals(option.getLongOpt())) {
                processPropertiesFromValues(props, option.getValuesList());
            }
        }
        return props;
    }
    public Option[] getOptions() {
        return options.toArray(Option.EMPTY_ARRAY);
    }
    public String getOptionValue(final char optionChar) {
        return getOptionValue(String.valueOf(optionChar));
    }
    public String getOptionValue(final char optionChar, final String defaultValue) {
        return getOptionValue(String.valueOf(optionChar), () -> defaultValue);
    }
    public String getOptionValue(final char optionChar, final Supplier<String> defaultValue) {
        return getOptionValue(String.valueOf(optionChar), defaultValue);
    }
    public String getOptionValue(final Option option) {
        final String[] values = getOptionValues(option);
        return values == null ? null : values[0];
    }
    public String getOptionValue(final Option option, final String defaultValue) {
        return getOptionValue(option, () -> defaultValue);
    }
    public String getOptionValue(final Option option, final Supplier<String> defaultValue) {
        final String answer = getOptionValue(option);
        return answer != null ? answer : get(defaultValue);
    }
    public String getOptionValue(final OptionGroup optionGroup) {
        final String[] values = getOptionValues(optionGroup);
        return values == null ? null : values[0];
    }
    public String getOptionValue(final OptionGroup optionGroup, final String defaultValue) {
        return getOptionValue(optionGroup, () -> defaultValue);
    }
    public String getOptionValue(final OptionGroup optionGroup, final Supplier<String> defaultValue) {
        final String answer = getOptionValue(optionGroup);
        return answer != null ? answer : get(defaultValue);
    }
    public String getOptionValue(final String optionName) {
        return getOptionValue(resolveOption(optionName));
    }
    public String getOptionValue(final String optionName, final String defaultValue) {
        return getOptionValue(resolveOption(optionName), () -> defaultValue);
    }
    public String getOptionValue(final String optionName, final Supplier<String> defaultValue) {
        return getOptionValue(resolveOption(optionName), defaultValue);
    }
    public String[] getOptionValues(final char optionChar) {
        return getOptionValues(String.valueOf(optionChar));
    }
    public String[] getOptionValues(final Option option) {
        if (option == null) {
            return null;
        }
        final List<String> values = new ArrayList<>();
        for (final Option processedOption : options) {
            if (processedOption.equals(option)) {
                if (option.isDeprecated()) {
                    handleDeprecated(option);
                }
                values.addAll(processedOption.getValuesList());
            }
        }
        return values.isEmpty() ? null : values.toArray(Util.EMPTY_STRING_ARRAY);
    }
    public String[] getOptionValues(final OptionGroup optionGroup) {
        if (optionGroup == null || !optionGroup.isSelected()) {
            return null;
        }
        return getOptionValues(optionGroup.getSelected());
    }
    public String[] getOptionValues(final String optionName) {
        return getOptionValues(resolveOption(optionName));
    }
    public <T> T getParsedOptionValue(final char optionChar) throws ParseException {
        return getParsedOptionValue(String.valueOf(optionChar));
    }
    public <T> T getParsedOptionValue(final char optionChar, final Supplier<T> defaultValue)
            throws ParseException {
        return getParsedOptionValue(String.valueOf(optionChar), defaultValue);
    }
    public <T> T getParsedOptionValue(final char optionChar, final T defaultValue)
            throws ParseException {
        return getParsedOptionValue(String.valueOf(optionChar), defaultValue);
    }
    public <T> T getParsedOptionValue(final Option option) throws ParseException {
        return getParsedOptionValue(option, () -> null);
    }
    @SuppressWarnings("unchecked")
    public <T> T getParsedOptionValue(final Option option, final Supplier<T> defaultValue)
            throws ParseException {
        if (option == null) {
            return get(defaultValue);
        }
        final String res = getOptionValue(option);
        try {
            if (res == null) {
                return get(defaultValue);
            }
            return (T) option.getConverter().apply(res);
        } catch (final Exception e) {
            throw ParseException.wrap(e);
        }
    }
    public <T> T getParsedOptionValue(final Option option, final T defaultValue)
            throws ParseException {
        return getParsedOptionValue(option, () -> defaultValue);
    }
    public <T> T getParsedOptionValue(final OptionGroup optionGroup) throws ParseException {
        return getParsedOptionValue(optionGroup, () -> null);
    }
    public <T> T getParsedOptionValue(final OptionGroup optionGroup, final Supplier<T> defaultValue)
            throws ParseException {
        if (optionGroup == null || !optionGroup.isSelected()) {
            return get(defaultValue);
        }
        return getParsedOptionValue(optionGroup.getSelected(), defaultValue);
    }
    public <T> T getParsedOptionValue(final OptionGroup optionGroup, final T defaultValue)
            throws ParseException {
        return getParsedOptionValue(optionGroup, () -> defaultValue);
    }
    public <T> T getParsedOptionValue(final String optionName) throws ParseException {
        return getParsedOptionValue(resolveOption(optionName));
    }
    public <T> T getParsedOptionValue(final String optionName, final Supplier<T> defaultValue)
            throws ParseException {
        return getParsedOptionValue(resolveOption(optionName), defaultValue);
    }
    public <T> T getParsedOptionValue(final String optionName, final T defaultValue)
            throws ParseException {
        return getParsedOptionValue(resolveOption(optionName), defaultValue);
    }
    public <T> T[] getParsedOptionValues(final char optionChar) throws ParseException {
        return getParsedOptionValues(String.valueOf(optionChar));
    }
    public <T> T[] getParsedOptionValues(final char optionChar, final Supplier<T[]> defaultValue)
            throws ParseException {
        return getParsedOptionValues(String.valueOf(optionChar), defaultValue);
    }
    public <T> T[] getParsedOptionValues(final char optionChar, final T[] defaultValue)
            throws ParseException {
        return getParsedOptionValues(String.valueOf(optionChar), defaultValue);
    }
    public <T> T[] getParsedOptionValues(final Option option) throws ParseException {
        return getParsedOptionValues(option, () -> null);
    }
    @SuppressWarnings("unchecked")
    public <T> T[] getParsedOptionValues(final Option option, final Supplier<T[]> defaultValue)
            throws ParseException {
        if (option == null) {
            return get(defaultValue);
        }
        Class<? extends T> clazz = (Class<? extends T>) option.getType();
        String[] values = getOptionValues(option);
        if (values == null) {
            return get(defaultValue);
        }
        T[] result = (T[]) Array.newInstance(clazz, values.length);
        try {
            for (int i = 0; i < values.length; i++) {
                result[i] = clazz.cast(option.getConverter().apply(values[i]));
            }
            return result;
        } catch (final Exception t) {
            throw ParseException.wrap(t);
        }
    }
    public <T> T[] getParsedOptionValues(final Option option, final T[] defaultValue)
            throws ParseException {
        return getParsedOptionValues(option, () -> defaultValue);
    }
    public <T> T[] getParsedOptionValues(final OptionGroup optionGroup) throws ParseException {
        return getParsedOptionValues(optionGroup, () -> null);
    }
    public <T> T[] getParsedOptionValues(
            final OptionGroup optionGroup, final Supplier<T[]> defaultValue) throws ParseException {
        if (optionGroup == null || !optionGroup.isSelected()) {
            return get(defaultValue);
        }
        return getParsedOptionValues(optionGroup.getSelected(), defaultValue);
    }
    public <T> T[] getParsedOptionValues(final OptionGroup optionGroup, final T[] defaultValue)
            throws ParseException {
        return getParsedOptionValues(optionGroup, () -> defaultValue);
    }
    public <T> T[] getParsedOptionValues(final String optionName) throws ParseException {
        return getParsedOptionValues(resolveOption(optionName));
    }
    public <T> T[] getParsedOptionValues(final String optionName, final Supplier<T[]> defaultValue)
            throws ParseException {
        return getParsedOptionValues(resolveOption(optionName), defaultValue);
    }
    public <T> T[] getParsedOptionValues(final String optionName, final T[] defaultValue)
            throws ParseException {
        return getParsedOptionValues(resolveOption(optionName), defaultValue);
    }
    private void handleDeprecated(final Option option) {
        if (deprecatedHandler != null) {
            deprecatedHandler.accept(option);
        }
    }
    public boolean hasOption(final char optionChar) {
        return hasOption(String.valueOf(optionChar));
    }
    public boolean hasOption(final Option option) {
        final boolean result = options.contains(option);
        if (result && option.isDeprecated()) {
            handleDeprecated(option);
        }
        return result;
    }
    public boolean hasOption(final OptionGroup optionGroup) {
        if (optionGroup == null || !optionGroup.isSelected()) {
            return false;
        }
        return hasOption(optionGroup.getSelected());
    }
    public boolean hasOption(final String optionName) {
        return hasOption(resolveOption(optionName));
    }
    public Iterator<Option> iterator() {
        return options.iterator();
    }
    private void processPropertiesFromValues(final Properties props, final List<String> values) {
        for (int i = 0; i < values.size(); i += 2) {
            if (i + 1 < values.size()) {
                props.put(values.get(i), values.get(i + 1));
            } else {
                props.put(values.get(i), "true");
            }
        }
    }
    private Option resolveOption(final String optionName) {
        final String actual = Util.stripLeadingHyphens(optionName);
        if (actual != null) {
            for (final Option option : options) {
                if (actual.equals(option.getOpt()) || actual.equals(option.getLongOpt())) {
                    return option;
                }
            }
        }
        return null;
    }
}

