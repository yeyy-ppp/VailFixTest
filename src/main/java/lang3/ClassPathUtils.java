package lang3;
import java.util.Objects;
public class ClassPathUtils {
    public static String packageToPath(final String path) {
        return Objects.requireNonNull(path, "path").replace('.', '/');
    }
    public static String pathToPackage(final String path) {
        return Objects.requireNonNull(path, "path").replace('/', '.');
    }
    public static String toFullyQualifiedName(final Class<?> context, final String resourceName) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(resourceName, "resourceName");
        return toFullyQualifiedName(context.getPackage(), resourceName);
    }
    public static String toFullyQualifiedName(final Package context, final String resourceName) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(resourceName, "resourceName");
        return context.getName() + "." + resourceName;
    }
    public static String toFullyQualifiedPath(final Class<?> context, final String resourceName) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(resourceName, "resourceName");
        return toFullyQualifiedPath(context.getPackage(), resourceName);
    }
    public static String toFullyQualifiedPath(final Package context, final String resourceName) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(resourceName, "resourceName");
        return packageToPath(context.getName()) + "/" + resourceName;
    }
    @Deprecated
    public ClassPathUtils() {
    }
}