package gson.internal.bind;
import gson.Gson;
import gson.JsonDeserializationContext;
import gson.JsonDeserializer;
import gson.JsonElement;
import gson.JsonParseException;
import gson.JsonSerializationContext;
import gson.JsonSerializer;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.internal.Streams;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;
public final class TreeTypeAdapter<T> extends SerializationDelegatingTypeAdapter<T> {
  private final JsonSerializer<T> serializer;
  private final JsonDeserializer<T> deserializer;
  final Gson gson;
  private final TypeToken<T> typeToken;
  private final TypeAdapterFactory skipPastForGetDelegateAdapter;
  private final GsonContextImpl context = new GsonContextImpl();
  private final boolean nullSafe;
  private volatile TypeAdapter<T> delegate;
  public TreeTypeAdapter(
      JsonSerializer<T> serializer,
      JsonDeserializer<T> deserializer,
      Gson gson,
      TypeToken<T> typeToken,
      TypeAdapterFactory skipPast,
      boolean nullSafe) {
    this.serializer = serializer;
    this.deserializer = deserializer;
    this.gson = gson;
    this.typeToken = typeToken;
    this.skipPastForGetDelegateAdapter = skipPast;
    this.nullSafe = nullSafe;
  }
  public TreeTypeAdapter(
      JsonSerializer<T> serializer,
      JsonDeserializer<T> deserializer,
      Gson gson,
      TypeToken<T> typeToken,
      TypeAdapterFactory skipPast) {
    this(serializer, deserializer, gson, typeToken, skipPast, true);
  }
  @Override
  public T read(JsonReader in) throws IOException {
    if (deserializer == null) {
      return delegate().read(in);
    }
    JsonElement value = Streams.parse(in);
    if (nullSafe && value.isJsonNull()) {
      return null;
    }
    return deserializer.deserialize(value, typeToken.getType(), context);
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
    if (serializer == null) {
      delegate().write(out, value);
      return;
    }
    if (nullSafe && value == null) {
      out.nullValue();
      return;
    }
    JsonElement tree = serializer.serialize(value, typeToken.getType(), context);
    Streams.write(tree, out);
  }
  TypeAdapter<T> delegate() {
    TypeAdapter<T> d = delegate;
    if (d == null) {
      d = delegate = gson.getDelegateAdapter(skipPastForGetDelegateAdapter, typeToken);
    }
    return d;
  }
  @Override
  public TypeAdapter<T> getSerializationDelegate() {
    return serializer != null ? this : delegate();
  }
  public static TypeAdapterFactory newFactory(TypeToken<?> exactType, Object typeAdapter) {
    return new SingleTypeFactory(typeAdapter, exactType, false, null);
  }
  public static TypeAdapterFactory newFactoryWithMatchRawType(
      TypeToken<?> exactType, Object typeAdapter) {
    boolean matchRawType = exactType.getType() == exactType.getRawType();
    return new SingleTypeFactory(typeAdapter, exactType, matchRawType, null);
  }
  public static TypeAdapterFactory newTypeHierarchyFactory(
      Class<?> hierarchyType, Object typeAdapter) {
    return new SingleTypeFactory(typeAdapter, null, false, hierarchyType);
  }
  static final class SingleTypeFactory implements TypeAdapterFactory {
    private final TypeToken<?> exactType;
    private final boolean matchRawType;
    private final Class<?> hierarchyType;
    private final JsonSerializer<?> serializer;
    private final JsonDeserializer<?> deserializer;
    SingleTypeFactory(
        Object typeAdapter, TypeToken<?> exactType, boolean matchRawType, Class<?> hierarchyType) {
      serializer = typeAdapter instanceof JsonSerializer ? (JsonSerializer<?>) typeAdapter : null;
      deserializer =
          typeAdapter instanceof JsonDeserializer ? (JsonDeserializer<?>) typeAdapter : null;
      if (serializer == null && deserializer == null) {
        Objects.requireNonNull(typeAdapter);
        throw new IllegalArgumentException(
            "Type adapter "
                + typeAdapter.getClass().getName()
                + " must implement JsonSerializer or JsonDeserializer");
      }
      this.exactType = exactType;
      this.matchRawType = matchRawType;
      this.hierarchyType = hierarchyType;
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      boolean matches =
          exactType != null
              ? exactType.equals(type) || (matchRawType && exactType.getType() == type.getRawType())
              : hierarchyType.isAssignableFrom(type.getRawType());
      return matches
          ? new TreeTypeAdapter<>(
              (JsonSerializer<T>) serializer, (JsonDeserializer<T>) deserializer, gson, type, this)
          : null;
    }
  }
  private final class GsonContextImpl
      implements JsonSerializationContext, JsonDeserializationContext {
    @Override
    public JsonElement serialize(Object src) {
      return gson.toJsonTree(src);
    }
    @Override
    public JsonElement serialize(Object src, Type typeOfSrc) {
      return gson.toJsonTree(src, typeOfSrc);
    }
    @Override
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <R> R deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
      return gson.fromJson(json, typeOfT);
    }
  }
}