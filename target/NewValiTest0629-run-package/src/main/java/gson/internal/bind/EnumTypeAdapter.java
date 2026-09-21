package gson.internal.bind;
import gson.Gson;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.annotations.SerializedName;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
  static final TypeAdapterFactory FACTORY =
      new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          Class<? super T> rawType = typeToken.getRawType();
          if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
            return null;
          }
          if (!rawType.isEnum()) {
            rawType = rawType.getSuperclass();
          }
          @SuppressWarnings({"rawtypes", "unchecked"})
          TypeAdapter<T> adapter = (TypeAdapter<T>) new EnumTypeAdapter(rawType);
          return adapter;
        }
      };
  private static int calculateHashMapCapacity(int numMappings) {
    return (int) Math.ceil(numMappings / 0.75F);
  }
  private final Map<String, T> nameToConstant;
  private final Map<String, T> stringToConstant;
  private final Map<T, String> constantToName;
  private EnumTypeAdapter(Class<T> classOfT) {
    try {
      Field[] fields = classOfT.getDeclaredFields();
      int constantCount = 0;
      for (Field f : fields) {
        if (f.isEnumConstant()) {
          fields[constantCount++] = f;
        }
      }
      fields = Arrays.copyOf(fields, constantCount);
      int hashMapCapacity = calculateHashMapCapacity(constantCount);
      nameToConstant = new HashMap<>(hashMapCapacity);
      stringToConstant = new HashMap<>(hashMapCapacity);
      constantToName = new HashMap<>(hashMapCapacity);
      AccessibleObject.setAccessible(fields, true);
      for (Field constantField : fields) {
        @SuppressWarnings("unchecked")
        T constant = (T) constantField.get(null);
        String name = constant.name();
        String toStringVal = constant.toString();
        SerializedName annotation = constantField.getAnnotation(SerializedName.class);
        if (annotation != null) {
          name = annotation.value();
          for (String alternate : annotation.alternate()) {
            nameToConstant.put(alternate, constant);
          }
        }
        nameToConstant.put(name, constant);
        stringToConstant.put(toStringVal, constant);
        constantToName.put(constant, name);
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  @Override
  public T read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String key = in.nextString();
    T constant = nameToConstant.get(key);
    return (constant == null) ? stringToConstant.get(key) : constant;
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
    out.value(value == null ? null : constantToName.get(value));
  }
}