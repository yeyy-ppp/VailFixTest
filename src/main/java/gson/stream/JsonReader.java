package gson.stream;
import gson.Strictness;
import gson.internal.JsonReaderInternalAccess;
import gson.internal.TroubleshootingGuide;
import gson.internal.bind.JsonTreeReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Objects;
public class JsonReader implements Closeable {
  private static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;
  private static final int PEEKED_NONE = 0;
  private static final int PEEKED_BEGIN_OBJECT = 1;
  private static final int PEEKED_END_OBJECT = 2;
  private static final int PEEKED_BEGIN_ARRAY = 3;
  private static final int PEEKED_END_ARRAY = 4;
  private static final int PEEKED_TRUE = 5;
  private static final int PEEKED_FALSE = 6;
  private static final int PEEKED_NULL = 7;
  private static final int PEEKED_SINGLE_QUOTED = 8;
  private static final int PEEKED_DOUBLE_QUOTED = 9;
  private static final int PEEKED_UNQUOTED = 10;
  private static final int PEEKED_BUFFERED = 11;
  private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
  private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
  private static final int PEEKED_UNQUOTED_NAME = 14;
  private static final int PEEKED_LONG = 15;
  private static final int PEEKED_NUMBER = 16;
  private static final int PEEKED_EOF = 17;
  private static final int NUMBER_CHAR_NONE = 0;
  private static final int NUMBER_CHAR_SIGN = 1;
  private static final int NUMBER_CHAR_DIGIT = 2;
  private static final int NUMBER_CHAR_DECIMAL = 3;
  private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
  private static final int NUMBER_CHAR_EXP_E = 5;
  private static final int NUMBER_CHAR_EXP_SIGN = 6;
  private static final int NUMBER_CHAR_EXP_DIGIT = 7;
  private final Reader in;
  private Strictness strictness = Strictness.LEGACY_STRICT;
  static final int DEFAULT_NESTING_LIMIT = 255;
  private int nestingLimit = DEFAULT_NESTING_LIMIT;
  static final int BUFFER_SIZE = 1024;
  private final char[] buffer = new char[BUFFER_SIZE];
  private int pos = 0;
  private int limit = 0;
  private int lineNumber = 0;
  private int lineStart = 0;
  int peeked = PEEKED_NONE;
  private long peekedLong;
  private int peekedNumberLength;
  private String peekedString;
  private int[] stack = new int[32];
  private int stackSize = 0;
  {
    stack[stackSize++] = JsonScope.EMPTY_DOCUMENT;
  }
  private String[] pathNames = new String[32];
  private int[] pathIndices = new int[32];
  public JsonReader(Reader in) {
    this.in = Objects.requireNonNull(in, "in == null");
  }
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public final void setLenient(boolean lenient) {
    setStrictness(lenient ? Strictness.LENIENT : Strictness.LEGACY_STRICT);
  }
  public final boolean isLenient() {
    return strictness == Strictness.LENIENT;
  }
  public final void setStrictness(Strictness strictness) {
    Objects.requireNonNull(strictness);
    this.strictness = strictness;
  }
  public final Strictness getStrictness() {
    return strictness;
  }
  public final void setNestingLimit(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Invalid nesting limit: " + limit);
    }
    this.nestingLimit = limit;
  }
  public final int getNestingLimit() {
    return nestingLimit;
  }
  public void beginArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_ARRAY) {
      push(JsonScope.EMPTY_ARRAY);
      pathIndices[stackSize - 1] = 0;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("BEGIN_ARRAY");
    }
  }
  public void endArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_ARRAY) {
      stackSize--;
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("END_ARRAY");
    }
  }
  public void beginObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_OBJECT) {
      push(JsonScope.EMPTY_OBJECT);
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("BEGIN_OBJECT");
    }
  }
  public void endObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_OBJECT) {
      stackSize--;
      pathNames[stackSize] = null;
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("END_OBJECT");
    }
  }
  public boolean hasNext() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY && p != PEEKED_EOF;
  }
  public JsonToken peek() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    switch (p) {
      case PEEKED_BEGIN_OBJECT:
        return JsonToken.BEGIN_OBJECT;
      case PEEKED_END_OBJECT:
        return JsonToken.END_OBJECT;
      case PEEKED_BEGIN_ARRAY:
        return JsonToken.BEGIN_ARRAY;
      case PEEKED_END_ARRAY:
        return JsonToken.END_ARRAY;
      case PEEKED_SINGLE_QUOTED_NAME:
      case PEEKED_DOUBLE_QUOTED_NAME:
      case PEEKED_UNQUOTED_NAME:
        return JsonToken.NAME;
      case PEEKED_TRUE:
      case PEEKED_FALSE:
        return JsonToken.BOOLEAN;
      case PEEKED_NULL:
        return JsonToken.NULL;
      case PEEKED_SINGLE_QUOTED:
      case PEEKED_DOUBLE_QUOTED:
      case PEEKED_UNQUOTED:
      case PEEKED_BUFFERED:
        return JsonToken.STRING;
      case PEEKED_LONG:
      case PEEKED_NUMBER:
        return JsonToken.NUMBER;
      case PEEKED_EOF:
        return JsonToken.END_DOCUMENT;
      default:
        throw new AssertionError();
    }
  }
  @SuppressWarnings("fallthrough")
  int doPeek() throws IOException {
    int peekStack = stack[stackSize - 1];
    if (peekStack == JsonScope.EMPTY_ARRAY) {
      stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
    } else if (peekStack == JsonScope.NONEMPTY_ARRAY) {
      int c = nextNonWhitespace(true);
      switch (c) {
        case ']':
          peeked = PEEKED_END_ARRAY;
          return peeked;
        case ';':
          checkLenient();
        case ',':
          break;
        default:
          throw syntaxError("Unterminated array");
      }
    } else if (peekStack == JsonScope.EMPTY_OBJECT || peekStack == JsonScope.NONEMPTY_OBJECT) {
      stack[stackSize - 1] = JsonScope.DANGLING_NAME;
      if (peekStack == JsonScope.NONEMPTY_OBJECT) {
        int c = nextNonWhitespace(true);
        switch (c) {
          case '}':
            peeked = PEEKED_END_OBJECT;
            return peeked;
          case ';':
            checkLenient();
          case ',':
            break;
          default:
            throw syntaxError("Unterminated object");
        }
      }
      int c = nextNonWhitespace(true);
      switch (c) {
        case '"':
          peeked = PEEKED_DOUBLE_QUOTED_NAME;
          return peeked;
        case '\'':
          checkLenient();
          peeked = PEEKED_SINGLE_QUOTED_NAME;
          return peeked;
        case '}':
          if (peekStack != JsonScope.NONEMPTY_OBJECT) {
            peeked = PEEKED_END_OBJECT;
            return peeked;
          } else {
            throw syntaxError("Expected name");
          }
        default:
          checkLenient();
          pos--;
          if (isLiteral((char) c)) {
            peeked = PEEKED_UNQUOTED_NAME;
            return peeked;
          } else {
            throw syntaxError("Expected name");
          }
      }
    } else if (peekStack == JsonScope.DANGLING_NAME) {
      stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
      int c = nextNonWhitespace(true);
      switch (c) {
        case ':':
          break;
        case '=':
          checkLenient();
          if ((pos < limit || fillBuffer(1)) && buffer[pos] == '>') {
            pos++;
          }
          break;
        default:
          throw syntaxError("Expected ':'");
      }
    } else if (peekStack == JsonScope.EMPTY_DOCUMENT) {
      if (strictness == Strictness.LENIENT) {
        consumeNonExecutePrefix();
      }
      stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
    } else if (peekStack == JsonScope.NONEMPTY_DOCUMENT) {
      int c = nextNonWhitespace(false);
      if (c == -1) {
        peeked = PEEKED_EOF;
        return peeked;
      } else {
        checkLenient();
        pos--;
      }
    } else if (peekStack == JsonScope.CLOSED) {
      throw new IllegalStateException("JsonReader is closed");
    }
    int c = nextNonWhitespace(true);
    switch (c) {
      case ']':
        if (peekStack == JsonScope.EMPTY_ARRAY) {
          peeked = PEEKED_END_ARRAY;
          return peeked;
        }
      case ';':
      case ',':
        if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
          checkLenient();
          pos--;
          peeked = PEEKED_NULL;
          return peeked;
        } else {
          throw syntaxError("Unexpected value");
        }
      case '\'':
        checkLenient();
        peeked = PEEKED_SINGLE_QUOTED;
        return peeked;
      case '"':
        peeked = PEEKED_DOUBLE_QUOTED;
        return peeked;
      case '[':
        peeked = PEEKED_BEGIN_ARRAY;
        return peeked;
      case '{':
        peeked = PEEKED_BEGIN_OBJECT;
        return peeked;
      default:
        pos--;
    }
    int result = peekKeyword();
    if (result != PEEKED_NONE) {
      return result;
    }
    result = peekNumber();
    if (result != PEEKED_NONE) {
      return result;
    }
    if (!isLiteral(buffer[pos])) {
      throw syntaxError("Expected value");
    }
    checkLenient();
    peeked = PEEKED_UNQUOTED;
    return peeked;
  }
  private int peekKeyword() throws IOException {
    char c = buffer[pos];
    String keyword;
    String keywordUpper;
    int peeking;
    if (c == 't' || c == 'T') {
      keyword = "true";
      keywordUpper = "TRUE";
      peeking = PEEKED_TRUE;
    } else if (c == 'f' || c == 'F') {
      keyword = "false";
      keywordUpper = "FALSE";
      peeking = PEEKED_FALSE;
    } else if (c == 'n' || c == 'N') {
      keyword = "null";
      keywordUpper = "NULL";
      peeking = PEEKED_NULL;
    } else {
      return PEEKED_NONE;
    }
    boolean allowsUpperCased = strictness != Strictness.STRICT;
    int length = keyword.length();
    for (int i = 0; i < length; i++) {
      if (pos + i >= limit && !fillBuffer(i + 1)) {
        return PEEKED_NONE;
      }
      c = buffer[pos + i];
      boolean matched = c == keyword.charAt(i) || (allowsUpperCased && c == keywordUpper.charAt(i));
      if (!matched) {
        return PEEKED_NONE;
      }
    }
    if ((pos + length < limit || fillBuffer(length + 1)) && isLiteral(buffer[pos + length])) {
      return PEEKED_NONE;
    }
    pos += length;
    peeked = peeking;
    return peeked;
  }
  private int peekNumber() throws IOException {
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;
    long value = 0;
    boolean negative = false;
    boolean fitsInLong = true;
    int last = NUMBER_CHAR_NONE;
    int i = 0;
    charactersOfNumber:
    for (; true; i++) {
      if (p + i == l) {
        if (i == buffer.length) {
          return PEEKED_NONE;
        }
        if (!fillBuffer(i + 1)) {
          break;
        }
        p = pos;
        l = limit;
      }
      char c = buffer[p + i];
      switch (c) {
        case '-':
          if (last == NUMBER_CHAR_NONE) {
            negative = true;
            last = NUMBER_CHAR_SIGN;
            continue;
          } else if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;
        case '+':
          if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;
        case 'e':
        case 'E':
          if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT) {
            last = NUMBER_CHAR_EXP_E;
            continue;
          }
          return PEEKED_NONE;
        case '.':
          if (last == NUMBER_CHAR_DIGIT) {
            last = NUMBER_CHAR_DECIMAL;
            continue;
          }
          return PEEKED_NONE;
        default:
          if (c < '0' || c > '9') {
            if (!isLiteral(c)) {
              break charactersOfNumber;
            }
            return PEEKED_NONE;
          }
          if (last == NUMBER_CHAR_SIGN || last == NUMBER_CHAR_NONE) {
            value = -(c - '0');
            last = NUMBER_CHAR_DIGIT;
          } else if (last == NUMBER_CHAR_DIGIT) {
            if (value == 0) {
              return PEEKED_NONE;
            }
            long newValue = value * 10 - (c - '0');
            fitsInLong &=
                value > MIN_INCOMPLETE_INTEGER
                    || (value == MIN_INCOMPLETE_INTEGER && newValue < value);
            value = newValue;
          } else if (last == NUMBER_CHAR_DECIMAL) {
            last = NUMBER_CHAR_FRACTION_DIGIT;
          } else if (last == NUMBER_CHAR_EXP_E || last == NUMBER_CHAR_EXP_SIGN) {
            last = NUMBER_CHAR_EXP_DIGIT;
          }
      }
    }
    if (last == NUMBER_CHAR_DIGIT
        && fitsInLong
        && (value != Long.MIN_VALUE || negative)
        && (value != 0 || !negative)) {
      peekedLong = negative ? value : -value;
      pos += i;
      peeked = PEEKED_LONG;
      return peeked;
    } else if (last == NUMBER_CHAR_DIGIT
        || last == NUMBER_CHAR_FRACTION_DIGIT
        || last == NUMBER_CHAR_EXP_DIGIT) {
      peekedNumberLength = i;
      peeked = PEEKED_NUMBER;
      return peeked;
    } else {
      return PEEKED_NONE;
    }
  }
  @SuppressWarnings("fallthrough")
  private boolean isLiteral(char c) throws IOException {
    switch (c) {
      case '/':
      case '\\':
      case ';':
      case '#':
      case '=':
        checkLenient();
      case '{':
      case '}':
      case '[':
      case ']':
      case ':':
      case ',':
      case ' ':
      case '\t':
      case '\f':
      case '\r':
      case '\n':
        return false;
      default:
        return true;
    }
  }
  public String nextName() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED_NAME) {
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
      result = nextQuotedValue('"');
    } else {
      throw unexpectedTokenError("a name");
    }
    peeked = PEEKED_NONE;
    pathNames[stackSize - 1] = result;
    return result;
  }
  public String nextString() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED) {
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED) {
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED) {
      result = nextQuotedValue('"');
    } else if (p == PEEKED_BUFFERED) {
      result = peekedString;
      peekedString = null;
    } else if (p == PEEKED_LONG) {
      result = Long.toString(peekedLong);
    } else if (p == PEEKED_NUMBER) {
      result = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else {
      throw unexpectedTokenError("a string");
    }
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }
  public boolean nextBoolean() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_TRUE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return true;
    } else if (p == PEEKED_FALSE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return false;
    }
    throw unexpectedTokenError("a boolean");
  }
  public void nextNull() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_NULL) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
    } else {
      throw unexpectedTokenError("null");
    }
  }
  public double nextDouble() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return (double) peekedLong;
    }
    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED) {
      peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
    } else if (p == PEEKED_UNQUOTED) {
      peekedString = nextUnquotedValue();
    } else if (p != PEEKED_BUFFERED) {
      throw unexpectedTokenError("a double");
    }
    peeked = PEEKED_BUFFERED;
    double result = Double.parseDouble(peekedString);
    if (strictness != Strictness.LENIENT && (Double.isNaN(result) || Double.isInfinite(result))) {
      throw syntaxError("JSON forbids NaN and infinities: " + result);
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }
  public long nextLong() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return peekedLong;
    }
    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        long result = Long.parseLong(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
      }
    } else {
      throw unexpectedTokenError("a long");
    }
    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString);
    long result = (long) asDouble;
    if (result != asDouble) {
      throw new NumberFormatException("Expected a long but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }
  private String nextQuotedValue(char quote) throws IOException {
    char[] buffer = this.buffer;
    StringBuilder builder = null;
    while (true) {
      int p = pos;
      int l = limit;
      int start = p;
      while (p < l) {
        int c = buffer[p++];
        if (strictness == Strictness.STRICT && c < 0x20) {
          throw syntaxError(
              "Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");
        } else if (c == quote) {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            return new String(buffer, start, len);
          } else {
            builder.append(buffer, start, len);
            return builder.toString();
          }
        } else if (c == '\\') {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            int estimatedLength = (len + 1) * 2;
            builder = new StringBuilder(Math.max(estimatedLength, 16));
          }
          builder.append(buffer, start, len);
          builder.append(readEscapeCharacter());
          p = pos;
          l = limit;
          start = p;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }
      if (builder == null) {
        int estimatedLength = (p - start) * 2;
        builder = new StringBuilder(Math.max(estimatedLength, 16));
      }
      builder.append(buffer, start, p - start);
      pos = p;
      if (!fillBuffer(1)) {
        throw syntaxError("Unterminated string");
      }
    }
  }
  @SuppressWarnings("fallthrough")
  private String nextUnquotedValue() throws IOException {
    StringBuilder builder = null;
    int i = 0;
    findNonLiteralCharacter:
    while (true) {
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
          case '/':
          case '\\':
          case ';':
          case '#':
          case '=':
            checkLenient();
          case '{':
          case '}':
          case '[':
          case ']':
          case ':':
          case ',':
          case ' ':
          case '\t':
          case '\f':
          case '\r':
          case '\n':
            break findNonLiteralCharacter;
          default:
        }
      }
      if (i < buffer.length) {
        if (fillBuffer(i + 1)) {
          continue;
        } else {
          break;
        }
      }
      if (builder == null) {
        builder = new StringBuilder(Math.max(i, 16));
      }
      builder.append(buffer, pos, i);
      pos += i;
      i = 0;
      if (!fillBuffer(1)) {
        break;
      }
    }
    String result =
        (builder == null) ? new String(buffer, pos, i) : builder.append(buffer, pos, i).toString();
    pos += i;
    return result;
  }
  private void skipQuotedValue(char quote) throws IOException {
    char[] buffer = this.buffer;
    do {
      int p = pos;
      int l = limit;
      while (p < l) {
        int c = buffer[p++];
        if (c == quote) {
          pos = p;
          return;
        } else if (c == '\\') {
          pos = p;
          char unused = readEscapeCharacter();
          p = pos;
          l = limit;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }
      pos = p;
    } while (fillBuffer(1));
    throw syntaxError("Unterminated string");
  }
  @SuppressWarnings("fallthrough")
  private void skipUnquotedValue() throws IOException {
    do {
      int i = 0;
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
          case '/':
          case '\\':
          case ';':
          case '#':
          case '=':
            checkLenient();
          case '{':
          case '}':
          case '[':
          case ']':
          case ':':
          case ',':
          case ' ':
          case '\t':
          case '\f':
          case '\r':
          case '\n':
            pos += i;
            return;
          default:
        }
      }
      pos += i;
    } while (fillBuffer(1));
  }
  public int nextInt() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    int result;
    if (p == PEEKED_LONG) {
      result = (int) peekedLong;
      if (peekedLong != result) {
        throw new NumberFormatException("Expected an int but was " + peekedLong + locationString());
      }
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return result;
    }
    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        result = Integer.parseInt(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
      }
    } else {
      throw unexpectedTokenError("an int");
    }
    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString);
    result = (int) asDouble;
    if (result != asDouble) {
      throw new NumberFormatException("Expected an int but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }
  @Override
  public void close() throws IOException {
    peeked = PEEKED_NONE;
    stack[0] = JsonScope.CLOSED;
    stackSize = 1;
    in.close();
  }
  public void skipValue() throws IOException {
    int count = 0;
    do {
      int p = peeked;
      if (p == PEEKED_NONE) {
        p = doPeek();
      }
      switch (p) {
        case PEEKED_BEGIN_ARRAY:
          push(JsonScope.EMPTY_ARRAY);
          count++;
          break;
        case PEEKED_BEGIN_OBJECT:
          push(JsonScope.EMPTY_OBJECT);
          count++;
          break;
        case PEEKED_END_ARRAY:
          stackSize--;
          count--;
          break;
        case PEEKED_END_OBJECT:
          if (count == 0) {
            pathNames[stackSize - 1] = null;
          }
          stackSize--;
          count--;
          break;
        case PEEKED_UNQUOTED:
          skipUnquotedValue();
          break;
        case PEEKED_SINGLE_QUOTED:
          skipQuotedValue('\'');
          break;
        case PEEKED_DOUBLE_QUOTED:
          skipQuotedValue('"');
          break;
        case PEEKED_UNQUOTED_NAME:
          skipUnquotedValue();
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_SINGLE_QUOTED_NAME:
          skipQuotedValue('\'');
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_DOUBLE_QUOTED_NAME:
          skipQuotedValue('"');
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_NUMBER:
          pos += peekedNumberLength;
          break;
        case PEEKED_EOF:
          return;
        default:
      }
      peeked = PEEKED_NONE;
    } while (count > 0);
    pathIndices[stackSize - 1]++;
  }
  private void push(int newTop) throws MalformedJsonException {
    if (stackSize - 1 >= nestingLimit) {
      throw new MalformedJsonException(
          "Nesting limit " + nestingLimit + " reached" + locationString());
    }
    if (stackSize == stack.length) {
      int newLength = stackSize * 2;
      stack = Arrays.copyOf(stack, newLength);
      pathIndices = Arrays.copyOf(pathIndices, newLength);
      pathNames = Arrays.copyOf(pathNames, newLength);
    }
    stack[stackSize++] = newTop;
  }
  private boolean fillBuffer(int minimum) throws IOException {
    char[] buffer = this.buffer;
    lineStart -= pos;
    if (limit != pos) {
      limit -= pos;
      System.arraycopy(buffer, pos, buffer, 0, limit);
    } else {
      limit = 0;
    }
    pos = 0;
    int total;
    while ((total = in.read(buffer, limit, buffer.length - limit)) != -1) {
      limit += total;
      if (lineNumber == 0 && lineStart == 0 && limit > 0 && buffer[0] == '\ufeff') {
        pos++;
        lineStart++;
        minimum++;
      }
      if (limit >= minimum) {
        return true;
      }
    }
    return false;
  }
  private int nextNonWhitespace(boolean throwOnEof) throws IOException {
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;
    while (true) {
      if (p == l) {
        pos = p;
        if (!fillBuffer(1)) {
          break;
        }
        p = pos;
        l = limit;
      }
      int c = buffer[p++];
      if (c == '\n') {
        lineNumber++;
        lineStart = p;
        continue;
      } else if (c == ' ' || c == '\r' || c == '\t') {
        continue;
      }
      if (c == '/') {
        pos = p;
        if (p == l) {
          pos--;
          boolean charsLoaded = fillBuffer(2);
          pos++;
          if (!charsLoaded) {
            return c;
          }
        }
        checkLenient();
        char peek = buffer[pos];
        switch (peek) {
          case '*':
            pos++;
            if (!skipTo("*/")) {
              throw syntaxError("Unterminated comment");
            }
            p = pos + 2;
            l = limit;
            continue;
          case '/':
            pos++;
            skipToEndOfLine();
            p = pos;
            l = limit;
            continue;
          default:
            return c;
        }
      } else if (c == '#') {
        pos = p;
        checkLenient();
        skipToEndOfLine();
        p = pos;
        l = limit;
      } else {
        pos = p;
        return c;
      }
    }
    if (throwOnEof) {
      throw new EOFException("End of input" + locationString());
    } else {
      return -1;
    }
  }
  private void checkLenient() throws MalformedJsonException {
    if (strictness != Strictness.LENIENT) {
      throw syntaxError(
          "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON");
    }
  }
  private void skipToEndOfLine() throws IOException {
    while (pos < limit || fillBuffer(1)) {
      char c = buffer[pos++];
      if (c == '\n') {
        lineNumber++;
        lineStart = pos;
        break;
      } else if (c == '\r') {
        break;
      }
    }
  }
  private boolean skipTo(String toFind) throws IOException {
    int length = toFind.length();
    outer:
    for (; pos + length <= limit || fillBuffer(length); pos++) {
      if (buffer[pos] == '\n') {
        lineNumber++;
        lineStart = pos + 1;
        continue;
      }
      for (int c = 0; c < length; c++) {
        if (buffer[pos + c] != toFind.charAt(c)) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }
  @Override
  public String toString() {
    return getClass().getSimpleName() + locationString();
  }
  String locationString() {
    int line = lineNumber + 1;
    int column = pos - lineStart + 1;
    return " at line " + line + " column " + column + " path " + getPath();
  }
  private String getPath(boolean usePreviousPath) {
    StringBuilder result = new StringBuilder().append('$');
    for (int i = 0; i < stackSize; i++) {
      int scope = stack[i];
      switch (scope) {
        case JsonScope.EMPTY_ARRAY:
        case JsonScope.NONEMPTY_ARRAY:
          int pathIndex = pathIndices[i];
          if (usePreviousPath && pathIndex > 0 && i == stackSize - 1) {
            pathIndex--;
          }
          result.append('[').append(pathIndex).append(']');
          break;
        case JsonScope.EMPTY_OBJECT:
        case JsonScope.DANGLING_NAME:
        case JsonScope.NONEMPTY_OBJECT:
          result.append('.');
          if (pathNames[i] != null) {
            result.append(pathNames[i]);
          }
          break;
        case JsonScope.NONEMPTY_DOCUMENT:
        case JsonScope.EMPTY_DOCUMENT:
        case JsonScope.CLOSED:
          break;
        default:
          throw new AssertionError("Unknown scope value: " + scope);
      }
    }
    return result.toString();
  }
  public String getPath() {
    return getPath(false);
  }
  public String getPreviousPath() {
    return getPath(true);
  }
  @SuppressWarnings("fallthrough")
  private char readEscapeCharacter() throws IOException {
    if (pos == limit && !fillBuffer(1)) {
      throw syntaxError("Unterminated escape sequence");
    }
    char escaped = buffer[pos++];
    switch (escaped) {
      case 'u':
        if (pos + 4 > limit && !fillBuffer(4)) {
          throw syntaxError("Unterminated escape sequence");
        }
        int result = 0;
        for (int i = pos, end = i + 4; i < end; i++) {
          char c = buffer[i];
          result <<= 4;
          if (c >= '0' && c <= '9') {
            result += (c - '0');
          } else if (c >= 'a' && c <= 'f') {
            result += (c - 'a' + 10);
          } else if (c >= 'A' && c <= 'F') {
            result += (c - 'A' + 10);
          } else {
            throw syntaxError("Malformed Unicode escape \\u" + new String(buffer, pos, 4));
          }
        }
        pos += 4;
        return (char) result;
      case 't':
        return '\t';
      case 'b':
        return '\b';
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 'f':
        return '\f';
      case '\n':
        if (strictness == Strictness.STRICT) {
          throw syntaxError("Cannot escape a newline character in strict mode");
        }
        lineNumber++;
        lineStart = pos;
      case '\'':
        if (strictness == Strictness.STRICT) {
          throw syntaxError("Invalid escaped character \"'\" in strict mode");
        }
      case '"':
      case '\\':
      case '/':
        return escaped;
      default:
        throw syntaxError("Invalid escape sequence");
    }
  }
  private MalformedJsonException syntaxError(String message) throws MalformedJsonException {
    throw new MalformedJsonException(
        message + locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json"));
  }
  private IllegalStateException unexpectedTokenError(String expected) throws IOException {
    JsonToken peeked = peek();
    String troubleshootingId =
        peeked == JsonToken.NULL ? "adapter-not-null-safe" : "unexpected-json-structure";
    return new IllegalStateException(
        "Expected "
            + expected
            + " but was "
            + peek()
            + locationString()
            + "\nSee "
            + TroubleshootingGuide.createUrl(troubleshootingId));
  }
  private void consumeNonExecutePrefix() throws IOException {
    int unused = nextNonWhitespace(true);
    pos--;
    if (pos + 5 > limit && !fillBuffer(5)) {
      return;
    }
    int p = pos;
    char[] buf = buffer;
    if (buf[p] != ')'
        || buf[p + 1] != ']'
        || buf[p + 2] != '}'
        || buf[p + 3] != '\''
        || buf[p + 4] != '\n') {
      return;
    }
    pos += 5;
  }
  static {
    JsonReaderInternalAccess.INSTANCE =
        new JsonReaderInternalAccess() {
          @Override
          public void promoteNameToValue(JsonReader reader) throws IOException {
            if (reader instanceof JsonTreeReader) {
              ((JsonTreeReader) reader).promoteNameToValue();
              return;
            }
            int p = reader.peeked;
            if (p == PEEKED_NONE) {
              p = reader.doPeek();
            }
            if (p == PEEKED_DOUBLE_QUOTED_NAME) {
              reader.peeked = PEEKED_DOUBLE_QUOTED;
            } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
              reader.peeked = PEEKED_SINGLE_QUOTED;
            } else if (p == PEEKED_UNQUOTED_NAME) {
              reader.peeked = PEEKED_UNQUOTED;
            } else {
              throw reader.unexpectedTokenError("a name");
            }
          }
        };
  }
}