package lang3.concurrent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lang3.exception.UncheckedInterruptedException;
final class UncheckedFutureImpl<V> extends AbstractFutureProxy<V> implements UncheckedFuture<V> {
    UncheckedFutureImpl(final Future<V> future) {
        super(future);
    }
    @Override
    public V get() {
        try {
            return super.get();
        } catch (final InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        } catch (final ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }
    @Override
    public V get(final long timeout, final TimeUnit unit) {
        try {
            return super.get(timeout, unit);
        } catch (final InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        } catch (final ExecutionException e) {
            throw new UncheckedExecutionException(e);
        } catch (final TimeoutException e) {
            throw new UncheckedTimeoutException(e);
        }
    }
}