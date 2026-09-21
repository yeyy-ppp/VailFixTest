package cli.help;

import cli.Option;
import cli.OptionGroup;
import cli.Options;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractHelpFormatter {
    public abstract static class Builder<B extends Builder<B, T>, T extends AbstractHelpFormatter>
            implements Supplier<T> {
        private Comparator<Option> comparator = DEFAULT_COMPARATOR;
        private HelpAppendable helpAppendable = TextHelpAppendable.systemOut();
        private OptionFormatter.Builder optionFormatBuilder = OptionFormatter.builder();
        private String optionGroupSeparator = DEFAULT_OPTION_GROUP_SEPARATOR;
        protected Builder() {
        }
        @SuppressWarnings("unchecked")
        protected B asThis() {
            return (B) this;
        }
        protected Comparator<Option> getComparator() {
            return comparator;
        }
        protected HelpAppendable getHelpAppendable() {
            return helpAppendable;
        }
        protected OptionFormatter.Builder getOptionFormatBuilder() {
            return optionFormatBuilder;
        }
        protected String getOptionGroupSeparator() {
            return optionGroupSeparator;
        }
        public B setComparator(final Comparator<Option> comparator) {
            this.comparator = comparator;
            return asThis();
        }
        public B setHelpAppendable(final HelpAppendable helpAppendable) {
            this.helpAppendable =
                    helpAppendable != null ? helpAppendable : TextHelpAppendable.systemOut();
            return asThis();
        }
        public B setOptionFormatBuilder(final OptionFormatter.Builder optionFormatBuilder) {
            this.optionFormatBuilder =
                    optionFormatBuilder != null ? optionFormatBuilder : OptionFormatter.builder();
            return asThis();
        }
        public B setOptionGroupSeparator(final String optionGroupSeparator) {
            this.optionGroupSeparator = Util.defaultValue(optionGroupSeparator, "");
            return asThis();
        }
    }
    public static final Comparator<Option> DEFAULT_COMPARATOR =
            (opt1, opt2) -> opt1.getKey().compareToIgnoreCase(opt2.getKey());
    public static final String DEFAULT_OPTION_GROUP_SEPARATOR = " | ";
    public static final String DEFAULT_SYNTAX_PREFIX = "usage: ";
    private final Comparator<Option> comparator;
    private final HelpAppendable helpAppendable;
    private final OptionFormatter.Builder optionFormatBuilder;
    private final String optionGroupSeparator;
    private String syntaxPrefix = DEFAULT_SYNTAX_PREFIX;
    protected AbstractHelpFormatter(final Builder<?, ?> builder) {
        this.helpAppendable = Objects.requireNonNull(builder.getHelpAppendable(), "helpAppendable");
        this.optionFormatBuilder =
                Objects.requireNonNull(builder.getOptionFormatBuilder(), "optionFormatBuilder");
        this.comparator = Objects.requireNonNull(builder.getComparator(), "comparator");
        this.optionGroupSeparator = Util.defaultValue(builder.getOptionGroupSeparator(), "");
    }
    protected Comparator<Option> getComparator() {
        return comparator;
    }
    protected HelpAppendable getHelpAppendable() {
        return helpAppendable;
    }
    protected OptionFormatter.Builder getOptionFormatBuilder() {
        return optionFormatBuilder;
    }
    public final OptionFormatter getOptionFormatter(final Option option) {
        return optionFormatBuilder.build(option);
    }
    protected String getOptionGroupSeparator() {
        return optionGroupSeparator;
    }
    public final HelpAppendable getSerializer() {
        return helpAppendable;
    }
    public final String getSyntaxPrefix() {
        return syntaxPrefix;
    }
    protected abstract TableDefinition getTableDefinition(Iterable<Option> options);
    public void printHelp(
            final String cmdLineSyntax,
            final String header,
            final Iterable<Option> options,
            final String footer,
            final boolean autoUsage)
            throws IOException {
        if (Util.isEmpty(cmdLineSyntax)) {
            throw new IllegalArgumentException("cmdLineSyntax not provided");
        }
        if (autoUsage) {
            helpAppendable.appendParagraphFormat(
                    "%s %s %s", syntaxPrefix, cmdLineSyntax, toSyntaxOptions(options));
        } else {
            helpAppendable.appendParagraphFormat("%s %s", syntaxPrefix, cmdLineSyntax);
        }
        if (!Util.isEmpty(header)) {
            helpAppendable.appendParagraph(header);
        }
        helpAppendable.appendTable(getTableDefinition(options));
        if (!Util.isEmpty(footer)) {
            helpAppendable.appendParagraph(footer);
        }
    }
    public final void printHelp(
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final String footer,
            final boolean autoUsage)
            throws IOException {
        printHelp(cmdLineSyntax, header, options.getOptions(), footer, autoUsage);
    }
    public final void printOptions(final Iterable<Option> options) throws IOException {
        printOptions(getTableDefinition(options));
    }
    public final void printOptions(final Options options) throws IOException {
        printOptions(options.getOptions());
    }
    public final void printOptions(final TableDefinition tableDefinition) throws IOException {
        helpAppendable.appendTable(tableDefinition);
    }
    public final void setSyntaxPrefix(final String prefix) {
        this.syntaxPrefix = prefix;
    }
    public List<Option> sort(final Iterable<Option> options) {
        final List<Option> result = new ArrayList<>();
        if (options != null) {
            options.forEach(result::add);
            result.sort(comparator);
        }
        return result;
    }
    public List<Option> sort(final Options options) {
        return sort(options == null ? null : options.getOptions());
    }
    public final String toArgName(final String argName) {
        return optionFormatBuilder.toArgName(argName);
    }
    public String toSyntaxOptions(final Iterable<Option> options) {
        return toSyntaxOptions(options, o -> null);
    }
    protected String toSyntaxOptions(
            final Iterable<Option> options, final Function<Option, OptionGroup> lookup) {
        final Collection<OptionGroup> processedGroups = new ArrayList<>();
        final List<Option> optList = sort(options);
        final StringBuilder buff = new StringBuilder();
        String prefix = "";
        for (final Option option : optList) {
            final OptionGroup group = lookup.apply(option);
            if (group != null) {
                if (!processedGroups.contains(group)) {
                    processedGroups.add(group);
                    buff.append(prefix).append(toSyntaxOptions(group));
                    prefix = " ";
                }
            }
            else {
                buff.append(prefix).append(optionFormatBuilder.build(option).toSyntaxOption());
                prefix = " ";
            }
        }
        return buff.toString();
    }
    public String toSyntaxOptions(final OptionGroup group) {
        final StringBuilder buff = new StringBuilder();
        final List<Option> optList = sort(group.getOptions());
        OptionFormatter formatter = null;
        final Iterator<Option> iter = optList.iterator();
        while (iter.hasNext()) {
            formatter = optionFormatBuilder.build(iter.next());
            buff.append(formatter.toSyntaxOption(true));
            if (iter.hasNext()) {
                buff.append(optionGroupSeparator);
            }
        }
        if (formatter != null) {
            return group.isRequired() ? buff.toString() : formatter.toOptional(buff.toString());
        }
        return "";
    }
    public String toSyntaxOptions(final Options options) {
        return toSyntaxOptions(options.getOptions(), options::getOptionGroup);
    }
}