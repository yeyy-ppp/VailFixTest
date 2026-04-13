package cli.help;

import cli.DeprecatedAttributes;
import cli.Option;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OptionFormatter {
    public static final class Builder implements Supplier<OptionFormatter> {
        private final String[] argNameDelimiters;
        private String defaultArgName;
        private Function<Option, String> deprecatedFormatFunction;
        private String longOptPrefix;
        private String optPrefix;
        private String optSeparator;
        private String optArgSeparator;
        private final String[] optionalDelimiters;
        private BiFunction<OptionFormatter, Boolean, String> syntaxFormatFunction;
        private Builder() {
            argNameDelimiters = Arrays.copyOf(DEFAULT_ARG_NAME_DELIMITERS, 2);
            defaultArgName = DEFAULT_ARG_NAME;
            deprecatedFormatFunction = NO_DEPRECATED_FORMAT;
            longOptPrefix = DEFAULT_LONG_OPT_PREFIX;
            optPrefix = DEFAULT_OPT_PREFIX;
            optSeparator = DEFAULT_OPT_SEPARATOR;
            optArgSeparator = DEFAULT_OPT_ARG_SEPARATOR;
            optionalDelimiters = Arrays.copyOf(DEFAULT_OPTIONAL_DELIMITERS, 2);
        }
        public Builder(final OptionFormatter optionFormatter) {
            optionalDelimiters = Arrays.copyOf(optionFormatter.optionalDelimiters, 2);
            argNameDelimiters = Arrays.copyOf(optionFormatter.argNameDelimiters, 2);
            defaultArgName = optionFormatter.defaultArgName;
            optPrefix = optionFormatter.optPrefix;
            longOptPrefix = optionFormatter.longOptPrefix;
            optSeparator = optionFormatter.optSeparator;
            deprecatedFormatFunction = optionFormatter.deprecatedFormatFunction;
            syntaxFormatFunction = optionFormatter.syntaxFormatFunction;
        }
        public OptionFormatter build(final Option option) {
            return new OptionFormatter(option, this);
        }
        @Override
        public OptionFormatter get() {
            return null;
        }
        public Builder setArgumentNameDelimiters(final String begin, final String end) {
            this.argNameDelimiters[0] = Util.defaultValue(begin, "");
            this.argNameDelimiters[1] = Util.defaultValue(end, "");
            return this;
        }
        public Builder setDefaultArgName(final String name) {
            this.defaultArgName = Util.defaultValue(name, DEFAULT_ARG_NAME);
            return this;
        }
        public Builder setDeprecatedFormatFunction(
                final Function<Option, String> deprecatedFormatFunction) {
            this.deprecatedFormatFunction = deprecatedFormatFunction;
            return this;
        }
        public Builder setLongOptPrefix(final String prefix) {
            this.longOptPrefix = Util.defaultValue(prefix, "");
            return this;
        }
        public Builder setOptArgSeparator(final String optArgSeparator) {
            this.optArgSeparator = Util.defaultValue(optArgSeparator, "");
            return this;
        }
        public Builder setOptionalDelimiters(final String begin, final String end) {
            this.optionalDelimiters[0] = Util.defaultValue(begin, "");
            this.optionalDelimiters[1] = Util.defaultValue(end, "");
            return this;
        }
        public Builder setOptPrefix(final String optPrefix) {
            this.optPrefix = Util.defaultValue(optPrefix, "");
            return this;
        }
        public Builder setOptSeparator(final String optSeparator) {
            this.optSeparator = Util.defaultValue(optSeparator, "");
            return this;
        }
        public Builder setSyntaxFormatFunction(
                final BiFunction<OptionFormatter, Boolean, String> syntaxFormatFunction) {
            this.syntaxFormatFunction = syntaxFormatFunction;
            return this;
        }
        public String toArgName(final String argName) {
            return argNameDelimiters[0] + Util.defaultValue(argName, "") + argNameDelimiters[1];
        }

    }
    private static final String[] DEFAULT_OPTIONAL_DELIMITERS = {"[", "]"};
    private static final String[] DEFAULT_ARG_NAME_DELIMITERS = {"<", ">"};
    public static final String DEFAULT_ARG_NAME = "arg";
    public static final Function<Option, String> SIMPLE_DEPRECATED_FORMAT =
            o -> "[Deprecated] " + Util.defaultValue(o.getDescription(), "");
    public static final Function<Option, String> COMPLEX_DEPRECATED_FORMAT =
            o -> {
                final StringBuilder sb = new StringBuilder("[Deprecated");
                final DeprecatedAttributes attr = o.getDeprecated();
                if (attr.isForRemoval()) {
                    sb.append(" for removal");
                }
                if (!Util.isEmpty(attr.getSince())) {
                    sb.append(" since ").append(attr.getSince());
                }
                if (!Util.isEmpty(attr.getDescription())) {
                    sb.append(". ").append(attr.getDescription());
                }
                sb.append("]");
                if (!Util.isEmpty(o.getDescription())) {
                    sb.append(" ").append(o.getDescription());
                }
                return sb.toString();
            };
    public static final Function<Option, String> NO_DEPRECATED_FORMAT =
            o -> Util.defaultValue(o.getDescription(), "");
    public static final String DEFAULT_SYNTAX_PREFIX = "usage: ";
    public static final String DEFAULT_OPT_PREFIX = "-";
    public static final String DEFAULT_LONG_OPT_PREFIX = "--";
    public static final String DEFAULT_OPT_SEPARATOR = ", ";
    public static final String DEFAULT_OPT_ARG_SEPARATOR = " ";
    public static Builder builder() {
        return new Builder();
    }
    public static OptionFormatter from(final Option option) {
        return new Builder().build(option);
    }
    private final String[] argNameDelimiters;
    private final String defaultArgName;
    private final Function<Option, String> deprecatedFormatFunction;
    private final String longOptPrefix;
    private final String optPrefix;
    private final String optSeparator;
    private final String optArgSeparator;
    private final String[] optionalDelimiters;
    private final BiFunction<OptionFormatter, Boolean, String> syntaxFormatFunction;
    private final Option option;
    private OptionFormatter(final Option option, final Builder builder) {
        this.optionalDelimiters = builder.optionalDelimiters;
        this.argNameDelimiters = builder.argNameDelimiters;
        this.defaultArgName = builder.defaultArgName;
        this.optPrefix = builder.optPrefix;
        this.longOptPrefix = builder.longOptPrefix;
        this.optSeparator = builder.optSeparator;
        this.optArgSeparator = builder.optArgSeparator;
        this.deprecatedFormatFunction = builder.deprecatedFormatFunction;
        this.option = option;
        this.syntaxFormatFunction =
                builder.syntaxFormatFunction != null
                        ? builder.syntaxFormatFunction
                        : (o, required) -> {
                    final StringBuilder buff = new StringBuilder();
                    final String argName = o.getArgName();
                    buff.append(Util.defaultValue(o.getOpt(), o.getLongOpt()));
                    if (!Util.isEmpty(argName)) {
                        buff.append(optArgSeparator).append(argName);
                    }
                    final boolean requiredFlg = required == null ? o.isRequired() : required;
                    return requiredFlg ? buff.toString() : o.toOptional(buff.toString());
                };
    }
    public String getArgName() {
        return option.hasArg()
                ? argNameDelimiters[0]
                + Util.defaultValue(option.getArgName(), defaultArgName)
                + argNameDelimiters[1]
                : "";
    }
    public String getBothOpt() {
        final String lOpt = getLongOpt();

        final StringBuilder sb = new StringBuilder(getOpt());
        if (sb.length() > 0 && !Util.isEmpty(lOpt)) {
            sb.append(optSeparator);
        }
        return sb.append(getLongOpt()).toString();
    }
    public String getDescription() {
        return option.isDeprecated()
                ? deprecatedFormatFunction.apply(option)
                : Util.defaultValue(option.getDescription(), "");
    }
    public String getLongOpt() {
        return Util.isEmpty(option.getLongOpt()) ? "" : longOptPrefix + option.getLongOpt();
    }
    public String getOpt() {
        return Util.isEmpty(option.getOpt()) ? "" : optPrefix + option.getOpt();
    }
    public String getSince() {
        return Util.defaultValue(option.getSince(), "--");
    }
    public boolean isRequired() {
        return option.isRequired();
    }
    public String toOptional(final String text) {
        if (Util.isEmpty(text)) {
            return "";
        }
        return optionalDelimiters[0] + text + optionalDelimiters[1];
    }
    public String toSyntaxOption() {
        return toSyntaxOption(isRequired());
    }
    public String toSyntaxOption(final boolean isRequired) {
        return syntaxFormatFunction.apply(this, isRequired);
    }
}
