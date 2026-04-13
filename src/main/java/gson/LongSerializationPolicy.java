package gson;
import gson.internal.bind.TypeAdapters;
public enum LongSerializationPolicy {
  DEFAULT() {
    @Override
    public JsonElement serialize(Long value) {
      if (value == null) {
        return JsonNull.INSTANCE;
      }
      return new JsonPrimitive(value);
    }
    @Override
    TypeAdapter<Number> typeAdapter() {
      return TypeAdapters.LONG;
    }
  },
  STRING() {
    @Override
    public JsonElement serialize(Long value) {
      if (value == null) {
        return JsonNull.INSTANCE;
      }
      return new JsonPrimitive(value.toString());
    }
    @Override
    TypeAdapter<Number> typeAdapter() {
      return TypeAdapters.LONG_AS_STRING;
    }
  };
  public abstract JsonElement serialize(Long value);
  abstract TypeAdapter<Number> typeAdapter();
}