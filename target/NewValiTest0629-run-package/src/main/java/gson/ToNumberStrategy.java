package gson;
import gson.stream.JsonReader;
import java.io.IOException;
public interface ToNumberStrategy {
  Number readNumber(JsonReader in) throws IOException;
}