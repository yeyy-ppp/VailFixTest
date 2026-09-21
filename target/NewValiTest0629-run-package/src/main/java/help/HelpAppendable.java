package help;
import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;
import java.util.IllegalFormatException;
public interface HelpAppendable extends Appendable {
    default void appendFormat(final String format, final Object... args) throws IOException {
        append(String.format(format, args));
    }
    void appendHeader(int level, CharSequence text) throws IOException;
    void appendList(boolean ordered, Collection<CharSequence> list) throws IOException;
    void appendParagraph(CharSequence paragraph) throws IOException;
    default void appendParagraphFormat(final String format, final Object... args) throws IOException {
        appendParagraph(String.format(format, args));
    }
    void appendTable(TableDefinition table) throws IOException;
    void appendTitle(CharSequence title) throws IOException;
}