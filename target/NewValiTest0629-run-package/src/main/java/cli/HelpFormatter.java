package cli;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
@Deprecated
public class HelpFormatter {
    public static class Builder implements Supplier<HelpFormatter> {
        public Builder() {
        }
        private static final Function<Option, String> DEFAULT_DEPRECATED_FORMAT =
                o -> "[Deprecated] " + getDescription(o);
        private Function<Option, String> deprecatedFormatFunction = DEFAULT_DEPRECATED_FORMAT;
        private PrintWriter printStream = createDefaultPrintWriter();
        private boolean showSince;
        @Override
        public HelpFormatter get() {
            return new HelpFormatter(deprecatedFormatFunction, printStream, showSince);
        }
        public Builder setPrintWriter(final PrintWriter printWriter) {
            this.printStream = Objects.requireNonNull(printWriter, "printWriter");
            return this;
        }
        public Builder setShowDeprecated(final boolean useDefaultFormat) {
            return setShowDeprecated(useDefaultFormat ? DEFAULT_DEPRECATED_FORMAT : null);
        }
        public Builder setShowDeprecated(final Function<Option, String> deprecatedFormatFunction) {
            this.deprecatedFormatFunction = deprecatedFormatFunction;
            return this;
        }
        public Builder setShowSince(final boolean showSince) {
            this.showSince = showSince;
            return this;
        }
    }
    private static final class OptionComparator implements Comparator<Option>, Serializable {
        private static final long serialVersionUID = 5305467873966684014L;
        @Override
        public int compare(final Option opt1, final Option opt2) {
            return opt1.getKey().compareToIgnoreCase(opt2.getKey());
        }
    }
    private static final String HEADER_OPTIONS = "Options";
    private static final String HEADER_SINCE = "Since";
    private static final String HEADER_DESCRIPTION = "Description";
    public static final int DEFAULT_WIDTH = 74;
    public static final int DEFAULT_LEFT_PAD = 1;
    public static final int DEFAULT_DESC_PAD = 3;
    public static final String DEFAULT_SYNTAX_PREFIX = "usage: ";
    public static final String DEFAULT_OPT_PREFIX = "-";
    public static final String DEFAULT_LONG_OPT_PREFIX = "--";
    public static final String DEFAULT_LONG_OPT_SEPARATOR = " ";
    public static final String DEFAULT_ARG_NAME = "arg";
    public static Builder builder() {
        return new Builder();
    }
    public static PrintWriter createDefaultPrintWriter() {
        return new PrintWriter(System.out);
    }
    public static String getDescription(final Option option) {
        final String desc = option.getDescription();
        return desc == null ? "" : desc;
    }
    @Deprecated public int defaultWidth = DEFAULT_WIDTH;
    @Deprecated public int defaultLeftPad = DEFAULT_LEFT_PAD;
    @Deprecated public int defaultDescPad = DEFAULT_DESC_PAD;
    @Deprecated public String defaultSyntaxPrefix = DEFAULT_SYNTAX_PREFIX;
    @Deprecated public String defaultNewLine = System.lineSeparator();
    @Deprecated public String defaultOptPrefix = DEFAULT_OPT_PREFIX;
    @Deprecated public String defaultLongOptPrefix = DEFAULT_LONG_OPT_PREFIX;
    @Deprecated public String defaultArgName = DEFAULT_ARG_NAME;
    protected Comparator<Option> optionComparator = new OptionComparator();
    private final Function<Option, String> deprecatedFormatFunction;
    private final PrintWriter printWriter;
    private final boolean showSince;
    private String longOptSeparator = DEFAULT_LONG_OPT_SEPARATOR;
    public HelpFormatter() {
        this(null, createDefaultPrintWriter(), false);
    }
    private HelpFormatter(
            final Function<Option, String> deprecatedFormatFunction,
            final PrintWriter printWriter,
            final boolean showSince) {
        this.deprecatedFormatFunction = deprecatedFormatFunction;
        this.printWriter = printWriter;
        this.showSince = showSince;
    }
    private void appendOption(final StringBuilder buff, final Option option, final boolean required) {
        if (!required) {
            buff.append("[");
        }
        if (option.getOpt() != null) {
            buff.append("-").append(option.getOpt());
        } else {
            buff.append("--").append(option.getLongOpt());
        }
        if (option.hasArg() && (option.getArgName() == null || !option.getArgName().isEmpty())) {
            buff.append(option.getOpt() == null ? longOptSeparator : " ");
            buff.append("<")
                    .append(option.getArgName() != null ? option.getArgName() : getArgName())
                    .append(">");
        }
        if (!required) {
            buff.append("]");
        }
    }
    private void appendOptionGroup(final StringBuilder buff, final OptionGroup group) {
        if (!group.isRequired()) {
            buff.append("[");
        }
        final List<Option> optList = new ArrayList<>(group.getOptions());
        if (getOptionComparator() != null) {
            Collections.sort(optList, getOptionComparator());
        }
        for (final Iterator<Option> it = optList.iterator(); it.hasNext(); ) {
            appendOption(buff, it.next(), true);
            if (it.hasNext()) {
                buff.append(" | ");
            }
        }
        if (!group.isRequired()) {
            buff.append("]");
        }
    }
    <A extends Appendable> A appendOptions(
            final A sb, final int width, final Options options, final int leftPad, final int descPad)
            throws IOException {
        final String lpad = createPadding(leftPad);
        final String dpad = createPadding(descPad);
        int max = 0;
        final int maxSince = showSince ? determineMaxSinceLength(options) + leftPad : 0;
        final List<StringBuilder> prefixList = new ArrayList<>();
        final List<Option> optList = options.helpOptions();
        if (getOptionComparator() != null) {
            Collections.sort(optList, getOptionComparator());
        }
        for (final Option option : optList) {
            final StringBuilder optBuf = new StringBuilder();
            if (option.getOpt() == null) {
                optBuf.append(lpad).append("   ").append(getLongOptPrefix()).append(option.getLongOpt());
            } else {
                optBuf.append(lpad).append(getOptPrefix()).append(option.getOpt());
                if (option.hasLongOpt()) {
                    optBuf.append(',').append(getLongOptPrefix()).append(option.getLongOpt());
                }
            }
            if (option.hasArg()) {
                final String argName = option.getArgName();
                if (argName != null && argName.isEmpty()) {
                    optBuf.append(' ');
                } else {
                    optBuf.append(option.hasLongOpt() ? longOptSeparator : " ");
                    optBuf
                            .append("<")
                            .append(argName != null ? option.getArgName() : getArgName())
                            .append(">");
                }
            }
            prefixList.add(optBuf);
            max = Math.max(optBuf.length() + maxSince, max);
        }
        final int nextLineTabStop = max + descPad;
        if (showSince) {
            final StringBuilder optHeader =
                    new StringBuilder(HEADER_OPTIONS)
                            .append(createPadding(max - maxSince - HEADER_OPTIONS.length() + leftPad))
                            .append(HEADER_SINCE);
            optHeader
                    .append(createPadding(max - optHeader.length()))
                    .append(lpad)
                    .append(HEADER_DESCRIPTION);
            appendWrappedText(sb, width, nextLineTabStop, optHeader.toString());
            sb.append(getNewLine());
        }
        int x = 0;
        for (final Iterator<Option> it = optList.iterator(); it.hasNext(); ) {
            final Option option = it.next();
            final StringBuilder optBuf = new StringBuilder(prefixList.get(x++).toString());
            if (optBuf.length() < max) {
                optBuf.append(createPadding(max - maxSince - optBuf.length()));
                if (showSince) {
                    optBuf.append(lpad).append(option.getSince() == null ? "-" : option.getSince());
                }
                optBuf.append(createPadding(max - optBuf.length()));
            }
            optBuf.append(dpad);
            if (deprecatedFormatFunction != null && option.isDeprecated()) {
                optBuf.append(deprecatedFormatFunction.apply(option).trim());
            } else if (option.getDescription() != null) {
                optBuf.append(option.getDescription());
            }
            appendWrappedText(sb, width, nextLineTabStop, optBuf.toString());
            if (it.hasNext()) {
                sb.append(getNewLine());
            }
        }
        return sb;
    }
    <A extends Appendable> A appendWrappedText(
            final A appendable, final int width, final int nextLineTabStop, final String text)
            throws IOException {
        String render = text;
        int nextLineTabStopPos = nextLineTabStop;
        int pos = findWrapPos(render, width, 0);
        if (pos == -1) {
            appendable.append(rtrim(render));
            return appendable;
        }
        appendable.append(rtrim(render.substring(0, pos))).append(getNewLine());
        if (nextLineTabStopPos >= width) {
            nextLineTabStopPos = 1;
        }
        final String padding = createPadding(nextLineTabStopPos);
        while (true) {
            render = padding + render.substring(pos).trim();
            pos = findWrapPos(render, width, 0);
            if (pos == -1) {
                appendable.append(render);
                return appendable;
            }
            if (render.length() > width && pos == nextLineTabStopPos - 1) {
                pos = width;
            }
            appendable.append(rtrim(render.substring(0, pos))).append(getNewLine());
        }
    }
    protected String createPadding(final int len) {
        final char[] padding = new char[len];
        Arrays.fill(padding, ' ');
        return new String(padding);
    }
    private int determineMaxSinceLength(final Options options) {
        final int minLen = HEADER_SINCE.length();
        final int len =
                options.getOptions().stream()
                        .map(o -> o.getSince() == null ? minLen : o.getSince().length())
                        .max(Integer::compareTo)
                        .orElse(minLen);
        return len < minLen ? minLen : len;
    }
    protected int findWrapPos(final String text, final int width, final int startPos) {
        int pos = text.indexOf(Char.LF, startPos);
        if (pos != -1 && pos <= width) {
            return pos + 1;
        }
        pos = text.indexOf(Char.TAB, startPos);
        if (pos != -1 && pos <= width) {
            return pos + 1;
        }
        if (startPos + width >= text.length()) {
            return -1;
        }
        for (pos = startPos + width; pos >= startPos; --pos) {
            final char c = text.charAt(pos);
            if (c == Char.SP || c == Char.LF || c == Char.CR) {
                break;
            }
        }
        if (pos > startPos) {
            return pos;
        }
        pos = startPos + width;
        return pos == text.length() ? -1 : pos;
    }
    public String getArgName() {
        return defaultArgName;
    }
    public int getDescPadding() {
        return defaultDescPad;
    }
    public int getLeftPadding() {
        return defaultLeftPad;
    }
    public String getLongOptPrefix() {
        return defaultLongOptPrefix;
    }
    public String getLongOptSeparator() {
        return longOptSeparator;
    }
    public String getNewLine() {
        return defaultNewLine;
    }
    public Comparator<Option> getOptionComparator() {
        return optionComparator;
    }
    public String getOptPrefix() {
        return defaultOptPrefix;
    }
    public String getSyntaxPrefix() {
        return defaultSyntaxPrefix;
    }
    public int getWidth() {
        return defaultWidth;
    }
    public void printHelp(
            final int width,
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final String footer) {
        printHelp(width, cmdLineSyntax, header, options, footer, false);
    }
    public void printHelp(
            final int width,
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final String footer,
            final boolean autoUsage) {
        final PrintWriter pw = new PrintWriter(printWriter);
        printHelp(
                pw,
                width,
                cmdLineSyntax,
                header,
                options,
                getLeftPadding(),
                getDescPadding(),
                footer,
                autoUsage);
        pw.flush();
    }
    public void printHelp(
            final PrintWriter pw,
            final int width,
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final int leftPad,
            final int descPad,
            final String footer) {
        printHelp(pw, width, cmdLineSyntax, header, options, leftPad, descPad, footer, false);
    }
    public void printHelp(
            final PrintWriter pw,
            final int width,
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final int leftPad,
            final int descPad,
            final String footer,
            final boolean autoUsage) {
        if (Util.isEmpty(cmdLineSyntax)) {
            throw new IllegalArgumentException("cmdLineSyntax not provided");
        }
        if (autoUsage) {
            printUsage(pw, width, cmdLineSyntax, options);
        } else {
            printUsage(pw, width, cmdLineSyntax);
        }
        if (header != null && !header.isEmpty()) {
            printWrapped(pw, width, header);
        }
        printOptions(pw, width, options, leftPad, descPad);
        if (footer != null && !footer.isEmpty()) {
            printWrapped(pw, width, footer);
        }
    }
    public void printHelp(final String cmdLineSyntax, final Options options) {
        printHelp(getWidth(), cmdLineSyntax, null, options, null, false);
    }
    public void printHelp(
            final String cmdLineSyntax, final Options options, final boolean autoUsage) {
        printHelp(getWidth(), cmdLineSyntax, null, options, null, autoUsage);
    }
    public void printHelp(
            final String cmdLineSyntax, final String header, final Options options, final String footer) {
        printHelp(cmdLineSyntax, header, options, footer, false);
    }
    public void printHelp(
            final String cmdLineSyntax,
            final String header,
            final Options options,
            final String footer,
            final boolean autoUsage) {
        printHelp(getWidth(), cmdLineSyntax, header, options, footer, autoUsage);
    }
    public void printOptions(
            final PrintWriter pw,
            final int width,
            final Options options,
            final int leftPad,
            final int descPad) {
        try {
            pw.println(appendOptions(new StringBuilder(), width, options, leftPad, descPad));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public void printUsage(final PrintWriter pw, final int width, final String cmdLineSyntax) {
        final int argPos = cmdLineSyntax.indexOf(' ') + 1;
        printWrapped(pw, width, getSyntaxPrefix().length() + argPos, getSyntaxPrefix() + cmdLineSyntax);
    }
    public void printUsage(
            final PrintWriter pw, final int width, final String app, final Options options) {
        final StringBuilder buff = new StringBuilder(getSyntaxPrefix()).append(app).append(Char.SP);
        final Collection<OptionGroup> processedGroups = new ArrayList<>();
        final List<Option> optList = new ArrayList<>(options.getOptions());
        if (getOptionComparator() != null) {
            Collections.sort(optList, getOptionComparator());
        }
        for (final Iterator<Option> it = optList.iterator(); it.hasNext(); ) {
            final Option option = it.next();
            final OptionGroup group = options.getOptionGroup(option);
            if (group != null) {
                if (!processedGroups.contains(group)) {
                    processedGroups.add(group);
                    appendOptionGroup(buff, group);
                }
            }
            else {
                appendOption(buff, option, option.isRequired());
            }
            if (it.hasNext()) {
                buff.append(Char.SP);
            }
        }
        printWrapped(pw, width, buff.toString().indexOf(' ') + 1, buff.toString());
    }
    public void printWrapped(
            final PrintWriter pw, final int width, final int nextLineTabStop, final String text) {
        pw.println(
                renderWrappedTextBlock(new StringBuilder(text.length()), width, nextLineTabStop, text));
    }
    public void printWrapped(final PrintWriter pw, final int width, final String text) {
        printWrapped(pw, width, 0, text);
    }
    protected StringBuffer renderOptions(
            final StringBuffer sb,
            final int width,
            final Options options,
            final int leftPad,
            final int descPad) {
        try {
            return appendOptions(sb, width, options, leftPad, descPad);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    protected StringBuffer renderWrappedText(
            final StringBuffer sb, final int width, final int nextLineTabStop, final String text) {
        try {
            return appendWrappedText(sb, width, nextLineTabStop, text);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    private <A extends Appendable> A renderWrappedTextBlock(
            final A appendable, final int width, final int nextLineTabStop, final String text) {
        try {
            final BufferedReader in = new BufferedReader(new StringReader(text));
            String line;
            boolean firstLine = true;
            while ((line = in.readLine()) != null) {
                if (!firstLine) {
                    appendable.append(getNewLine());
                } else {
                    firstLine = false;
                }
                appendWrappedText(appendable, width, nextLineTabStop, line);
            }
        } catch (final IOException e) {
        }
        return appendable;
    }
    protected String rtrim(final String s) {
        if (Util.isEmpty(s)) {
            return s;
        }
        int pos = s.length();
        while (pos > 0 && Character.isWhitespace(s.charAt(pos - 1))) {
            --pos;
        }
        return s.substring(0, pos);
    }
    public void setArgName(final String name) {
        this.defaultArgName = name;
    }
    public void setDescPadding(final int padding) {
        this.defaultDescPad = padding;
    }
    public void setLeftPadding(final int padding) {
        this.defaultLeftPad = padding;
    }
    public void setLongOptPrefix(final String prefix) {
        this.defaultLongOptPrefix = prefix;
    }
    public void setLongOptSeparator(final String longOptSeparator) {
        this.longOptSeparator = longOptSeparator;
    }
    public void setNewLine(final String newline) {
        this.defaultNewLine = newline;
    }
    public void setOptionComparator(final Comparator<Option> comparator) {
        this.optionComparator = comparator;
    }
    public void setOptPrefix(final String prefix) {
        this.defaultOptPrefix = prefix;
    }
    public void setSyntaxPrefix(final String prefix) {
        this.defaultSyntaxPrefix = prefix;
    }
    public void setWidth(final int width) {
        this.defaultWidth = width;
    }
}