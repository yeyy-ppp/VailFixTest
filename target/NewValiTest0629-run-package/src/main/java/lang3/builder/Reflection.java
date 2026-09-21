package lang3.builder;
import java.lang.reflect.Field;
import java.util.Objects;
final class Reflection {
    static Object getUnchecked(final Field field, final Object obj) {
        try {
            return Objects.requireNonNull(field, "field").get(obj);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}