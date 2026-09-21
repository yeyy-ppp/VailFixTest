package gson.internal.bind;
import gson.Gson;
import gson.TypeAdapter;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
final class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {
  private final Gson context;
  private final TypeAdapter<T> delegate;
  private final Type type;
  TypeAdapterRuntimeTypeWrapper(Gson context, TypeAdapter<T> delegate, Type type) {
    this.context = context;
    this.delegate = delegate;
    this.type = type;
  }
  @Override
  public T read(JsonReader in) throws IOException {
    return delegate.read(in);
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
    TypeAdapter<T> chosen = delegate;
    Type runtimeType = getRuntimeTypeIfMoreSpecific(type, value);
    if (runtimeType != type) {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> runtimeTypeAdapter =
          (TypeAdapter<T>) context.getAdapter(TypeToken.get(runtimeType));
      if (!(runtimeTypeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter)) {
        chosen = runtimeTypeAdapter;
      } else if (!isReflective(delegate)) {
        chosen = delegate;
      } else {
        chosen = runtimeTypeAdapter;
      }
    }
    chosen.write(out, value);
  }
  private static boolean isReflective(TypeAdapter<?> typeAdapter) {
    while (typeAdapter instanceof SerializationDelegatingTypeAdapter) {
      TypeAdapter<?> delegate =
          ((SerializationDelegatingTypeAdapter<?>) typeAdapter).getSerializationDelegate();
      if (delegate == typeAdapter) {
        break;
      }
      typeAdapter = delegate;
    }
    return typeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter;
  }
  private static Type getRuntimeTypeIfMoreSpecific(Type type, Object value) {
    if (value != null && (type instanceof Class<?> || type instanceof TypeVariable<?>)) {
      type = value.getClass();
    }
    return type;
  }
}