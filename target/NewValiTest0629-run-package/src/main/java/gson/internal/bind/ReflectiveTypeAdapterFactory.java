package gson.internal.bind;
import gson.FieldNamingStrategy;
import gson.Gson;
import gson.JsonIOException;
import gson.JsonParseException;
import gson.JsonSyntaxException;
import gson.ReflectionAccessFilter;
import gson.ReflectionAccessFilter.FilterResult;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.annotations.JsonAdapter;
import gson.annotations.SerializedName;
import gson.internal.ConstructorConstructor;
import gson.internal.Excluder;
import gson.internal.GsonTypes;
import gson.internal.ObjectConstructor;
import gson.internal.Primitives;
import gson.internal.ReflectionAccessFilterHelper;
import gson.internal.TroubleshootingGuide;
import gson.internal.reflect.ReflectionHelper;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final Excluder excluder;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
  private final List<ReflectionAccessFilter> reflectionFilters;
  public ReflectiveTypeAdapterFactory(
      ConstructorConstructor constructorConstructor,
      FieldNamingStrategy fieldNamingPolicy,
      Excluder excluder,
      JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.constructorConstructor = constructorConstructor;
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.excluder = excluder;
    this.jsonAdapterFactory = jsonAdapterFactory;
    this.reflectionFilters = reflectionFilters;
  }
  private boolean includeField(Field f, boolean serialize) {
    return !excluder.excludeField(f, serialize);
  }
  @SuppressWarnings("MixedMutabilityReturnType")
  private List<String> getFieldNames(Field f) {
    String fieldName;
    List<String> alternates;
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      fieldName = fieldNamingPolicy.translateName(f);
      alternates = fieldNamingPolicy.alternateNames(f);
    } else {
      fieldName = annotation.value();
      alternates = Arrays.asList(annotation.alternate());
    }
    if (alternates.isEmpty()) {
      return Collections.singletonList(fieldName);
    }
    List<String> fieldNames = new ArrayList<>(alternates.size() + 1);
    fieldNames.add(fieldName);
    fieldNames.addAll(alternates);
    return fieldNames;
  }
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();
    if (!Object.class.isAssignableFrom(raw)) {
      return null;
    }
    if (ReflectionHelper.isAnonymousOrNonStaticLocal(raw)) {
      return new TypeAdapter<T>() {
        @Override
        public T read(JsonReader in) throws IOException {
          in.skipValue();
          return null;
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
          out.nullValue();
        }
        @Override
        public String toString() {
          return "AnonymousOrNonStaticLocalClassAdapter";
        }
      };
    }
    FilterResult filterResult =
        ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
    if (filterResult == FilterResult.BLOCK_ALL) {
      throw new JsonIOException(
          "ReflectionAccessFilter does not permit using reflection for "
              + raw
              + ". Register a TypeAdapter for this type or adjust the access filter.");
    }
    boolean blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
    if (ReflectionHelper.isRecord(raw)) {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> adapter =
          (TypeAdapter<T>)
              new RecordAdapter<>(
                  raw, getBoundFields(gson, type, raw, blockInaccessible, true), blockInaccessible);
      return adapter;
    }
    ObjectConstructor<T> constructor = constructorConstructor.get(type, true);
    return new FieldReflectionAdapter<>(
        constructor, getBoundFields(gson, type, raw, blockInaccessible, false));
  }
  private static <M extends AccessibleObject & Member> void checkAccessible(
      Object object, M member) {
    if (!ReflectionAccessFilterHelper.canAccess(
        member, Modifier.isStatic(member.getModifiers()) ? null : object)) {
      String memberDescription = ReflectionHelper.getAccessibleObjectDescription(member, true);
      throw new JsonIOException(
          memberDescription
              + " is not accessible and ReflectionAccessFilter does not permit making it"
              + " accessible. Register a TypeAdapter for the declaring type, adjust the access"
              + " filter or increase the visibility of the element and its declaring type.");
    }
  }
  private BoundField createBoundField(
      Gson context,
      Field field,
      Method accessor,
      String serializedName,
      TypeToken<?> fieldType,
      boolean serialize,
      boolean blockInaccessible) {
    boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
    int modifiers = field.getModifiers();
    boolean isStaticFinalField = Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
    TypeAdapter<?> mapped = null;
    if (annotation != null) {
      mapped =
          jsonAdapterFactory.getTypeAdapter(
              constructorConstructor, context, fieldType, annotation, false);
    }
    boolean jsonAdapterPresent = mapped != null;
    if (mapped == null) {
      mapped = context.getAdapter(fieldType);
    }
    @SuppressWarnings("unchecked")
    TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) mapped;
    TypeAdapter<Object> writeTypeAdapter;
    if (serialize) {
      writeTypeAdapter =
          jsonAdapterPresent
              ? typeAdapter
              : new TypeAdapterRuntimeTypeWrapper<>(context, typeAdapter, fieldType.getType());
    } else {
      writeTypeAdapter = typeAdapter;
    }
    return new BoundField(serializedName, field) {
      @Override
      void write(JsonWriter writer, Object source) throws IOException, IllegalAccessException {
        if (blockInaccessible) {
          if (accessor == null) {
            checkAccessible(source, field);
          } else {
            checkAccessible(source, accessor);
          }
        }
        Object fieldValue;
        if (accessor != null) {
          try {
            fieldValue = accessor.invoke(source);
          } catch (InvocationTargetException e) {
            String accessorDescription =
                ReflectionHelper.getAccessibleObjectDescription(accessor, false);
            throw new JsonIOException(
                "Accessor " + accessorDescription + " threw exception", e.getCause());
          }
        } else {
          fieldValue = field.get(source);
        }
        if (fieldValue == source) {
          return;
        }
        writer.name(serializedName);
        writeTypeAdapter.write(writer, fieldValue);
      }
      @Override
      void readIntoArray(JsonReader reader, int index, Object[] target)
          throws IOException, JsonParseException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue == null && isPrimitive) {
          throw new JsonParseException(
              "null is not allowed as value for record component '"
                  + fieldName
                  + "' of primitive type; at path "
                  + reader.getPath());
        }
        target[index] = fieldValue;
      }
      @Override
      void readIntoField(JsonReader reader, Object target)
          throws IOException, IllegalAccessException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          if (blockInaccessible) {
            checkAccessible(target, field);
          } else if (isStaticFinalField) {
            String fieldDescription = ReflectionHelper.getAccessibleObjectDescription(field, false);
            throw new JsonIOException("Cannot set value of 'static final' " + fieldDescription);
          }
          field.set(target, fieldValue);
        }
      }
    };
  }
  private static class FieldsData {
    static final FieldsData EMPTY = new FieldsData(Collections.emptyMap(), Collections.emptyList());
    final Map<String, BoundField> deserializedFields;
    final List<BoundField> serializedFields;
    FieldsData(Map<String, BoundField> deserializedFields, List<BoundField> serializedFields) {
      this.deserializedFields = deserializedFields;
      this.serializedFields = serializedFields;
    }
  }
  private static IllegalArgumentException createDuplicateFieldException(
      Class<?> declaringType, String duplicateName, Field field1, Field field2) {
    throw new IllegalArgumentException(
        "Class "
            + declaringType.getName()
            + " declares multiple JSON fields named '"
            + duplicateName
            + "'; conflict is caused by fields "
            + ReflectionHelper.fieldToString(field1)
            + " and "
            + ReflectionHelper.fieldToString(field2)
            + "\nSee "
            + TroubleshootingGuide.createUrl("duplicate-fields"));
  }
  private FieldsData getBoundFields(
      Gson context, TypeToken<?> type, Class<?> raw, boolean blockInaccessible, boolean isRecord) {
    if (raw.isInterface()) {
      return FieldsData.EMPTY;
    }
    Map<String, BoundField> deserializedFields = new LinkedHashMap<>();
    Map<String, BoundField> serializedFields = new LinkedHashMap<>();
    Class<?> originalRaw = raw;
    while (raw != Object.class) {
      Field[] fields = raw.getDeclaredFields();
      if (raw != originalRaw && fields.length > 0) {
        FilterResult filterResult =
            ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
        if (filterResult == FilterResult.BLOCK_ALL) {
          throw new JsonIOException(
              "ReflectionAccessFilter does not permit using reflection for "
                  + raw
                  + " (supertype of "
                  + originalRaw
                  + "). Register a TypeAdapter for this type or adjust the access filter.");
        }
        blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
      }
      for (Field field : fields) {
        boolean serialize = includeField(field, true);
        boolean deserialize = includeField(field, false);
        if (!serialize && !deserialize) {
          continue;
        }
        Method accessor = null;
        if (isRecord) {
          if (Modifier.isStatic(field.getModifiers())) {
            deserialize = false;
          } else {
            accessor = ReflectionHelper.getAccessor(raw, field);
            if (!blockInaccessible) {
              ReflectionHelper.makeAccessible(accessor);
            }
            if (accessor.getAnnotation(SerializedName.class) != null
                && field.getAnnotation(SerializedName.class) == null) {
              String methodDescription =
                  ReflectionHelper.getAccessibleObjectDescription(accessor, false);
              throw new JsonIOException(
                  "@SerializedName on " + methodDescription + " is not supported");
            }
          }
        }
        if (!blockInaccessible && accessor == null) {
          ReflectionHelper.makeAccessible(field);
        }
        Type fieldType = GsonTypes.resolve(type.getType(), raw, field.getGenericType());
        List<String> fieldNames = getFieldNames(field);
        String serializedName = fieldNames.get(0);
        BoundField boundField =
            createBoundField(
                context,
                field,
                accessor,
                serializedName,
                TypeToken.get(fieldType),
                serialize,
                blockInaccessible);
        if (deserialize) {
          for (String name : fieldNames) {
            BoundField replaced = deserializedFields.put(name, boundField);
            if (replaced != null) {
              throw createDuplicateFieldException(originalRaw, name, replaced.field, field);
            }
          }
        }
        if (serialize) {
          BoundField replaced = serializedFields.put(serializedName, boundField);
          if (replaced != null) {
            throw createDuplicateFieldException(originalRaw, serializedName, replaced.field, field);
          }
        }
      }
      type = TypeToken.get(GsonTypes.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return new FieldsData(deserializedFields, new ArrayList<>(serializedFields.values()));
  }
  abstract static class BoundField {
    final String serializedName;
    final Field field;
    final String fieldName;
    protected BoundField(String serializedName, Field field) {
      this.serializedName = serializedName;
      this.field = field;
      this.fieldName = field.getName();
    }
    abstract void write(JsonWriter writer, Object source)
        throws IOException, IllegalAccessException;
    abstract void readIntoArray(JsonReader reader, int index, Object[] target)
        throws IOException, JsonParseException;
    abstract void readIntoField(JsonReader reader, Object target)
        throws IOException, IllegalAccessException;
  }
  public abstract static class Adapter<T, A> extends TypeAdapter<T> {
    private final FieldsData fieldsData;
    Adapter(FieldsData fieldsData) {
      this.fieldsData = fieldsData;
    }
    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      try {
        for (BoundField boundField : fieldsData.serializedFields) {
          boundField.write(out, value);
        }
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      out.endObject();
    }
    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      A accumulator = createAccumulator();
      Map<String, BoundField> deserializedFields = fieldsData.deserializedFields;
      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();
          BoundField field = deserializedFields.get(name);
          if (field == null) {
            in.skipValue();
          } else {
            readField(accumulator, in, field);
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      in.endObject();
      return finalize(accumulator);
    }
    abstract A createAccumulator();
    abstract void readField(A accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException;
    abstract T finalize(A accumulator);
  }
  private static final class FieldReflectionAdapter<T> extends Adapter<T, T> {
    private final ObjectConstructor<T> constructor;
    FieldReflectionAdapter(ObjectConstructor<T> constructor, FieldsData fieldsData) {
      super(fieldsData);
      this.constructor = constructor;
    }
    @Override
    T createAccumulator() {
      return constructor.construct();
    }
    @Override
    void readField(T accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException {
      field.readIntoField(in, accumulator);
    }
    @Override
    T finalize(T accumulator) {
      return accumulator;
    }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, String value) {

    }

    @Override
    public String read(com.google.gson.stream.JsonReader in) {
      return null;
    }
  }
  private static final class RecordAdapter<T> extends Adapter<T, Object[]> {
    static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = primitiveDefaults();
    private final Constructor<T> constructor;
    private final Object[] constructorArgsDefaults;
    private final Map<String, Integer> componentIndices = new HashMap<>();
    RecordAdapter(Class<T> raw, FieldsData fieldsData, boolean blockInaccessible) {
      super(fieldsData);
      constructor = ReflectionHelper.getCanonicalRecordConstructor(raw);
      if (blockInaccessible) {
        checkAccessible(null, constructor);
      } else {
        ReflectionHelper.makeAccessible(constructor);
      }
      String[] componentNames = ReflectionHelper.getRecordComponentNames(raw);
      for (int i = 0; i < componentNames.length; i++) {
        componentIndices.put(componentNames[i], i);
      }
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      constructorArgsDefaults = new Object[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        constructorArgsDefaults[i] = PRIMITIVE_DEFAULTS.get(parameterTypes[i]);
      }
    }
    private static Map<Class<?>, Object> primitiveDefaults() {
      Map<Class<?>, Object> zeroes = new HashMap<>();
      zeroes.put(byte.class, (byte) 0);
      zeroes.put(short.class, (short) 0);
      zeroes.put(int.class, 0);
      zeroes.put(long.class, 0L);
      zeroes.put(float.class, 0F);
      zeroes.put(double.class, 0D);
      zeroes.put(char.class, '\0');
      zeroes.put(boolean.class, false);
      return zeroes;
    }
    @Override
    Object[] createAccumulator() {
      return constructorArgsDefaults.clone();
    }
    @Override
    void readField(Object[] accumulator, JsonReader in, BoundField field) throws IOException {
      Integer componentIndex = componentIndices.get(field.fieldName);
      if (componentIndex == null) {
        throw new IllegalStateException(
            "Could not find the index in the constructor '"
                + ReflectionHelper.constructorToString(constructor)
                + "' for field with name '"
                + field.fieldName
                + "', unable to determine which argument in the constructor the field corresponds"
                + " to. This is unexpected behavior, as we expect the RecordComponents to have the"
                + " same names as the fields in the Java class, and that the order of the"
                + " RecordComponents is the same as the order of the canonical constructor"
                + " parameters.");
      }
      field.readIntoArray(in, componentIndex, accumulator);
    }
    @Override
    T finalize(Object[] accumulator) {
      try {
        return constructor.newInstance(accumulator);
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      catch (InstantiationException | IllegalArgumentException e) {
        throw new RuntimeException(
            "Failed to invoke constructor '"
                + ReflectionHelper.constructorToString(constructor)
                + "' with args "
                + Arrays.toString(accumulator),
            e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(
            "Failed to invoke constructor '"
                + ReflectionHelper.constructorToString(constructor)
                + "' with args "
                + Arrays.toString(accumulator),
            e.getCause());
      }
    }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, String value) {

    }

    @Override
    public String read(com.google.gson.stream.JsonReader in) {
      return null;
    }
  }
}