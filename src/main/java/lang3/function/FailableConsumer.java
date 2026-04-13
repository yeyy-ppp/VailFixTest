package lang3.function;
import java.util.Objects;
import java.util.function.Function;
@FunctionalInterface
public interface FailableConsumer<T, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableConsumer NOP = Function.identity()::apply;
    @SuppressWarnings("unchecked")
    static <T, E extends Throwable> FailableConsumer<T, E> nop() {
        return NOP;
    }
    void accept(T object) throws E;
    default FailableConsumer<T, E> andThen(final FailableConsumer<? super T, E> after) {
        Objects.requireNonNull(after);
        return (final T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}