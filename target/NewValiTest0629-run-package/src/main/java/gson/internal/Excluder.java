package gson.internal;
import gson.ExclusionStrategy;
import gson.FieldAttributes;
import gson.Gson;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.annotations.Expose;
import gson.annotations.Since;
import gson.annotations.Until;
import gson.internal.reflect.ReflectionHelper;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public final class Excluder implements TypeAdapterFactory, Cloneable {
  private static final double IGNORE_VERSIONS = -1.0d;
  public static final Excluder DEFAULT = new Excluder();
  private double version = IGNORE_VERSIONS;
  private int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
  private boolean serializeInnerClasses = true;
  private boolean requireExpose;
  private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
  private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();
  @Override
  protected Excluder clone() {
    try {
      return (Excluder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }
  public Excluder withVersion(double ignoreVersionsAfter) {
    Excluder result = clone();
    result.version = ignoreVersionsAfter;
    return result;
  }
  public Excluder withModifiers(int... modifiers) {
    Excluder result = clone();
    result.modifiers = 0;
    for (int modifier : modifiers) {
      result.modifiers |= modifier;
    }
    return result;
  }
  public Excluder disableInnerClassSerialization() {
    Excluder result = clone();
    result.serializeInnerClasses = false;
    return result;
  }
  public Excluder excludeFieldsWithoutExposeAnnotation() {
    Excluder result = clone();
    result.requireExpose = true;
    return result;
  }
  public Excluder withExclusionStrategy(
      ExclusionStrategy exclusionStrategy, boolean serialization, boolean deserialization) {
    Excluder result = clone();
    if (serialization) {
      result.serializationStrategies = new ArrayList<>(serializationStrategies);
      result.serializationStrategies.add(exclusionStrategy);
    }
    if (deserialization) {
      result.deserializationStrategies = new ArrayList<>(deserializationStrategies);
      result.deserializationStrategies.add(exclusionStrategy);
    }
    return result;
  }
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<?> rawType = type.getRawType();
    boolean skipSerialize = excludeClass(rawType, true);
    boolean skipDeserialize = excludeClass(rawType, false);
    if (!skipSerialize && !skipDeserialize) {
      return null;
    }
    return new TypeAdapter<T>() {
      private volatile TypeAdapter<T> delegate;
      @Override
      public T read(JsonReader in) throws IOException {
        if (skipDeserialize) {
          in.skipValue();
          return null;
        }
        return delegate().read(in);
      }

      @Override
      public void write(com.google.gson.stream.JsonWriter out, String value) {

      }

      @Override
      public String read(com.google.gson.stream.JsonReader in) {
        return null;
      }

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (skipSerialize) {
          out.nullValue();
          return;
        }
        delegate().write(out, value);
      }
      private TypeAdapter<T> delegate() {
        TypeAdapter<T> d = delegate;
        if (d == null) {
          d = delegate = gson.getDelegateAdapter(Excluder.this, type);
        }
        return d;
      }
    };
  }
  public boolean excludeField(Field field, boolean serialize) {
    if ((modifiers & field.getModifiers()) != 0) {
      return true;
    }
    if (version != Excluder.IGNORE_VERSIONS
        && !isValidVersion(field.getAnnotation(Since.class), field.getAnnotation(Until.class))) {
      return true;
    }
    if (field.isSynthetic()) {
      return true;
    }
    if (requireExpose) {
      Expose annotation = field.getAnnotation(Expose.class);
      if (annotation == null || (serialize ? !annotation.serialize() : !annotation.deserialize())) {
        return true;
      }
    }
    if (excludeClass(field.getType(), serialize)) {
      return true;
    }
    List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    if (!list.isEmpty()) {
      FieldAttributes fieldAttributes = new FieldAttributes(field);
      for (ExclusionStrategy exclusionStrategy : list) {
        if (exclusionStrategy.shouldSkipField(fieldAttributes)) {
          return true;
        }
      }
    }
    return false;
  }
  public boolean excludeClass(Class<?> clazz, boolean serialize) {
    if (version != Excluder.IGNORE_VERSIONS
        && !isValidVersion(clazz.getAnnotation(Since.class), clazz.getAnnotation(Until.class))) {
      return true;
    }
    if (!serializeInnerClasses && isInnerClass(clazz)) {
      return true;
    }
    if (!serialize
        && !Enum.class.isAssignableFrom(clazz)
        && ReflectionHelper.isAnonymousOrNonStaticLocal(clazz)) {
      return true;
    }
    List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    for (ExclusionStrategy exclusionStrategy : list) {
      if (exclusionStrategy.shouldSkipClass(clazz)) {
        return true;
      }
    }
    return false;
  }
  private static boolean isInnerClass(Class<?> clazz) {
    return clazz.isMemberClass() && !ReflectionHelper.isStatic(clazz);
  }
  private boolean isValidVersion(Since since, Until until) {
    return isValidSince(since) && isValidUntil(until);
  }
  private boolean isValidSince(Since annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version >= annotationVersion;
    }
    return true;
  }
  private boolean isValidUntil(Until annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version < annotationVersion;
    }
    return true;
  }
}