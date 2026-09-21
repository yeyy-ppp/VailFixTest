package cli;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public class Option implements Cloneable, Serializable {
    public static final class Builder {
        private static final Class<String> DEFAULT_TYPE = String.class;
        private static Class<?> toType(final Class<?> type) {
            return type != null ? type : DEFAULT_TYPE;
        }
        private int argCount = UNINITIALIZED;
        private String argName;
        private Converter<?, ?> converter;
        private DeprecatedAttributes deprecated;
        private String description;
        private String longOption;
        private String option;
        private boolean optionalArg;
        private boolean required;
        private String since;
        private Class<?> type = DEFAULT_TYPE;
        private char valueSeparator;
        Builder(final String option) throws IllegalArgumentException {
            option(option);
        }
        public Builder argName(final String argName) {
            this.argName = argName;
            return this;
        }
        public Option build() {
            if (option == null && longOption == null) {
                throw new IllegalArgumentException("Either opt or longOpt must be specified");
            }
            return new Option(this);
        }
        public Builder converter(final Converter<?, ?> converter) {
            this.converter = converter;
            return this;
        }
        public Builder deprecated() {
            return deprecated(DeprecatedAttributes.DEFAULT);
        }
        public Builder deprecated(final DeprecatedAttributes deprecated) {
            this.deprecated = deprecated;
            return this;
        }
        public Builder desc(final String description) {
            this.description = description;
            return this;
        }
        public Builder hasArg() {
            return hasArg(true);
        }
        public Builder hasArg(final boolean hasArg) {
            argCount = hasArg ? 1 : UNINITIALIZED;
            return this;
        }
        public Builder hasArgs() {
            argCount = UNLIMITED_VALUES;
            return this;
        }
        public Builder longOpt(final String longOption) {
            this.longOption = longOption;
            return this;
        }
        public Builder numberOfArgs(final int argCount) {
            this.argCount = argCount;
            return this;
        }
        public Builder option(final String option) throws IllegalArgumentException {
            this.option = OptionValidator.validate(option);
            return this;
        }
        public Builder optionalArg(final boolean optionalArg) {
            if (optionalArg && argCount == UNINITIALIZED) {
                argCount = 1;
            }
            this.optionalArg = optionalArg;
            return this;
        }
        public Builder required() {
            return required(true);
        }
        public Builder required(final boolean required) {
            this.required = required;
            return this;
        }
        public Builder since(final String since) {
            this.since = since;
            return this;
        }
        public Builder type(final Class<?> type) {
            this.type = toType(type);
            return this;
        }
        public Builder valueSeparator() {
            return valueSeparator(Char.EQUAL);
        }
        public Builder valueSeparator(final char valueSeparator) {
            this.valueSeparator = valueSeparator;
            return this;
        }
    }
    static final Option[] EMPTY_ARRAY = {};
    private static final long serialVersionUID = 1L;
    public static final int UNINITIALIZED = -1;
    public static final int UNLIMITED_VALUES = -2;
    public static Builder builder() {
        return builder(null);
    }
    public static Builder builder(final String option) {
        return new Builder(option);
    }
    private int argCount = UNINITIALIZED;
    private String argName;
    private transient Converter<?, ?> converter;
    private final transient DeprecatedAttributes deprecated;
    private String description;
    private String longOption;
    private final String option;
    private boolean optionalArg;
    private boolean required;
    private String since;
    private Class<?> type = String.class;
    private List<String> values = new ArrayList<>();
    private char valueSeparator;
    Option(final Builder builder) {
        this.argName = builder.argName;
        this.description = builder.description;
        this.longOption = builder.longOption;
        this.argCount = builder.argCount;
        this.option = builder.option;
        this.optionalArg = builder.optionalArg;
        this.deprecated = builder.deprecated;
        this.required = builder.required;
        this.since = builder.since;
        this.type = builder.type;
        this.valueSeparator = builder.valueSeparator;
        this.converter = builder.converter;
    }
    public Option(final String option, final boolean hasArg, final String description)
            throws IllegalArgumentException {
        this(option, null, hasArg, description);
    }
    public Option(final String option, final String description) throws IllegalArgumentException {
        this(option, null, false, description);
    }
    public Option(
            final String option, final String longOption, final boolean hasArg, final String description)
            throws IllegalArgumentException {
        this.deprecated = null;
        this.option = OptionValidator.validate(option);
        this.longOption = longOption;
        if (hasArg) {
            this.argCount = 1;
        }
        this.description = description;
    }
    boolean acceptsArg() {
        return (hasArg() || hasArgs() || hasOptionalArg())
                && (argCount <= 0 || values.size() < argCount);
    }
    private void add(final String value) {
        if (!acceptsArg()) {
            throw new IllegalArgumentException("Cannot add value, list full.");
        }
        values.add(value);
    }
    @Deprecated
    public boolean addValue(final String value) {
        throw new UnsupportedOperationException(
                "The addValue method is not intended for client use. Subclasses should use the processValue method instead.");
    }
    void clearValues() {
        values.clear();
    }
    @Override
    public Object clone() {
        try {
            final Option option = (Option) super.clone();
            option.values = new ArrayList<>(values);
            return option;
        } catch (final CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
    }
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Option)) {
            return false;
        }
        final Option other = (Option) obj;
        return Objects.equals(longOption, other.longOption) && Objects.equals(option, other.option);
    }
    public String getArgName() {
        return argName;
    }
    public int getArgs() {
        return argCount;
    }
    public Converter<?, ?> getConverter() {
        return converter == null ? TypeHandler.getDefault().getConverter(type) : converter;
    }
    public DeprecatedAttributes getDeprecated() {
        return deprecated;
    }
    public String getDescription() {
        return description;
    }
    public int getId() {
        return getKey().charAt(0);
    }
    public String getKey() {
        return option == null ? longOption : option;
    }
    public String getLongOpt() {
        return longOption;
    }
    public String getOpt() {
        return option;
    }
    public String getSince() {
        return since;
    }
    public Object getType() {
        return type;
    }
    public String getValue() {
        return hasNoValues() ? null : values.get(0);
    }
    public String getValue(final int index) throws IndexOutOfBoundsException {
        return hasNoValues() ? null : values.get(index);
    }
    public String getValue(final String defaultValue) {
        final String value = getValue();
        return value != null ? value : defaultValue;
    }
    static final String[] EMPTY_STRING_ARRAY = {};
    public String[] getValues() {
        return hasNoValues() ? null : values.toArray(EMPTY_STRING_ARRAY);
    }
    public char getValueSeparator() {
        return valueSeparator;
    }
    public List<String> getValuesList() {
        return values;
    }
    public boolean hasArg() {
        return argCount > 0 || argCount == UNLIMITED_VALUES;
    }
    public boolean hasArgName() {
        return argName != null && !argName.isEmpty();
    }
    public boolean hasArgs() {
        return argCount > 1 || argCount == UNLIMITED_VALUES;
    }
    @Override
    public int hashCode() {
        return Objects.hash(longOption, option);
    }
    public boolean hasLongOpt() {
        return longOption != null;
    }
    private boolean hasNoValues() {
        return values.isEmpty();
    }
    public boolean hasOptionalArg() {
        return optionalArg;
    }
    public boolean hasValueSeparator() {
        return valueSeparator > 0;
    }
    public boolean isDeprecated() {
        return deprecated != null;
    }
    public boolean isRequired() {
        return required;
    }
    void processValue(final String value) {
        if (argCount == UNINITIALIZED) {
            throw new IllegalArgumentException("NO_ARGS_ALLOWED");
        }
        String add = value;
        if (hasValueSeparator()) {
            final char sep = getValueSeparator();
            int index = add.indexOf(sep);
            while (index != -1) {
                if (values.size() == argCount - 1) {
                    break;
                }
                add(add.substring(0, index));
                add = add.substring(index + 1);
                index = add.indexOf(sep);
            }
        }
        add(add);
    }
    boolean requiresArg() {
        if (optionalArg) {
            return false;
        }
        if (argCount == UNLIMITED_VALUES) {
            return values.isEmpty();
        }
        return acceptsArg();
    }
    public void setArgName(final String argName) {
        this.argName = argName;
    }
    public void setArgs(final int num) {
        this.argCount = num;
    }
    public void setConverter(final Converter<?, ?> converter) {
        this.converter = converter;
    }
    public void setDescription(final String description) {
        this.description = description;
    }
    public void setLongOpt(final String longOpt) {
        this.longOption = longOpt;
    }
    public void setOptionalArg(final boolean optionalArg) {
        this.optionalArg = optionalArg;
    }
    public void setRequired(final boolean required) {
        this.required = required;
    }
    public void setType(final Class<?> type) {
        this.type = Builder.toType(type);
    }
    @Deprecated
    public void setType(final Object type) {
        setType((Class<?>) type);
    }
    public void setValueSeparator(final char valueSeparator) {
        this.valueSeparator = valueSeparator;
    }
    String toDeprecatedString() {
        if (!isDeprecated()) {
            return "";
        }
        final StringBuilder buf =
                new StringBuilder().append("Option '").append(option).append(Char.APOS);
        if (longOption != null) {
            buf.append(Char.APOS).append(longOption).append(Char.APOS);
        }
        buf.append(": ").append(deprecated);
        return buf.toString();
    }
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder().append("[ ");
        buf.append("Option ");
        buf.append(option);
        if (longOption != null) {
            buf.append(Char.SP).append(longOption);
        }
        if (isDeprecated()) {
            buf.append(Char.SP);
            buf.append(deprecated.toString());
        }
        if (hasArgs()) {
            buf.append("[ARG...]");
        } else if (hasArg()) {
            buf.append(" [ARG]");
        }
        return buf.append(" :: ")
                .append(description)
                .append(" :: ")
                .append(type)
                .append(" ]")
                .toString();
    }
}

