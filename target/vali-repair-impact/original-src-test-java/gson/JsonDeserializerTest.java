package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Type;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonParseException;

public class JsonDeserializerTest {

    @Test
    void testDeserialize_JsonNull() throws JsonParseException {
        JsonDeserializer<String> deserializer = new SimpleStringDeserializer();
        JsonElement json = JsonNull.INSTANCE;
        Type type = String.class;
        JsonDeserializationContext context = new MockContext();

        String result = deserializer.deserialize(json, type, context);
        assertNull(result);
    }

    @Test
    void testDeserialize_NullJsonElement() throws JsonParseException {
        JsonDeserializer<String> deserializer = new SimpleStringDeserializer();
        Type type = String.class;
        JsonDeserializationContext context = new MockContext();

        String result = deserializer.deserialize(null, type, context);
        assertNull(result);
    }

    @Test
    void testDeserialize_UnsupportedType() {
        JsonDeserializer<String> deserializer = new SimpleStringDeserializer();
        JsonElement json = new JsonObject();
        Type type = String.class;
        JsonDeserializationContext context = new MockContext();

        assertThrows(JsonParseException.class, () -> {
            deserializer.deserialize(json, type, context);
        });
    }

    private static class SimpleStringDeserializer implements JsonDeserializer<String> {
        @Override
        public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            if (json == null || json instanceof JsonNull) {
                return null;
            }
            throw new JsonParseException("Unsupported type");
        }
    }

    private static class MockContext implements JsonDeserializationContext {
        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            return null;
        }
    }
}