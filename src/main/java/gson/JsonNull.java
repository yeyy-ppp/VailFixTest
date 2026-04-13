package gson;
public final class JsonNull extends JsonElement {
  public static final JsonNull INSTANCE = new JsonNull();
  @Deprecated
  public JsonNull() {
  }
  @Override
  public JsonNull deepCopy() {
    return INSTANCE;
  }
  @Override
  public int hashCode() {
    return JsonNull.class.hashCode();
  }
  @Override
  public boolean equals(Object other) {
    return other instanceof JsonNull;
  }
}