package lang3.concurrent;
@FunctionalInterface
public interface Computable<I, O> {
    O compute(I arg) throws InterruptedException;
}