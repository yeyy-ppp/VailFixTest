package gson.internal;
import gson.ReflectionAccessFilter;
import gson.ReflectionAccessFilter.FilterResult;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;
public class ReflectionAccessFilterHelper {
  private ReflectionAccessFilterHelper() {}
  public static boolean isJavaType(Class<?> c) {
    return isJavaType(c.getName());
  }
  static boolean isJavaType(String className) {
    return className.startsWith("java.") || className.startsWith("javax.");
  }
  public static boolean isAndroidType(Class<?> c) {
    return isAndroidType(c.getName());
  }
  static boolean isAndroidType(String className) {
    return className.startsWith("android.")
        || className.startsWith("androidx.")
        || isJavaType(className);
  }
  public static boolean isAnyPlatformType(Class<?> c) {
    String className = c.getName();
    return isAndroidType(className)
        || className.startsWith("kotlin.")
        || className.startsWith("kotlinx.")
        || className.startsWith("scala.");
  }
  public static FilterResult getFilterResult(
      List<ReflectionAccessFilter> reflectionFilters, Class<?> c) {
    for (ReflectionAccessFilter filter : reflectionFilters) {
      FilterResult result = filter.check(c);
      if (result != FilterResult.INDECISIVE) {
        return result;
      }
    }
    return FilterResult.ALLOW;
  }
  public static boolean canAccess(AccessibleObject accessibleObject, Object object) {
    return AccessChecker.INSTANCE.canAccess(accessibleObject, object);
  }
  private abstract static class AccessChecker {
    static final AccessChecker INSTANCE;
    static {
      AccessChecker accessChecker = null;
      if (JavaVersion.isJava9OrLater()) {
        try {
          Method canAccessMethod =
              AccessibleObject.class.getDeclaredMethod("canAccess", Object.class);
          accessChecker =
              new AccessChecker() {
                @Override
                public boolean canAccess(AccessibleObject accessibleObject, Object object) {
                  try {
                    return (Boolean) canAccessMethod.invoke(accessibleObject, object);
                  } catch (Exception e) {
                    throw new RuntimeException("Failed invoking canAccess", e);
                  }
                }
              };
        } catch (NoSuchMethodException ignored) {
        }
      }
      if (accessChecker == null) {
        accessChecker =
            new AccessChecker() {
              @Override
              public boolean canAccess(AccessibleObject accessibleObject, Object object) {
                return true;
              }
            };
      }
      INSTANCE = accessChecker;
    }
    abstract boolean canAccess(AccessibleObject accessibleObject, Object object);
  }
}