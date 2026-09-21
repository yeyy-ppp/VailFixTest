package lang3.function;
@FunctionalInterface
public interface FailableCallable<R, E extends Throwable> {
    R call() throws E;
}