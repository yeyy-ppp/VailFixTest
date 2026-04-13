package csv;
import java.io.IOException;
import java.util.Formatter;
import java.util.IllegalFormatException;
public class CSVException extends IOException {
    private static final long serialVersionUID = 1L;
    public CSVException(final String format, final Object... args) {
        super(String.format(format, args));
    }
}