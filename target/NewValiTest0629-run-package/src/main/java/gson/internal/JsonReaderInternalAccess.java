package gson.internal;
import gson.stream.JsonReader;
import java.io.IOException;
public abstract class JsonReaderInternalAccess {
  @SuppressWarnings({"ConstantField", "NonFinalStaticField"})
  public static volatile JsonReaderInternalAccess INSTANCE;
  public abstract void promoteNameToValue(JsonReader reader) throws IOException;
}