package csv;
import static csv.Constants.EOF;
import java.io.Closeable;
import java.io.IOException;
public final class Lexer implements Closeable {
    private static final String CR_STRING = Character.toString(Constants.CR);
    private static final String LF_STRING = Character.toString(Constants.LF);
    private final char[] delimiter;
    private final char[] delimiterBuf;
    private final char[] escapeDelimiterBuf;
    private final int escape;
    private final int quoteChar;
    private final int commentStart;
    private final boolean ignoreSurroundingSpaces;
    private final boolean ignoreEmptyLines;
    private final boolean lenientEof;
    private final boolean trailingData;
    private final ExtendedBufferedReader reader;
    private String firstEol;
    private boolean isLastTokenDelimiter;
    Lexer(final CSVFormat format, final ExtendedBufferedReader reader) {
        this.reader = reader;
        this.delimiter = format.getDelimiterCharArray();
        this.escape = nullToDisabled(format.getEscapeCharacter());
        this.quoteChar = nullToDisabled(format.getQuoteCharacter());
        this.commentStart = nullToDisabled(format.getCommentMarker());
        this.ignoreSurroundingSpaces = format.getIgnoreSurroundingSpaces();
        this.ignoreEmptyLines = format.getIgnoreEmptyLines();
        this.lenientEof = format.getLenientEof();
        this.trailingData = format.getTrailingData();
        this.delimiterBuf = new char[delimiter.length - 1];
        this.escapeDelimiterBuf = new char[2 * delimiter.length - 1];
    }
    private void appendNextEscapedCharacterToToken(final Token token) throws IOException {
        if (isEscapeDelimiter()) {
            token.content.append(delimiter);
        } else {
            final int unescaped = readEscape();
            if (unescaped == EOF) {
                token.content.append((char) escape).append((char) reader.getLastChar());
            } else {
                token.content.append((char) unescaped);
            }
        }
    }
    @Override
    public void close() throws IOException {
        reader.close();
    }
    long getBytesRead() {
        return reader.getBytesRead();
    }
    long getCharacterPosition() {
        return reader.getPosition();
    }
    long getCurrentLineNumber() {
        return reader.getLineNumber();
    }
    String getFirstEol() {
        return firstEol;
    }
    boolean isClosed() {
        return reader.isClosed();
    }
    boolean isCommentStart(final int ch) {
        return ch == commentStart;
    }
    boolean isDelimiter(final int ch) throws IOException {
        isLastTokenDelimiter = false;
        if (ch != delimiter[0]) {
            return false;
        }
        if (delimiter.length == 1) {
            isLastTokenDelimiter = true;
            return true;
        }
        reader.peek(delimiterBuf);
        for (int i = 0; i < delimiterBuf.length; i++) {
            if (delimiterBuf[i] != delimiter[i + 1]) {
                return false;
            }
        }
        final int count = reader.read(delimiterBuf, 0, delimiterBuf.length);
        isLastTokenDelimiter = count != EOF;
        return isLastTokenDelimiter;
    }
    boolean isEndOfFile(final int ch) {
        return ch == EOF;
    }
    boolean isEscape(final int ch) {
        return ch == escape;
    }
    boolean isEscapeDelimiter() throws IOException {
        reader.peek(escapeDelimiterBuf);
        if (escapeDelimiterBuf[0] != delimiter[0]) {
            return false;
        }
        for (int i = 1; i < delimiter.length; i++) {
            if (escapeDelimiterBuf[2 * i] != delimiter[i] || escapeDelimiterBuf[2 * i - 1] != escape) {
                return false;
            }
        }
        final int count = reader.read(escapeDelimiterBuf, 0, escapeDelimiterBuf.length);
        return count != EOF;
    }
    boolean isMetaChar(final int ch) {
        return ch == escape || ch == quoteChar || ch == commentStart;
    }
    boolean isQuoteChar(final int ch) {
        return ch == quoteChar;
    }
    boolean isStartOfLine(final int ch) {
        return ch == Constants.LF || ch == Constants.CR || ch == Constants.UNDEFINED;
    }
    Token nextToken(final Token token) throws IOException {
        int lastChar = reader.getLastChar();
        int c = reader.read();
        boolean eol = readEndOfLine(c);
        if (ignoreEmptyLines) {
            while (eol && isStartOfLine(lastChar)) {
                lastChar = c;
                c = reader.read();
                eol = readEndOfLine(c);
                if (isEndOfFile(c)) {
                    token.type = Token.Type.EOF;
                    return token;
                }
            }
        }
        if (isEndOfFile(lastChar) || !isLastTokenDelimiter && isEndOfFile(c)) {
            token.type = Token.Type.EOF;
            return token;
        }
        if (isStartOfLine(lastChar) && isCommentStart(c)) {
            final String line = reader.readLine();
            if (line == null) {
                token.type = Token.Type.EOF;
                return token;
            }
            final String comment = line.trim();
            token.content.append(comment);
            token.type = Token.Type.COMMENT;
            return token;
        }
        while (token.type == Token.Type.INVALID) {
            if (ignoreSurroundingSpaces) {
                while (Character.isWhitespace((char) c) && !isDelimiter(c) && !eol) {
                    c = reader.read();
                    eol = readEndOfLine(c);
                }
            }
            if (isDelimiter(c)) {
                token.type = Token.Type.TOKEN;
            } else if (eol) {
                token.type = Token.Type.EORECORD;
            } else if (isQuoteChar(c)) {
                parseEncapsulatedToken(token);
            } else if (isEndOfFile(c)) {
                token.type = Token.Type.EOF;
                token.isReady = true;
            } else {
                parseSimpleToken(token, c);
            }
        }
        return token;
    }
    private int nullToDisabled(final Character c) {
        return c == null ? Constants.UNDEFINED : c.charValue();
    }
    private Token parseEncapsulatedToken(final Token token) throws IOException {
        token.isQuoted = true;
        final long startLineNumber = getCurrentLineNumber();
        int c;
        while (true) {
            c = reader.read();
            if (isQuoteChar(c)) {
                if (isQuoteChar(reader.peek())) {
                    c = reader.read();
                    token.content.append((char) c);
                } else {
                    while (true) {
                        c = reader.read();
                        if (isDelimiter(c)) {
                            token.type = Token.Type.TOKEN;
                            return token;
                        }
                        if (isEndOfFile(c)) {
                            token.type = Token.Type.EOF;
                            token.isReady = true;
                            return token;
                        }
                        if (readEndOfLine(c)) {
                            token.type = Token.Type.EORECORD;
                            return token;
                        }
                        if (trailingData) {
                            token.content.append((char) c);
                        } else if (!Character.isWhitespace((char) c)) {
                            throw new CSVException("Invalid character between encapsulated token and delimiter at line: %,d, position: %,d",
                                    getCurrentLineNumber(), getCharacterPosition());
                        }
                    }
                }
            } else if (isEscape(c)) {
                appendNextEscapedCharacterToToken(token);
            } else if (isEndOfFile(c)) {
                if (lenientEof) {
                    token.type = Token.Type.EOF;
                    token.isReady = true;
                    return token;
                }
                throw new CSVException("(startline %,d) EOF reached before encapsulated token finished", startLineNumber);
            } else {
                token.content.append((char) c);
            }
        }
    }
    private Token parseSimpleToken(final Token token, final int ch) throws IOException {
        int cur = ch;
        while (true) {
            if (readEndOfLine(cur)) {
                token.type = Token.Type.EORECORD;
                break;
            }
            if (isEndOfFile(cur)) {
                token.type = Token.Type.EOF;
                token.isReady = true;
                break;
            }
            if (isDelimiter(cur)) {
                token.type = Token.Type.TOKEN;
                break;
            }
            if (isEscape(cur)) {
                appendNextEscapedCharacterToToken(token);
            } else {
                token.content.append((char) cur);
            }
            cur = reader.read();
        }
        if (ignoreSurroundingSpaces) {
            trimTrailingSpaces(token.content);
        }
        return token;
    }
    boolean readEndOfLine(final int ch) throws IOException {
        int cur = ch;
        if (cur == Constants.CR && reader.peek() == Constants.LF) {
            cur = reader.read();
            if (firstEol == null) {
                this.firstEol = Constants.CRLF;
            }
        }
        if (firstEol == null) {
            if (cur == Constants.LF) {
                this.firstEol = LF_STRING;
            } else if (cur == Constants.CR) {
                this.firstEol = CR_STRING;
            }
        }
        return cur == Constants.LF || cur == Constants.CR;
    }
    int readEscape() throws IOException {
        final int ch = reader.read();
        switch (ch) {
        case 'r':
            return Constants.CR;
        case 'n':
            return Constants.LF;
        case 't':
            return Constants.TAB;
        case 'b':
            return Constants.BACKSPACE;
        case 'f':
            return Constants.FF;
        case Constants.CR:
        case Constants.LF:
        case Constants.FF:
        case Constants.TAB:
        case Constants.BACKSPACE:
            return ch;
        case EOF:
            throw new CSVException("EOF while processing escape sequence");
        default:
            if (isMetaChar(ch)) {
                return ch;
            }
            return EOF;
        }
    }
    void trimTrailingSpaces(final StringBuilder buffer) {
        int length = buffer.length();
        while (length > 0 && Character.isWhitespace(buffer.charAt(length - 1))) {
            length--;
        }
        if (length != buffer.length()) {
            buffer.setLength(length);
        }
    }
}