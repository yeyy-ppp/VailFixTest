package gson.internal.bind;
import gson.Gson;
import gson.ToNumberPolicy;
import gson.ToNumberStrategy;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.internal.LinkedTreeMap;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
public final class ObjectTypeAdapter extends TypeAdapter<Object> {
  private static final TypeAdapterFactory DOUBLE_FACTORY = newFactory(ToNumberPolicy.DOUBLE);
  private final Gson gson;
  private final ToNumberStrategy toNumberStrategy;
  private ObjectTypeAdapter(Gson gson, ToNumberStrategy toNumberStrategy) {
    this.gson = gson;
    this.toNumberStrategy = toNumberStrategy;
  }
  private static TypeAdapterFactory newFactory(ToNumberStrategy toNumberStrategy) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == Object.class) {
          return (TypeAdapter<T>) new ObjectTypeAdapter(gson, toNumberStrategy);
        }
        return null;
      }
    };
  }
  public static TypeAdapterFactory getFactory(ToNumberStrategy toNumberStrategy) {
    if (toNumberStrategy == ToNumberPolicy.DOUBLE) {
      return DOUBLE_FACTORY;
    } else {
      return newFactory(toNumberStrategy);
    }
  }
  private Object tryBeginNesting(JsonReader in, JsonToken peeked) throws IOException {
    switch (peeked) {
      case BEGIN_ARRAY:
        in.beginArray();
        return new ArrayList<>();
      case BEGIN_OBJECT:
        in.beginObject();
        return new LinkedTreeMap<>();
      default:
        return null;
    }
  }
  private Object readTerminal(JsonReader in, JsonToken peeked) throws IOException {
    switch (peeked) {
      case STRING:
        return in.nextString();
      case NUMBER:
        return toNumberStrategy.readNumber(in);
      case BOOLEAN:
        return in.nextBoolean();
      case NULL:
        in.nextNull();
        return null;
      default:
        throw new IllegalStateException("Unexpected token: " + peeked);
    }
  }
  @Override
  public Object read(JsonReader in) throws IOException {
    Object current;
    JsonToken peeked = in.peek();
    current = tryBeginNesting(in, peeked);
    if (current == null) {
      return readTerminal(in, peeked);
    }
    Deque<Object> stack = new ArrayDeque<>();
    while (true) {
      while (in.hasNext()) {
        String name = null;
        if (current instanceof Map) {
          name = in.nextName();
        }
        peeked = in.peek();
        Object value = tryBeginNesting(in, peeked);
        boolean isNesting = value != null;
        if (value == null) {
          value = readTerminal(in, peeked);
        }
        if (current instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>) current;
          list.add(value);
        } else {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) current;
          map.put(name, value);
        }
        if (isNesting) {
          stack.addLast(current);
          current = value;
        }
      }
      if (current instanceof List) {
        in.endArray();
      } else {
        in.endObject();
      }
      if (stack.isEmpty()) {
        return current;
      } else {
        current = stack.removeLast();
      }
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
  public void write(JsonWriter out, Object value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    @SuppressWarnings("unchecked")
    TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) gson.getAdapter(value.getClass());
    if (typeAdapter instanceof ObjectTypeAdapter) {
      out.beginObject();
      out.endObject();
      return;
    }
    typeAdapter.write(out, value);
  }
}