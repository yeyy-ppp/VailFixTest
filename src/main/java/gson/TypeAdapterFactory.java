package gson;
import gson.reflect.TypeToken;
public interface TypeAdapterFactory {
  <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type);
}