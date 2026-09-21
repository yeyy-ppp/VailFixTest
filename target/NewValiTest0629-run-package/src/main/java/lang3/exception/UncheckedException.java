package lang3.exception;
public class UncheckedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public UncheckedException(final Throwable cause) {
        super(cause);
    }
}