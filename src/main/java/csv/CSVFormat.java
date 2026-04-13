package csv;
import static csv.Constants.EOF;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
public final class CSVFormat implements Serializable {
    public static class Builder implements Supplier<CSVFormat> {
        public static Builder create() {
            return new Builder()
                    .setDelimiter(Constants.COMMA)
                    .setQuote(Constants.DOUBLE_QUOTE_CHAR)
                    .setRecordSeparator(Constants.CRLF)
                    .setIgnoreEmptyLines(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL);
        }
        public static Builder create(final CSVFormat csvFormat) {
            return new Builder(csvFormat);
        }
        private boolean allowMissingColumnNames;
        private boolean autoFlush;
        private Character commentMarker;
        private String delimiter;
        private DuplicateHeaderMode duplicateHeaderMode;
        private Character escapeCharacter;
        private String[] headerComments;
        private String[] headers;
        private boolean ignoreEmptyLines;
        private boolean ignoreHeaderCase;
        private boolean ignoreSurroundingSpaces;
        private String nullString;
        private Character quoteCharacter;
        private String quotedNullString;
        private QuoteMode quoteMode;
        private String recordSeparator;
        private boolean skipHeaderRecord;
        private boolean lenientEof;
        private boolean trailingData;
        private boolean trailingDelimiter;
        private boolean trim;
        private long maxRows;
        private Builder() {
        }
        private Builder(final CSVFormat csvFormat) {
            this.allowMissingColumnNames = csvFormat.allowMissingColumnNames;
            this.autoFlush = csvFormat.autoFlush;
            this.commentMarker = csvFormat.commentMarker;
            this.delimiter = csvFormat.delimiter;
            this.duplicateHeaderMode = csvFormat.duplicateHeaderMode;
            this.escapeCharacter = csvFormat.escapeCharacter;
            this.headerComments = csvFormat.headerComments;
            this.headers = csvFormat.headers;
            this.ignoreEmptyLines = csvFormat.ignoreEmptyLines;
            this.ignoreHeaderCase = csvFormat.ignoreHeaderCase;
            this.ignoreSurroundingSpaces = csvFormat.ignoreSurroundingSpaces;
            this.lenientEof = csvFormat.lenientEof;
            this.maxRows = csvFormat.maxRows;
            this.nullString = csvFormat.nullString;
            this.quoteCharacter = csvFormat.quoteCharacter;
            this.quoteMode = csvFormat.quoteMode;
            this.quotedNullString = csvFormat.quotedNullString;
            this.recordSeparator = csvFormat.recordSeparator;
            this.skipHeaderRecord = csvFormat.skipHeaderRecord;
            this.trailingData = csvFormat.trailingData;
            this.trailingDelimiter = csvFormat.trailingDelimiter;
            this.trim = csvFormat.trim;
        }
        @Deprecated
        public CSVFormat build() {
            return get();
        }
        @Override
        public CSVFormat get() {
            return new CSVFormat(this);
        }
        @Deprecated
        public Builder setAllowDuplicateHeaderNames(final boolean allowDuplicateHeaderNames) {
            setDuplicateHeaderMode(allowDuplicateHeaderNames ? DuplicateHeaderMode.ALLOW_ALL : DuplicateHeaderMode.ALLOW_EMPTY);
            return this;
        }
        public Builder setAllowMissingColumnNames(final boolean allowMissingColumnNames) {
            this.allowMissingColumnNames = allowMissingColumnNames;
            return this;
        }
        public Builder setAutoFlush(final boolean autoFlush) {
            this.autoFlush = autoFlush;
            return this;
        }
        public Builder setCommentMarker(final char commentMarker) {
            setCommentMarker(Character.valueOf(commentMarker));
            return this;
        }
        public Builder setCommentMarker(final Character commentMarker) {
            if (isLineBreak(commentMarker)) {
                throw new IllegalArgumentException("The comment start marker character cannot be a line break");
            }
            this.commentMarker = commentMarker;
            return this;
        }
        public Builder setDelimiter(final char delimiter) {
            return setDelimiter(String.valueOf(delimiter));
        }
        public Builder setDelimiter(final String delimiter) {
            if (containsLineBreak(delimiter)) {
                throw new IllegalArgumentException("The delimiter cannot be a line break");
            }
            if (delimiter.isEmpty()) {
                throw new IllegalArgumentException("The delimiter cannot be empty");
            }
            this.delimiter = delimiter;
            return this;
        }
        public Builder setDuplicateHeaderMode(final DuplicateHeaderMode duplicateHeaderMode) {
            this.duplicateHeaderMode = Objects.requireNonNull(duplicateHeaderMode, "duplicateHeaderMode");
            return this;
        }
        public Builder setEscape(final char escapeCharacter) {
            setEscape(Character.valueOf(escapeCharacter));
            return this;
        }
        public Builder setEscape(final Character escapeCharacter) {
            if (isLineBreak(escapeCharacter)) {
                throw new IllegalArgumentException("The escape character cannot be a line break");
            }
            this.escapeCharacter = escapeCharacter;
            return this;
        }
        public Builder setHeader(final Class<? extends Enum<?>> headerEnum) {
            String[] header = null;
            if (headerEnum != null) {
                final Enum<?>[] enumValues = headerEnum.getEnumConstants();
                header = new String[enumValues.length];
                Arrays.setAll(header, i -> enumValues[i].name());
            }
            return setHeader(header);
        }
        public Builder setHeader(final ResultSet resultSet) throws SQLException {
            return setHeader(resultSet != null ? resultSet.getMetaData() : null);
        }
        public Builder setHeader(final ResultSetMetaData resultSetMetaData) throws SQLException {
            String[] labels = null;
            if (resultSetMetaData != null) {
                final int columnCount = resultSetMetaData.getColumnCount();
                labels = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    labels[i] = resultSetMetaData.getColumnLabel(i + 1);
                }
            }
            return setHeader(labels);
        }
        public Builder setHeader(final String... header) {
            this.headers = CSVFormat.clone(header);
            return this;
        }
        public Builder setHeaderComments(final Object... headerComments) {
            this.headerComments = CSVFormat.clone(toStringArray(headerComments));
            return this;
        }
        public Builder setHeaderComments(final String... headerComments) {
            this.headerComments = CSVFormat.clone(headerComments);
            return this;
        }
        public Builder setIgnoreEmptyLines(final boolean ignoreEmptyLines) {
            this.ignoreEmptyLines = ignoreEmptyLines;
            return this;
        }
        public Builder setIgnoreHeaderCase(final boolean ignoreHeaderCase) {
            this.ignoreHeaderCase = ignoreHeaderCase;
            return this;
        }
        public Builder setIgnoreSurroundingSpaces(final boolean ignoreSurroundingSpaces) {
            this.ignoreSurroundingSpaces = ignoreSurroundingSpaces;
            return this;
        }
        public Builder setLenientEof(final boolean lenientEof) {
            this.lenientEof = lenientEof;
            return this;
        }
        public Builder setMaxRows(final long maxRows) {
            this.maxRows = maxRows;
            return this;
        }
        public Builder setNullString(final String nullString) {
            this.nullString = nullString;
            this.quotedNullString = quoteCharacter + nullString + quoteCharacter;
            return this;
        }
        public Builder setQuote(final char quoteCharacter) {
            setQuote(Character.valueOf(quoteCharacter));
            return this;
        }
        public Builder setQuote(final Character quoteCharacter) {
            if (isLineBreak(quoteCharacter)) {
                throw new IllegalArgumentException("The quoteCharacter cannot be a line break");
            }
            this.quoteCharacter = quoteCharacter;
            return this;
        }
        public Builder setQuoteMode(final QuoteMode quoteMode) {
            this.quoteMode = quoteMode;
            return this;
        }
        public Builder setRecordSeparator(final char recordSeparator) {
            this.recordSeparator = String.valueOf(recordSeparator);
            return this;
        }
        public Builder setRecordSeparator(final String recordSeparator) {
            this.recordSeparator = recordSeparator;
            return this;
        }
        public Builder setSkipHeaderRecord(final boolean skipHeaderRecord) {
            this.skipHeaderRecord = skipHeaderRecord;
            return this;
        }
        public Builder setTrailingData(final boolean trailingData) {
            this.trailingData = trailingData;
            return this;
        }
        public Builder setTrailingDelimiter(final boolean trailingDelimiter) {
            this.trailingDelimiter = trailingDelimiter;
            return this;
        }
        public Builder setTrim(final boolean trim) {
            this.trim = trim;
            return this;
        }
    }
    public enum Predefined {
        Default(DEFAULT),
        Excel(EXCEL),
        InformixUnload(INFORMIX_UNLOAD),
        InformixUnloadCsv(INFORMIX_UNLOAD_CSV),
        MongoDBCsv(MONGODB_CSV),
        MongoDBTsv(MONGODB_TSV),
        MySQL(MYSQL),
        Oracle(ORACLE),
        PostgreSQLCsv(POSTGRESQL_CSV),
        PostgreSQLText(POSTGRESQL_TEXT),
        RFC4180(CSVFormat.RFC4180),
        TDF(CSVFormat.TDF);
        private final CSVFormat format;
        Predefined(final CSVFormat format) {
            this.format = format;
        }
        public CSVFormat getFormat() {
            return format;
        }
    }
    public static final CSVFormat DEFAULT = new CSVFormat(Builder.create());
    public static final CSVFormat EXCEL = DEFAULT.builder()
            .setIgnoreEmptyLines(false)
            .setAllowMissingColumnNames(true)
            .setTrailingData(true)
            .setLenientEof(true)
            .get();
    public static final CSVFormat INFORMIX_UNLOAD = DEFAULT.builder()
            .setDelimiter(Constants.PIPE)
            .setEscape(Constants.BACKSLASH)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setRecordSeparator(Constants.LF)
            .get();
    public static final CSVFormat INFORMIX_UNLOAD_CSV = DEFAULT.builder()
            .setDelimiter(Constants.COMMA)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setRecordSeparator(Constants.LF)
            .get();
    public static final CSVFormat MONGODB_CSV = DEFAULT.builder()
            .setDelimiter(Constants.COMMA)
            .setEscape(Constants.DOUBLE_QUOTE_CHAR)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setQuoteMode(QuoteMode.MINIMAL)
            .get();
    public static final CSVFormat MONGODB_TSV = DEFAULT.builder()
            .setDelimiter(Constants.TAB)
            .setEscape(Constants.DOUBLE_QUOTE_CHAR)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setQuoteMode(QuoteMode.MINIMAL)
            .setSkipHeaderRecord(false)
            .get();
    public static final CSVFormat MYSQL = DEFAULT.builder()
            .setDelimiter(Constants.TAB)
            .setEscape(Constants.BACKSLASH)
            .setIgnoreEmptyLines(false)
            .setQuote(null)
            .setRecordSeparator(Constants.LF)
            .setNullString(Constants.SQL_NULL_STRING)
            .setQuoteMode(QuoteMode.ALL_NON_NULL)
            .get();
    public static final CSVFormat ORACLE = DEFAULT.builder()
            .setDelimiter(Constants.COMMA)
            .setEscape(Constants.BACKSLASH)
            .setIgnoreEmptyLines(false)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setNullString(Constants.SQL_NULL_STRING)
            .setTrim(true)
            .setRecordSeparator(System.lineSeparator())
            .setQuoteMode(QuoteMode.MINIMAL)
            .get();
    public static final CSVFormat POSTGRESQL_CSV = DEFAULT.builder()
            .setDelimiter(Constants.COMMA)
            .setEscape(null)
            .setIgnoreEmptyLines(false)
            .setQuote(Constants.DOUBLE_QUOTE_CHAR)
            .setRecordSeparator(Constants.LF)
            .setNullString(Constants.EMPTY)
            .setQuoteMode(QuoteMode.ALL_NON_NULL)
            .get();
    public static final CSVFormat POSTGRESQL_TEXT = DEFAULT.builder()
            .setDelimiter(Constants.TAB)
            .setEscape(Constants.BACKSLASH)
            .setIgnoreEmptyLines(false)
            .setQuote(null)
            .setRecordSeparator(Constants.LF)
            .setNullString(Constants.SQL_NULL_STRING)
            .setQuoteMode(QuoteMode.ALL_NON_NULL)
            .get();
    public static final CSVFormat RFC4180 = DEFAULT.builder().setIgnoreEmptyLines(false).get();
    private static final long serialVersionUID = 2L;
    public static final CSVFormat TDF = DEFAULT.builder()
            .setDelimiter(Constants.TAB)
            .setIgnoreSurroundingSpaces(true)
            .get();
    @SafeVarargs
    static <T> T[] clone(final T... values) {
        return values == null ? null : values.clone();
    }
    private static boolean contains(final String source, final char searchCh) {
        return Objects.requireNonNull(source, "source").indexOf(searchCh) >= 0;
    }
    private static boolean containsLineBreak(final String source) {
        return contains(source, Constants.CR) || contains(source, Constants.LF);
    }
    static CSVFormat copy(final CSVFormat format) {
        return format != null ? format.copy() : null;
    }
    static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
    private static boolean isLineBreak(final char c) {
        return c == Constants.LF || c == Constants.CR;
    }
    private static boolean isLineBreak(final Character c) {
        return c != null && isLineBreak(c.charValue());
    }
    private static boolean isTrimChar(final char ch) {
        return ch <= Constants.SP;
    }
    private static boolean isTrimChar(final CharSequence charSequence, final int pos) {
        return isTrimChar(charSequence.charAt(pos));
    }
    public static CSVFormat newFormat(final char delimiter) {
        return new CSVFormat(new Builder().setDelimiter(delimiter));
    }
    static String[] toStringArray(final Object[] values) {
        if (values == null) {
            return null;
        }
        final String[] strings = new String[values.length];
        Arrays.setAll(strings, i -> Objects.toString(values[i], null));
        return strings;
    }
    static CharSequence trim(final CharSequence charSequence) {
        if (charSequence instanceof String) {
            return ((String) charSequence).trim();
        }
        final int count = charSequence.length();
        int len = count;
        int pos = 0;
        while (pos < len && isTrimChar(charSequence, pos)) {
            pos++;
        }
        while (pos < len && isTrimChar(charSequence, len - 1)) {
            len--;
        }
        return pos > 0 || len < count ? charSequence.subSequence(pos, len) : charSequence;
    }
    public static CSVFormat valueOf(final String format) {
        return Predefined.valueOf(format).getFormat();
    }
    private final DuplicateHeaderMode duplicateHeaderMode;
    private final boolean allowMissingColumnNames;
    private final boolean autoFlush;
    private final Character commentMarker;
    private final String delimiter;
    private final Character escapeCharacter;
    private final String[] headers;
    private final String[] headerComments;
    private final boolean ignoreEmptyLines;
    private final boolean ignoreHeaderCase;
    private final boolean ignoreSurroundingSpaces;
    private final String nullString;
    private final Character quoteCharacter;
    private final String quotedNullString;
    private final QuoteMode quoteMode;
    private final String recordSeparator;
    private final boolean skipHeaderRecord;
    private final boolean lenientEof;
    private final boolean trailingData;
    private final boolean trailingDelimiter;
    private final boolean trim;
    private final long maxRows;
    private CSVFormat(final Builder builder) {
        this.allowMissingColumnNames = builder.allowMissingColumnNames;
        this.autoFlush = builder.autoFlush;
        this.commentMarker = builder.commentMarker;
        this.delimiter = builder.delimiter;
        this.duplicateHeaderMode = builder.duplicateHeaderMode;
        this.escapeCharacter = builder.escapeCharacter;
        this.headerComments = builder.headerComments;
        this.headers = builder.headers;
        this.ignoreEmptyLines = builder.ignoreEmptyLines;
        this.ignoreHeaderCase = builder.ignoreHeaderCase;
        this.ignoreSurroundingSpaces = builder.ignoreSurroundingSpaces;
        this.lenientEof = builder.lenientEof;
        this.maxRows = builder.maxRows;
        this.nullString = builder.nullString;
        this.quoteCharacter = builder.quoteCharacter;
        this.quoteMode = builder.quoteMode;
        this.quotedNullString = builder.quotedNullString;
        this.recordSeparator = builder.recordSeparator;
        this.skipHeaderRecord = builder.skipHeaderRecord;
        this.trailingData = builder.trailingData;
        this.trailingDelimiter = builder.trailingDelimiter;
        this.trim = builder.trim;
        validate();
    }
    private void append(final char c, final Appendable appendable) throws IOException {
        appendable.append(c);
    }
    private void append(final CharSequence csq, final Appendable appendable) throws IOException {
        appendable.append(csq);
    }
    public Builder builder() {
        return Builder.create(this);
    }
    CSVFormat copy() {
        return builder().get();
    }
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CSVFormat other = (CSVFormat) obj;
        return allowMissingColumnNames == other.allowMissingColumnNames && autoFlush == other.autoFlush &&
                Objects.equals(commentMarker, other.commentMarker) && Objects.equals(delimiter, other.delimiter) &&
                duplicateHeaderMode == other.duplicateHeaderMode && Objects.equals(escapeCharacter, other.escapeCharacter) &&
                Arrays.equals(headerComments, other.headerComments) && Arrays.equals(headers, other.headers) &&
                ignoreEmptyLines == other.ignoreEmptyLines && ignoreHeaderCase == other.ignoreHeaderCase &&
                ignoreSurroundingSpaces == other.ignoreSurroundingSpaces && lenientEof == other.lenientEof &&
                Objects.equals(nullString, other.nullString) && Objects.equals(quoteCharacter, other.quoteCharacter) &&
                quoteMode == other.quoteMode && Objects.equals(quotedNullString, other.quotedNullString) &&
                Objects.equals(recordSeparator, other.recordSeparator) && skipHeaderRecord == other.skipHeaderRecord &&
                trailingData == other.trailingData && trailingDelimiter == other.trailingDelimiter && trim == other.trim;
    }
    private void escape(final char c, final Appendable appendable) throws IOException {
        append(escapeCharacter.charValue(), appendable);
        append(c, appendable);
    }
    public String format(final Object... values) {
        return Uncheck.get(() -> format_(values));
    }
    private String format_(final Object... values) throws IOException {
        final StringWriter out = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(out, this)) {
            csvPrinter.printRecord(values);
            final String res = out.toString();
            final int len = recordSeparator != null ? res.length() - recordSeparator.length() : res.length();
            return res.substring(0, len);
        }
    }
    @Deprecated
    public boolean getAllowDuplicateHeaderNames() {
        return duplicateHeaderMode == DuplicateHeaderMode.ALLOW_ALL;
    }
    public boolean getAllowMissingColumnNames() {
        return allowMissingColumnNames;
    }
    public boolean getAutoFlush() {
        return autoFlush;
    }
    public Character getCommentMarker() {
        return commentMarker;
    }
    @Deprecated
    public char getDelimiter() {
        return delimiter.charAt(0);
    }
    char[] getDelimiterCharArray() {
        return delimiter.toCharArray();
    }
    public String getDelimiterString() {
        return delimiter;
    }
    public DuplicateHeaderMode getDuplicateHeaderMode() {
        return duplicateHeaderMode;
    }
    char getEscapeChar() {
        return escapeCharacter != null ? escapeCharacter.charValue() : 0;
    }
    public Character getEscapeCharacter() {
        return escapeCharacter;
    }
    public String[] getHeader() {
        return headers != null ? headers.clone() : null;
    }
    public String[] getHeaderComments() {
        return headerComments != null ? headerComments.clone() : null;
    }
    public boolean getIgnoreEmptyLines() {
        return ignoreEmptyLines;
    }
    public boolean getIgnoreHeaderCase() {
        return ignoreHeaderCase;
    }
    public boolean getIgnoreSurroundingSpaces() {
        return ignoreSurroundingSpaces;
    }
    public boolean getLenientEof() {
        return lenientEof;
    }
    public long getMaxRows() {
        return maxRows;
    }
    public String getNullString() {
        return nullString;
    }
    public Character getQuoteCharacter() {
        return quoteCharacter;
    }
    public QuoteMode getQuoteMode() {
        return quoteMode;
    }
    public String getRecordSeparator() {
        return recordSeparator;
    }
    public boolean getSkipHeaderRecord() {
        return skipHeaderRecord;
    }
    public boolean getTrailingData() {
        return trailingData;
    }
    public boolean getTrailingDelimiter() {
        return trailingDelimiter;
    }
    public boolean getTrim() {
        return trim;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(headerComments);
        result = prime * result + Arrays.hashCode(headers);
        return prime * result + Objects.hash(allowMissingColumnNames, autoFlush, commentMarker, delimiter, duplicateHeaderMode, escapeCharacter,
                ignoreEmptyLines, ignoreHeaderCase, ignoreSurroundingSpaces, lenientEof, nullString, quoteCharacter, quoteMode, quotedNullString,
                recordSeparator, skipHeaderRecord, trailingData, trailingDelimiter, trim);
    }
    public boolean isCommentMarkerSet() {
        return commentMarker != null;
    }
    private boolean isDelimiter(final char ch0, final CharSequence charSeq, final int startIndex, final char[] delimiter, final int delimiterLength) {
        if (ch0 != delimiter[0]) {
            return false;
        }
        final int len = charSeq.length();
        if (startIndex + delimiterLength > len) {
            return false;
        }
        for (int i = 1; i < delimiterLength; i++) {
            if (charSeq.charAt(startIndex + i) != delimiter[i]) {
                return false;
            }
        }
        return true;
    }
    public boolean isEscapeCharacterSet() {
        return escapeCharacter != null;
    }
    public boolean isNullStringSet() {
        return nullString != null;
    }
    public boolean isQuoteCharacterSet() {
        return quoteCharacter != null;
    }
    <T> IOStream<T> limit(final IOStream<T> stream) {
        return useMaxRows() ? stream.limit(getMaxRows()) : stream;
    }
    public CSVParser parse(final Reader reader) throws IOException {
        return CSVParser.builder().setReader(reader).setFormat(this).get();
    }
    public CSVPrinter print(final Appendable out) throws IOException {
        return new CSVPrinter(out, this);
    }
    public CSVPrinter print(final File out, final Charset charset) throws IOException {
        return print(out.toPath(), charset);
    }
    private void print(final InputStream inputStream, final Appendable out, final boolean newRecord) throws IOException {
        if (!newRecord) {
            append(getDelimiterString(), out);
        }
        final boolean quoteCharacterSet = isQuoteCharacterSet();
        if (quoteCharacterSet) {
            append(getQuoteCharacter().charValue(), out);
        }
        try (OutputStream outputStream = new Base64OutputStream(new AppendableOutputStream<>(out))) {
            IOUtilsHelper.copy(inputStream, outputStream);
        }
        if (quoteCharacterSet) {
            append(getQuoteCharacter().charValue(), out);
        }
    }
    public synchronized void print(final Object value, final Appendable out, final boolean newRecord) throws IOException {
        CharSequence charSequence;
        if (value == null) {
            if (null == nullString) {
                charSequence = Constants.EMPTY;
            } else if (QuoteMode.ALL == quoteMode) {
                charSequence = quotedNullString;
            } else {
                charSequence = nullString;
            }
        } else if (value instanceof CharSequence) {
            charSequence = (CharSequence) value;
        } else if (value instanceof Reader) {
            print((Reader) value, out, newRecord);
            return;
        } else if (value instanceof InputStream) {
            print((InputStream) value, out, newRecord);
            return;
        } else {
            charSequence = value.toString();
        }
        charSequence = getTrim() ? trim(charSequence) : charSequence;
        print(value, charSequence, out, newRecord);
    }
    private synchronized void print(final Object object, final CharSequence value, final Appendable out, final boolean newRecord) throws IOException {
        final int offset = 0;
        final int len = value.length();
        if (!newRecord) {
            out.append(getDelimiterString());
        }
        if (object == null) {
            out.append(value);
        } else if (isQuoteCharacterSet()) {
            printWithQuotes(object, value, out, newRecord);
        } else if (isEscapeCharacterSet()) {
            printWithEscapes(value, out);
        } else {
            out.append(value, offset, len);
        }
    }
    @SuppressWarnings("resource")
    public CSVPrinter print(final Path out, final Charset charset) throws IOException {
        return print(Files.newBufferedWriter(out, charset));
    }
    private void print(final Reader reader, final Appendable out, final boolean newRecord) throws IOException {
        if (!newRecord) {
            append(getDelimiterString(), out);
        }
        if (isQuoteCharacterSet()) {
            printWithQuotes(reader, out);
        } else if (isEscapeCharacterSet()) {
            printWithEscapes(reader, out);
        } else if (out instanceof Writer) {
            IOUtilsHelper.copyLarge(reader, (Writer) out);
        } else {
            IOUtilsHelper.copy(reader, out);
        }
    }
    public CSVPrinter printer() throws IOException {
        return new CSVPrinter(System.out, this);
    }
    public synchronized void println(final Appendable appendable) throws IOException {
        if (getTrailingDelimiter()) {
            append(getDelimiterString(), appendable);
        }
        if (recordSeparator != null) {
            append(recordSeparator, appendable);
        }
    }
    public synchronized void printRecord(final Appendable appendable, final Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            print(values[i], appendable, i == 0);
        }
        println(appendable);
    }
    private void printWithEscapes(final CharSequence charSeq, final Appendable appendable) throws IOException {
        int start = 0;
        int pos = 0;
        final int end = charSeq.length();
        final char[] delimArray = getDelimiterCharArray();
        final int delimLength = delimArray.length;
        final char escape = getEscapeChar();
        while (pos < end) {
            char c = charSeq.charAt(pos);
            final boolean isDelimiterStart = isDelimiter(c, charSeq, pos, delimArray, delimLength);
            final boolean isCr = c == Constants.CR;
            final boolean isLf = c == Constants.LF;
            if (isCr || isLf || c == escape || isDelimiterStart) {
                if (pos > start) {
                    appendable.append(charSeq, start, pos);
                }
                if (isLf) {
                    c = 'n';
                } else if (isCr) {
                    c = 'r';
                }
                escape(c, appendable);
                if (isDelimiterStart) {
                    for (int i = 1; i < delimLength; i++) {
                        pos++;
                        escape(charSeq.charAt(pos), appendable);
                    }
                }
                start = pos + 1;
            }
            pos++;
        }
        if (pos > start) {
            appendable.append(charSeq, start, pos);
        }
    }
    private void printWithEscapes(final Reader reader, final Appendable appendable) throws IOException {
        int start = 0;
        int pos = 0;
        @SuppressWarnings("resource")
        final ExtendedBufferedReader bufferedReader = new ExtendedBufferedReader(reader);
        final char[] delimArray = getDelimiterCharArray();
        final int delimLength = delimArray.length;
        final char escape = getEscapeChar();
        final StringBuilder builder = new StringBuilder(IOUtilsHelper.DEFAULT_BUFFER_SIZE);
        int c;
        final char[] lookAheadBuffer = new char[delimLength - 1];
        while (EOF != (c = bufferedReader.read())) {
            builder.append((char) c);
            Arrays.fill(lookAheadBuffer, (char) 0);
            bufferedReader.peek(lookAheadBuffer);
            final String test = builder.toString() + new String(lookAheadBuffer);
            final boolean isDelimiterStart = isDelimiter((char) c, test, pos, delimArray, delimLength);
            final boolean isCr = c == Constants.CR;
            final boolean isLf = c == Constants.LF;
            if (isCr || isLf || c == escape || isDelimiterStart) {
                if (pos > start) {
                    append(builder.substring(start, pos), appendable);
                    builder.setLength(0);
                    pos = -1;
                }
                if (isLf) {
                    c = 'n';
                } else if (isCr) {
                    c = 'r';
                }
                escape((char) c, appendable);
                if (isDelimiterStart) {
                    for (int i = 1; i < delimLength; i++) {
                        escape((char) bufferedReader.read(), appendable);
                    }
                }
                start = pos + 1;
            }
            pos++;
        }
        if (pos > start) {
            appendable.append(builder, start, pos);
        }
    }
    private void printWithQuotes(final Object object, final CharSequence charSeq, final Appendable out, final boolean newRecord) throws IOException {
        boolean quote = false;
        int start = 0;
        int pos = 0;
        final int len = charSeq.length();
        final char[] delim = getDelimiterCharArray();
        final int delimLength = delim.length;
        final char quoteChar = getQuoteCharacter().charValue();
        final char escapeChar = isEscapeCharacterSet() ? getEscapeChar() : quoteChar;
        QuoteMode quoteModePolicy = getQuoteMode();
        if (quoteModePolicy == null) {
            quoteModePolicy = QuoteMode.MINIMAL;
        }
        switch (quoteModePolicy) {
        case ALL:
        case ALL_NON_NULL:
            quote = true;
            break;
        case NON_NUMERIC:
            quote = !(object instanceof Number);
            break;
        case NONE:
            printWithEscapes(charSeq, out);
            return;
        case MINIMAL:
            if (len <= 0) {
                if (newRecord) {
                    quote = true;
                }
            } else {
                char c = charSeq.charAt(pos);
                if (c <= Constants.COMMENT) {
                    quote = true;
                } else {
                    while (pos < len) {
                        c = charSeq.charAt(pos);
                        if (c == Constants.LF || c == Constants.CR || c == quoteChar || c == escapeChar || isDelimiter(c, charSeq, pos, delim, delimLength)) {
                            quote = true;
                            break;
                        }
                        pos++;
                    }
                    if (!quote) {
                        pos = len - 1;
                        c = charSeq.charAt(pos);
                        if (isTrimChar(c)) {
                            quote = true;
                        }
                    }
                }
            }
            if (!quote) {
                out.append(charSeq, start, len);
                return;
            }
            break;
        default:
            throw new IllegalStateException("Unexpected Quote value: " + quoteModePolicy);
        }
        if (!quote) {
            out.append(charSeq, start, len);
            return;
        }
        out.append(quoteChar);
        while (pos < len) {
            final char c = charSeq.charAt(pos);
            if (c == quoteChar || c == escapeChar) {
                out.append(charSeq, start, pos);
                out.append(escapeChar);
                start = pos;
            }
            pos++;
        }
        out.append(charSeq, start, pos);
        out.append(quoteChar);
    }
    private void printWithQuotes(final Reader reader, final Appendable appendable) throws IOException {
        if (getQuoteMode() == QuoteMode.NONE) {
            printWithEscapes(reader, appendable);
            return;
        }
        final char quote = getQuoteCharacter().charValue();
        append(quote, appendable);
        int c;
        while (EOF != (c = reader.read())) {
            append((char) c, appendable);
            if (c == quote) {
                append(quote, appendable);
            }
        }
        append(quote, appendable);
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Delimiter=<").append(delimiter).append('>');
        if (isEscapeCharacterSet()) {
            sb.append(Constants.SP);
            sb.append("Escape=<").append(escapeCharacter).append('>');
        }
        if (isQuoteCharacterSet()) {
            sb.append(Constants.SP);
            sb.append("QuoteChar=<").append(quoteCharacter).append('>');
        }
        if (quoteMode != null) {
            sb.append(Constants.SP);
            sb.append("QuoteMode=<").append(quoteMode).append('>');
        }
        if (isCommentMarkerSet()) {
            sb.append(Constants.SP);
            sb.append("CommentStart=<").append(commentMarker).append('>');
        }
        if (isNullStringSet()) {
            sb.append(Constants.SP);
            sb.append("NullString=<").append(nullString).append('>');
        }
        if (recordSeparator != null) {
            sb.append(Constants.SP);
            sb.append("RecordSeparator=<").append(recordSeparator).append('>');
        }
        if (getIgnoreEmptyLines()) {
            sb.append(" EmptyLines:ignored");
        }
        if (getIgnoreSurroundingSpaces()) {
            sb.append(" SurroundingSpaces:ignored");
        }
        if (getIgnoreHeaderCase()) {
            sb.append(" IgnoreHeaderCase:ignored");
        }
        sb.append(" SkipHeaderRecord:").append(skipHeaderRecord);
        if (headerComments != null) {
            sb.append(Constants.SP);
            sb.append("HeaderComments:").append(Arrays.toString(headerComments));
        }
        if (headers != null) {
            sb.append(Constants.SP);
            sb.append("Header:").append(Arrays.toString(headers));
        }
        return sb.toString();
    }
    String trim(final String value) {
        return getTrim() ? value.trim() : value;
    }
    boolean useMaxRows() {
        return getMaxRows() > 0;
    }
    boolean useRow(final long rowNum) {
        return !useMaxRows() || rowNum <= getMaxRows();
    }
    private void validate() throws IllegalArgumentException {
        if (quoteCharacter != null && contains(delimiter, quoteCharacter.charValue())) {
            throw new IllegalArgumentException("The quoteChar character and the delimiter cannot be the same ('" + quoteCharacter + "')");
        }
        if (escapeCharacter != null && contains(delimiter, escapeCharacter.charValue())) {
            throw new IllegalArgumentException("The escape character and the delimiter cannot be the same ('" + escapeCharacter + "')");
        }
        if (commentMarker != null && contains(delimiter, commentMarker.charValue())) {
            throw new IllegalArgumentException("The comment start character and the delimiter cannot be the same ('" + commentMarker + "')");
        }
        if (quoteCharacter != null && quoteCharacter.equals(commentMarker)) {
            throw new IllegalArgumentException("The comment start character and the quoteChar cannot be the same ('" + commentMarker + "')");
        }
        if (escapeCharacter != null && escapeCharacter.equals(commentMarker)) {
            throw new IllegalArgumentException("The comment start and the escape character cannot be the same ('" + commentMarker + "')");
        }
        if (escapeCharacter == null && quoteMode == QuoteMode.NONE) {
            throw new IllegalArgumentException("Quote mode set to NONE but no escape character is set");
        }
        if (headers != null && duplicateHeaderMode != DuplicateHeaderMode.ALLOW_ALL) {
            final Set<String> dupCheckSet = new HashSet<>(headers.length);
            final boolean emptyDuplicatesAllowed = duplicateHeaderMode == DuplicateHeaderMode.ALLOW_EMPTY;
            for (final String header : headers) {
                final boolean blank = isBlank(header);
                final boolean containsHeader = !dupCheckSet.add(blank ? "" : header);
                if (containsHeader && !(blank && emptyDuplicatesAllowed)) {
                    throw new IllegalArgumentException(String.format(
                            "The header contains a duplicate name: \"%s\" in %s. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().", header,
                            Arrays.toString(headers)));
                }
            }
        }
    }
    @Deprecated
    public CSVFormat withAllowDuplicateHeaderNames() {
        return builder().setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).get();
    }
    @Deprecated
    public CSVFormat withAllowDuplicateHeaderNames(final boolean allowDuplicateHeaderNames) {
        final DuplicateHeaderMode mode = allowDuplicateHeaderNames ? DuplicateHeaderMode.ALLOW_ALL : DuplicateHeaderMode.ALLOW_EMPTY;
        return builder().setDuplicateHeaderMode(mode).get();
    }
    @Deprecated
    public CSVFormat withAllowMissingColumnNames() {
        return builder().setAllowMissingColumnNames(true).get();
    }
    @Deprecated
    public CSVFormat withAllowMissingColumnNames(final boolean allowMissingColumnNames) {
        return builder().setAllowMissingColumnNames(allowMissingColumnNames).get();
    }
    @Deprecated
    public CSVFormat withAutoFlush(final boolean autoFlush) {
        return builder().setAutoFlush(autoFlush).get();
    }
    @Deprecated
    public CSVFormat withCommentMarker(final char commentMarker) {
        return builder().setCommentMarker(commentMarker).get();
    }
    @Deprecated
    public CSVFormat withCommentMarker(final Character commentMarker) {
        return builder().setCommentMarker(commentMarker).get();
    }
    @Deprecated
    public CSVFormat withDelimiter(final char delimiter) {
        return builder().setDelimiter(delimiter).get();
    }
    @Deprecated
    public CSVFormat withEscape(final char escape) {
        return builder().setEscape(escape).get();
    }
    @Deprecated
    public CSVFormat withEscape(final Character escape) {
        return builder().setEscape(escape).get();
    }
    @Deprecated
    public CSVFormat withFirstRecordAsHeader() {
        return builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
    }
    @Deprecated
    public CSVFormat withHeader(final Class<? extends Enum<?>> headerEnum) {
        return builder().setHeader(headerEnum).get();
    }
    @Deprecated
    public CSVFormat withHeader(final ResultSet resultSet) throws SQLException {
        return builder().setHeader(resultSet).get();
    }
    @Deprecated
    public CSVFormat withHeader(final ResultSetMetaData resultSetMetaData) throws SQLException {
        return builder().setHeader(resultSetMetaData).get();
    }
    @Deprecated
    public CSVFormat withHeader(final String... header) {
        return builder().setHeader(header).get();
    }
    @Deprecated
    public CSVFormat withHeaderComments(final Object... headerComments) {
        return builder().setHeaderComments(headerComments).get();
    }
    @Deprecated
    public CSVFormat withIgnoreEmptyLines() {
        return builder().setIgnoreEmptyLines(true).get();
    }
    @Deprecated
    public CSVFormat withIgnoreEmptyLines(final boolean ignoreEmptyLines) {
        return builder().setIgnoreEmptyLines(ignoreEmptyLines).get();
    }
    @Deprecated
    public CSVFormat withIgnoreHeaderCase() {
        return builder().setIgnoreHeaderCase(true).get();
    }
    @Deprecated
    public CSVFormat withIgnoreHeaderCase(final boolean ignoreHeaderCase) {
        return builder().setIgnoreHeaderCase(ignoreHeaderCase).get();
    }
    @Deprecated
    public CSVFormat withIgnoreSurroundingSpaces() {
        return builder().setIgnoreSurroundingSpaces(true).get();
    }
    @Deprecated
    public CSVFormat withIgnoreSurroundingSpaces(final boolean ignoreSurroundingSpaces) {
        return builder().setIgnoreSurroundingSpaces(ignoreSurroundingSpaces).get();
    }
    @Deprecated
    public CSVFormat withNullString(final String nullString) {
        return builder().setNullString(nullString).get();
    }
    @Deprecated
    public CSVFormat withQuote(final char quoteChar) {
        return builder().setQuote(quoteChar).get();
    }
    @Deprecated
    public CSVFormat withQuote(final Character quoteChar) {
        return builder().setQuote(quoteChar).get();
    }
    @Deprecated
    public CSVFormat withQuoteMode(final QuoteMode quoteMode) {
        return builder().setQuoteMode(quoteMode).get();
    }
    @Deprecated
    public CSVFormat withRecordSeparator(final char recordSeparator) {
        return builder().setRecordSeparator(recordSeparator).get();
    }
    @Deprecated
    public CSVFormat withRecordSeparator(final String recordSeparator) {
        return builder().setRecordSeparator(recordSeparator).get();
    }
    @Deprecated
    public CSVFormat withSkipHeaderRecord() {
        return builder().setSkipHeaderRecord(true).get();
    }
    @Deprecated
    public CSVFormat withSkipHeaderRecord(final boolean skipHeaderRecord) {
        return builder().setSkipHeaderRecord(skipHeaderRecord).get();
    }
    @Deprecated
    public CSVFormat withSystemRecordSeparator() {
        return builder().setRecordSeparator(System.lineSeparator()).get();
    }
    @Deprecated
    public CSVFormat withTrailingDelimiter() {
        return builder().setTrailingDelimiter(true).get();
    }
    @Deprecated
    public CSVFormat withTrailingDelimiter(final boolean trailingDelimiter) {
        return builder().setTrailingDelimiter(trailingDelimiter).get();
    }
    @Deprecated
    public CSVFormat withTrim() {
        return builder().setTrim(true).get();
    }
    @Deprecated
    public CSVFormat withTrim(final boolean trim) {
        return builder().setTrim(trim).get();
    }
}