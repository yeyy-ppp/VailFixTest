package gson.internal.bind;
import gson.Gson;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.internal.GsonTypes;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
public final class ArrayTypeAdapter<E> extends TypeAdapter<Object> {
  public static final TypeAdapterFactory FACTORY =
      new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          Type type = typeToken.getType();
          if (!(type instanceof GenericArrayType
              || (type instanceof Class && ((Class<?>) type).isArray()))) {
            return null;
          }
          Type componentType = GsonTypes.getArrayComponentType(type);
          TypeAdapter<?> componentTypeAdapter = gson.getAdapter(TypeToken.get(componentType));
          @SuppressWarnings({"unchecked", "rawtypes"})
          TypeAdapter<T> arrayAdapter =
              new ArrayTypeAdapter(gson, componentTypeAdapter, GsonTypes.getRawType(componentType));
          return arrayAdapter;
        }
      };
  private final Class<E> componentType;
  private final TypeAdapter<E> componentTypeAdapter;
  public ArrayTypeAdapter(
      Gson context, TypeAdapter<E> componentTypeAdapter, Class<E> componentType) {
    this.componentTypeAdapter =
        new TypeAdapterRuntimeTypeWrapper<>(context, componentTypeAdapter, componentType);
    this.componentType = componentType;
  }
  @Override
  public Object read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    ArrayList<E> list = new ArrayList<>();
    in.beginArray();
    while (in.hasNext()) {
      E instance = componentTypeAdapter.read(in);
      list.add(instance);
    }
    in.endArray();
    int size = list.size();
    if (componentType.isPrimitive()) {
      Object array = Array.newInstance(componentType, size);
      for (int i = 0; i < size; i++) {
        Array.set(array, i, list.get(i));
      }
      return array;
    }
    else {
      @SuppressWarnings("unchecked")
      E[] array = (E[]) Array.newInstance(componentType, size);
      return list.toArray(array);
    }
  }

  @Override
  public void write(com.google.gson.stream.JsonWriter out, String value) {

  }

  @Override
  public String read(com.google.gson.stream.JsonReader in) {
    return null;
  }

  @Override
  public void write(JsonWriter out, Object array) throws IOException {
    if (array == null) {
      out.nullValue();
      return;
    }
    out.beginArray();
    for (int i = 0, length = Array.getLength(array); i < length; i++) {
      @SuppressWarnings("unchecked")
      E value = (E) Array.get(array, i);
      componentTypeAdapter.write(out, value);
    }
    out.endArray();
  }
}