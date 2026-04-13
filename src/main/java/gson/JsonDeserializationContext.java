package gson;
import java.lang.reflect.Type;
public interface JsonDeserializationContext {
  @SuppressWarnings("TypeParameterUnusedInFormals")
  <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException;
}