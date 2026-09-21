package lang3.function;
@FunctionalInterface
public interface FailableIntFunction<R, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableIntFunction NOP = t -> null;
    @SuppressWarnings("unchecked")
    static <R, E extends Throwable> FailableIntFunction<R, E> nop() {
        return NOP;
    }
    R apply(int input) throws E;
}