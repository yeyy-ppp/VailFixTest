package gson;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gson.internal.NonNullElementWrapperList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public final class JsonArray extends JsonElement implements Iterable<JsonElement> {
  private final ArrayList<JsonElement> elements;
  @SuppressWarnings("deprecation")
  public JsonArray() {
    elements = new ArrayList<>();
  }
  @SuppressWarnings("deprecation")
  public JsonArray(int capacity) {
    elements = new ArrayList<>(capacity);
  }
  @Override
  public JsonArray deepCopy() {
    if (!elements.isEmpty()) {
      JsonArray result = new JsonArray(elements.size());
      for (JsonElement element : elements) {
        result.add(element.deepCopy());
      }
      return result;
    }
    return new JsonArray();
  }
  public void add(Boolean bool) {
    elements.add(bool == null ? JsonNull.INSTANCE : new JsonPrimitive(bool));
  }
  public void add(Character character) {
    elements.add(character == null ? JsonNull.INSTANCE : new JsonPrimitive(character));
  }
  public void add(Number number) {
    elements.add(number == null ? JsonNull.INSTANCE : new JsonPrimitive(number));
  }
  public void add(String string) {
    elements.add(string == null ? JsonNull.INSTANCE : new JsonPrimitive(string));
  }
  public void add(JsonElement element) {
    if (element == null) {
      element = JsonNull.INSTANCE;
    }
    elements.add(element);
  }
  public void addAll(JsonArray array) {
    elements.addAll(array.elements);
  }
  @CanIgnoreReturnValue
  public JsonElement set(int index, JsonElement element) {
    return elements.set(index, element == null ? JsonNull.INSTANCE : element);
  }
  @CanIgnoreReturnValue
  public boolean remove(JsonElement element) {
    return elements.remove(element);
  }
  @CanIgnoreReturnValue
  public JsonElement remove(int index) {
    return elements.remove(index);
  }
  public boolean contains(JsonElement element) {
    return elements.contains(element);
  }
  public int size() {
    return elements.size();
  }
  public boolean isEmpty() {
    return elements.isEmpty();
  }
  @Override
  public Iterator<JsonElement> iterator() {
    return elements.iterator();
  }
  public JsonElement get(int i) {
    return elements.get(i);
  }
  private JsonElement getAsSingleElement() {
    int size = elements.size();
    if (size == 1) {
      return elements.get(0);
    }
    throw new IllegalStateException("Array must have size 1, but has size " + size);
  }
  @Override
  public Number getAsNumber() {
    return getAsSingleElement().getAsNumber();
  }
  @Override
  public String getAsString() {
    return getAsSingleElement().getAsString();
  }
  @Override
  public double getAsDouble() {
    return getAsSingleElement().getAsDouble();
  }
  @Override
  public BigDecimal getAsBigDecimal() {
    return getAsSingleElement().getAsBigDecimal();
  }
  @Override
  public BigInteger getAsBigInteger() {
    return getAsSingleElement().getAsBigInteger();
  }
  @Override
  public float getAsFloat() {
    return getAsSingleElement().getAsFloat();
  }
  @Override
  public long getAsLong() {
    return getAsSingleElement().getAsLong();
  }
  @Override
  public int getAsInt() {
    return getAsSingleElement().getAsInt();
  }
  @Override
  public byte getAsByte() {
    return getAsSingleElement().getAsByte();
  }
  @Deprecated
  @Override
  public char getAsCharacter() {
    return getAsSingleElement().getAsCharacter();
  }
  @Override
  public short getAsShort() {
    return getAsSingleElement().getAsShort();
  }
  @Override
  public boolean getAsBoolean() {
    return getAsSingleElement().getAsBoolean();
  }
  public List<JsonElement> asList() {
    return new NonNullElementWrapperList<>(elements);
  }
  @Override
  public boolean equals(Object o) {
    return (o == this) || (o instanceof JsonArray && ((JsonArray) o).elements.equals(elements));
  }
  @Override
  public int hashCode() {
    return elements.hashCode();
  }
}