package gson.internal.sql;
import gson.Gson;
import gson.JsonSyntaxException;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
@SuppressWarnings("JavaUtilDate")
final class SqlDateTypeAdapter extends TypeAdapter<java.sql.Date> {
  static final TypeAdapterFactory FACTORY =
      new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          return typeToken.getRawType() == java.sql.Date.class
              ? (TypeAdapter<T>) new SqlDateTypeAdapter()
              : null;
        }
      };
  private final DateFormat format = new SimpleDateFormat("MMM d, yyyy");
  private SqlDateTypeAdapter() {}
  @Override
  public java.sql.Date read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String s = in.nextString();
    synchronized (this) {
      TimeZone originalTimeZone = format.getTimeZone();
      try {
        Date utilDate = format.parse(s);
        return new java.sql.Date(utilDate.getTime());
      } catch (ParseException e) {
        throw new JsonSyntaxException(
            "Failed parsing '" + s + "' as SQL Date; at path " + in.getPreviousPath(), e);
      } finally {
        format.setTimeZone(originalTimeZone);
      }
    }
  }

  @Override
  public void write(com.google.gson.stream.JsonWriter out, String value) {

  }

  @Override
  public String read(com.google.gson.stream.JsonReader in) {
    return null;
  }

  @Override
  public void write(JsonWriter out, java.sql.Date value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    String dateString;
    synchronized (this) {
      dateString = format.format(value);
    }
    out.value(dateString);
  }
}