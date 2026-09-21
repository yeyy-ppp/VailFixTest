package lang3.concurrent;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public interface UncheckedFuture<V> extends Future<V> {
    static <T> Stream<UncheckedFuture<T>> map(final Collection<Future<T>> futures) {
        return futures.stream().map(UncheckedFuture::on);
    }
    static <T> Collection<UncheckedFuture<T>> on(final Collection<Future<T>> futures) {
        return map(futures).collect(Collectors.toList());
    }
    static <T> UncheckedFuture<T> on(final Future<T> future) {
        return new UncheckedFutureImpl<>(future);
    }
    @Override
    V get();
    @Override
    V get(long timeout, TimeUnit unit);
}