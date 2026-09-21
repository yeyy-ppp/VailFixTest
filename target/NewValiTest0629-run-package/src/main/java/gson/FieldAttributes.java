package gson;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
public final class FieldAttributes {
  private final Field field;
  public FieldAttributes(Field f) {
    this.field = Objects.requireNonNull(f);
  }
  public Class<?> getDeclaringClass() {
    return field.getDeclaringClass();
  }
  public String getName() {
    return field.getName();
  }
  public Type getDeclaredType() {
    return field.getGenericType();
  }
  public Class<?> getDeclaredClass() {
    return field.getType();
  }
  public <T extends Annotation> T getAnnotation(Class<T> annotation) {
    return field.getAnnotation(annotation);
  }
  public Collection<Annotation> getAnnotations() {
    return Arrays.asList(field.getAnnotations());
  }
  public boolean hasModifier(int modifier) {
    return (field.getModifiers() & modifier) != 0;
  }
  @Override
  public String toString() {
    return field.toString();
  }
}