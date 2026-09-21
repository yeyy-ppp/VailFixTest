package lang3.function;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.surefire.shared.lang3.function.*;
import org.apache.maven.surefire.shared.lang3.stream.Streams;

public class Failable {
    public static <T, U, E extends Throwable> void accept(final FailableBiConsumer<T, U, E> consumer, final T object1,
        final U object2) {
        run(consumer, () -> consumer.accept(object1, object2));
    }
    public static <T, E extends Throwable> void accept(final FailableConsumer<T, E> consumer, final T object) {
        run(consumer, () -> consumer.accept(object));
    }
    public static <E extends Throwable> void accept(final FailableDoubleConsumer<E> consumer, final double value) {
        run(consumer, () -> consumer.accept(value));
    }
    public static <E extends Throwable> void accept(final FailableIntConsumer<E> consumer, final int value) {
        run(consumer, () -> consumer.accept(value));
    }
    public static <E extends Throwable> void accept(final FailableLongConsumer<E> consumer, final long value) {
        run(consumer, () -> consumer.accept(value));
    }
    public static <T, U, R, E extends Throwable> R apply(final FailableBiFunction<T, U, R, E> function, final T input1,
        final U input2) {
        return get(() -> function.apply(input1, input2));
    }
    public static <T, R, E extends Throwable> R apply(final FailableFunction<T, R, E> function, final T input) {
        return get(() -> function.apply(input));
    }
    public static <E extends Throwable> double applyAsDouble(final FailableDoubleBinaryOperator<E> function,
        final double left, final double right) {
        return getAsDouble(() -> function.applyAsDouble(left, right));
    }
    public static <T, R, E extends Throwable> R applyNonNull(final T value, final FailableFunction<? super T, ? extends R, E> mapper) throws E {
        return value != null ? Objects.requireNonNull(mapper, "mapper").apply(value) : null;
    }
    public static <T, U, R, E1 extends Throwable, E2 extends Throwable> R applyNonNull(final T value1,
            final FailableFunction<? super T, ? extends U, E1> mapper1, final FailableFunction<? super U, ? extends R, E2> mapper2) throws E1, E2 {
        return applyNonNull(applyNonNull(value1, mapper1), mapper2);
    }
    public static <T, U, V, R, E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> R applyNonNull(final T value1,
            final FailableFunction<? super T, ? extends U, E1> mapper1, final FailableFunction<? super U, ? extends V, E2> mapper2,
            final FailableFunction<? super V, ? extends R, E3> mapper3) throws E1, E2, E3 {
        return applyNonNull(applyNonNull(applyNonNull(value1, mapper1), mapper2), mapper3);
    }
    public static <T, U> BiConsumer<T, U> asBiConsumer(final FailableBiConsumer<T, U, ?> consumer) {
        return (input1, input2) -> accept(consumer, input1, input2);
    }
    public static <T, U, R> BiFunction<T, U, R> asBiFunction(final FailableBiFunction<T, U, R, ?> function) {
        return (input1, input2) -> apply(function, input1, input2);
    }
    public static <T, U> BiPredicate<T, U> asBiPredicate(final FailableBiPredicate<T, U, ?> predicate) {
        return (input1, input2) -> test(predicate, input1, input2);
    }
    public static <V> Callable<V> asCallable(final FailableCallable<V, ?> callable) {
        return () -> call(callable);
    }
    public static <T> Consumer<T> asConsumer(final FailableConsumer<T, ?> consumer) {
        return input -> accept(consumer, input);
    }
    public static <T, R> Function<T, R> asFunction(final FailableFunction<T, R, ?> function) {
        return input -> apply(function, input);
    }
    public static <T> Predicate<T> asPredicate(final FailablePredicate<T, ?> predicate) {
        return input -> test(predicate, input);
    }
    public static Runnable asRunnable(final FailableRunnable<?> runnable) {
        return () -> run(runnable);
    }
    public static <T> Supplier<T> asSupplier(final FailableSupplier<T, ?> supplier) {
        return () -> get(supplier);
    }
    public static <V, E extends Throwable> V call(final FailableCallable<V, E> callable) {
        return get(callable::call);
    }
    public static <T, E extends Throwable> T get(final FailableSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (final Throwable t) {
            throw rethrow(t);
        }
    }
    public static <E extends Throwable> boolean getAsBoolean(final FailableBooleanSupplier<E> supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (final Throwable t) {
            throw rethrow(t);
        }
    }
    public static <E extends Throwable> double getAsDouble(final FailableDoubleSupplier<E> supplier) {
        try {
            return supplier.getAsDouble();
        } catch (final Throwable t) {
            throw rethrow(t);
        }
    }
    public static RuntimeException rethrow(final Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable");
        if (throwable instanceof IOException) {
            throw new UncheckedIOException((IOException) throwable);
        }
        throw new UndeclaredThrowableException(throwable);
    }
    public static <E extends Throwable> void run(final FailableRunnable<E> runnable) {
        if (runnable != null) {
            try {
                runnable.run();
            } catch (final Throwable t) {
                throw rethrow(t);
            }
        }
    }
    private static <E extends Throwable> void run(final Object test, final FailableRunnable<E> runnable) {
        if (runnable != null && test != null) {
            try {
                runnable.run();
            } catch (final Throwable t) {
                throw rethrow(t);
            }
        }
    }
    public static <T> Streams.FailableStream<T> stream(final Stream<T> stream) {
        return new Streams.FailableStream<>(stream);
    }
    public static <T, U, E extends Throwable> boolean test(final FailableBiPredicate<T, U, E> predicate,
        final T object1, final U object2) {
        return getAsBoolean(() -> predicate.test(object1, object2));
    }
    public static <T, E extends Throwable> boolean test(final FailablePredicate<T, E> predicate, final T object) {
        return getAsBoolean(() -> predicate.test(object));
    }
    private Failable() {
    }
}