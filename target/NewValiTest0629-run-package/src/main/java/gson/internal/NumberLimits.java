package gson.internal;
import java.math.BigDecimal;
import java.math.BigInteger;
public class NumberLimits {
  private NumberLimits() {}
  private static final int MAX_NUMBER_STRING_LENGTH = 10_000;
  private static void checkNumberStringLength(String s) {
    if (s.length() > MAX_NUMBER_STRING_LENGTH) {
      throw new NumberFormatException("Number string too large: " + s.substring(0, 30) + "...");
    }
  }
  public static BigDecimal parseBigDecimal(String s) throws NumberFormatException {
    checkNumberStringLength(s);
    BigDecimal decimal = new BigDecimal(s);
    if (Math.abs((long) decimal.scale()) >= 10_000) {
      throw new NumberFormatException("Number has unsupported scale: " + s);
    }
    return decimal;
  }
  public static BigInteger parseBigInteger(String s) throws NumberFormatException {
    checkNumberStringLength(s);
    return new BigInteger(s);
  }
}