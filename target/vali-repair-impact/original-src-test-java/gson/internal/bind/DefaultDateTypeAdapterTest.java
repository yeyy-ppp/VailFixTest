package gson.internal.bind;
import gson.Gson;
import gson.JsonSyntaxException;
import gson.TypeAdapter;
import gson.TypeAdapterFactory;
import gson.reflect.TypeToken;
import gson.stream.JsonReader;
import gson.stream.JsonToken;
import gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultDateTypeAdapterTest {
    private final DefaultDateTypeAdapter.DateType<Date> dateType = DefaultDateTypeAdapter.DateType.DATE;

    @Test
    void testCreateWithDateType() {
        TypeAdapter<?> adapter = DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY.create(new Gson(), TypeToken.get(Date.class));
        assertNotNull(adapter);
        assertNull(DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY.create(new Gson(), TypeToken.get(Object.class)));
    }

    @Test
    void testFactoryToString() {
        assertEquals("DefaultDateTypeAdapter#DEFAULT_STYLE_FACTORY", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY.toString());
    }

    @Test
    void testConstructorWithPattern() throws Exception {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        DefaultDateTypeAdapter<Date> defaultAdapter = (DefaultDateTypeAdapter<Date>) adapter;
        Field dateFormatsField = DefaultDateTypeAdapter.class.getDeclaredField("dateFormats");
        dateFormatsField.setAccessible(true);
        List<DateFormat> dateFormats = (List<DateFormat>) dateFormatsField.get(defaultAdapter);
        assertTrue(dateFormats.size() >= 1);
    }

    @Test
    void testConstructorWithStyles() throws Exception {
        TypeAdapterFactory factory = dateType.createAdapterFactory(DateFormat.SHORT, DateFormat.SHORT);
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        DefaultDateTypeAdapter<Date> defaultAdapter = (DefaultDateTypeAdapter<Date>) adapter;
        Field dateFormatsField = DefaultDateTypeAdapter.class.getDeclaredField("dateFormats");
        dateFormatsField.setAccessible(true);
        List<DateFormat> dateFormats = (List<DateFormat>) dateFormatsField.get(defaultAdapter);
        assertTrue(dateFormats.size() >= 2);
    }

    @Test
    void testWriteNull() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        adapter.write(jsonWriter, null);
        jsonWriter.close();
        assertEquals("null", writer.toString());
    }

    @Test
    void testWriteNonNull() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        Date date = new Date(0);
        adapter.write(jsonWriter, date);
        jsonWriter.close();
        String result = writer.toString();
        assertTrue(result.contains("1970") || result.contains("70"));
    }

    @Test
    void testReadNull() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        JsonReader reader = new JsonReader(new StringReader("null"));
        assertEquals(JsonToken.NULL, reader.peek());
        assertNull(adapter.read(reader));
    }

    @Test
    void testReadValidDate() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        JsonReader reader = new JsonReader(new StringReader("\"1970-01-01\""));
        Date date = adapter.read(reader);
        assertNotNull(date);
    }

    @Test
    void testReadWithParseException() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        JsonReader reader = new JsonReader(new StringReader("\"invalid\""));
        assertThrows(JsonSyntaxException.class, () -> adapter.read(reader));
    }

    @Test
    void testDeserializeToDateWithMultipleFormats() throws IOException {
        TypeAdapterFactory factory = dateType.createAdapterFactory(DateFormat.SHORT, DateFormat.SHORT);
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        JsonReader reader = new JsonReader(new StringReader("\"1/1/70 0:00 AM\""));
        Date date = adapter.read(reader);
        assertNotNull(date);
    }

    @Test
    void testToStringWithSimpleDateFormat() {
        TypeAdapterFactory factory = dateType.createAdapterFactory("yyyy-MM-dd");
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        DefaultDateTypeAdapter<Date> defaultAdapter = (DefaultDateTypeAdapter<Date>) adapter;
        assertTrue(defaultAdapter.toString().contains("DefaultDateTypeAdapter(yyyy-MM-dd)"));
    }

    @Test
    void testToStringWithNonSimpleDateFormat() {
        TypeAdapterFactory factory = dateType.createAdapterFactory(DateFormat.FULL, DateFormat.FULL);
        TypeAdapter<Date> adapter = factory.create(new Gson(), TypeToken.get(Date.class));
        DefaultDateTypeAdapter<Date> defaultAdapter = (DefaultDateTypeAdapter<Date>) adapter;
        assertTrue(defaultAdapter.toString().contains("DefaultDateTypeAdapter("));
    }
}