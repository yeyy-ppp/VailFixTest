package csv;
import static csv.Constants.CR;
import static csv.Constants.LF;
import static csv.Constants.SP;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
public final class CSVPrinter implements Flushable, Closeable {
    private final Appendable appendable;
    private final CSVFormat format;
    private boolean newRecord = true;
    private long recordCount;
    private final ReentrantLock lock = new ReentrantLock();
    public CSVPrinter(final Appendable appendable, final CSVFormat format) throws IOException {
        Objects.requireNonNull(appendable, "appendable");
        Objects.requireNonNull(format, "format");
        this.appendable = appendable;
        this.format = format.copy();
        final String[] headerComments = format.getHeaderComments();
        if (headerComments != null) {
            for (final String line : headerComments) {
                printComment(line);
            }
        }
        if (format.getHeader() != null && !format.getSkipHeaderRecord()) {
            this.printRecord((Object[]) format.getHeader());
        }
    }
    @Override
    public void close() throws IOException {
        close(false);
    }
    public void close(final boolean flush) throws IOException {
        if (flush || format.getAutoFlush()) {
            flush();
        }
        if (appendable instanceof Closeable) {
            ((Closeable) appendable).close();
        }
    }
    void endOfRecord() throws IOException {
        println();
        recordCount++;
    }
    @Override
    public void flush() throws IOException {
        if (appendable instanceof Flushable) {
            ((Flushable) appendable).flush();
        }
    }
    public Appendable getOut() {
        return appendable;
    }
    public long getRecordCount() {
        return recordCount;
    }
    public void print(final Object value) throws IOException {
        lock.lock();
        try {
            printRaw(value);
        } finally {
            lock.unlock();
        }
    }
    public void printComment(final String comment) throws IOException {
        lock.lock();
        try {
            if (comment == null || !format.isCommentMarkerSet()) {
                return;
            }
            if (!newRecord) {
                println();
            }
            appendable.append(format.getCommentMarker().charValue());
            appendable.append(SP);
            for (int i = 0; i < comment.length(); i++) {
                final char c = comment.charAt(i);
                switch (c) {
                case CR:
                    if (i + 1 < comment.length() && comment.charAt(i + 1) == LF) {
                        i++;
                    }
                case LF:
                    println();
                    appendable.append(format.getCommentMarker().charValue());
                    appendable.append(SP);
                    break;
                default:
                    appendable.append(c);
                    break;
                }
            }
            println();
        } finally {
            lock.unlock();
        }
    }
    public void printHeaders(final ResultSet resultSet) throws IOException, SQLException {
        lock.lock();
        try {
            try (IOStream<String> stream = IOStream.of(format.builder().setHeader(resultSet).get().getHeader())) {
                stream.forEachOrdered(this::print);
            }
            println();
        } finally {
            lock.unlock();
        }
    }
    public void println() throws IOException {
        lock.lock();
        try {
            format.println(appendable);
            newRecord = true;
        } finally {
            lock.unlock();
        }
    }
    private void printRaw(final Object value) throws IOException {
        format.print(value, appendable, newRecord);
        newRecord = false;
    }
    @SuppressWarnings("resource")
    public void printRecord(final Iterable<?> values) throws IOException {
        lock.lock();
        try {
            IOStream.of(values).forEachOrdered(this::print);
            endOfRecord();
        } finally {
            lock.unlock();
        }
    }
    public void printRecord(final Object... values) throws IOException {
        printRecord(Arrays.asList(values));
    }
    @SuppressWarnings("resource")
    public void printRecord(final Stream<?> stream) throws IOException {
        lock.lock();
        try {
            IOStream.adapt(stream).forEachOrdered(stream.isParallel() ? this::printRaw : this::print);
            endOfRecord();
        } finally {
            lock.unlock();
        }
    }
    private void printRecordObject(final Object value) throws IOException {
        if (value instanceof Object[]) {
            this.printRecord((Object[]) value);
        } else if (value instanceof Iterable) {
            this.printRecord((Iterable<?>) value);
        } else {
            this.printRecord(value);
        }
    }
    @SuppressWarnings("resource")
    private void printRecords(final IOStream<?> stream) throws IOException {
        format.limit(stream).forEachOrdered(this::printRecordObject);
    }
    @SuppressWarnings("resource")
    public void printRecords(final Iterable<?> values) throws IOException {
        printRecords(IOStream.of(values));
    }
    public void printRecords(final Object... values) throws IOException {
        printRecords(Arrays.asList(values));
    }
    public void printRecords(final ResultSet resultSet) throws SQLException, IOException {
        final int columnCount = resultSet.getMetaData().getColumnCount();
        while (resultSet.next() && format.useRow(resultSet.getRow())) {
            lock.lock();
            try {
                for (int i = 1; i <= columnCount; i++) {
                    final Object object = resultSet.getObject(i);
                    if (object instanceof Clob) {
                        try (Reader reader = ((Clob) object).getCharacterStream()) {
                            print(reader);
                        }
                    } else if (object instanceof Blob) {
                        try (InputStream inputStream = ((Blob) object).getBinaryStream()) {
                            print(inputStream);
                        }
                    } else {
                        print(object);
                    }
                }
                endOfRecord();
            } finally {
                lock.unlock();
            }
        }
    }
    public void printRecords(final ResultSet resultSet, final boolean printHeader) throws SQLException, IOException {
        if (printHeader) {
            printHeaders(resultSet);
        }
        printRecords(resultSet);
    }
    @SuppressWarnings({ "resource" })
    public void printRecords(final Stream<?> values) throws IOException {
        printRecords(IOStream.adapt(values));
    }
}