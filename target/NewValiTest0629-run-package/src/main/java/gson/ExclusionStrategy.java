package gson;
public interface ExclusionStrategy {
  boolean shouldSkipField(FieldAttributes f);
  boolean shouldSkipClass(Class<?> clazz);
}