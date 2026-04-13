package csv;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
final class IOStream<T> implements AutoCloseable {
    private final Stream<T> stream;
    private IOStream(final Stream<T> stream) {
        this.stream = stream;
    }
    public static <T> IOStream<T> adapt(final Stream<T> stream) {
        return new IOStream<>(stream);
    }
    public static <T> IOStream<T> of(final Iterable<T> iterable) {
        if (iterable == null) {
            return new IOStream<>(Stream.empty());
        }
        return new IOStream<>(StreamSupport.stream(iterable.spliterator(), false));
    }
    @SafeVarargs
    public static <T> IOStream<T> of(final T... values) {
        if (values == null) {
            return new IOStream<>(Stream.empty());
        }
        return new IOStream<>(Stream.of(values));
    }
    public IOStream<T> limit(final long maxSize) {
        return new IOStream<>(stream.limit(maxSize));
    }
    public void forEachOrdered(final IOConsumer<T> action) {
        try {
            stream.forEachOrdered(t -> {
                try {
                    action.accept(t);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
    public boolean isParallel() {
        return stream.isParallel();
    }
    @Override
    public void close() {
        stream.close();
    }
    @FunctionalInterface
    interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }
}