package gson.internal.bind;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gson.JsonArray;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public final class JsonTreeWriter extends JsonWriter {
  private static final Writer UNWRITABLE_WRITER =
      new Writer() {
        @Override
        public void write(char[] buffer, int offset, int counter) {
          throw new AssertionError();
        }
        @Override
        public void flush() {
          throw new AssertionError();
        }
        @Override
        public void close() {
          throw new AssertionError();
        }
      };
  private static final JsonPrimitive SENTINEL_CLOSED = new JsonPrimitive("closed");
  private final List<JsonElement> stack = new ArrayList<>();
  private String pendingName;
  private JsonElement product = JsonNull.INSTANCE;
  public JsonTreeWriter() {
    super(UNWRITABLE_WRITER);
  }
  public JsonElement get() {
    if (!stack.isEmpty()) {
      throw new IllegalStateException("Expected one JSON element but was " + stack);
    }
    return product;
  }
  private JsonElement peek() {
    return stack.get(stack.size() - 1);
  }
  private void put(JsonElement value) {
    if (pendingName != null) {
      if (!value.isJsonNull() || getSerializeNulls()) {
        JsonObject object = (JsonObject) peek();
        object.add(pendingName, value);
      }
      pendingName = null;
    } else if (stack.isEmpty()) {
      product = value;
    } else {
      JsonElement element = peek();
      if (element instanceof JsonArray) {
        ((JsonArray) element).add(value);
      } else {
        throw new IllegalStateException();
      }
    }
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter beginArray() throws IOException {
    JsonArray array = new JsonArray();
    put(array);
    stack.add(array);
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter endArray() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    JsonElement element = peek();
    if (element instanceof JsonArray) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter beginObject() throws IOException {
    JsonObject object = new JsonObject();
    put(object);
    stack.add(object);
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter endObject() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    JsonElement element = peek();
    if (element instanceof JsonObject) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter name(String name) throws IOException {
    Objects.requireNonNull(name, "name == null");
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException("Did not expect a name");
    }
    JsonElement element = peek();
    if (element instanceof JsonObject) {
      pendingName = name;
      return this;
    }
    throw new IllegalStateException("Please begin an object before writing a name.");
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(boolean value) throws IOException {
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(float value) throws IOException {
    if (!isLenient() && (Float.isNaN(value) || Float.isInfinite(value))) {
      throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
    }
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(double value) throws IOException {
    if (!isLenient() && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
    }
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(long value) throws IOException {
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    if (!isLenient()) {
      double d = value.doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
      }
    }
    put(new JsonPrimitive(value));
    return this;
  }
  @CanIgnoreReturnValue
  @Override
  public JsonWriter nullValue() throws IOException {
    put(JsonNull.INSTANCE);
    return this;
  }
  @Override
  public JsonWriter jsonValue(String value) throws IOException {
    throw new UnsupportedOperationException();
  }
  @Override
  public void flush() throws IOException {}
  @Override
  public void close() throws IOException {
    if (!stack.isEmpty()) {
      throw new IOException("Incomplete document");
    }
    stack.add(SENTINEL_CLOSED);
  }
}