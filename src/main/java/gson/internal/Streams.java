package gson.internal;
import gson.JsonElement;
import gson.JsonIOException;
import gson.JsonNull;
import gson.JsonParseException;
import gson.JsonSyntaxException;
import gson.internal.bind.JsonElementTypeAdapter;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import gson.stream.MalformedJsonException;
import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
public final class Streams {
  private Streams() {
    throw new UnsupportedOperationException();
  }
  public static JsonElement parse(JsonReader reader) throws JsonParseException {
    boolean isEmpty = true;
    try {
      JsonToken unused = reader.peek();
      isEmpty = false;
      return JsonElementTypeAdapter.ADAPTER.read(reader);
    } catch (EOFException e) {
      if (isEmpty) {
        return JsonNull.INSTANCE;
      }
      throw new JsonSyntaxException(e);
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (NumberFormatException e) {
      throw new JsonSyntaxException(e);
    }
  }
  public static void write(JsonElement element, JsonWriter writer) throws IOException {
    JsonElementTypeAdapter.ADAPTER.write(writer, element);
  }
  public static Writer writerForAppendable(Appendable appendable) {
    return appendable instanceof Writer ? (Writer) appendable : new AppendableWriter(appendable);
  }
  static final class AppendableWriter extends Writer {
    private final Appendable appendable;
    private final CurrentWrite currentWrite = new CurrentWrite();
    AppendableWriter(Appendable appendable) {
      this.appendable = appendable;
    }
    @SuppressWarnings("UngroupedOverloads")
    @Override
    public void write(char[] chars, int offset, int length) throws IOException {
      currentWrite.setChars(chars);
      appendable.append(currentWrite, offset, offset + length);
    }
    @Override
    public void flush() throws IOException {
      if (appendable instanceof Flushable) {
        ((Flushable) appendable).flush();
      }
    }
    @Override
    public void close() throws IOException {
      if (appendable instanceof Closeable) {
        ((Closeable) appendable).close();
      }
    }
    @Override
    public void write(int i) throws IOException {
      appendable.append((char) i);
    }
    @Override
    public void write(String str, int off, int len) throws IOException {
      Objects.requireNonNull(str);
      appendable.append(str, off, off + len);
    }
    @Override
    public Writer append(CharSequence csq) throws IOException {
      appendable.append(csq);
      return this;
    }
    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
      appendable.append(csq, start, end);
      return this;
    }
    static class CurrentWrite implements CharSequence {
      private char[] chars;
      private String cachedString;
      void setChars(char[] chars) {
        this.chars = chars;
        this.cachedString = null;
      }
      @Override
      public int length() {
        return chars.length;
      }
      @Override
      public char charAt(int i) {
        return chars[i];
      }
      @Override
      public CharSequence subSequence(int start, int end) {
        return new String(chars, start, end - start);
      }
      @Override
      public String toString() {
        if (cachedString == null) {
          cachedString = new String(chars);
        }
        return cachedString;
      }
    }
  }
}