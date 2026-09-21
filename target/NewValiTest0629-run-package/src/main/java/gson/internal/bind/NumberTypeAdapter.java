package gson.internal.bind;
import gson.Gson;
import gson.JsonSyntaxException;
import gson.ToNumberPolicy;
import gson.ToNumberStrategy;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
public final class NumberTypeAdapter extends TypeAdapter<Number> {
  private static final TypeAdapterFactory LAZILY_PARSED_NUMBER_FACTORY =
      newFactory(ToNumberPolicy.LAZILY_PARSED_NUMBER);
  private final ToNumberStrategy toNumberStrategy;
  private NumberTypeAdapter(ToNumberStrategy toNumberStrategy) {
    this.toNumberStrategy = toNumberStrategy;
  }
  private static TypeAdapterFactory newFactory(ToNumberStrategy toNumberStrategy) {
    NumberTypeAdapter adapter = new NumberTypeAdapter(toNumberStrategy);
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        return type.getRawType() == Number.class ? (TypeAdapter<T>) adapter : null;
      }
    };
  }
  public static TypeAdapterFactory getFactory(ToNumberStrategy toNumberStrategy) {
    if (toNumberStrategy == ToNumberPolicy.LAZILY_PARSED_NUMBER) {
      return LAZILY_PARSED_NUMBER_FACTORY;
    } else {
      return newFactory(toNumberStrategy);
    }
  }
  @Override
  public Number read(JsonReader in) throws IOException {
    JsonToken jsonToken = in.peek();
    switch (jsonToken) {
      case NULL:
        in.nextNull();
        return null;
      case NUMBER:
      case STRING:
        return toNumberStrategy.readNumber(in);
      default:
        throw new JsonSyntaxException(
            "Expecting number, got: " + jsonToken + "; at path " + in.getPath());
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
  public void write(JsonWriter out, Number value) throws IOException {
    out.value(value);
  }
}