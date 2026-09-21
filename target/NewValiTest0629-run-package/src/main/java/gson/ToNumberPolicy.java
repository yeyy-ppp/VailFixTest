package gson;
import gson.internal.LazilyParsedNumber;
import gson.internal.NumberLimits;
import gson.stream.JsonReader;
import gson.stream.MalformedJsonException;
import java.io.IOException;
import java.math.BigDecimal;
public enum ToNumberPolicy implements ToNumberStrategy {
  DOUBLE {
    @Override
    public Double readNumber(JsonReader in) throws IOException {
      return in.nextDouble();
    }
  },
  LAZILY_PARSED_NUMBER {
    @Override
    public Number readNumber(JsonReader in) throws IOException {
      return new LazilyParsedNumber(in.nextString());
    }
  },
  LONG_OR_DOUBLE {
    @Override
    public Number readNumber(JsonReader in) throws IOException, JsonParseException {
      String value = in.nextString();
      if (value.indexOf('.') >= 0) {
        return parseAsDouble(value, in);
      } else {
        try {
          return Long.parseLong(value);
        } catch (NumberFormatException e) {
          return parseAsDouble(value, in);
        }
      }
    }
    private Number parseAsDouble(String value, JsonReader in) throws IOException {
      try {
        Double d = Double.valueOf(value);
        if ((d.isInfinite() || d.isNaN()) && !in.isLenient()) {
          throw new MalformedJsonException(
              "JSON forbids NaN and infinities: " + d + "; at path " + in.getPreviousPath());
        }
        return d;
      } catch (NumberFormatException e) {
        throw new JsonParseException(
            "Cannot parse " + value + "; at path " + in.getPreviousPath(), e);
      }
    }
  },
  BIG_DECIMAL {
    @Override
    public BigDecimal readNumber(JsonReader in) throws IOException {
      String value = in.nextString();
      try {
        return NumberLimits.parseBigDecimal(value);
      } catch (NumberFormatException e) {
        throw new JsonParseException(
            "Cannot parse " + value + "; at path " + in.getPreviousPath(), e);
      }
    }
  }
}