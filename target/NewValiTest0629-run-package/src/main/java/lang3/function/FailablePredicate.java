package lang3.function;
import java.util.Objects;
@FunctionalInterface
public interface FailablePredicate<T, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailablePredicate FALSE = t -> false;
    @SuppressWarnings("rawtypes")
    FailablePredicate TRUE = t -> true;
    default FailablePredicate<T, E> and(final FailablePredicate<? super T, E> other) {
        Objects.requireNonNull(other);
        return t -> test(t) && other.test(t);
    }
    default FailablePredicate<T, E> or(final FailablePredicate<? super T, E> other) {
        Objects.requireNonNull(other);
        return t -> test(t) || other.test(t);
    }
    boolean test(T object) throws E;
}