package lang3.reflect;
import java.lang.reflect.AccessibleObject;
class AccessibleObjects {
    static boolean isAccessible(final AccessibleObject accessibleObject) {
        return accessibleObject == null || accessibleObject.isAccessible();
    }
    static boolean setAccessible(final AccessibleObject accessibleObject) {
        if (!isAccessible(accessibleObject)) {
            accessibleObject.setAccessible(true);
            return true;
        }
        return false;
    }
}