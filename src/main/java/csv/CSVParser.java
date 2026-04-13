package csv;
import static csv.Token.Type.TOKEN;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
public final class CSVParser implements Iterable<CSVRecord>, Closeable {
    public static class Builder extends AbstractStreamBuilder<CSVParser, Builder> {
        private CSVFormat format;
        private long characterOffset;
        private long recordNumber = 1;
        private boolean trackBytes;
        protected Builder() {
        }
        @SuppressWarnings("resource")
        @Override
        public CSVParser get() throws IOException {
            return new CSVParser(getReader(), format != null ? format : CSVFormat.DEFAULT, characterOffset, recordNumber, getCharset(), trackBytes);
        }
        public Builder setCharacterOffset(final long characterOffset) {
            this.characterOffset = characterOffset;
            return asThis();
        }
        public Builder setFormat(final CSVFormat format) {
            this.format = CSVFormat.copy(format);
            return asThis();
        }
        public Builder setRecordNumber(final long recordNumber) {
            this.recordNumber = recordNumber;
            return asThis();
        }
        public Builder setTrackBytes(final boolean trackBytes) {
            this.trackBytes = trackBytes;
            return asThis();
        }
    }
    final class CSVRecordIterator implements Iterator<CSVRecord> {
        private CSVRecord current;
        private CSVRecord getNextRecord() {
            CSVRecord record = null;
            if (format.useRow(recordNumber + 1)) {
                record = Uncheck.get(CSVParser.this::nextRecord);
            }
            return record;
        }
        @Override
        public boolean hasNext() {
            if (isClosed()) {
                return false;
            }
            if (current == null) {
                current = getNextRecord();
            }
            return current != null;
        }
        @Override
        public CSVRecord next() {
            if (isClosed()) {
                throw new NoSuchElementException("CSVParser has been closed");
            }
            CSVRecord next = current;
            current = null;
            if (next == null) {
                next = getNextRecord();
                if (next == null) {
                    throw new NoSuchElementException("No more CSV records available");
                }
            }
            return next;
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    private static final class Headers {
        final Map<String, Integer> headerMap;
        final List<String> headerNames;
        Headers(final Map<String, Integer> headerMap, final List<String> headerNames) {
            this.headerMap = headerMap;
            this.headerNames = headerNames;
        }
    }
    public static Builder builder() {
        return new Builder();
    }
    public static CSVParser parse(final File file, final Charset charset, final CSVFormat format) throws IOException {
        Objects.requireNonNull(file, "file");
        return parse(file.toPath(), charset, format);
    }
    public static CSVParser parse(final InputStream inputStream, final Charset charset, final CSVFormat format)
            throws IOException {
        return parse(new InputStreamReader(inputStream, Charsets.toCharset(charset)), format);
    }
    @SuppressWarnings("resource")
    public static CSVParser parse(final Path path, final Charset charset, final CSVFormat format) throws IOException {
        Objects.requireNonNull(path, "path");
        return parse(Files.newInputStream(path), charset, format);
    }
    public static CSVParser parse(final Reader reader, final CSVFormat format) throws IOException {
        return builder().setReader(reader).setFormat(format).get();
    }
    public static CSVParser parse(final String string, final CSVFormat format) throws IOException {
        Objects.requireNonNull(string, "string");
        return parse(new StringReader(string), format);
    }
    @SuppressWarnings("resource")
    public static CSVParser parse(final URL url, final Charset charset, final CSVFormat format) throws IOException {
        Objects.requireNonNull(url, "url");
        return parse(url.openStream(), charset, format);
    }
    private String headerComment;
    private String trailerComment;
    private final CSVFormat format;
    private final Headers headers;
    private final Lexer lexer;
    private final CSVRecordIterator csvRecordIterator;
    private final List<String> recordList = new ArrayList<>();
    private long recordNumber;
    private final long characterOffset;
    private final Token reusableToken = new Token();
    @Deprecated
    public CSVParser(final Reader reader, final CSVFormat format) throws IOException {
        this(reader, format, 0, 1);
    }
    @Deprecated
    public CSVParser(final Reader reader, final CSVFormat format, final long characterOffset, final long recordNumber) throws IOException {
        this(reader, format, characterOffset, recordNumber, null, false);
    }
    @SuppressWarnings("resource")
    private CSVParser(final Reader reader, final CSVFormat format, final long characterOffset, final long recordNumber, final Charset charset,
            final boolean trackBytes) throws IOException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(format, "format");
        this.format = format.copy();
        this.lexer = new Lexer(format, new ExtendedBufferedReader(reader, charset, trackBytes));
        this.csvRecordIterator = new CSVRecordIterator();
        this.headers = createHeaders();
        this.characterOffset = characterOffset;
        this.recordNumber = recordNumber - 1;
    }
    private void addRecordValue(final boolean lastRecord) {
        final String input = format.trim(reusableToken.content.toString());
        if (lastRecord && input.isEmpty() && format.getTrailingDelimiter()) {
            return;
        }
        recordList.add(handleNull(input));
    }
    @Override
    public void close() throws IOException {
        lexer.close();
    }
    private Map<String, Integer> createEmptyHeaderMap() {
        return format.getIgnoreHeaderCase() ?
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER) :
                new LinkedHashMap<>();
    }
    private Headers createHeaders() throws IOException {
        Map<String, Integer> headerMap = null;
        List<String> headerNames = null;
        final String[] formatHeader = format.getHeader();
        if (formatHeader != null) {
            headerMap = createEmptyHeaderMap();
            String[] headerRecord = null;
            if (formatHeader.length == 0) {
                final CSVRecord nextRecord = nextRecord();
                if (nextRecord != null) {
                    headerRecord = nextRecord.values();
                    headerComment = nextRecord.getComment();
                }
            } else {
                if (format.getSkipHeaderRecord()) {
                    final CSVRecord nextRecord = nextRecord();
                    if (nextRecord != null) {
                        headerComment = nextRecord.getComment();
                    }
                }
                headerRecord = formatHeader;
            }
            if (headerRecord != null) {
                boolean observedMissing = false;
                for (int i = 0; i < headerRecord.length; i++) {
                    final String header = headerRecord[i];
                    final boolean blankHeader = CSVFormat.isBlank(header);
                    if (blankHeader && !format.getAllowMissingColumnNames()) {
                        throw new IllegalArgumentException("A header name is missing in " + Arrays.toString(headerRecord));
                    }
                    final boolean containsHeader = blankHeader ? observedMissing : headerMap.containsKey(header);
                    final DuplicateHeaderMode headerMode = format.getDuplicateHeaderMode();
                    final boolean duplicatesAllowed = headerMode == DuplicateHeaderMode.ALLOW_ALL;
                    final boolean emptyDuplicatesAllowed = headerMode == DuplicateHeaderMode.ALLOW_EMPTY;
                    if (containsHeader && !duplicatesAllowed && !(blankHeader && emptyDuplicatesAllowed)) {
                        throw new IllegalArgumentException(String.format(
                                "The header contains a duplicate name: \"%s\" in %s. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().",
                                header, Arrays.toString(headerRecord)));
                    }
                    observedMissing |= blankHeader;
                    if (header != null) {
                        headerMap.put(header, Integer.valueOf(i));
                        if (headerNames == null) {
                            headerNames = new ArrayList<>(headerRecord.length);
                        }
                        headerNames.add(header);
                    }
                }
            }
        }
        return new Headers(headerMap, headerNames == null ? Collections.emptyList() : Collections.unmodifiableList(headerNames));
    }
    public long getCurrentLineNumber() {
        return lexer.getCurrentLineNumber();
    }
    public String getFirstEndOfLine() {
        return lexer.getFirstEol();
    }
    public String getHeaderComment() {
        return headerComment;
    }
    public Map<String, Integer> getHeaderMap() {
        if (headers.headerMap == null) {
            return null;
        }
        final Map<String, Integer> map = createEmptyHeaderMap();
        map.putAll(headers.headerMap);
        return map;
    }
    Map<String, Integer> getHeaderMapRaw() {
        return headers.headerMap;
    }
    public List<String> getHeaderNames() {
        return Collections.unmodifiableList(headers.headerNames);
    }
    public long getRecordNumber() {
        return recordNumber;
    }
    public List<CSVRecord> getRecords() {
        return stream().collect(Collectors.toList());
    }
    public String getTrailerComment() {
        return trailerComment;
    }
    private String handleNull(final String input) {
        final boolean isQuoted = reusableToken.isQuoted;
        final String nullString = format.getNullString();
        final boolean strictQuoteMode = isStrictQuoteMode();
        if (input.equals(nullString)) {
            return strictQuoteMode && isQuoted ? input : null;
        }
        return strictQuoteMode && nullString == null && input.isEmpty() && !isQuoted ? null : input;
    }
    public boolean hasHeaderComment() {
        return headerComment != null;
    }
    public boolean hasTrailerComment() {
        return trailerComment != null;
    }
    public boolean isClosed() {
        return lexer.isClosed();
    }
    private boolean isStrictQuoteMode() {
        return format.getQuoteMode() == QuoteMode.ALL_NON_NULL ||
               format.getQuoteMode() == QuoteMode.NON_NUMERIC;
    }
    @Override
    public Iterator<CSVRecord> iterator() {
        return csvRecordIterator;
    }
    CSVRecord nextRecord() throws IOException {
        CSVRecord result = null;
        recordList.clear();
        StringBuilder sb = null;
        final long startCharPosition = lexer.getCharacterPosition() + characterOffset;
        final long startBytePosition = lexer.getBytesRead() + characterOffset;
        do {
            reusableToken.reset();
            lexer.nextToken(reusableToken);
            switch (reusableToken.type) {
            case TOKEN:
                addRecordValue(false);
                break;
            case EORECORD:
                addRecordValue(true);
                break;
            case EOF:
                if (reusableToken.isReady) {
                    addRecordValue(true);
                } else if (sb != null) {
                    trailerComment = sb.toString();
                }
                break;
            case INVALID:
                throw new CSVException("(line %,d) invalid parse sequence", getCurrentLineNumber());
            case COMMENT:
                if (sb == null) {
                    sb = new StringBuilder();
                } else {
                    sb.append(Constants.LF);
                }
                sb.append(reusableToken.content);
                reusableToken.type = TOKEN;
                break;
            default:
                throw new CSVException("Unexpected Token type: %s", reusableToken.type);
            }
        } while (reusableToken.type == TOKEN);
        if (!recordList.isEmpty()) {
            recordNumber++;
            result = new CSVRecord(this, recordList.toArray(Constants.EMPTY_STRING_ARRAY), Objects.toString(sb, null), recordNumber, startCharPosition,
                    startBytePosition);
        }
        return result;
    }
    public Stream<CSVRecord> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}