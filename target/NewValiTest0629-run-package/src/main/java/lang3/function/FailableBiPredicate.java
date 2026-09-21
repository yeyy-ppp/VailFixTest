package lang3.function;
import java.util.Objects;
@FunctionalInterface
public interface FailableBiPredicate<T, U, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableBiPredicate FALSE = (t, u) -> false;
    @SuppressWarnings("rawtypes")
    FailableBiPredicate TRUE = (t, u) -> true;
    @SuppressWarnings("unchecked")
    default FailableBiPredicate<T, U, E> and(final FailableBiPredicate<? super T, ? super U, E> other) {
        Objects.requireNonNull(other);
        return (final T t, final U u) -> test(t, u) && other.test(t, u);
    }
    default FailableBiPredicate<T, U, E> or(final FailableBiPredicate<? super T, ? super U, E> other) {
        Objects.requireNonNull(other);
        return (final T t, final U u) -> test(t, u) || other.test(t, u);
    }
    boolean test(T object1, U object2) throws E;
}