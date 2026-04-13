package csv;
import static csv.Token.Type.INVALID;
public final class Token {
    enum Type {
        INVALID,
        TOKEN,
        EOF,
        EORECORD,
        COMMENT
    }
    private static final int DEFAULT_CAPACITY = 50;
    Type type = Type.INVALID;
    final StringBuilder content = new StringBuilder(DEFAULT_CAPACITY);
    boolean isReady;
    boolean isQuoted;
    void reset() {
        content.setLength(0);
        type = Type.INVALID;
        isReady = false;
        isQuoted = false;
    }
    @Override
    public String toString() {
        return type + " [" + content.toString() + "]";
    }
}