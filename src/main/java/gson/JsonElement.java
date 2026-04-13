package gson;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gson.internal.Streams;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
public abstract class JsonElement {
  @Deprecated
  public JsonElement() {}
  public abstract JsonElement deepCopy();
  public boolean isJsonArray() {
    return this instanceof JsonArray;
  }
  public boolean isJsonObject() {
    return this instanceof JsonObject;
  }
  public boolean isJsonPrimitive() {
    return this instanceof JsonPrimitive;
  }
  public boolean isJsonNull() {
    return this instanceof JsonNull;
  }
  public JsonObject getAsJsonObject() {
    if (isJsonObject()) {
      return (JsonObject) this;
    }
    throw new IllegalStateException("Not a JSON Object: " + this);
  }
  public JsonArray getAsJsonArray() {
    if (isJsonArray()) {
      return (JsonArray) this;
    }
    throw new IllegalStateException("Not a JSON Array: " + this);
  }
  public JsonPrimitive getAsJsonPrimitive() {
    if (isJsonPrimitive()) {
      return (JsonPrimitive) this;
    }
    throw new IllegalStateException("Not a JSON Primitive: " + this);
  }
  @CanIgnoreReturnValue
  public JsonNull getAsJsonNull() {
    if (isJsonNull()) {
      return (JsonNull) this;
    }
    throw new IllegalStateException("Not a JSON Null: " + this);
  }
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public Number getAsNumber() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public String getAsString() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public double getAsDouble() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public float getAsFloat() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public long getAsLong() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public int getAsInt() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public byte getAsByte() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  @Deprecated
  public char getAsCharacter() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public BigDecimal getAsBigDecimal() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public BigInteger getAsBigInteger() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  public short getAsShort() {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  @Override
  public String toString() {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      JsonWriter jsonWriter = new JsonWriter(Streams.writerForAppendable(stringBuilder));
      jsonWriter.setStrictness(Strictness.LENIENT);
      Streams.write(this, jsonWriter);
      return stringBuilder.toString();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}