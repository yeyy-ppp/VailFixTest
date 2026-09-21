package gson.internal.bind;
import gson.Gson;
import gson.JsonDeserializer;
import gson.JsonSerializer;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.annotations.JsonAdapter;
import gson.internal.ConstructorConstructor;
import gson.reflect.TypeToken;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
public final class JsonAdapterAnnotationTypeAdapterFactory implements TypeAdapterFactory {
  private static class DummyTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      throw new AssertionError("Factory should not be used");
    }
  }
  private static final TypeAdapterFactory TREE_TYPE_CLASS_DUMMY_FACTORY =
      new DummyTypeAdapterFactory();
  private static final TypeAdapterFactory TREE_TYPE_FIELD_DUMMY_FACTORY =
      new DummyTypeAdapterFactory();
  private final ConstructorConstructor constructorConstructor;
  private final ConcurrentMap<Class<?>, TypeAdapterFactory> adapterFactoryMap;
  public JsonAdapterAnnotationTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
    this.constructorConstructor = constructorConstructor;
    this.adapterFactoryMap = new ConcurrentHashMap<>();
  }
  private static JsonAdapter getAnnotation(Class<?> rawType) {
    return rawType.getAnnotation(JsonAdapter.class);
  }
  @SuppressWarnings("unchecked")
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> targetType) {
    Class<? super T> rawType = targetType.getRawType();
    JsonAdapter annotation = getAnnotation(rawType);
    if (annotation == null) {
      return null;
    }
    return (TypeAdapter<T>)
        getTypeAdapter(constructorConstructor, gson, targetType, annotation, true);
  }
  private static Object createAdapter(
      ConstructorConstructor constructorConstructor, Class<?> adapterClass) {
    boolean allowUnsafe = true;
    return constructorConstructor.get(TypeToken.get(adapterClass), allowUnsafe).construct();
  }
  private TypeAdapterFactory putFactoryAndGetCurrent(Class<?> rawType, TypeAdapterFactory factory) {
    TypeAdapterFactory existingFactory = adapterFactoryMap.putIfAbsent(rawType, factory);
    return existingFactory != null ? existingFactory : factory;
  }
  TypeAdapter<?> getTypeAdapter(
      ConstructorConstructor constructorConstructor,
      Gson gson,
      TypeToken<?> type,
      JsonAdapter annotation,
      boolean isClassAnnotation) {
    Object instance = createAdapter(constructorConstructor, annotation.value());
    TypeAdapter<?> typeAdapter;
    boolean nullSafe = annotation.nullSafe();
    if (instance instanceof TypeAdapter) {
      typeAdapter = (TypeAdapter<?>) instance;
    } else if (instance instanceof TypeAdapterFactory) {
      TypeAdapterFactory factory = (TypeAdapterFactory) instance;
      if (isClassAnnotation) {
        factory = putFactoryAndGetCurrent(type.getRawType(), factory);
      }
      typeAdapter = factory.create(gson, type);
    } else if (instance instanceof JsonSerializer || instance instanceof JsonDeserializer) {
      JsonSerializer<?> serializer =
          instance instanceof JsonSerializer ? (JsonSerializer<?>) instance : null;
      JsonDeserializer<?> deserializer =
          instance instanceof JsonDeserializer ? (JsonDeserializer<?>) instance : null;
      TypeAdapterFactory skipPast;
      if (isClassAnnotation) {
        skipPast = TREE_TYPE_CLASS_DUMMY_FACTORY;
      } else {
        skipPast = TREE_TYPE_FIELD_DUMMY_FACTORY;
      }
      @SuppressWarnings({"unchecked", "rawtypes"})
      TypeAdapter<?> tempAdapter =
          new TreeTypeAdapter(serializer, deserializer, gson, type, skipPast, nullSafe);
      typeAdapter = tempAdapter;
      nullSafe = false;
    } else {
      throw new IllegalArgumentException(
          "Invalid attempt to bind an instance of "
              + instance.getClass().getName()
              + " as a @JsonAdapter for "
              + type.toString()
              + ". @JsonAdapter value must be a TypeAdapter, TypeAdapterFactory,"
              + " JsonSerializer or JsonDeserializer.");
    }
    if (typeAdapter != null && nullSafe) {
      typeAdapter = typeAdapter.nullSafe();
    }
    return typeAdapter;
  }
  public boolean isClassJsonAdapterFactory(TypeToken<?> type, TypeAdapterFactory factory) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(factory);
    if (factory == TREE_TYPE_CLASS_DUMMY_FACTORY) {
      return true;
    }
    Class<?> rawType = type.getRawType();
    TypeAdapterFactory existingFactory = adapterFactoryMap.get(rawType);
    if (existingFactory != null) {
      return existingFactory == factory;
    }
    JsonAdapter annotation = getAnnotation(rawType);
    if (annotation == null) {
      return false;
    }
    Class<?> adapterClass = annotation.value();
    if (!TypeAdapterFactory.class.isAssignableFrom(adapterClass)) {
      return false;
    }
    Object adapter = createAdapter(constructorConstructor, adapterClass);
    TypeAdapterFactory newFactory = (TypeAdapterFactory) adapter;
    return putFactoryAndGetCurrent(rawType, newFactory) == factory;
  }
}