package gson;
import gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
public interface FieldNamingStrategy {
  String translateName(Field f);
  default List<String> alternateNames(Field f) {
    return Collections.emptyList();
  }
}