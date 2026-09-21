package lang3.function;
import java.util.Objects;
import java.util.function.BiConsumer;
@FunctionalInterface
public interface FailableBiConsumer<T, U, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableBiConsumer NOP = (t, u) -> {  };
    @SuppressWarnings("unchecked")
    static <T, U, E extends Throwable> FailableBiConsumer<T, U, E> nop() {
        return NOP;
    }
    void accept(T t, U u) throws E;
    default FailableBiConsumer<T, U, E> andThen(final FailableBiConsumer<? super T, ? super U, E> after) {
        Objects.requireNonNull(after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}