package cli;
public class ParseException extends Exception {
    private static final long serialVersionUID = 9112808380089253192L;
    public static ParseException wrap(final Throwable e) throws UnsupportedOperationException {
        if (e instanceof UnsupportedOperationException) {
            throw (UnsupportedOperationException) e;
        }
        if (e instanceof ParseException) {
            return (ParseException) e;
        }
        return new ParseException(e);
    }
    public ParseException(final String message) {
        super(message);
    }
    public ParseException(final Throwable e) {
        super(e);
    }
}

