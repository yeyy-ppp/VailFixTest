package help;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
public class TextHelpAppendable extends FilterHelpAppendable {
    public static final int DEFAULT_WIDTH = 74;
    public static final int DEFAULT_LEFT_PAD = 1;
    public static final int DEFAULT_INDENT = 3;
    public static final int DEFAULT_LIST_INDENT = 7;
    private static final String BLANK_LINE = "";
    private static final Set<Character> BREAK_CHAR_SET =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    '\t',
                                    '\n',
                                    '\f',
                                    '\r',
                                    (char) Character.LINE_SEPARATOR,
                                    (char) Character.PARAGRAPH_SEPARATOR,
                                    '\u000b',
                                    '\u001c',
                                    '\u001d',
                                    '\u001e',
                                    '\u001f'
                            )));
    public static int indexOfWrap(final CharSequence text, final int width, final int startPos) {
        if (width < 1) {
            throw new IllegalArgumentException("Width must be greater than 0");
        }
        final int limit = Math.min(startPos + width, text.length() - 1);
        for (int idx = startPos; idx < limit; idx++) {
            if (BREAK_CHAR_SET.contains(text.charAt(idx))) {
                return idx;
            }
        }
        if (startPos + width >= text.length()) {
            return text.length();
        }
        int pos;
        for (pos = limit; pos >= startPos; --pos) {
            if (Util.isWhitespace(text.charAt(pos))) {
                break;
            }
        }
        return pos > startPos ? pos : limit - 1;
    }
    protected static TextHelpAppendable systemOut() {
        return new TextHelpAppendable(System.out);
    }
    private final TextStyle.Builder textStyleBuilder;
    public TextHelpAppendable(final Appendable output) {
        super(output);
        textStyleBuilder =
                TextStyle.builder()
                        .setMaxWidth(DEFAULT_WIDTH)
                        .setLeftPad(DEFAULT_LEFT_PAD)
                        .setIndent(DEFAULT_INDENT);
    }
    protected TableDefinition adjustTableFormat(final TableDefinition table) {
        final List<TextStyle.Builder> styleBuilders = new ArrayList<>();
        for (int i = 0; i < table.columnTextStyles().size(); i++) {
            final TextStyle style = table.columnTextStyles().get(i);
            final TextStyle.Builder builder = TextStyle.builder().setTextStyle(style);
            styleBuilders.add(builder);
            final String header = table.headers().get(i);
            if (style.getMaxWidth() < header.length()
                    || style.getMaxWidth() == TextStyle.UNSET_MAX_WIDTH) {
                builder.setMaxWidth(header.length());
            }
            if (style.getMinWidth() < header.length()) {
                builder.setMinWidth(header.length());
            }
            for (final List<String> row : table.rows()) {
                final String cell = row.get(i);
                if (cell.length() > builder.getMaxWidth()) {
                    builder.setMaxWidth(cell.length());
                }
            }
        }
        int calcWidth = 0;
        int adjustedMaxWidth = textStyleBuilder.getMaxWidth();
        for (final TextStyle.Builder builder : styleBuilders) {
            adjustedMaxWidth -= builder.getLeftPad();
            if (builder.isScalable()) {
                calcWidth += builder.getMaxWidth();
            } else {
                adjustedMaxWidth -= builder.getMaxWidth();
            }
        }
        if (calcWidth > adjustedMaxWidth) {
            final double fraction = adjustedMaxWidth * 1.0 / calcWidth;
            for (int i = 0; i < styleBuilders.size(); i++) {
                final TextStyle.Builder builder = styleBuilders.get(i);
                if (builder.isScalable()) {
                    styleBuilders.set(i, resize(builder, fraction));
                }
            }
        }
        final List<TextStyle> styles = new ArrayList<>();
        for (final TextStyle.Builder builder : styleBuilders) {
            styles.add(builder.get());
        }
        return TableDefinition.from(table.caption(), styles, table.headers(), table.rows());
    }
    @Override
    public void appendHeader(final int level, final CharSequence text) throws IOException {
        if (!Util.isEmpty(text)) {
            if (level < 1) {
                throw new IllegalArgumentException("level must be at least 1");
            }
            final char[] fillChars = {'=', '%', '+', '_'};
            final int idx = Math.min(level, fillChars.length) - 1;
            final TextStyle style = textStyleBuilder.get();
            final Queue<String> queue = makeColumnQueue(text, style);
            queue.add(
                    Util.repeatSpace(style.getLeftPad())
                            + Util.repeat(Math.min(text.length(), style.getMaxWidth()), fillChars[idx]));
            queue.add(BLANK_LINE);
            printQueue(queue);
        }
    }
    @Override
    public void appendList(final boolean ordered, final Collection<CharSequence> list)
            throws IOException {
        if (list != null && !list.isEmpty()) {
            final TextStyle.Builder builder =
                    TextStyle.builder()
                            .setLeftPad(textStyleBuilder.getLeftPad())
                            .setIndent(DEFAULT_LIST_INDENT);
            int i = 1;
            for (final CharSequence line : list) {
                final String entry =
                        ordered
                                ? String.format(" %s. %s", i++, Util.defaultValue(line, BLANK_LINE))
                                : String.format(" * %s", Util.defaultValue(line, BLANK_LINE));
                builder.setMaxWidth(Math.min(textStyleBuilder.getMaxWidth(), entry.length()));
                printQueue(makeColumnQueue(entry, builder.get()));
            }
            output.append(System.lineSeparator());
        }
    }
    @Override
    public void appendParagraph(final CharSequence paragraph) throws IOException {
        if (!Util.isEmpty(paragraph)) {
            final Queue<String> queue = makeColumnQueue(paragraph, textStyleBuilder.get());
            queue.add(BLANK_LINE);
            printQueue(queue);
        }
    }
    @Override
    public void appendTable(final TableDefinition rawTable) throws IOException {
        final TableDefinition table = adjustTableFormat(rawTable);
        appendParagraph(table.caption());
        final List<TextStyle> headerStyles = new ArrayList<>();
        for (final TextStyle style : table.columnTextStyles()) {
            headerStyles.add(
                    TextStyle.builder().setTextStyle(style).setAlignment(TextStyle.Alignment.CENTER).get());
        }
        writeColumnQueues(makeColumnQueues(table.headers(), headerStyles), headerStyles);
        for (final List<String> row : table.rows()) {
            writeColumnQueues(makeColumnQueues(row, table.columnTextStyles()), table.columnTextStyles());
        }
        output.append(System.lineSeparator());
    }
    @Override
    public void appendTitle(final CharSequence title) throws IOException {
        if (!Util.isEmpty(title)) {
            final TextStyle style = textStyleBuilder.get();
            final Queue<String> queue = makeColumnQueue(title, style);
            queue.add(
                    Util.repeatSpace(style.getLeftPad())
                            + Util.repeat(Math.min(title.length(), style.getMaxWidth()), '#'));
            queue.add(BLANK_LINE);
            printQueue(queue);
        }
    }
    public int getIndent() {
        return textStyleBuilder.getIndent();
    }
    public int getLeftPad() {
        return textStyleBuilder.getLeftPad();
    }
    public int getMaxWidth() {
        return textStyleBuilder.getMaxWidth();
    }
    public TextStyle.Builder getTextStyleBuilder() {
        return textStyleBuilder;
    }
    protected Queue<String> makeColumnQueue(final CharSequence columnData, final TextStyle style) {
        final String lpad = Util.repeatSpace(style.getLeftPad());
        final String indent = Util.repeatSpace(style.getIndent());
        final Queue<String> result = new LinkedList<>();
        int wrapPos = 0;
        int nextPos;
        final int wrappedMaxWidth = style.getMaxWidth() - indent.length();
        while (wrapPos < columnData.length()) {
            final int workingWidth = wrapPos == 0 ? style.getMaxWidth() : wrappedMaxWidth;
            nextPos = indexOfWrap(columnData, workingWidth, wrapPos);
            final CharSequence working = columnData.subSequence(wrapPos, nextPos);
            result.add(lpad + style.pad(wrapPos > 0, working));
            wrapPos = Util.indexOfNonWhitespace(columnData, nextPos);
            wrapPos = wrapPos == -1 ? nextPos : wrapPos;
        }
        return result;
    }
    protected List<Queue<String>> makeColumnQueues(
            final List<String> columnData, final List<TextStyle> styles) {
        final List<Queue<String>> result = new ArrayList<>();
        for (int i = 0; i < columnData.size(); i++) {
            result.add(makeColumnQueue(columnData.get(i), styles.get(i)));
        }
        return result;
    }
    private void printQueue(final Queue<String> queue) throws IOException {
        for (final String s : queue) {
            appendFormat("%s%n", Util.rtrim(s));
        }
    }
    public void printWrapped(final String text) throws IOException {
        printQueue(makeColumnQueue(text, this.textStyleBuilder.get()));
    }
    public void printWrapped(final String text, final TextStyle style) throws IOException {
        printQueue(makeColumnQueue(text, style));
    }
    private int resize(final int orig, final double fraction) {
        return (int) (orig * fraction);
    }
    protected TextStyle.Builder resize(final TextStyle.Builder builder, final double fraction) {
        final double indentFrac = builder.getIndent() * 1.0 / builder.getMaxWidth();
        builder.setMaxWidth(Math.max(resize(builder.getMaxWidth(), fraction), builder.getMinWidth()));
        final int maxAdjust = builder.getMaxWidth() / 3;
        int newIndent = builder.getMaxWidth() == 1 ? 0 : builder.getIndent();
        if (newIndent > maxAdjust) {
            newIndent = Math.min(resize(builder.getIndent(), indentFrac), maxAdjust);
        }
        builder.setIndent(newIndent);
        return builder;
    }
    public void setIndent(final int indent) {
        textStyleBuilder.setIndent(indent);
    }
    public void setLeftPad(final int leftPad) {
        textStyleBuilder.setLeftPad(leftPad);
    }
    public void setMaxWidth(final int maxWidth) {
        textStyleBuilder.setMaxWidth(maxWidth);
    }
    protected void writeColumnQueues(
            final List<Queue<String>> columnQueues, final List<TextStyle> styles) throws IOException {
        boolean moreData = true;
        final String lPad = Util.repeatSpace(textStyleBuilder.get().getLeftPad());
        while (moreData) {
            output.append(lPad);
            moreData = false;
            for (int i = 0; i < columnQueues.size(); i++) {
                final TextStyle style = styles.get(i);
                final Queue<String> columnQueue = columnQueues.get(i);
                final String line = columnQueue.poll();
                if (Util.isEmpty(line)) {
                    int maxWidth = style.getMaxWidth();
                    if (maxWidth == TextStyle.UNSET_MAX_WIDTH || maxWidth < 0) {
                        maxWidth = 0;
                    }
                    output.append(Util.repeatSpace(maxWidth + style.getLeftPad()));
                } else {
                    output.append(line);
                }
                moreData |= !columnQueue.isEmpty();
            }
            output.append(System.lineSeparator());
        }
    }
}