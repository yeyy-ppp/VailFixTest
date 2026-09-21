package lang3.concurrent;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public abstract class AbstractFutureProxy<V> implements Future<V> {
    private final Future<V> future;
    public AbstractFutureProxy(final Future<V> future) {
        this.future = Objects.requireNonNull(future, "future");
    }
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }
    @Override
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }
    @Override
    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
    public Future<V> getFuture() {
        return future;
    }
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }
    @Override
    public boolean isDone() {
        return future.isDone();
    }
}