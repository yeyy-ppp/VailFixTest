package gson;
import gson.internal.LazilyParsedNumber;
import gson.internal.NumberLimits;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
public final class JsonPrimitive extends JsonElement {
  private final Object value;
  @SuppressWarnings({"deprecation", "UnnecessaryBoxedVariable"})
  public JsonPrimitive(Boolean bool) {
    value = Objects.requireNonNull(bool);
  }
  @SuppressWarnings("deprecation")
  public JsonPrimitive(Number number) {
    value = Objects.requireNonNull(number);
  }
  @SuppressWarnings("deprecation")
  public JsonPrimitive(String string) {
    value = Objects.requireNonNull(string);
  }
  @SuppressWarnings({"deprecation", "UnnecessaryBoxedVariable"})
  public JsonPrimitive(Character c) {
    value = Objects.requireNonNull(c).toString();
  }
  @Override
  public JsonPrimitive deepCopy() {
    return this;
  }
  public boolean isBoolean() {
    return value instanceof Boolean;
  }
  @Override
  public boolean getAsBoolean() {
    if (isBoolean()) {
      return (Boolean) value;
    }
    return Boolean.parseBoolean(getAsString());
  }
  public boolean isNumber() {
    return value instanceof Number;
  }
  @Override
  public Number getAsNumber() {
    if (value instanceof Number) {
      return (Number) value;
    } else if (value instanceof String) {
      return new LazilyParsedNumber((String) value);
    }
    throw new UnsupportedOperationException("Primitive is neither a number nor a string");
  }
  public boolean isString() {
    return value instanceof String;
  }
  @Override
  public String getAsString() {
    if (value instanceof String) {
      return (String) value;
    } else if (isNumber()) {
      return getAsNumber().toString();
    } else if (isBoolean()) {
      return ((Boolean) value).toString();
    }
    throw new AssertionError("Unexpected value type: " + value.getClass());
  }
  @Override
  public double getAsDouble() {
    return isNumber() ? getAsNumber().doubleValue() : Double.parseDouble(getAsString());
  }
  @Override
  public BigDecimal getAsBigDecimal() {
    return value instanceof BigDecimal
        ? (BigDecimal) value
        : NumberLimits.parseBigDecimal(getAsString());
  }
  @Override
  public BigInteger getAsBigInteger() {
    return value instanceof BigInteger
        ? (BigInteger) value
        : isIntegral(this)
            ? BigInteger.valueOf(this.getAsNumber().longValue())
            : NumberLimits.parseBigInteger(this.getAsString());
  }
  @Override
  public float getAsFloat() {
    return isNumber() ? getAsNumber().floatValue() : Float.parseFloat(getAsString());
  }
  @Override
  public long getAsLong() {
    return isNumber() ? getAsNumber().longValue() : Long.parseLong(getAsString());
  }
  @Override
  public short getAsShort() {
    return isNumber() ? getAsNumber().shortValue() : Short.parseShort(getAsString());
  }
  @Override
  public int getAsInt() {
    return isNumber() ? getAsNumber().intValue() : Integer.parseInt(getAsString());
  }
  @Override
  public byte getAsByte() {
    return isNumber() ? getAsNumber().byteValue() : Byte.parseByte(getAsString());
  }
  @Deprecated
  @Override
  public char getAsCharacter() {
    String s = getAsString();
    if (s.isEmpty()) {
      throw new UnsupportedOperationException("String value is empty");
    } else {
      return s.charAt(0);
    }
  }
  @Override
  public int hashCode() {
    if (value == null) {
      return 31;
    }
    if (isIntegral(this)) {
      long value = getAsNumber().longValue();
      return (int) (value ^ (value >>> 32));
    }
    if (value instanceof Number) {
      long value = Double.doubleToLongBits(getAsNumber().doubleValue());
      return (int) (value ^ (value >>> 32));
    }
    return value.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    JsonPrimitive other = (JsonPrimitive) obj;
    if (value == null) {
      return other.value == null;
    }
    if (isIntegral(this) && isIntegral(other)) {
      return (this.value instanceof BigInteger || other.value instanceof BigInteger)
          ? this.getAsBigInteger().equals(other.getAsBigInteger())
          : this.getAsNumber().longValue() == other.getAsNumber().longValue();
    }
    if (value instanceof Number && other.value instanceof Number) {
      if (value instanceof BigDecimal && other.value instanceof BigDecimal) {
        return this.getAsBigDecimal().compareTo(other.getAsBigDecimal()) == 0;
      }
      double thisAsDouble = this.getAsDouble();
      double otherAsDouble = other.getAsDouble();
      return (thisAsDouble == otherAsDouble)
          || (Double.isNaN(thisAsDouble) && Double.isNaN(otherAsDouble));
    }
    return value.equals(other.value);
  }
  private static boolean isIntegral(JsonPrimitive primitive) {
    if (primitive.value instanceof Number) {
      Number number = (Number) primitive.value;
      return number instanceof BigInteger
          || number instanceof Long
          || number instanceof Integer
          || number instanceof Short
          || number instanceof Byte;
    }
    return false;
  }
}