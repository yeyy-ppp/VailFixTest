package lang3.function;
@FunctionalInterface
public interface FailableSupplier<T, E extends Throwable> {
    @SuppressWarnings("rawtypes")
    FailableSupplier NUL = () -> null;
    @SuppressWarnings("unchecked")
    static <T, E extends Exception> FailableSupplier<T, E> nul() {
        return NUL;
    }
    T get() throws E;
}