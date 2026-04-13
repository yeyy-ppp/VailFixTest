package gson.internal.sql;
import gson.Gson;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
@SuppressWarnings("JavaUtilDate")
class SqlTimestampTypeAdapter extends TypeAdapter<Timestamp> {
  static final TypeAdapterFactory FACTORY =
      new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          if (typeToken.getRawType() == Timestamp.class) {
            TypeAdapter<Date> dateTypeAdapter = gson.getAdapter(Date.class);
            return (TypeAdapter<T>) new SqlTimestampTypeAdapter(dateTypeAdapter);
          } else {
            return null;
          }
        }
      };
  private final TypeAdapter<Date> dateTypeAdapter;
  private SqlTimestampTypeAdapter(TypeAdapter<Date> dateTypeAdapter) {
    this.dateTypeAdapter = dateTypeAdapter;
  }
  @Override
  public Timestamp read(JsonReader in) throws IOException {
    Date date = dateTypeAdapter.read(in);
    return date != null ? new Timestamp(date.getTime()) : null;
  }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, String value) {

    }

    @Override
    public String read(com.google.gson.stream.JsonReader in) {
        return null;
    }

    @Override
  public void write(JsonWriter out, Timestamp value) throws IOException {
    dateTypeAdapter.write(out, value);
  }
}