package gson;
import gson.internal.Streams;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;
public final class JsonStreamParser implements Iterator<JsonElement> {
  private final JsonReader parser;
  private final Object lock;
  public JsonStreamParser(String json) {
    this(new StringReader(json));
  }
  public JsonStreamParser(Reader reader) {
    parser = new JsonReader(reader);
    parser.setStrictness(Strictness.LENIENT);
    lock = new Object();
  }
  @Override
  public JsonElement next() throws JsonParseException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    try {
      return Streams.parse(parser);
    } catch (StackOverflowError | OutOfMemoryError e) {
      throw new JsonParseException("Failed parsing JSON source to Json", e);
    }
  }
  @Override
  public boolean hasNext() {
    synchronized (lock) {
      try {
        return parser.peek() != JsonToken.END_DOCUMENT;
      } catch (MalformedJsonException e) {
        throw new JsonSyntaxException(e);
      } catch (IOException e) {
        throw new JsonIOException(e);
      }
    }
  }
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}