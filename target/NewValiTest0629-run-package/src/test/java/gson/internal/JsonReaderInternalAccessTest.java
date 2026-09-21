package gson.internal;

import gson.stream.JsonReader;
import gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonReaderInternalAccessTest {

    @Test
    void testPromoteNameToValue() throws IOException {
        JsonReaderInternalAccess instance = JsonReaderInternalAccess.INSTANCE;
        if (instance == null) {
            fail("JsonReaderInternalAccess.INSTANCE is not set");
        }

        String json = "{\"name\":\"value\"}";
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.beginObject();
        assertEquals(JsonToken.NAME, reader.peek());

        instance.promoteNameToValue(reader);

        assertEquals(JsonToken.STRING, reader.peek());
        assertEquals("name", reader.nextString());
        assertEquals(JsonToken.STRING, reader.peek());
        assertEquals("value", reader.nextString());
        reader.endObject();
    }

    @Test
    void testPromoteNameToValue_NotInNameState() throws IOException {
        JsonReaderInternalAccess instance = JsonReaderInternalAccess.INSTANCE;
        if (instance == null) {
            fail("JsonReaderInternalAccess.INSTANCE is not set");
        }

        String json = "[\"not an object\"]";
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.beginArray();
        assertEquals(JsonToken.STRING, reader.peek());

        try {
            instance.promoteNameToValue(reader);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    void testPromoteNameToValue_EndOfDocument() throws IOException {
        JsonReaderInternalAccess instance = JsonReaderInternalAccess.INSTANCE;
        if (instance == null) {
            fail("JsonReaderInternalAccess.INSTANCE is not set");
        }

        String json = "{}";
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.beginObject();
        reader.endObject();

        try {
            instance.promoteNameToValue(reader);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    void testPromoteNameToValue_EmptyObject() throws IOException {
        JsonReaderInternalAccess instance = JsonReaderInternalAccess.INSTANCE;
        if (instance == null) {
            fail("JsonReaderInternalAccess.INSTANCE is not set");
        }

        String json = "{}";
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.beginObject();
        assertEquals(JsonToken.END_OBJECT, reader.peek());

        try {
            instance.promoteNameToValue(reader);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }
}