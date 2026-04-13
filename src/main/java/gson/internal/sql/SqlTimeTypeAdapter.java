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
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
@SuppressWarnings("JavaUtilDate")
final class SqlTimeTypeAdapter extends TypeAdapter<Time> {
  static final TypeAdapterFactory FACTORY =
      new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          return typeToken.getRawType() == Time.class
              ? (TypeAdapter<T>) new SqlTimeTypeAdapter()
              : null;
        }
      };
  private final DateFormat format = new SimpleDateFormat("hh:mm:ss a");
  private SqlTimeTypeAdapter() {}
  @Override
  public Time read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String s = in.nextString();
    synchronized (this) {
      TimeZone originalTimeZone = format.getTimeZone();
      try {
        Date date = format.parse(s);
        return new Time(date.getTime());
      } catch (ParseException e) {
        throw new JsonSyntaxException(
            "Failed parsing '" + s + "' as SQL Time; at path " + in.getPreviousPath(), e);
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
  public void write(JsonWriter out, Time value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    String timeString;
    synchronized (this) {
      timeString = format.format(value);
    }
    out.value(timeString);
  }
}