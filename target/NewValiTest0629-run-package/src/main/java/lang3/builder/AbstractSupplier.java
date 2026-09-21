package lang3.builder;
import lang3.function.FailableSupplier;
public abstract class AbstractSupplier<T, B extends AbstractSupplier<T, B, E>, E extends Throwable> implements FailableSupplier<T, E> {
    public AbstractSupplier() {
    }
    @SuppressWarnings("unchecked")
    protected B asThis() {
        return (B) this;
    }
}