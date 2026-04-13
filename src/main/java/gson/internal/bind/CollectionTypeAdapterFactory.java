package gson.internal.bind;
import gson.Gson;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.internal.ConstructorConstructor;
import gson.internal.GsonTypes;
import gson.internal.ObjectConstructor;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
public final class CollectionTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;
  public CollectionTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
    this.constructorConstructor = constructorConstructor;
  }
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Type type = typeToken.getType();
    Class<? super T> rawType = typeToken.getRawType();
    if (!Collection.class.isAssignableFrom(rawType)) {
      return null;
    }
    Type elementType = GsonTypes.getCollectionElementType(type, rawType);
    TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));
    TypeAdapter<?> wrappedTypeAdapter =
        new TypeAdapterRuntimeTypeWrapper<>(gson, elementTypeAdapter, elementType);
    boolean allowUnsafe = false;
    ObjectConstructor<T> constructor = constructorConstructor.get(typeToken, allowUnsafe);
    @SuppressWarnings({"unchecked", "rawtypes"})
    TypeAdapter<T> result = new Adapter(wrappedTypeAdapter, constructor);
    return result;
  }
  static final class Adapter<E> extends TypeAdapter<Collection<E>> {
    private final TypeAdapter<E> elementTypeAdapter;
    private final ObjectConstructor<? extends Collection<E>> constructor;
    Adapter(
        TypeAdapter<E> elementTypeAdapter, ObjectConstructor<? extends Collection<E>> constructor) {
      this.elementTypeAdapter = elementTypeAdapter;
      this.constructor = constructor;
    }
    @Override
    public Collection<E> read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      Collection<E> collection = constructor.construct();
      in.beginArray();
      while (in.hasNext()) {
        E instance = elementTypeAdapter.read(in);
        collection.add(instance);
      }
      in.endArray();
      return collection;
    }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, String value) {

    }

    @Override
    public String read(com.google.gson.stream.JsonReader in) {
      return null;
    }

    @Override
    public void write(JsonWriter out, Collection<E> collection) throws IOException {
      if (collection == null) {
        out.nullValue();
        return;
      }
      out.beginArray();
      for (E element : collection) {
        elementTypeAdapter.write(out, element);
      }
      out.endArray();
    }
  }
}