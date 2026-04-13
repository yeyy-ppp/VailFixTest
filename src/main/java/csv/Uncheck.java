package csv;
import java.util.concurrent.Callable;
final class Uncheck {
    public static <T> T get(final Callable<T> callable) {
        try {
            return callable.call();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private Uncheck() {
    }
}