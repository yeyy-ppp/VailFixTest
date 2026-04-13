package lang3.function;
import java.util.Objects;
@FunctionalInterface
public interface FailableBiFunction<T, U, R, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableBiFunction NOP = (t, u) -> null;
    @SuppressWarnings("unchecked")
    static <T, U, R, E extends Throwable> FailableBiFunction<T, U, R, E> nop() {
        return NOP;
    }
    default <V> FailableBiFunction<T, U, V, E> andThen(final FailableFunction<? super R, ? extends V, E> after) {
        Objects.requireNonNull(after);
        return (final T t, final U u) -> after.apply(apply(t, u));
    }
    R apply(T input1, U input2) throws E;
}