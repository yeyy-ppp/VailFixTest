package gson.stream;
import static gson.stream.JsonScope.DANGLING_NAME;
import static gson.stream.JsonScope.EMPTY_ARRAY;
import static gson.stream.JsonScope.EMPTY_DOCUMENT;
import static gson.stream.JsonScope.EMPTY_OBJECT;
import static gson.stream.JsonScope.NONEMPTY_ARRAY;
import static gson.stream.JsonScope.NONEMPTY_DOCUMENT;
import static gson.stream.JsonScope.NONEMPTY_OBJECT;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gson.FormattingStyle;
import gson.Strictness;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
public class JsonWriter implements Closeable, Flushable {
  private static final Pattern VALID_JSON_NUMBER_PATTERN =
      Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?");
  private static final String[] REPLACEMENT_CHARS;
  private static final String[] HTML_SAFE_REPLACEMENT_CHARS;
  static {
    REPLACEMENT_CHARS = new String[128];
    for (int i = 0; i <= 0x1f; i++) {
      REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
    }
    REPLACEMENT_CHARS['"'] = "\\\"";
    REPLACEMENT_CHARS['\\'] = "\\\\";
    REPLACEMENT_CHARS['\t'] = "\\t";
    REPLACEMENT_CHARS['\b'] = "\\b";
    REPLACEMENT_CHARS['\n'] = "\\n";
    REPLACEMENT_CHARS['\r'] = "\\r";
    REPLACEMENT_CHARS['\f'] = "\\f";
    HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
    HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
    HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
    HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
    HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
    HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
  }
  private final Writer out;
  private int[] stack = new int[32];
  private int stackSize = 0;
  {
    push(EMPTY_DOCUMENT);
  }
  private FormattingStyle formattingStyle;
  private String formattedColon;
  private String formattedComma;
  private boolean usesEmptyNewlineAndIndent;
  private Strictness strictness = Strictness.LEGACY_STRICT;
  private boolean htmlSafe;
  private String deferredName;
  private boolean serializeNulls = true;
  public JsonWriter(Writer out) {
    this.out = Objects.requireNonNull(out, "out == null");
    setFormattingStyle(FormattingStyle.COMPACT);
  }
  public final void setIndent(String indent) {
    if (indent.isEmpty()) {
      setFormattingStyle(FormattingStyle.COMPACT);
    } else {
      setFormattingStyle(FormattingStyle.PRETTY.withIndent(indent));
    }
  }
  public final void setFormattingStyle(FormattingStyle formattingStyle) {
    this.formattingStyle = Objects.requireNonNull(formattingStyle);
    this.formattedComma = ",";
    if (this.formattingStyle.usesSpaceAfterSeparators()) {
      this.formattedColon = ": ";
      if (this.formattingStyle.getNewline().isEmpty()) {
        this.formattedComma = ", ";
      }
    } else {
      this.formattedColon = ":";
    }
    this.usesEmptyNewlineAndIndent =
        this.formattingStyle.getNewline().isEmpty() && this.formattingStyle.getIndent().isEmpty();
  }
  public final FormattingStyle getFormattingStyle() {
    return formattingStyle;
  }
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public final void setLenient(boolean lenient) {
    setStrictness(lenient ? Strictness.LENIENT : Strictness.LEGACY_STRICT);
  }
  public boolean isLenient() {
    return strictness == Strictness.LENIENT;
  }
  public final void setStrictness(Strictness strictness) {
    this.strictness = Objects.requireNonNull(strictness);
  }
  public final Strictness getStrictness() {
    return strictness;
  }
  public final void setHtmlSafe(boolean htmlSafe) {
    this.htmlSafe = htmlSafe;
  }
  public final boolean isHtmlSafe() {
    return htmlSafe;
  }
  public final void setSerializeNulls(boolean serializeNulls) {
    this.serializeNulls = serializeNulls;
  }
  public final boolean getSerializeNulls() {
    return serializeNulls;
  }
  @CanIgnoreReturnValue
  public JsonWriter beginArray() throws IOException {
    writeDeferredName();
    return openScope(EMPTY_ARRAY, '[');
  }
  @CanIgnoreReturnValue
  public JsonWriter endArray() throws IOException {
    return closeScope(EMPTY_ARRAY, NONEMPTY_ARRAY, ']');
  }
  @CanIgnoreReturnValue
  public JsonWriter beginObject() throws IOException {
    writeDeferredName();
    return openScope(EMPTY_OBJECT, '{');
  }
  @CanIgnoreReturnValue
  public JsonWriter endObject() throws IOException {
    return closeScope(EMPTY_OBJECT, NONEMPTY_OBJECT, '}');
  }
  @CanIgnoreReturnValue
  private JsonWriter openScope(int empty, char openBracket) throws IOException {
    beforeValue();
    push(empty);
    out.write(openBracket);
    return this;
  }
  @CanIgnoreReturnValue
  private JsonWriter closeScope(int empty, int nonempty, char closeBracket) throws IOException {
    int context = peek();
    if (context != nonempty && context != empty) {
      throw new IllegalStateException("Nesting problem.");
    }
    if (deferredName != null) {
      throw new IllegalStateException("Dangling name: " + deferredName);
    }
    stackSize--;
    if (context == nonempty) {
      newline();
    }
    out.write(closeBracket);
    return this;
  }
  private void push(int newTop) {
    if (stackSize == stack.length) {
      stack = Arrays.copyOf(stack, stackSize * 2);
    }
    stack[stackSize++] = newTop;
  }
  private int peek() {
    if (stackSize == 0) {
      throw new IllegalStateException("JsonWriter is closed.");
    }
    return stack[stackSize - 1];
  }
  private void replaceTop(int topOfStack) {
    stack[stackSize - 1] = topOfStack;
  }
  @CanIgnoreReturnValue
  public JsonWriter name(String name) throws IOException {
    Objects.requireNonNull(name, "name == null");
    if (deferredName != null) {
      throw new IllegalStateException("Already wrote a name, expecting a value.");
    }
    int context = peek();
    if (context != EMPTY_OBJECT && context != NONEMPTY_OBJECT) {
      throw new IllegalStateException("Please begin an object before writing a name.");
    }
    deferredName = name;
    return this;
  }
  private void writeDeferredName() throws IOException {
    if (deferredName != null) {
      beforeName();
      string(deferredName);
      deferredName = null;
    }
  }
  @CanIgnoreReturnValue
  public JsonWriter value(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    string(value);
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(boolean value) throws IOException {
    writeDeferredName();
    beforeValue();
    out.write(value ? "true" : "false");
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    out.write(value ? "true" : "false");
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(float value) throws IOException {
    writeDeferredName();
    if (strictness != Strictness.LENIENT && (Float.isNaN(value) || Float.isInfinite(value))) {
      throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }
    beforeValue();
    out.append(Float.toString(value));
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(double value) throws IOException {
    writeDeferredName();
    if (strictness != Strictness.LENIENT && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }
    beforeValue();
    out.append(Double.toString(value));
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(long value) throws IOException {
    writeDeferredName();
    beforeValue();
    out.write(Long.toString(value));
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    String string = value.toString();
    Class<? extends Number> numberClass = value.getClass();
    if (!alwaysCreatesValidJsonNumber(numberClass)) {
      if (string.equals("-Infinity") || string.equals("Infinity") || string.equals("NaN")) {
        if (strictness != Strictness.LENIENT) {
          throw new IllegalArgumentException("Numeric values must be finite, but was " + string);
        }
      } else if (numberClass != Float.class
          && numberClass != Double.class
          && !VALID_JSON_NUMBER_PATTERN.matcher(string).matches()) {
        throw new IllegalArgumentException(
            "String created by " + numberClass + " is not a valid JSON number: " + string);
      }
    }
    beforeValue();
    out.append(string);
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter nullValue() throws IOException {
    if (deferredName != null) {
      if (serializeNulls) {
        writeDeferredName();
      } else {
        deferredName = null;
        return this;
      }
    }
    beforeValue();
    out.write("null");
    return this;
  }
  @CanIgnoreReturnValue
  public JsonWriter jsonValue(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    out.append(value);
    return this;
  }
  @Override
  public void flush() throws IOException {
    if (stackSize == 0) {
      throw new IllegalStateException("JsonWriter is closed.");
    }
    out.flush();
  }
  @Override
  public void close() throws IOException {
    out.close();
    int size = stackSize;
    if (size > 1 || (size == 1 && stack[size - 1] != NONEMPTY_DOCUMENT)) {
      throw new IOException("Incomplete document");
    }
    stackSize = 0;
  }
  private static boolean alwaysCreatesValidJsonNumber(Class<? extends Number> c) {
    return c == Integer.class
        || c == Long.class
        || c == Byte.class
        || c == Short.class
        || c == BigDecimal.class
        || c == BigInteger.class
        || c == AtomicInteger.class
        || c == AtomicLong.class;
  }
  private void string(String value) throws IOException {
    String[] replacements = htmlSafe ? HTML_SAFE_REPLACEMENT_CHARS : REPLACEMENT_CHARS;
    out.write('\"');
    int last = 0;
    int length = value.length();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      String replacement;
      if (c < 128) {
        replacement = replacements[c];
        if (replacement == null) {
          continue;
        }
      } else if (c == '\u2028') {
        replacement = "\\u2028";
      } else if (c == '\u2029') {
        replacement = "\\u2029";
      } else {
        continue;
      }
      if (last < i) {
        out.write(value, last, i - last);
      }
      out.write(replacement);
      last = i + 1;
    }
    if (last < length) {
      out.write(value, last, length - last);
    }
    out.write('\"');
  }
  private void newline() throws IOException {
    if (usesEmptyNewlineAndIndent) {
      return;
    }
    out.write(formattingStyle.getNewline());
    for (int i = 1, size = stackSize; i < size; i++) {
      out.write(formattingStyle.getIndent());
    }
  }
  private void beforeName() throws IOException {
    int context = peek();
    if (context == NONEMPTY_OBJECT) {
      out.write(formattedComma);
    } else if (context != EMPTY_OBJECT) {
      throw new IllegalStateException("Nesting problem.");
    }
    newline();
    replaceTop(DANGLING_NAME);
  }
  @SuppressWarnings("fallthrough")
  private void beforeValue() throws IOException {
    switch (peek()) {
      case NONEMPTY_DOCUMENT:
        if (strictness != Strictness.LENIENT) {
          throw new IllegalStateException("JSON must have only one top-level value.");
        }
      case EMPTY_DOCUMENT:
        replaceTop(NONEMPTY_DOCUMENT);
        break;
      case EMPTY_ARRAY:
        replaceTop(NONEMPTY_ARRAY);
        newline();
        break;
      case NONEMPTY_ARRAY:
        out.append(formattedComma);
        newline();
        break;
      case DANGLING_NAME:
        out.append(formattedColon);
        replaceTop(NONEMPTY_OBJECT);
        break;
      default:
        throw new IllegalStateException("Nesting problem.");
    }
  }
}