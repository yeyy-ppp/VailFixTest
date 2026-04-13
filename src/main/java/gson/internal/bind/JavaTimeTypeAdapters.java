package gson.internal.bind;
import static java.lang.Math.toIntExact;
import gson.Gson;
import gson.JsonSyntaxException;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.internal.bind.TypeAdapters.IntegerFieldsTypeAdapter;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
@IgnoreJRERequirement
final class JavaTimeTypeAdapters implements TypeAdapters.FactorySupplier {
  @Override
  public TypeAdapterFactory get() {
    return JAVA_TIME_FACTORY;
  }
  private static final TypeAdapter<Duration> DURATION =
      new IntegerFieldsTypeAdapter<Duration>("seconds", "nanos") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        Duration create(long[] values) {
          return Duration.ofSeconds(values[0], values[1]);
        }
        @Override
        @SuppressWarnings("JavaDurationGetSecondsGetNano")
        long[] integerValues(Duration duration) {
          return new long[] {duration.getSeconds(), duration.getNano()};
        }
      };
  private static final TypeAdapter<Instant> INSTANT =
      new IntegerFieldsTypeAdapter<Instant>("seconds", "nanos") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        Instant create(long[] values) {
          return Instant.ofEpochSecond(values[0], values[1]);
        }
        @Override
        @SuppressWarnings("JavaInstantGetSecondsGetNano")
        long[] integerValues(Instant instant) {
          return new long[] {instant.getEpochSecond(), instant.getNano()};
        }
      };
  private static final TypeAdapter<LocalDate> LOCAL_DATE =
      new IntegerFieldsTypeAdapter<LocalDate>("year", "month", "day") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        LocalDate create(long[] values) {
          return LocalDate.of(toIntExact(values[0]), toIntExact(values[1]), toIntExact(values[2]));
        }
        @Override
        long[] integerValues(LocalDate localDate) {
          return new long[] {
            localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()
          };
        }
      };
  public static final TypeAdapter<LocalTime> LOCAL_TIME =
      new IntegerFieldsTypeAdapter<LocalTime>("hour", "minute", "second", "nano") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        LocalTime create(long[] values) {
          return LocalTime.of(
              toIntExact(values[0]),
              toIntExact(values[1]),
              toIntExact(values[2]),
              toIntExact(values[3]));
        }
        @Override
        long[] integerValues(LocalTime localTime) {
          return new long[] {
            localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano()
          };
        }
      };
  private static TypeAdapter<LocalDateTime> localDateTime(Gson gson) {
    TypeAdapter<LocalDate> localDateAdapter = gson.getAdapter(LocalDate.class);
    TypeAdapter<LocalTime> localTimeAdapter = gson.getAdapter(LocalTime.class);
    return new TypeAdapter<LocalDateTime>() {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, String value) {

        }

        @Override
        public String read(com.google.gson.stream.JsonReader in) {
            return null;
        }
      @Override
      public LocalDateTime read(JsonReader in) throws IOException {
        LocalDate localDate = null;
        LocalTime localTime = null;
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
          String name = in.nextName();
          switch (name) {
            case "date":
              localDate = localDateAdapter.read(in);
              break;
            case "time":
              localTime = localTimeAdapter.read(in);
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        return LocalDateTime.of(
            requireNonNullField(localDate, "date", in), requireNonNullField(localTime, "time", in));
      }
      @Override
      public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.beginObject();
        out.name("date");
        localDateAdapter.write(out, value.toLocalDate());
        out.name("time");
        localTimeAdapter.write(out, value.toLocalTime());
        out.endObject();
      }
    }.nullSafe();
  }
  private static final TypeAdapter<MonthDay> MONTH_DAY =
      new IntegerFieldsTypeAdapter<MonthDay>("month", "day") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        MonthDay create(long[] values) {
          return MonthDay.of(toIntExact(values[0]), toIntExact(values[1]));
        }
        @Override
        long[] integerValues(MonthDay monthDay) {
          return new long[] {monthDay.getMonthValue(), monthDay.getDayOfMonth()};
        }
      };
  private static TypeAdapter<OffsetDateTime> offsetDateTime(Gson gson) {
    TypeAdapter<LocalDateTime> localDateTimeAdapter = localDateTime(gson);
    TypeAdapter<ZoneOffset> zoneOffsetAdapter = gson.getAdapter(ZoneOffset.class);
    return new TypeAdapter<OffsetDateTime>() {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, String value) {

        }

        @Override
        public String read(com.google.gson.stream.JsonReader in) {
            return null;
        }
      @Override
      public OffsetDateTime read(JsonReader in) throws IOException {
        in.beginObject();
        LocalDateTime localDateTime = null;
        ZoneOffset zoneOffset = null;
        while (in.peek() != JsonToken.END_OBJECT) {
          String name = in.nextName();
          switch (name) {
            case "dateTime":
              localDateTime = localDateTimeAdapter.read(in);
              break;
            case "offset":
              zoneOffset = zoneOffsetAdapter.read(in);
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        return OffsetDateTime.of(
            requireNonNullField(localDateTime, "dateTime", in),
            requireNonNullField(zoneOffset, "offset", in));
      }
      @Override
      public void write(JsonWriter out, OffsetDateTime value) throws IOException {
        out.beginObject();
        out.name("dateTime");
        localDateTimeAdapter.write(out, value.toLocalDateTime());
        out.name("offset");
        zoneOffsetAdapter.write(out, value.getOffset());
        out.endObject();
      }
    }.nullSafe();
  }
  private static TypeAdapter<OffsetTime> offsetTime(Gson gson) {
    TypeAdapter<LocalTime> localTimeAdapter = gson.getAdapter(LocalTime.class);
    TypeAdapter<ZoneOffset> zoneOffsetAdapter = gson.getAdapter(ZoneOffset.class);
    return new TypeAdapter<OffsetTime>() {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, String value) {

        }

        @Override
        public String read(com.google.gson.stream.JsonReader in) {
            return null;
        }
      @Override
      public OffsetTime read(JsonReader in) throws IOException {
        in.beginObject();
        LocalTime localTime = null;
        ZoneOffset zoneOffset = null;
        while (in.peek() != JsonToken.END_OBJECT) {
          String name = in.nextName();
          switch (name) {
            case "time":
              localTime = localTimeAdapter.read(in);
              break;
            case "offset":
              zoneOffset = zoneOffsetAdapter.read(in);
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        return OffsetTime.of(
            requireNonNullField(localTime, "time", in),
            requireNonNullField(zoneOffset, "offset", in));
      }
      @Override
      public void write(JsonWriter out, OffsetTime value) throws IOException {
        out.beginObject();
        out.name("time");
        localTimeAdapter.write(out, value.toLocalTime());
        out.name("offset");
        zoneOffsetAdapter.write(out, value.getOffset());
        out.endObject();
      }
    }.nullSafe();
  }
  private static final TypeAdapter<Period> PERIOD =
      new IntegerFieldsTypeAdapter<Period>("years", "months", "days") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        Period create(long[] values) {
          return Period.of(toIntExact(values[0]), toIntExact(values[1]), toIntExact(values[2]));
        }
        @Override
        long[] integerValues(Period period) {
          return new long[] {period.getYears(), period.getMonths(), period.getDays()};
        }
      };
  private static final TypeAdapter<Year> YEAR =
      new IntegerFieldsTypeAdapter<Year>("year") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        Year create(long[] values) {
          return Year.of(toIntExact(values[0]));
        }
        @Override
        long[] integerValues(Year year) {
          return new long[] {year.getValue()};
        }
      };
  private static final TypeAdapter<YearMonth> YEAR_MONTH =
      new IntegerFieldsTypeAdapter<YearMonth>("year", "month") {
          @Override
          public void write(com.google.gson.stream.JsonWriter out, String value) {

          }

          @Override
          public String read(com.google.gson.stream.JsonReader in) {
              return null;
          }
        @Override
        YearMonth create(long[] values) {
          return YearMonth.of(toIntExact(values[0]), toIntExact(values[1]));
        }
        @Override
        long[] integerValues(YearMonth yearMonth) {
          return new long[] {yearMonth.getYear(), yearMonth.getMonthValue()};
        }
      };
  private static final TypeAdapter<ZoneId> ZONE_ID =
      new TypeAdapter<ZoneId>() {
        @Override
        public ZoneId read(JsonReader in) throws IOException {
          in.beginObject();
          String id = null;
          Integer totalSeconds = null;
          while (in.peek() != JsonToken.END_OBJECT) {
            String name = in.nextName();
            switch (name) {
              case "id":
                id = in.nextString();
                break;
              case "totalSeconds":
                totalSeconds = in.nextInt();
                break;
              default:
                in.skipValue();
            }
          }
          in.endObject();
          if (id != null) {
            return ZoneId.of(id);
          } else if (totalSeconds != null) {
            return ZoneOffset.ofTotalSeconds(totalSeconds);
          } else {
            throw new JsonSyntaxException(
                "Missing id or totalSeconds field; at path " + in.getPreviousPath());
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
        public void write(JsonWriter out, ZoneId value) throws IOException {
          if (value instanceof ZoneOffset) {
            out.beginObject();
            out.name("totalSeconds");
            out.value(((ZoneOffset) value).getTotalSeconds());
            out.endObject();
          } else {
            out.beginObject();
            out.name("id");
            out.value(value.getId());
            out.endObject();
          }
        }
      }.nullSafe();
  private static TypeAdapter<ZonedDateTime> zonedDateTime(Gson gson) {
    TypeAdapter<LocalDateTime> localDateTimeAdapter = localDateTime(gson);
    TypeAdapter<ZoneOffset> zoneOffsetAdapter = gson.getAdapter(ZoneOffset.class);
    TypeAdapter<ZoneId> zoneIdAdapter = gson.getAdapter(ZoneId.class);
    return new TypeAdapter<ZonedDateTime>() {
      @Override
      public ZonedDateTime read(JsonReader in) throws IOException {
        in.beginObject();
        LocalDateTime localDateTime = null;
        ZoneOffset zoneOffset = null;
        ZoneId zoneId = null;
        while (in.peek() != JsonToken.END_OBJECT) {
          String name = in.nextName();
          switch (name) {
            case "dateTime":
              localDateTime = localDateTimeAdapter.read(in);
              break;
            case "offset":
              zoneOffset = zoneOffsetAdapter.read(in);
              break;
            case "zone":
              zoneId = zoneIdAdapter.read(in);
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        return ZonedDateTime.ofInstant(
            requireNonNullField(localDateTime, "dateTime", in),
            requireNonNullField(zoneOffset, "offset", in),
            requireNonNullField(zoneId, "zone", in));
      }

        @Override
        public void write(com.google.gson.stream.JsonWriter out, String value) {

        }

        @Override
        public String read(com.google.gson.stream.JsonReader in) {
            return null;
        }

        @Override
      public void write(JsonWriter out, ZonedDateTime value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        out.beginObject();
        out.name("dateTime");
        localDateTimeAdapter.write(out, value.toLocalDateTime());
        out.name("offset");
        zoneOffsetAdapter.write(out, value.getOffset());
        out.name("zone");
        zoneIdAdapter.write(out, value.getZone());
        out.endObject();
      }
    }.nullSafe();
  }
  static final TypeAdapterFactory JAVA_TIME_FACTORY =
      new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          Class<? super T> rawType = typeToken.getRawType();
          if (!rawType.getName().startsWith("java.time.")) {
            return null;
          }
          TypeAdapter<?> adapter = null;
          if (rawType == Duration.class) {
            adapter = DURATION;
          } else if (rawType == Instant.class) {
            adapter = INSTANT;
          } else if (rawType == LocalDate.class) {
            adapter = LOCAL_DATE;
          } else if (rawType == LocalTime.class) {
            adapter = LOCAL_TIME;
          } else if (rawType == LocalDateTime.class) {
            adapter = localDateTime(gson);
          } else if (rawType == MonthDay.class) {
            adapter = MONTH_DAY;
          } else if (rawType == OffsetDateTime.class) {
            adapter = offsetDateTime(gson);
          } else if (rawType == OffsetTime.class) {
            adapter = offsetTime(gson);
          } else if (rawType == Period.class) {
            adapter = PERIOD;
          } else if (rawType == Year.class) {
            adapter = YEAR;
          } else if (rawType == YearMonth.class) {
            adapter = YEAR_MONTH;
          } else if (rawType == ZoneId.class || rawType == ZoneOffset.class) {
            adapter = ZONE_ID;
          } else if (rawType == ZonedDateTime.class) {
            adapter = zonedDateTime(gson);
          }
          @SuppressWarnings("unchecked")
          TypeAdapter<T> result = (TypeAdapter<T>) adapter;
          return result;
        }
      };
  private static <T> T requireNonNullField(T field, String fieldName, JsonReader reader) {
    if (field == null) {
      throw new JsonSyntaxException(
          "Missing " + fieldName + " field; at path " + reader.getPreviousPath());
    }
    return field;
  }
}