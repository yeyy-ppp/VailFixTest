package lang3.reflect;
import org.apache.maven.surefire.shared.lang3.ClassUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class MemberUtils {
    private static final class Executable {
        private static Executable of(final Constructor<?> constructor) {
            return new Executable(constructor);
        }
        private static Executable of(final Method method) {
            return new Executable(method);
        }
        private final Class<?>[] parameterTypes;
        private final boolean isVarArgs;
        private Executable(final Constructor<?> constructor) {
            parameterTypes = constructor.getParameterTypes();
            isVarArgs = constructor.isVarArgs();
        }
        private Executable(final Method method) {
            parameterTypes = method.getParameterTypes();
            isVarArgs = method.isVarArgs();
        }
        public Class<?>[] getParameterTypes() {
            return parameterTypes;
        }
        public boolean isVarArgs() {
            return isVarArgs;
        }
    }
    private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
    private static final Class<?>[] WIDENING_PRIMITIVE_TYPES = {
            Byte.TYPE,
            Short.TYPE,
            Character.TYPE,
            Integer.TYPE,
            Long.TYPE,
            Float.TYPE,
            Double.TYPE
    };
    static int compareConstructorFit(final Constructor<?> left, final Constructor<?> right, final Class<?>[] actual) {
      return compareParameterTypes(Executable.of(left), Executable.of(right), actual);
    }
    static int compareMethodFit(final Method left, final Method right, final Class<?>[] actual) {
      return compareParameterTypes(Executable.of(left), Executable.of(right), actual);
    }
    private static int compareParameterTypes(final Executable left, final Executable right, final Class<?>[] actual) {
        final float leftCost = getTotalTransformationCost(actual, left);
        final float rightCost = getTotalTransformationCost(actual, right);
        return Float.compare(leftCost, rightCost);
    }
    private static float getObjectTransformationCost(Class<?> srcClass, final Class<?> destClass) {
        if (destClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, destClass);
        }
        float cost = 0.0f;
        while (srcClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && ClassUtils.isAssignable(srcClass, destClass)) {
                cost += 0.25f;
                break;
            }
            cost++;
            srcClass = srcClass.getSuperclass();
        }
        if (srcClass == null) {
            cost += 1.5f;
        }
        return cost;
    }
    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
        if (srcClass == null) {
            return 1.5f;
        }
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            cost += 0.1f;
            cls = ClassUtils.wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != destClass && i < WIDENING_PRIMITIVE_TYPES.length; i++) {
            if (cls == WIDENING_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < WIDENING_PRIMITIVE_TYPES.length - 1) {
                    cls = WIDENING_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }
    private static float getTotalTransformationCost(final Class<?>[] srcArgs, final Executable executable) {
        final Class<?>[] destArgs = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();
        float totalCost = 0.0f;
        final long normalArgsLen = isVarArgs ? destArgs.length - 1 : destArgs.length;
        if (srcArgs.length < normalArgsLen) {
            return Float.MAX_VALUE;
        }
        for (int i = 0; i < normalArgsLen; i++) {
            totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i]);
        }
        if (isVarArgs) {
            final boolean noVarArgsPassed = srcArgs.length < destArgs.length;
            final boolean explicitArrayForVarargs = srcArgs.length == destArgs.length && srcArgs[srcArgs.length - 1] != null
                    && srcArgs[srcArgs.length - 1].isArray();
            final float varArgsCost = 0.001f;
            final Class<?> destClass = destArgs[destArgs.length - 1].getComponentType();
            if (noVarArgsPassed) {
                totalCost += getObjectTransformationCost(destClass, Object.class) + varArgsCost;
            } else if (explicitArrayForVarargs) {
                final Class<?> sourceClass = srcArgs[srcArgs.length - 1].getComponentType();
                totalCost += getObjectTransformationCost(sourceClass, destClass) + varArgsCost;
            } else {
                for (int i = destArgs.length - 1; i < srcArgs.length; i++) {
                    final Class<?> srcClass = srcArgs[i];
                    totalCost += getObjectTransformationCost(srcClass, destClass) + varArgsCost;
                }
            }
        }
        return totalCost;
    }
    static boolean isAccessible(final Member member) {
        return isPublic(member) && !member.isSynthetic();
    }
    static boolean isMatchingConstructor(final Constructor<?> method, final Class<?>[] parameterTypes) {
        return isMatchingExecutable(Executable.of(method), parameterTypes);
    }
    private static boolean isMatchingExecutable(final Executable method, final Class<?>[] parameterTypes) {
        final Class<?>[] methodParameterTypes = method.getParameterTypes();
        if (ClassUtils.isAssignable(parameterTypes, methodParameterTypes, true)) {
            return true;
        }
        if (method.isVarArgs()) {
            int i;
            for (i = 0; i < methodParameterTypes.length - 1 && i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], methodParameterTypes[i], true)) {
                    return false;
                }
            }
            final Class<?> varArgParameterType = methodParameterTypes[methodParameterTypes.length - 1].getComponentType();
            for (; i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], varArgParameterType, true)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    static boolean isMatchingMethod(final Method method, final Class<?>[] parameterTypes) {
      return isMatchingExecutable(Executable.of(method), parameterTypes);
    }
    static boolean isPackage(final int modifiers) {
        return (modifiers & ACCESS_TEST) == 0;
    }
    static boolean isPublic(final Member member) {
        return member != null && Modifier.isPublic(member.getModifiers());
    }
    static boolean isStatic(final Member member) {
        return member != null && Modifier.isStatic(member.getModifiers());
    }
    static <T extends AccessibleObject> T setAccessibleWorkaround(final T obj) {
        if (AccessibleObjects.isAccessible(obj)) {
            return obj;
        }
        final Member m = (Member) obj;
        if (isPublic(m) && isPackage(m.getDeclaringClass().getModifiers())) {
            try {
                obj.setAccessible(true);
                return obj;
            } catch (final SecurityException ignored) {
            }
        }
        return obj;
    }
}