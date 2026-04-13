package lang3.function;
import java.util.function.Supplier;
public class Suppliers {
    @SuppressWarnings("rawtypes")
    private static Supplier NUL = () -> null;
    public static <T> T get(final Supplier<T> supplier) {
        return supplier == null ? null : supplier.get();
    }
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> nul() {
        return NUL;
    }
    @Deprecated
    public Suppliers() {
    }
}