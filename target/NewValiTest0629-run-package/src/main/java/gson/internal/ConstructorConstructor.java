package gson.internal;
import gson.InstanceCreator;
import gson.JsonIOException;
import gson.ReflectionAccessFilter;
import gson.ReflectionAccessFilter.FilterResult;
import gson.internal.reflect.ReflectionHelper;
import gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
public final class ConstructorConstructor {
  private final Map<Type, InstanceCreator<?>> instanceCreators;
  private final boolean useJdkUnsafe;
  private final List<ReflectionAccessFilter> reflectionFilters;
  public ConstructorConstructor(
      Map<Type, InstanceCreator<?>> instanceCreators,
      boolean useJdkUnsafe,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.instanceCreators = instanceCreators;
    this.useJdkUnsafe = useJdkUnsafe;
    this.reflectionFilters = reflectionFilters;
  }
  static String checkInstantiable(Class<?> c) {
    int modifiers = c.getModifiers();
    if (Modifier.isInterface(modifiers)) {
      return "Interfaces can't be instantiated! Register an InstanceCreator"
          + " or a TypeAdapter for this type. Interface name: "
          + c.getName();
    }
    if (Modifier.isAbstract(modifiers)) {
      return "Abstract classes can't be instantiated! Adjust the R8 configuration or register"
          + " an InstanceCreator or a TypeAdapter for this type. Class name: "
          + c.getName()
          + "\nSee "
          + TroubleshootingGuide.createUrl("r8-abstract-class");
    }
    return null;
  }
  public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
    return get(typeToken, true);
  }
  public <T> ObjectConstructor<T> get(TypeToken<T> typeToken, boolean allowUnsafe) {
    Type type = typeToken.getType();
    Class<? super T> rawType = typeToken.getRawType();
    @SuppressWarnings("unchecked")
    InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
    if (typeCreator != null) {
      return new InstanceCreatorConstructor<>(typeCreator, type);
    }
    @SuppressWarnings("unchecked")
    InstanceCreator<T> rawTypeCreator = (InstanceCreator<T>) instanceCreators.get(rawType);
    if (rawTypeCreator != null) {
      return new InstanceCreatorConstructor<>(rawTypeCreator, type);
    }
    ObjectConstructor<T> specialConstructor = newSpecialCollectionConstructor(type, rawType);
    if (specialConstructor != null) {
      return specialConstructor;
    }
    FilterResult filterResult =
        ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, rawType);
    ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType, filterResult);
    if (defaultConstructor != null) {
      return defaultConstructor;
    }
    ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
    if (defaultImplementation != null) {
      return defaultImplementation;
    }
    String exceptionMessage = checkInstantiable(rawType);
    if (exceptionMessage != null) {
      return new ThrowingObjectConstructor<>(exceptionMessage);
    }
    if (!allowUnsafe) {
      String message =
          "Unable to create instance of "
              + rawType
              + "; Register an InstanceCreator or a TypeAdapter for this type.";
      return new ThrowingObjectConstructor<>(message);
    }
    if (filterResult != FilterResult.ALLOW) {
      String message =
          "Unable to create instance of "
              + rawType
              + "; ReflectionAccessFilter does not permit using reflection or Unsafe. Register an"
              + " InstanceCreator or a TypeAdapter for this type or adjust the access filter to"
              + " allow using reflection.";
      return new ThrowingObjectConstructor<>(message);
    }
    return newUnsafeAllocator(rawType);
  }
  private static <T> ObjectConstructor<T> newSpecialCollectionConstructor(
      Type type, Class<? super T> rawType) {
    if (EnumSet.class.isAssignableFrom(rawType)) {
      return () -> {
        if (type instanceof ParameterizedType) {
          Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
          if (elementType instanceof Class) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            T set = (T) EnumSet.noneOf((Class) elementType);
            return set;
          } else {
            throw new JsonIOException("Invalid EnumSet type: " + type);
          }
        } else {
          throw new JsonIOException("Invalid EnumSet type: " + type);
        }
      };
    }
    else if (rawType == EnumMap.class) {
      return () -> {
        if (type instanceof ParameterizedType) {
          Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
          if (elementType instanceof Class) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            T map = (T) new EnumMap((Class) elementType);
            return map;
          } else {
            throw new JsonIOException("Invalid EnumMap type: " + type);
          }
        } else {
          throw new JsonIOException("Invalid EnumMap type: " + type);
        }
      };
    }
    return null;
  }
  private static <T> ObjectConstructor<T> newDefaultConstructor(
      Class<? super T> rawType, FilterResult filterResult) {
    if (Modifier.isAbstract(rawType.getModifiers())) {
      return null;
    }
    Constructor<? super T> constructor;
    try {
      constructor = rawType.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      return null;
    }
    boolean canAccess =
        filterResult == FilterResult.ALLOW
            || (ReflectionAccessFilterHelper.canAccess(constructor, null)
                && (filterResult != FilterResult.BLOCK_ALL
                    || Modifier.isPublic(constructor.getModifiers())));
    if (!canAccess) {
      String message =
          "Unable to invoke no-args constructor of "
              + rawType
              + ";"
              + " constructor is not accessible and ReflectionAccessFilter does not permit making"
              + " it accessible. Register an InstanceCreator or a TypeAdapter for this type, change"
              + " the visibility of the constructor or adjust the access filter.";
      return new ThrowingObjectConstructor<>(message);
    }
    if (filterResult == FilterResult.ALLOW) {
      String exceptionMessage = ReflectionHelper.tryMakeAccessible(constructor);
      if (exceptionMessage != null) {
        return new ThrowingObjectConstructor<>(exceptionMessage);
      }
    }
    return () -> {
      try {
        @SuppressWarnings("unchecked")
        T newInstance = (T) constructor.newInstance();
        return newInstance;
      }
      catch (InstantiationException e) {
        throw new RuntimeException(
            "Failed to invoke constructor '"
                + ReflectionHelper.constructorToString(constructor)
                + "' with no args",
            e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(
            "Failed to invoke constructor '"
                + ReflectionHelper.constructorToString(constructor)
                + "' with no args",
            e.getCause());
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
    };
  }
  private static <T> ObjectConstructor<T> newDefaultImplementationConstructor(
      Type type, Class<? super T> rawType) {
    if (Collection.class.isAssignableFrom(rawType)) {
      @SuppressWarnings("unchecked")
      ObjectConstructor<T> constructor = (ObjectConstructor<T>) newCollectionConstructor(rawType);
      return constructor;
    }
    if (Map.class.isAssignableFrom(rawType)) {
      @SuppressWarnings("unchecked")
      ObjectConstructor<T> constructor = (ObjectConstructor<T>) newMapConstructor(type, rawType);
      return constructor;
    }
    return null;
  }
  private static ObjectConstructor<? extends Collection<?>> newCollectionConstructor(
      Class<?> rawType) {
    if (rawType.isAssignableFrom(ArrayList.class)) {
      return ArrayList::new;
    }
    else if (rawType.isAssignableFrom(LinkedHashSet.class)) {
      return LinkedHashSet::new;
    }
    else if (rawType.isAssignableFrom(TreeSet.class)) {
      return TreeSet::new;
    }
    else if (rawType.isAssignableFrom(ArrayDeque.class)) {
      return ArrayDeque::new;
    }
    return null;
  }
  private static boolean hasStringKeyType(Type mapType) {
    if (!(mapType instanceof ParameterizedType)) {
      return true;
    }
    Type[] typeArguments = ((ParameterizedType) mapType).getActualTypeArguments();
    if (typeArguments.length == 0) {
      return false;
    }
    return GsonTypes.getRawType(typeArguments[0]) == String.class;
  }
  private static ObjectConstructor<? extends Map<?, Object>> newMapConstructor(
      Type type, Class<?> rawType) {
    if (rawType.isAssignableFrom(LinkedTreeMap.class) && hasStringKeyType(type)) {
      return () -> new LinkedTreeMap<>();
    } else if (rawType.isAssignableFrom(LinkedHashMap.class)) {
      return LinkedHashMap::new;
    }
    else if (rawType.isAssignableFrom(TreeMap.class)) {
      return TreeMap::new;
    }
    else if (rawType.isAssignableFrom(ConcurrentHashMap.class)) {
      return ConcurrentHashMap::new;
    }
    else if (rawType.isAssignableFrom(ConcurrentSkipListMap.class)) {
      return ConcurrentSkipListMap::new;
    }
    return null;
  }
  private <T> ObjectConstructor<T> newUnsafeAllocator(Class<? super T> rawType) {
    if (useJdkUnsafe) {
      return () -> {
        try {
          @SuppressWarnings("unchecked")
          T newInstance = (T) UnsafeAllocator.INSTANCE.newInstance(rawType);
          return newInstance;
        } catch (Exception e) {
          throw new RuntimeException(
              ("Unable to create instance of "
                  + rawType
                  + ". Registering an InstanceCreator or a TypeAdapter for this type, or adding a"
                  + " no-args constructor may fix this problem."),
              e);
        }
      };
    } else {
      String exceptionMessage =
          "Unable to create instance of "
              + rawType
              + "; usage of JDK Unsafe is disabled. Registering an InstanceCreator or a TypeAdapter"
              + " for this type, adding a no-args constructor, or enabling usage of JDK Unsafe may"
              + " fix this problem.";
      if (rawType.getDeclaredConstructors().length == 0) {
        exceptionMessage +=
            " Or adjust your R8 configuration to keep the no-args constructor of the class.";
      }
      return new ThrowingObjectConstructor<>(exceptionMessage);
    }
  }
  @Override
  public String toString() {
    return instanceCreators.toString();
  }
  private static final class ThrowingObjectConstructor<T> implements ObjectConstructor<T> {
    private final String exceptionMessage;
    ThrowingObjectConstructor(String exceptionMessage) {
      this.exceptionMessage = exceptionMessage;
    }
    @Override
    public T construct() {
      throw new JsonIOException(exceptionMessage);
    }
  }
  private static final class InstanceCreatorConstructor<T> implements ObjectConstructor<T> {
    private final InstanceCreator<T> instanceCreator;
    private final Type type;
    InstanceCreatorConstructor(InstanceCreator<T> instanceCreator, Type type) {
      this.instanceCreator = instanceCreator;
      this.type = type;
    }
    @Override
    public T construct() {
      return instanceCreator.createInstance(type);
    }
  }
}