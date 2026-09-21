package gson;
import static gson.internal.bind.TypeAdapters.atomicLongAdapter;
import static gson.internal.bind.TypeAdapters.atomicLongArrayAdapter;

import com.google.gson.internal.GsonBuildConfig;
import gson.internal.ConstructorConstructor;
import gson.internal.Excluder;
import gson.internal.Primitives;
import gson.internal.Streams;
import gson.internal.bind.ArrayTypeAdapter;
import gson.internal.bind.CollectionTypeAdapterFactory;
import gson.internal.bind.DefaultDateTypeAdapter;
import gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import gson.internal.bind.JsonTreeReader;
import gson.internal.bind.JsonTreeWriter;
import gson.internal.bind.MapTypeAdapterFactory;
import gson.internal.bind.NumberTypeAdapter;
import gson.internal.bind.ObjectTypeAdapter;
import gson.internal.bind.ReflectiveTypeAdapterFactory;
import gson.internal.bind.SerializationDelegatingTypeAdapter;
import gson.internal.bind.TypeAdapters;
import gson.internal.sql.SqlTypesSupport;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import gson.stream.MalformedJsonException;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
public final class Gson {
  static final boolean DEFAULT_JSON_NON_EXECUTABLE = false;
  static final Strictness DEFAULT_STRICTNESS = null;
  static final FormattingStyle DEFAULT_FORMATTING_STYLE = FormattingStyle.COMPACT;
  static final boolean DEFAULT_ESCAPE_HTML = true;
  static final boolean DEFAULT_SERIALIZE_NULLS = false;
  static final boolean DEFAULT_COMPLEX_MAP_KEYS = false;
  static final boolean DEFAULT_SPECIALIZE_FLOAT_VALUES = false;
  static final boolean DEFAULT_USE_JDK_UNSAFE = true;
  static final String DEFAULT_DATE_PATTERN = null;
  static final FieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = FieldNamingPolicy.IDENTITY;
  static final ToNumberStrategy DEFAULT_OBJECT_TO_NUMBER_STRATEGY = ToNumberPolicy.DOUBLE;
  static final ToNumberStrategy DEFAULT_NUMBER_TO_NUMBER_STRATEGY =
      ToNumberPolicy.LAZILY_PARSED_NUMBER;
  private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";
  @SuppressWarnings("ThreadLocalUsage")
  private final ThreadLocal<Map<TypeToken<?>, TypeAdapter<?>>> threadLocalAdapterResults =
      new ThreadLocal<>();
  private final ConcurrentMap<TypeToken<?>, TypeAdapter<?>> typeTokenCache =
      new ConcurrentHashMap<>();
  private final ConstructorConstructor constructorConstructor;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
  final List<TypeAdapterFactory> factories;
  final Excluder excluder;
  final FieldNamingStrategy fieldNamingStrategy;
  final Map<Type, InstanceCreator<?>> instanceCreators;
  final boolean serializeNulls;
  final boolean complexMapKeySerialization;
  final boolean generateNonExecutableJson;
  final boolean htmlSafe;
  final FormattingStyle formattingStyle;
  final Strictness strictness;
  final boolean serializeSpecialFloatingPointValues;
  final boolean useJdkUnsafe;
  final String datePattern;
  final int dateStyle;
  final int timeStyle;
  final LongSerializationPolicy longSerializationPolicy;
  final List<TypeAdapterFactory> builderFactories;
  final List<TypeAdapterFactory> builderHierarchyFactories;
  final ToNumberStrategy objectToNumberStrategy;
  final ToNumberStrategy numberToNumberStrategy;
  final List<ReflectionAccessFilter> reflectionFilters;
  public Gson() {
    this(
        Excluder.DEFAULT,
        DEFAULT_FIELD_NAMING_STRATEGY,
        Collections.emptyMap(),
        DEFAULT_SERIALIZE_NULLS,
        DEFAULT_COMPLEX_MAP_KEYS,
        DEFAULT_JSON_NON_EXECUTABLE,
        DEFAULT_ESCAPE_HTML,
        DEFAULT_FORMATTING_STYLE,
        DEFAULT_STRICTNESS,
        DEFAULT_SPECIALIZE_FLOAT_VALUES,
        DEFAULT_USE_JDK_UNSAFE,
        LongSerializationPolicy.DEFAULT,
        DEFAULT_DATE_PATTERN,
        DateFormat.DEFAULT,
        DateFormat.DEFAULT,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        DEFAULT_OBJECT_TO_NUMBER_STRATEGY,
        DEFAULT_NUMBER_TO_NUMBER_STRATEGY,
        Collections.emptyList());
  }
  Gson(
      Excluder excluder,
      FieldNamingStrategy fieldNamingStrategy,
      Map<Type, InstanceCreator<?>> instanceCreators,
      boolean serializeNulls,
      boolean complexMapKeySerialization,
      boolean generateNonExecutableGson,
      boolean htmlSafe,
      FormattingStyle formattingStyle,
      Strictness strictness,
      boolean serializeSpecialFloatingPointValues,
      boolean useJdkUnsafe,
      LongSerializationPolicy longSerializationPolicy,
      String datePattern,
      int dateStyle,
      int timeStyle,
      List<TypeAdapterFactory> builderFactories,
      List<TypeAdapterFactory> builderHierarchyFactories,
      List<TypeAdapterFactory> factoriesToBeAdded,
      ToNumberStrategy objectToNumberStrategy,
      ToNumberStrategy numberToNumberStrategy,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.excluder = excluder;
    this.fieldNamingStrategy = fieldNamingStrategy;
    this.instanceCreators = instanceCreators;
    this.constructorConstructor =
        new ConstructorConstructor(instanceCreators, useJdkUnsafe, reflectionFilters);
    this.serializeNulls = serializeNulls;
    this.complexMapKeySerialization = complexMapKeySerialization;
    this.generateNonExecutableJson = generateNonExecutableGson;
    this.htmlSafe = htmlSafe;
    this.formattingStyle = formattingStyle;
    this.strictness = strictness;
    this.serializeSpecialFloatingPointValues = serializeSpecialFloatingPointValues;
    this.useJdkUnsafe = useJdkUnsafe;
    this.longSerializationPolicy = longSerializationPolicy;
    this.datePattern = datePattern;
    this.dateStyle = dateStyle;
    this.timeStyle = timeStyle;
    this.builderFactories = builderFactories;
    this.builderHierarchyFactories = builderHierarchyFactories;
    this.objectToNumberStrategy = objectToNumberStrategy;
    this.numberToNumberStrategy = numberToNumberStrategy;
    this.reflectionFilters = reflectionFilters;
    List<TypeAdapterFactory> factories = new ArrayList<>();
    factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
    factories.add(ObjectTypeAdapter.getFactory(objectToNumberStrategy));
    factories.add(excluder);
    factories.addAll(factoriesToBeAdded);
    factories.add(TypeAdapters.STRING_FACTORY);
    factories.add(TypeAdapters.INTEGER_FACTORY);
    factories.add(TypeAdapters.BOOLEAN_FACTORY);
    factories.add(TypeAdapters.BYTE_FACTORY);
    factories.add(TypeAdapters.SHORT_FACTORY);
    TypeAdapter<Number> longAdapter = longSerializationPolicy.typeAdapter();
    factories.add(TypeAdapters.newFactory(long.class, Long.class, longAdapter));
    factories.add(TypeAdapters.newFactory(double.class, Double.class, doubleAdapter()));
    factories.add(TypeAdapters.newFactory(float.class, Float.class, floatAdapter()));
    factories.add(NumberTypeAdapter.getFactory(numberToNumberStrategy));
    factories.add(TypeAdapters.ATOMIC_INTEGER_FACTORY);
    factories.add(TypeAdapters.ATOMIC_BOOLEAN_FACTORY);
    factories.add(TypeAdapters.newFactory(AtomicLong.class, atomicLongAdapter(longAdapter)));
    factories.add(
        TypeAdapters.newFactory(AtomicLongArray.class, atomicLongArrayAdapter(longAdapter)));
    factories.add(TypeAdapters.ATOMIC_INTEGER_ARRAY_FACTORY);
    factories.add(TypeAdapters.CHARACTER_FACTORY);
    factories.add(TypeAdapters.STRING_BUILDER_FACTORY);
    factories.add(TypeAdapters.STRING_BUFFER_FACTORY);
    factories.add(TypeAdapters.BIG_DECIMAL_FACTORY);
    factories.add(TypeAdapters.BIG_INTEGER_FACTORY);
    factories.add(TypeAdapters.LAZILY_PARSED_NUMBER_FACTORY);
    factories.add(TypeAdapters.URL_FACTORY);
    factories.add(TypeAdapters.URI_FACTORY);
    factories.add(TypeAdapters.UUID_FACTORY);
    factories.add(TypeAdapters.CURRENCY_FACTORY);
    factories.add(TypeAdapters.LOCALE_FACTORY);
    factories.add(TypeAdapters.INET_ADDRESS_FACTORY);
    factories.add(TypeAdapters.BIT_SET_FACTORY);
    factories.add(DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
    factories.add(TypeAdapters.CALENDAR_FACTORY);
    factories.addAll(SqlTypesSupport.SQL_TYPE_FACTORIES);
    TypeAdapterFactory javaTimeFactory = TypeAdapters.javaTimeTypeAdapterFactory();
    if (javaTimeFactory != null) {
      factories.add(javaTimeFactory);
    }
    factories.add(ArrayTypeAdapter.FACTORY);
    factories.add(TypeAdapters.CLASS_FACTORY);
    factories.add(new CollectionTypeAdapterFactory(constructorConstructor));
    factories.add(new MapTypeAdapterFactory(constructorConstructor, complexMapKeySerialization));
    this.jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
    factories.add(jsonAdapterFactory);
    factories.add(TypeAdapters.ENUM_FACTORY);
    factories.add(
        new ReflectiveTypeAdapterFactory(
            constructorConstructor,
            fieldNamingStrategy,
            excluder,
            jsonAdapterFactory,
            reflectionFilters));
    this.factories = Collections.unmodifiableList(factories);
  }
  public GsonBuilder newBuilder() {
    return new GsonBuilder(this);
  }
  @Deprecated
  public Excluder excluder() {
    return excluder;
  }
  public FieldNamingStrategy fieldNamingStrategy() {
    return fieldNamingStrategy;
  }
  public boolean serializeNulls() {
    return serializeNulls;
  }
  public boolean htmlSafe() {
    return htmlSafe;
  }
  private TypeAdapter<Number> floatAdapter() {
    return serializeSpecialFloatingPointValues ? TypeAdapters.FLOAT : TypeAdapters.FLOAT_STRICT;
  }
  private TypeAdapter<Number> doubleAdapter() {
    return serializeSpecialFloatingPointValues ? TypeAdapters.DOUBLE : TypeAdapters.DOUBLE_STRICT;
  }
  public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    TypeAdapter<?> cached = typeTokenCache.get(type);
    if (cached != null) {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> adapter = (TypeAdapter<T>) cached;
      return adapter;
    }
    Map<TypeToken<?>, TypeAdapter<?>> threadCalls = threadLocalAdapterResults.get();
    boolean isInitialAdapterRequest = false;
    if (threadCalls == null) {
      threadCalls = new HashMap<>();
      threadLocalAdapterResults.set(threadCalls);
      isInitialAdapterRequest = true;
    } else {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> ongoingCall = (TypeAdapter<T>) threadCalls.get(type);
      if (ongoingCall != null) {
        return ongoingCall;
      }
    }
    TypeAdapter<T> candidate = null;
    try {
      FutureTypeAdapter<T> call = new FutureTypeAdapter<>();
      threadCalls.put(type, call);
      for (TypeAdapterFactory factory : factories) {
        candidate = factory.create(this, type);
        if (candidate != null) {
          call.setDelegate(candidate);
          threadCalls.put(type, candidate);
          break;
        }
      }
    } finally {
      if (isInitialAdapterRequest) {
        threadLocalAdapterResults.remove();
      }
    }
    if (candidate == null) {
      throw new IllegalArgumentException(
          "GSON (" + GsonBuildConfig.VERSION + ") cannot handle " + type);
    }
    if (isInitialAdapterRequest) {
      typeTokenCache.putAll(threadCalls);
    }
    return candidate;
  }
  public <T> TypeAdapter<T> getAdapter(Class<T> type) {
    return getAdapter(TypeToken.get(type));
  }
  public <T> TypeAdapter<T> getDelegateAdapter(TypeAdapterFactory skipPast, TypeToken<T> type) {
    Objects.requireNonNull(skipPast, "skipPast must not be null");
    Objects.requireNonNull(type, "type must not be null");
    if (jsonAdapterFactory.isClassJsonAdapterFactory(type, skipPast)) {
      skipPast = jsonAdapterFactory;
    }
    boolean skipPastFound = false;
    for (TypeAdapterFactory factory : factories) {
      if (!skipPastFound) {
        if (factory == skipPast) {
          skipPastFound = true;
        }
        continue;
      }
      TypeAdapter<T> candidate = factory.create(this, type);
      if (candidate != null) {
        return candidate;
      }
    }
    if (skipPastFound) {
      throw new IllegalArgumentException("GSON cannot serialize or deserialize " + type);
    } else {
      return getAdapter(type);
    }
  }
  public JsonElement toJsonTree(Object src) {
    if (src == null) {
      return JsonNull.INSTANCE;
    }
    return toJsonTree(src, src.getClass());
  }
  public JsonElement toJsonTree(Object src, Type typeOfSrc) {
    JsonTreeWriter writer = new JsonTreeWriter();
    toJson(src, typeOfSrc, writer);
    return writer.get();
  }
  public String toJson(Object src) {
    if (src == null) {
      return toJson(JsonNull.INSTANCE);
    }
    return toJson(src, src.getClass());
  }
  public String toJson(Object src, Type typeOfSrc) {
    StringBuilder writer = new StringBuilder();
    toJson(src, typeOfSrc, writer);
    return writer.toString();
  }
  public void toJson(Object src, Appendable writer) throws JsonIOException {
    if (src != null) {
      toJson(src, src.getClass(), writer);
    } else {
      toJson(JsonNull.INSTANCE, writer);
    }
  }
  public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
    try {
      JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
      toJson(src, typeOfSrc, jsonWriter);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
  public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
    @SuppressWarnings("unchecked")
    TypeAdapter<Object> adapter = (TypeAdapter<Object>) getAdapter(TypeToken.get(typeOfSrc));
    Strictness oldStrictness = writer.getStrictness();
    if (this.strictness != null) {
      writer.setStrictness(this.strictness);
    } else if (writer.getStrictness() == Strictness.LEGACY_STRICT) {
      writer.setStrictness(Strictness.LENIENT);
    }
    boolean oldHtmlSafe = writer.isHtmlSafe();
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setHtmlSafe(htmlSafe);
    writer.setSerializeNulls(serializeNulls);
    try {
      adapter.write(writer, src);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (AssertionError e) {
      throw new AssertionError(
          "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
    } finally {
      writer.setStrictness(oldStrictness);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }
  public String toJson(JsonElement jsonElement) {
    StringBuilder writer = new StringBuilder();
    toJson(jsonElement, writer);
    return writer.toString();
  }
  public void toJson(JsonElement jsonElement, Appendable writer) throws JsonIOException {
    try {
      JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
      toJson(jsonElement, jsonWriter);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
  public void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIOException {
    Strictness oldStrictness = writer.getStrictness();
    boolean oldHtmlSafe = writer.isHtmlSafe();
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setHtmlSafe(htmlSafe);
    writer.setSerializeNulls(serializeNulls);
    if (this.strictness != null) {
      writer.setStrictness(this.strictness);
    } else if (writer.getStrictness() == Strictness.LEGACY_STRICT) {
      writer.setStrictness(Strictness.LENIENT);
    }
    try {
      Streams.write(jsonElement, writer);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (AssertionError e) {
      throw new AssertionError(
          "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
    } finally {
      writer.setStrictness(oldStrictness);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }
  public JsonWriter newJsonWriter(Writer writer) throws IOException {
    if (generateNonExecutableJson) {
      writer.write(JSON_NON_EXECUTABLE_PREFIX);
    }
    JsonWriter jsonWriter = new JsonWriter(writer);
    jsonWriter.setFormattingStyle(formattingStyle);
    jsonWriter.setHtmlSafe(htmlSafe);
    jsonWriter.setStrictness(strictness == null ? Strictness.LEGACY_STRICT : strictness);
    jsonWriter.setSerializeNulls(serializeNulls);
    return jsonWriter;
  }
  public JsonReader newJsonReader(Reader reader) {
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setStrictness(strictness == null ? Strictness.LEGACY_STRICT : strictness);
    return jsonReader;
  }
  public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
    return fromJson(json, TypeToken.get(classOfT));
  }
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
    return (T) fromJson(json, TypeToken.get(typeOfT));
  }
  public <T> T fromJson(String json, TypeToken<T> typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    StringReader reader = new StringReader(json);
    return fromJson(reader, typeOfT);
  }
  public <T> T fromJson(Reader json, Class<T> classOfT)
      throws JsonSyntaxException, JsonIOException {
    return fromJson(json, TypeToken.get(classOfT));
  }
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    return (T) fromJson(json, TypeToken.get(typeOfT));
  }
  public <T> T fromJson(Reader json, TypeToken<T> typeOfT)
      throws JsonIOException, JsonSyntaxException {
    JsonReader jsonReader = newJsonReader(json);
    T object = fromJson(jsonReader, typeOfT);
    assertFullConsumption(object, jsonReader);
    return object;
  }
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  public <T> T fromJson(JsonReader reader, Type typeOfT)
      throws JsonIOException, JsonSyntaxException {
    return (T) fromJson(reader, TypeToken.get(typeOfT));
  }
  public <T> T fromJson(JsonReader reader, TypeToken<T> typeOfT)
      throws JsonIOException, JsonSyntaxException {
    boolean isEmpty = true;
    Strictness oldStrictness = reader.getStrictness();
    if (this.strictness != null) {
      reader.setStrictness(this.strictness);
    } else if (reader.getStrictness() == Strictness.LEGACY_STRICT) {
      reader.setStrictness(Strictness.LENIENT);
    }
    try {
      JsonToken unused = reader.peek();
      isEmpty = false;
      TypeAdapter<T> typeAdapter = getAdapter(typeOfT);
      T object = typeAdapter.read(reader);
      Class<?> expectedTypeWrapped = Primitives.wrap(typeOfT.getRawType());
      if (object != null && !expectedTypeWrapped.isInstance(object)) {
        throw new ClassCastException(
            "Type adapter '"
                + typeAdapter
                + "' returned wrong type; requested "
                + typeOfT.getRawType()
                + " but got instance of "
                + object.getClass()
                + "\nVerify that the adapter was registered for the correct type.");
      }
      return object;
    } catch (EOFException e) {
      if (isEmpty) {
        return null;
      }
      throw new JsonSyntaxException(e);
    } catch (IllegalStateException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonSyntaxException(e);
    } catch (AssertionError e) {
      throw new AssertionError(
          "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
    } finally {
      reader.setStrictness(oldStrictness);
    }
  }
  public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
    return fromJson(json, TypeToken.get(classOfT));
  }
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
    return (T) fromJson(json, TypeToken.get(typeOfT));
  }
  public <T> T fromJson(JsonElement json, TypeToken<T> typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    return fromJson(new JsonTreeReader(json), typeOfT);
  }
  private static void assertFullConsumption(Object obj, JsonReader reader) {
    try {
      if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonSyntaxException("JSON document was not fully consumed.");
      }
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
  static class FutureTypeAdapter<T> extends SerializationDelegatingTypeAdapter<T> {
    private TypeAdapter<T> delegate = null;
    public void setDelegate(TypeAdapter<T> typeAdapter) {
      if (delegate != null) {
        throw new AssertionError("Delegate is already set");
      }
      delegate = typeAdapter;
    }
    private TypeAdapter<T> delegate() {
      TypeAdapter<T> delegate = this.delegate;
      if (delegate == null) {
        throw new IllegalStateException(
            "Adapter for type with cyclic dependency has been used"
                + " before dependency has been resolved");
      }
      return delegate;
    }
    @Override
    public TypeAdapter<T> getSerializationDelegate() {
      return delegate();
    }
    @Override
    public T read(JsonReader in) throws IOException {
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
      delegate().write(out, value);
    }
  }
  @Override
  public String toString() {
    return "{serializeNulls:"
        + serializeNulls
        + ",factories:"
        + factories
        + ",instanceCreators:"
        + constructorConstructor
        + "}";
  }
}