package gson.internal;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
@SuppressWarnings("serial")
public final class LazilyParsedNumber extends Number {
  private final String value;
  public LazilyParsedNumber(String value) {
    this.value = value;
  }
  private BigDecimal asBigDecimal() {
    return NumberLimits.parseBigDecimal(value);
  }
  @Override
  public int intValue() {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      try {
        return (int) Long.parseLong(value);
      } catch (NumberFormatException nfe) {
        return asBigDecimal().intValue();
      }
    }
  }
  @Override
  public long longValue() {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return asBigDecimal().longValue();
    }
  }
  @Override
  public float floatValue() {
    return Float.parseFloat(value);
  }
  @Override
  public double doubleValue() {
    return Double.parseDouble(value);
  }
  @Override
  public String toString() {
    return value;
  }
  private Object writeReplace() throws ObjectStreamException {
    return asBigDecimal();
  }
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Deserialization is unsupported");
  }
  @Override
  public int hashCode() {
    return value.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LazilyParsedNumber) {
      LazilyParsedNumber other = (LazilyParsedNumber) obj;
      return value.equals(other.value);
    }
    return false;
  }
}