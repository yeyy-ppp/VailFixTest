package gson;
import java.lang.reflect.Type;
public interface JsonSerializationContext {
  JsonElement serialize(Object src);
  JsonElement serialize(Object src, Type typeOfSrc);
}