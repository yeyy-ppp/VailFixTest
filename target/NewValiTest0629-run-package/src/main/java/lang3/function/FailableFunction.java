package lang3.function;
import java.util.Objects;
@FunctionalInterface
public interface FailableFunction<T, R, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableFunction NOP = t -> null;
    static <T, R, E extends Throwable> FailableFunction<T, R, E> function(final FailableFunction<T, R, E> function) {
        return function;
    }
    static <T, E extends Throwable> FailableFunction<T, T, E> identity() {
        return t -> t;
    }
    @SuppressWarnings("unchecked")
    static <T, R, E extends Throwable> FailableFunction<T, R, E> nop() {
        return NOP;
    }
    default <V> FailableFunction<T, V, E> andThen(final FailableFunction<? super R, ? extends V, E> after) {
        Objects.requireNonNull(after);
        return (final T t) -> after.apply(apply(t));
    }
    R apply(T input) throws E;
}