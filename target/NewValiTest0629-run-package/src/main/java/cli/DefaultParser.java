package cli;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
public class DefaultParser implements CommandLineParser {
    public static final class Builder implements Supplier<DefaultParser> {
        private boolean allowPartialMatching = true;
        private Consumer<Option> deprecatedHandler = CommandLine.Builder.DEPRECATED_HANDLER;
        private Boolean stripLeadingAndTrailingQuotes;
        private Builder() {}
        @Deprecated
        public DefaultParser build() {
            return get();
        }
        @Override
        public DefaultParser get() {
            return new DefaultParser(
                    allowPartialMatching, stripLeadingAndTrailingQuotes, deprecatedHandler);
        }
        public Builder setAllowPartialMatching(final boolean allowPartialMatching) {
            this.allowPartialMatching = allowPartialMatching;
            return this;
        }
        public Builder setDeprecatedHandler(final Consumer<Option> deprecatedHandler) {
            this.deprecatedHandler = deprecatedHandler;
            return this;
        }
        public Builder setStripLeadingAndTrailingQuotes(final Boolean stripLeadingAndTrailingQuotes) {
            this.stripLeadingAndTrailingQuotes = stripLeadingAndTrailingQuotes;
            return this;
        }
    }
    public static Builder builder() {
        return new Builder();
    }
    static int indexOfEqual(final String token) {
        return token.indexOf(Char.EQUAL);
    }
    protected CommandLine cmd;
    protected cli.Options options;
    protected boolean stopAtNonOption;
    protected String currentToken;
    protected Option currentOption;
    protected boolean skipParsing;
    protected List expectedOpts;
    private final boolean allowPartialMatching;
    private final Boolean stripLeadingAndTrailingQuotes;
    private final Consumer<Option> deprecatedHandler;
    public DefaultParser() {
        this.allowPartialMatching = true;
        this.stripLeadingAndTrailingQuotes = null;
        this.deprecatedHandler = CommandLine.Builder.DEPRECATED_HANDLER;
    }
    public DefaultParser(final boolean allowPartialMatching) {
        this.allowPartialMatching = allowPartialMatching;
        this.stripLeadingAndTrailingQuotes = null;
        this.deprecatedHandler = CommandLine.Builder.DEPRECATED_HANDLER;
    }
    private DefaultParser(
            final boolean allowPartialMatching,
            final Boolean stripLeadingAndTrailingQuotes,
            final Consumer<Option> deprecatedHandler) {
        this.allowPartialMatching = allowPartialMatching;
        this.stripLeadingAndTrailingQuotes = stripLeadingAndTrailingQuotes;
        this.deprecatedHandler = deprecatedHandler;
    }
    void checkRequiredArgs() throws ParseException {
        if (currentOption != null && currentOption.requiresArg()) {
            if (isJavaProperty(currentOption.getKey()) && currentOption.getValuesList().size() == 1) {
                return;
            }
            throw new MissingArgumentException(currentOption);
        }
    }
    protected void checkRequiredOptions() throws MissingOptionException {
        if (!expectedOpts.isEmpty()) {
            throw new MissingOptionException(expectedOpts);
        }
    }
    private String getLongPrefix(final String token) {
        final String t = Util.stripLeadingHyphens(token);
        int i;
        String opt = null;
        for (i = t.length() - 2; i > 1; i--) {
            final String prefix = t.substring(0, i);
            if (options.hasLongOption(prefix)) {
                opt = prefix;
                break;
            }
        }
        return opt;
    }
    List<String> getMatchingLongOptions(final String token) {
        if (allowPartialMatching) {
            return options.getMatchingOptions(token);
        }
        final List<String> matches = new ArrayList<>(1);
        if (options.hasLongOption(token)) {
            matches.add(options.getOption(token).getLongOpt());
        }
        return matches;
    }
    protected void handleConcatenatedOptions(final String token) throws ParseException {
        for (int i = 1; i < token.length(); i++) {
            final String ch = String.valueOf(token.charAt(i));
            if (!options.hasOption(ch)) {
                handleUnknownToken(stopAtNonOption && i > 1 ? token.substring(i) : token);
                break;
            }
            handleOption(options.getOption(ch));
            if (currentOption != null && token.length() != i + 1) {
                currentOption.processValue(stripLeadingAndTrailingQuotesDefaultOff(token.substring(i + 1)));
                break;
            }
        }
    }
    private void handleLongOption(final String token) throws ParseException {
        if (indexOfEqual(token) == -1) {
            handleLongOptionWithoutEqual(token);
        } else {
            handleLongOptionWithEqual(token);
        }
    }
    void handleLongOptionWithEqual(final String token) throws ParseException {
        final int pos = indexOfEqual(token);
        final String value = token.substring(pos + 1);
        final String opt = token.substring(0, pos);
        final List<String> matchingOpts = getMatchingLongOptions(opt);
        if (matchingOpts.isEmpty()) {
            handleUnknownToken(currentToken);
        } else if (matchingOpts.size() > 1 && !options.hasLongOption(opt)) {
            throw new AmbiguousOptionException(opt, matchingOpts);
        } else {
            final String key = options.hasLongOption(opt) ? opt : matchingOpts.get(0);
            final Option option = options.getOption(key);
            if (option.acceptsArg()) {
                handleOption(option);
                currentOption.processValue(stripLeadingAndTrailingQuotesDefaultOff(value));
                currentOption = null;
            } else {
                handleUnknownToken(currentToken);
            }
        }
    }
    private void handleLongOptionWithoutEqual(final String token) throws ParseException {
        final List<String> matchingOpts = getMatchingLongOptions(token);
        if (matchingOpts.isEmpty()) {
            handleUnknownToken(currentToken);
        } else if (matchingOpts.size() > 1 && !options.hasLongOption(token)) {
            throw new AmbiguousOptionException(token, matchingOpts);
        } else {
            final String key = options.hasLongOption(token) ? token : matchingOpts.get(0);
            handleOption(options.getOption(key));
        }
    }
    void handleOption(final Option option) throws ParseException {
        checkRequiredArgs();
        final Option copy = (Option) option.clone();
        updateRequiredOptions(copy);
        cmd.addOption(copy);
        currentOption = copy.hasArg() ? copy : null;
    }
    public void handleProperties(final Properties properties) throws ParseException {
        if (properties == null) {
            return;
        }
        for (final Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
            final String option = e.nextElement().toString();
            final Option opt = options.getOption(option);
            if (opt == null) {
                throw new UnrecognizedOptionException("Default option wasn't defined", option);
            }
            final OptionGroup group = options.getOptionGroup(opt);
            final boolean selected = group != null && group.isSelected();
            if (!cmd.hasOption(option) && !selected) {
                final String value = properties.getProperty(option);
                if (opt.hasArg()) {
                    if (Util.isEmpty(opt.getValues())) {
                        opt.processValue(stripLeadingAndTrailingQuotesDefaultOff(value));
                    }
                } else if (!("yes".equalsIgnoreCase(value)
                        || "true".equalsIgnoreCase(value)
                        || "1".equalsIgnoreCase(value))) {
                    continue;
                }
                handleOption(opt);
                currentOption = null;
            }
        }
    }
    void handleShortAndLongOption(final String hyphenToken) throws ParseException {
        final String token = Util.stripLeadingHyphens(hyphenToken);
        final int pos = indexOfEqual(token);
        if (token.length() == 1) {
            if (options.hasShortOption(token)) {
                handleOption(options.getOption(token));
            } else {
                handleUnknownToken(hyphenToken);
            }
        } else if (pos == -1) {
            if (options.hasShortOption(token)) {
                handleOption(options.getOption(token));
            } else if (!getMatchingLongOptions(token).isEmpty()) {
                handleLongOptionWithoutEqual(hyphenToken);
            } else {
                final String opt = getLongPrefix(token);
                if (opt != null && options.getOption(opt).acceptsArg()) {
                    handleOption(options.getOption(opt));
                    currentOption.processValue(
                            stripLeadingAndTrailingQuotesDefaultOff(token.substring(opt.length())));
                    currentOption = null;
                } else if (isJavaProperty(token)) {
                    handleOption(options.getOption(token.substring(0, 1)));
                    currentOption.processValue(stripLeadingAndTrailingQuotesDefaultOff(token.substring(1)));
                    currentOption = null;
                } else {
                    handleConcatenatedOptions(hyphenToken);
                }
            }
        } else {
            final String opt = token.substring(0, pos);
            final String value = token.substring(pos + 1);
            if (opt.length() == 1) {
                final Option option = options.getOption(opt);
                if (option != null && option.acceptsArg()) {
                    handleOption(option);
                    currentOption.processValue(value);
                    currentOption = null;
                } else {
                    handleUnknownToken(hyphenToken);
                }
            } else if (isJavaProperty(opt)) {
                handleOption(options.getOption(opt.substring(0, 1)));
                currentOption.processValue(opt.substring(1));
                currentOption.processValue(value);
                currentOption = null;
            } else {
                handleLongOptionWithEqual(hyphenToken);
            }
        }
    }
    public void handleToken(final String token) throws ParseException {
        if (token != null) {
            currentToken = token;
            if (skipParsing) {
                cmd.addArg(token);
            } else if ("--".equals(token)) {
                skipParsing = true;
            } else if (currentOption != null && currentOption.acceptsArg() && isArgument(token)) {
                currentOption.processValue(stripLeadingAndTrailingQuotesDefaultOn(token));
            } else if (token.startsWith("--")) {
                handleLongOption(token);
            } else if (token.startsWith("-") && !"-".equals(token)) {
                handleShortAndLongOption(token);
            } else {
                handleUnknownToken(token);
            }
            if (currentOption != null && !currentOption.acceptsArg()) {
                currentOption = null;
            }
        }
    }
    void handleUnknownToken(final String token) throws ParseException {
        if (token.startsWith("-") && token.length() > 1 && !stopAtNonOption) {
            throw new UnrecognizedOptionException("Unrecognized option: " + token, token);
        }
        cmd.addArg(token);
        if (stopAtNonOption) {
            skipParsing = true;
        }
    }
    private boolean isArgument(final String token) {
        return !isOption(token) || isNegativeNumber(token);
    }
    public boolean isJavaProperty(final String token) {
        final String opt = token.isEmpty() ? null : token.substring(0, 1);
        final Option option = options.getOption(opt);
        return option != null && (option.getArgs() >= 2 || option.getArgs() == Option.UNLIMITED_VALUES);
    }
    public boolean isLongOption(final String token) {
        if (token == null || !token.startsWith("-") || token.length() == 1) {
            return false;
        }
        final int pos = indexOfEqual(token);
        final String t = pos == -1 ? token : token.substring(0, pos);
        if (!getMatchingLongOptions(t).isEmpty()) {
            return true;
        }
        if (getLongPrefix(token) != null && !token.startsWith("--")) {
            return true;
        }
        return false;
    }
    boolean isNegativeNumber(final String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }
    private boolean isOption(final String token) {
        return isLongOption(token) || isShortOption(token);
    }
    public boolean isShortOption(final String token) {
        if (token == null || !token.startsWith("-") || token.length() == 1) {
            return false;
        }
        final int pos = indexOfEqual(token);
        final String optName = pos == -1 ? token.substring(1) : token.substring(1, pos);
        if (options.hasShortOption(optName)) {
            return true;
        }
        return !optName.isEmpty() && options.hasShortOption(String.valueOf(optName.charAt(0)));
    }
    @Override
    public CommandLine parse(final Options options, final String[] arguments) throws ParseException {
        return parse(options, arguments, null);
    }
    @Override
    public CommandLine parse(
            final Options options, final String[] arguments, final boolean stopAtNonOption)
            throws ParseException {
        return parse(options, arguments, null, stopAtNonOption);
    }
    public CommandLine parse(
            final Options options, final String[] arguments, final Properties properties)
            throws ParseException {
        return parse(options, arguments, properties, false);
    }
    public CommandLine parse(
            final Options options,
            final String[] arguments,
            final Properties properties,
            final boolean stopAtNonOption)
            throws ParseException {
        this.options = options;
        this.stopAtNonOption = stopAtNonOption;
        skipParsing = false;
        currentOption = null;
        expectedOpts = new ArrayList<>(options.getRequiredOptions());
        for (final OptionGroup group : options.getOptionGroups()) {
            group.setSelected(null);
        }
        cmd = CommandLine.builder().setDeprecatedHandler(deprecatedHandler).get();
        if (arguments != null) {
            for (final String argument : arguments) {
                handleToken(argument);
            }
        }
        checkRequiredArgs();
        handleProperties(properties);
        checkRequiredOptions();
        return cmd;
    }
    private String stripLeadingAndTrailingQuotesDefaultOff(final String token) {
        if (stripLeadingAndTrailingQuotes != null && stripLeadingAndTrailingQuotes) {
            return Util.stripLeadingAndTrailingQuotes(token);
        }
        return token;
    }
    String stripLeadingAndTrailingQuotesDefaultOn(final String token) {
        if (stripLeadingAndTrailingQuotes == null || stripLeadingAndTrailingQuotes) {
            return Util.stripLeadingAndTrailingQuotes(token);
        }
        return token;
    }
    void updateRequiredOptions(final Option option) throws AlreadySelectedException {
        if (option.isRequired()) {
            expectedOpts.remove(option.getKey());
        }
        if (options.getOptionGroup(option) != null) {
            final OptionGroup group = options.getOptionGroup(option);
            if (group.isRequired()) {
                expectedOpts.remove(group);
            }
            group.setSelected(option);
        }
    }
}
