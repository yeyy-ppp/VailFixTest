package lang3.mutable;
import java.util.function.Supplier;
public interface Mutable<T> extends Supplier<T> {
    @Override
    default T get() {
        return getValue();
    }
    @Deprecated
    T getValue();
    void setValue(T value);
}