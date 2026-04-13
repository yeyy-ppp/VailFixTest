package lang3.exception;
public class UncheckedInterruptedException extends UncheckedException {
    private static final long serialVersionUID = 1L;
    public UncheckedInterruptedException(final Throwable cause) {
        super(cause);
    }
}