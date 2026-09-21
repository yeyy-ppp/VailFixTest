package csv;
import static csv.Constants.CR;
import static csv.Constants.EOF;
import static csv.Constants.LF;
import static csv.Constants.UNDEFINED;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
public final class ExtendedBufferedReader extends Reader {
    private final BufferedReader reader;
    private int lastChar = UNDEFINED;
    private int lastCharMark = UNDEFINED;
    private long lineNumber;
    private long lineNumberMark;
    private long position;
    private long positionMark;
    private long bytesRead;
    private long bytesReadMark;
    private final CharsetEncoder encoder;
    private boolean closed = false;
    ExtendedBufferedReader(final Reader reader) {
        this(reader, null, false);
    }
    ExtendedBufferedReader(final Reader reader, final Charset charset, final boolean trackBytes) {
        this.reader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        this.encoder = charset != null && trackBytes ? charset.newEncoder() : null;
    }
    @Override
    public void close() throws IOException {
        closed = true;
        lastChar = EOF;
        reader.close();
    }
    boolean isClosed() {
        return closed;
    }
    long getBytesRead() {
        return this.bytesRead;
    }
    private int getEncodedCharLength(final int current) throws CharacterCodingException {
        final char cChar = (char) current;
        final char lChar = (char) lastChar;
        if (!Character.isSurrogate(cChar)) {
            return encoder.encode(CharBuffer.wrap(new char[] { cChar })).limit();
        }
        if (Character.isHighSurrogate(cChar)) {
            return 0;
        }
        if (Character.isSurrogatePair(lChar, cChar)) {
            return encoder.encode(CharBuffer.wrap(new char[] { lChar, cChar })).limit();
        }
        throw new CharacterCodingException();
    }
    int getLastChar() {
        return lastChar;
    }
    long getLineNumber() {
        if (lastChar == CR || lastChar == LF || lastChar == UNDEFINED || lastChar == EOF) {
            return lineNumber;
        }
        return lineNumber + 1;
    }
    long getPosition() {
        return this.position;
    }
    int peek() throws IOException {
        reader.mark(1);
        final int c = reader.read();
        reader.reset();
        return c;
    }
    int peek(final char[] buf) throws IOException {
        if (buf == null || buf.length == 0) {
            return 0;
        }
        reader.mark(buf.length);
        final int count = reader.read(buf, 0, buf.length);
        reader.reset();
        return count;
    }
    public void mark(final int readAheadLimit) throws IOException {
        lineNumberMark = lineNumber;
        lastCharMark = lastChar;
        positionMark = position;
        bytesReadMark = bytesRead;
        reader.mark(readAheadLimit);
    }
    @Override
    public int read() throws IOException {
        final int current = reader.read();
        if (current == CR || current == LF && lastChar != CR ||
            current == EOF && lastChar != CR && lastChar != LF && lastChar != EOF) {
            lineNumber++;
        }
        if (encoder != null && current != EOF) {
            this.bytesRead += getEncodedCharLength(current);
        }
        lastChar = current;
        position++;
        return lastChar;
    }
    @Override
    public int read(final char[] buf, final int offset, final int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        final int len = reader.read(buf, offset, length);
        if (len > 0) {
            for (int i = offset; i < offset + len; i++) {
                final char ch = buf[i];
                if (ch == LF) {
                    if (CR != (i > offset ? buf[i - 1] : lastChar)) {
                        lineNumber++;
                    }
                } else if (ch == CR) {
                    lineNumber++;
                }
            }
            lastChar = buf[offset + len - 1];
        } else if (len == EOF) {
            lastChar = EOF;
        }
        position += len;
        return len;
    }
    public String readLine() throws IOException {
        if (peek() == EOF) {
            return null;
        }
        final StringBuilder buffer = new StringBuilder();
        while (true) {
            final int current = read();
            if (current == CR) {
                final int next = peek();
                if (next == LF) {
                    read();
                }
            }
            if (current == EOF || current == LF || current == CR) {
                break;
            }
            buffer.append((char) current);
        }
        return buffer.toString();
    }
    public void reset() throws IOException {
        lineNumber = lineNumberMark;
        lastChar = lastCharMark;
        position = positionMark;
        bytesRead = bytesReadMark;
        reader.reset();
    }
}