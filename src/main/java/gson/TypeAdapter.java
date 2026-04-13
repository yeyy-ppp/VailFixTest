package gson;
import gson.internal.Streams;
import gson.internal.bind.JsonTreeReader;
import gson.internal.bind.JsonTreeWriter;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
public abstract class TypeAdapter<T> {
  public TypeAdapter() {}
  public abstract void write(JsonWriter out, T value) throws IOException;
  public final void toJson(Writer out, T value) throws IOException {
    JsonWriter writer = new JsonWriter(out);
    write(writer, value);
  }
  public final String toJson(T value) {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      toJson(Streams.writerForAppendable(stringBuilder), value);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
    return stringBuilder.toString();
  }
  public final JsonElement toJsonTree(T value) {
    try {
      JsonTreeWriter jsonWriter = new JsonTreeWriter();
      write(jsonWriter, value);
      return jsonWriter.get();
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
  public abstract T read(JsonReader in) throws IOException;
  public final T fromJson(Reader in) throws IOException {
    JsonReader reader = new JsonReader(in);
    return read(reader);
  }
  public final T fromJson(String json) throws IOException {
    return fromJson(new StringReader(json));
  }
  public final T fromJsonTree(JsonElement jsonTree) {
    try {
      JsonReader jsonReader = new JsonTreeReader(jsonTree);
      return read(jsonReader);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
  public final TypeAdapter<T> nullSafe() {
    if (!(this instanceof TypeAdapter.NullSafeTypeAdapter)) {
      return new NullSafeTypeAdapter();
    }
    return this;
  }

    public abstract void write(com.google.gson.stream.JsonWriter out, String value);

  public abstract String read(com.google.gson.stream.JsonReader in);

  private final class NullSafeTypeAdapter extends TypeAdapter<T> {
    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        TypeAdapter.this.write(out, value);
      }
    }
    @Override
    public T read(JsonReader reader) throws IOException {
      if (reader.peek() == JsonToken.NULL) {
        reader.nextNull();
        return null;
      }
      return TypeAdapter.this.read(reader);
    }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, String value) {

    }

    @Override
    public String read(com.google.gson.stream.JsonReader in) {
      return null;
    }

    @Override
    public String toString() {
      return "NullSafeTypeAdapter[" + TypeAdapter.this + "]";
    }
  }
}