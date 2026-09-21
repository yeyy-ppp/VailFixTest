package gson;
import static gson.Gson.DEFAULT_COMPLEX_MAP_KEYS;
import static gson.Gson.DEFAULT_DATE_PATTERN;
import static gson.Gson.DEFAULT_ESCAPE_HTML;
import static gson.Gson.DEFAULT_FORMATTING_STYLE;
import static gson.Gson.DEFAULT_JSON_NON_EXECUTABLE;
import static gson.Gson.DEFAULT_NUMBER_TO_NUMBER_STRATEGY;
import static gson.Gson.DEFAULT_OBJECT_TO_NUMBER_STRATEGY;
import static gson.Gson.DEFAULT_SERIALIZE_NULLS;
import static gson.Gson.DEFAULT_SPECIALIZE_FLOAT_VALUES;
import static gson.Gson.DEFAULT_STRICTNESS;
import static gson.Gson.DEFAULT_USE_JDK_UNSAFE;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.InlineMe;
import gson.annotations.Since;
import gson.annotations.Until;
import gson.internal.Excluder;
import gson.internal.bind.DefaultDateTypeAdapter;
import gson.internal.bind.TreeTypeAdapter;
import gson.internal.bind.TypeAdapters;
import gson.internal.sql.SqlTypesSupport;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public final class GsonBuilder {
  private Excluder excluder = Excluder.DEFAULT;
  private LongSerializationPolicy longSerializationPolicy = LongSerializationPolicy.DEFAULT;
  private FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
  private final Map<Type, InstanceCreator<?>> instanceCreators = new HashMap<>();
  private final List<TypeAdapterFactory> factories = new ArrayList<>();
  private final List<TypeAdapterFactory> hierarchyFactories = new ArrayList<>();
  private boolean serializeNulls = DEFAULT_SERIALIZE_NULLS;
  private String datePattern = DEFAULT_DATE_PATTERN;
  private int dateStyle = DateFormat.DEFAULT;
  private int timeStyle = DateFormat.DEFAULT;
  private boolean complexMapKeySerialization = DEFAULT_COMPLEX_MAP_KEYS;
  private boolean serializeSpecialFloatingPointValues = DEFAULT_SPECIALIZE_FLOAT_VALUES;
  private boolean escapeHtmlChars = DEFAULT_ESCAPE_HTML;
  private FormattingStyle formattingStyle = DEFAULT_FORMATTING_STYLE;
  private boolean generateNonExecutableJson = DEFAULT_JSON_NON_EXECUTABLE;
  private Strictness strictness = DEFAULT_STRICTNESS;
  private boolean useJdkUnsafe = DEFAULT_USE_JDK_UNSAFE;
  private ToNumberStrategy objectToNumberStrategy = DEFAULT_OBJECT_TO_NUMBER_STRATEGY;
  private ToNumberStrategy numberToNumberStrategy = DEFAULT_NUMBER_TO_NUMBER_STRATEGY;
  private final ArrayDeque<ReflectionAccessFilter> reflectionFilters = new ArrayDeque<>();
  public GsonBuilder() {}
  GsonBuilder(Gson gson) {
    this.excluder = gson.excluder;
    this.fieldNamingPolicy = gson.fieldNamingStrategy;
    this.instanceCreators.putAll(gson.instanceCreators);
    this.serializeNulls = gson.serializeNulls;
    this.complexMapKeySerialization = gson.complexMapKeySerialization;
    this.generateNonExecutableJson = gson.generateNonExecutableJson;
    this.escapeHtmlChars = gson.htmlSafe;
    this.formattingStyle = gson.formattingStyle;
    this.strictness = gson.strictness;
    this.serializeSpecialFloatingPointValues = gson.serializeSpecialFloatingPointValues;
    this.longSerializationPolicy = gson.longSerializationPolicy;
    this.datePattern = gson.datePattern;
    this.dateStyle = gson.dateStyle;
    this.timeStyle = gson.timeStyle;
    this.factories.addAll(gson.builderFactories);
    this.hierarchyFactories.addAll(gson.builderHierarchyFactories);
    this.useJdkUnsafe = gson.useJdkUnsafe;
    this.objectToNumberStrategy = gson.objectToNumberStrategy;
    this.numberToNumberStrategy = gson.numberToNumberStrategy;
    this.reflectionFilters.addAll(gson.reflectionFilters);
  }
  @CanIgnoreReturnValue
  public GsonBuilder setVersion(double version) {
    if (Double.isNaN(version) || version < 0.0) {
      throw new IllegalArgumentException("Invalid version: " + version);
    }
    excluder = excluder.withVersion(version);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder excludeFieldsWithModifiers(int... modifiers) {
    Objects.requireNonNull(modifiers);
    excluder = excluder.withModifiers(modifiers);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder generateNonExecutableJson() {
    this.generateNonExecutableJson = true;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder excludeFieldsWithoutExposeAnnotation() {
    excluder = excluder.excludeFieldsWithoutExposeAnnotation();
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder serializeNulls() {
    this.serializeNulls = true;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder enableComplexMapKeySerialization() {
    complexMapKeySerialization = true;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder disableInnerClassSerialization() {
    excluder = excluder.disableInnerClassSerialization();
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setLongSerializationPolicy(LongSerializationPolicy serializationPolicy) {
    this.longSerializationPolicy = Objects.requireNonNull(serializationPolicy);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setFieldNamingPolicy(FieldNamingPolicy namingConvention) {
    return setFieldNamingStrategy(namingConvention);
  }
  @CanIgnoreReturnValue
  public GsonBuilder setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
    this.fieldNamingPolicy = Objects.requireNonNull(fieldNamingStrategy);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setObjectToNumberStrategy(ToNumberStrategy objectToNumberStrategy) {
    this.objectToNumberStrategy = Objects.requireNonNull(objectToNumberStrategy);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setNumberToNumberStrategy(ToNumberStrategy numberToNumberStrategy) {
    this.numberToNumberStrategy = Objects.requireNonNull(numberToNumberStrategy);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setExclusionStrategies(ExclusionStrategy... strategies) {
    Objects.requireNonNull(strategies);
    for (ExclusionStrategy strategy : strategies) {
      excluder = excluder.withExclusionStrategy(strategy, true, true);
    }
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder addSerializationExclusionStrategy(ExclusionStrategy strategy) {
    Objects.requireNonNull(strategy);
    excluder = excluder.withExclusionStrategy(strategy, true, false);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder addDeserializationExclusionStrategy(ExclusionStrategy strategy) {
    Objects.requireNonNull(strategy);
    excluder = excluder.withExclusionStrategy(strategy, false, true);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setPrettyPrinting() {
    return setFormattingStyle(FormattingStyle.PRETTY);
  }
  @CanIgnoreReturnValue
  public GsonBuilder setFormattingStyle(FormattingStyle formattingStyle) {
    this.formattingStyle = Objects.requireNonNull(formattingStyle);
    return this;
  }
  @Deprecated
  @InlineMe(
      replacement = "this.setStrictness(Strictness.LENIENT)",
      imports = "gson.Strictness")
  @CanIgnoreReturnValue
  public GsonBuilder setLenient() {
    return setStrictness(Strictness.LENIENT);
  }
  @CanIgnoreReturnValue
  public GsonBuilder setStrictness(Strictness strictness) {
    this.strictness = Objects.requireNonNull(strictness);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder disableHtmlEscaping() {
    this.escapeHtmlChars = false;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setDateFormat(String pattern) {
    if (pattern != null) {
      try {
        new SimpleDateFormat(pattern);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("The date pattern '" + pattern + "' is not valid", e);
      }
    }
    this.datePattern = pattern;
    return this;
  }
  @Deprecated
  @CanIgnoreReturnValue
  public GsonBuilder setDateFormat(int dateStyle) {
    this.dateStyle = checkDateFormatStyle(dateStyle);
    this.datePattern = null;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder setDateFormat(int dateStyle, int timeStyle) {
    this.dateStyle = checkDateFormatStyle(dateStyle);
    this.timeStyle = checkDateFormatStyle(timeStyle);
    this.datePattern = null;
    return this;
  }
  private static int checkDateFormatStyle(int style) {
    if (style < 0 || style > 3) {
      throw new IllegalArgumentException("Invalid style: " + style);
    }
    return style;
  }
  @CanIgnoreReturnValue
  public GsonBuilder registerTypeAdapter(Type type, Object typeAdapter) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(typeAdapter);
    if (!(typeAdapter instanceof JsonSerializer<?>
        || typeAdapter instanceof JsonDeserializer<?>
        || typeAdapter instanceof InstanceCreator<?>
        || typeAdapter instanceof TypeAdapter<?>)) {
      throw new IllegalArgumentException(
          "Class "
              + typeAdapter.getClass().getName()
              + " does not implement any supported type adapter class or interface");
    }
    if (hasNonOverridableAdapter(type)) {
      throw new IllegalArgumentException("Cannot override built-in adapter for " + type);
    }
    if (typeAdapter instanceof InstanceCreator<?>) {
      instanceCreators.put(type, (InstanceCreator<?>) typeAdapter);
    }
    if (typeAdapter instanceof JsonSerializer<?> || typeAdapter instanceof JsonDeserializer<?>) {
      TypeToken<?> typeToken = TypeToken.get(type);
      factories.add(TreeTypeAdapter.newFactoryWithMatchRawType(typeToken, typeAdapter));
    }
    if (typeAdapter instanceof TypeAdapter<?>) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      TypeAdapterFactory factory =
          TypeAdapters.newFactory(TypeToken.get(type), (TypeAdapter) typeAdapter);
      factories.add(factory);
    }
    return this;
  }
  private static boolean hasNonOverridableAdapter(Type type) {
    return type == Object.class;
  }
  @CanIgnoreReturnValue
  public GsonBuilder registerTypeAdapterFactory(TypeAdapterFactory factory) {
    Objects.requireNonNull(factory);
    factories.add(factory);
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder registerTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter) {
    Objects.requireNonNull(baseType);
    Objects.requireNonNull(typeAdapter);
    if (!(typeAdapter instanceof JsonSerializer<?>
        || typeAdapter instanceof JsonDeserializer<?>
        || typeAdapter instanceof TypeAdapter<?>)) {
      throw new IllegalArgumentException(
          "Class "
              + typeAdapter.getClass().getName()
              + " does not implement any supported type adapter class or interface");
    }
    if (typeAdapter instanceof JsonDeserializer || typeAdapter instanceof JsonSerializer) {
      hierarchyFactories.add(TreeTypeAdapter.newTypeHierarchyFactory(baseType, typeAdapter));
    }
    if (typeAdapter instanceof TypeAdapter<?>) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      TypeAdapterFactory factory =
          TypeAdapters.newTypeHierarchyFactory(baseType, (TypeAdapter) typeAdapter);
      factories.add(factory);
    }
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder serializeSpecialFloatingPointValues() {
    this.serializeSpecialFloatingPointValues = true;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder disableJdkUnsafe() {
    this.useJdkUnsafe = false;
    return this;
  }
  @CanIgnoreReturnValue
  public GsonBuilder addReflectionAccessFilter(ReflectionAccessFilter filter) {
    Objects.requireNonNull(filter);
    reflectionFilters.addFirst(filter);
    return this;
  }
  public Gson create() {
    List<TypeAdapterFactory> factories =
        new ArrayList<>(this.factories.size() + this.hierarchyFactories.size() + 3);
    factories.addAll(this.factories);
    Collections.reverse(factories);
    List<TypeAdapterFactory> hierarchyFactories = new ArrayList<>(this.hierarchyFactories);
    Collections.reverse(hierarchyFactories);
    factories.addAll(hierarchyFactories);
    addTypeAdaptersForDate(datePattern, dateStyle, timeStyle, factories);
    return new Gson(
        excluder,
        fieldNamingPolicy,
        new HashMap<>(instanceCreators),
        serializeNulls,
        complexMapKeySerialization,
        generateNonExecutableJson,
        escapeHtmlChars,
        formattingStyle,
        strictness,
        serializeSpecialFloatingPointValues,
        useJdkUnsafe,
        longSerializationPolicy,
        datePattern,
        dateStyle,
        timeStyle,
        new ArrayList<>(this.factories),
        new ArrayList<>(this.hierarchyFactories),
        factories,
        objectToNumberStrategy,
        numberToNumberStrategy,
        new ArrayList<>(reflectionFilters));
  }
  private static void addTypeAdaptersForDate(
      String datePattern, int dateStyle, int timeStyle, List<TypeAdapterFactory> factories) {
    TypeAdapterFactory dateAdapterFactory;
    boolean sqlTypesSupported = SqlTypesSupport.SUPPORTS_SQL_TYPES;
    TypeAdapterFactory sqlTimestampAdapterFactory = null;
    TypeAdapterFactory sqlDateAdapterFactory = null;
    if (datePattern != null && !datePattern.trim().isEmpty()) {
      dateAdapterFactory = DefaultDateTypeAdapter.DateType.DATE.createAdapterFactory(datePattern);
      if (sqlTypesSupported) {
        sqlTimestampAdapterFactory =
            SqlTypesSupport.TIMESTAMP_DATE_TYPE.createAdapterFactory(datePattern);
        sqlDateAdapterFactory = SqlTypesSupport.DATE_DATE_TYPE.createAdapterFactory(datePattern);
      }
    } else if (dateStyle != DateFormat.DEFAULT || timeStyle != DateFormat.DEFAULT) {
      dateAdapterFactory =
          DefaultDateTypeAdapter.DateType.DATE.createAdapterFactory(dateStyle, timeStyle);
      if (sqlTypesSupported) {
        sqlTimestampAdapterFactory =
            SqlTypesSupport.TIMESTAMP_DATE_TYPE.createAdapterFactory(dateStyle, timeStyle);
        sqlDateAdapterFactory =
            SqlTypesSupport.DATE_DATE_TYPE.createAdapterFactory(dateStyle, timeStyle);
      }
    } else {
      return;
    }
    factories.add(dateAdapterFactory);
    if (sqlTypesSupported) {
      factories.add(sqlTimestampAdapterFactory);
      factories.add(sqlDateAdapterFactory);
    }
  }
}