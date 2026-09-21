package lang3.concurrent;
import lang3.exception.UncheckedException;
public class UncheckedTimeoutException extends UncheckedException {
    private static final long serialVersionUID = 1L;
    public UncheckedTimeoutException(final Throwable cause) {
        super(cause);
    }
}