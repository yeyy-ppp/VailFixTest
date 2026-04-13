package gson.internal.bind;
import gson.JsonArray;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import gson.TypeAdapter;
import gson.internal.LazilyParsedNumber;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
public class JsonElementTypeAdapter extends TypeAdapter<JsonElement> {
  public static final JsonElementTypeAdapter ADAPTER = new JsonElementTypeAdapter();
  private JsonElementTypeAdapter() {}
  private JsonElement tryBeginNesting(JsonReader in, JsonToken peeked) throws IOException {
    switch (peeked) {
      case BEGIN_ARRAY:
        in.beginArray();
        return new JsonArray();
      case BEGIN_OBJECT:
        in.beginObject();
        return new JsonObject();
      default:
        return null;
    }
  }
  private JsonElement readTerminal(JsonReader in, JsonToken peeked) throws IOException {
    switch (peeked) {
      case STRING:
        return new JsonPrimitive(in.nextString());
      case NUMBER:
        String number = in.nextString();
        return new JsonPrimitive(new LazilyParsedNumber(number));
      case BOOLEAN:
        return new JsonPrimitive(in.nextBoolean());
      case NULL:
        in.nextNull();
        return JsonNull.INSTANCE;
      default:
        throw new IllegalStateException("Unexpected token: " + peeked);
    }
  }
  @Override
  public JsonElement read(JsonReader in) throws IOException {
    if (in instanceof JsonTreeReader) {
      return ((JsonTreeReader) in).nextJsonElement();
    }
    JsonElement current;
    JsonToken peeked = in.peek();
    current = tryBeginNesting(in, peeked);
    if (current == null) {
      return readTerminal(in, peeked);
    }
    Deque<JsonElement> stack = new ArrayDeque<>();
    while (true) {
      while (in.hasNext()) {
        String name = null;
        if (current instanceof JsonObject) {
          name = in.nextName();
        }
        peeked = in.peek();
        JsonElement value = tryBeginNesting(in, peeked);
        boolean isNesting = value != null;
        if (value == null) {
          value = readTerminal(in, peeked);
        }
        if (current instanceof JsonArray) {
          ((JsonArray) current).add(value);
        } else {
          ((JsonObject) current).add(name, value);
        }
        if (isNesting) {
          stack.addLast(current);
          current = value;
        }
      }
      if (current instanceof JsonArray) {
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
  public void write(JsonWriter out, JsonElement value) throws IOException {
    if (value == null || value.isJsonNull()) {
      out.nullValue();
    } else if (value.isJsonPrimitive()) {
      JsonPrimitive primitive = value.getAsJsonPrimitive();
      if (primitive.isNumber()) {
        out.value(primitive.getAsNumber());
      } else if (primitive.isBoolean()) {
        out.value(primitive.getAsBoolean());
      } else {
        out.value(primitive.getAsString());
      }
    } else if (value.isJsonArray()) {
      out.beginArray();
      for (JsonElement e : value.getAsJsonArray()) {
        write(out, e);
      }
      out.endArray();
    } else if (value.isJsonObject()) {
      out.beginObject();
      for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet()) {
        out.name(e.getKey());
        write(out, e.getValue());
      }
      out.endObject();
    } else {
      throw new IllegalArgumentException("Couldn't write " + value.getClass());
    }
  }
}