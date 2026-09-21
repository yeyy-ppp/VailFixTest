package lang3.concurrent;
import lang3.exception.UncheckedException;
public class UncheckedExecutionException extends UncheckedException {
    private static final long serialVersionUID = 1L;
    public UncheckedExecutionException(final Throwable cause) {
        super(cause);
    }
}