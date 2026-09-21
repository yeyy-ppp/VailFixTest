package gson;

import gson.JsonDeserializationContext;
import gson.JsonElement;
import gson.JsonPrimitive;
import gson.JsonParseException;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.*;

class JsonDeserializationContextTest {

    @Test
    void testDeserialize_NormalCase() throws JsonParseException {
        JsonDeserializationContext context = new MockDeserializationContext();
        JsonElement json = new JsonPrimitive("test");
        Type type = String.class;
        
        String result = context.deserialize(json, type);
        assertEquals("test", result);
    }

    @Test
    void testDeserialize_NullJsonElement() {
        JsonDeserializationContext context = new MockDeserializationContext();
        Type type = String.class;
        
        assertThrows(NullPointerException.class, () -> {
            context.deserialize(null, type);
        });
    }

    @Test
    void testDeserialize_NullType() {
        JsonDeserializationContext context = new MockDeserializationContext();
        JsonElement json = new JsonPrimitive("test");
        
        assertThrows(NullPointerException.class, () -> {
            context.deserialize(json, null);
        });
    }

    @Test
    void testDeserialize_TypeMismatch() {
        JsonDeserializationContext context = new MockDeserializationContext();
        JsonElement json = new JsonPrimitive("test");
        Type type = Integer.class;
        
        assertThrows(JsonParseException.class, () -> {
            context.deserialize(json, type);
        });
    }

    static class MockDeserializationContext implements JsonDeserializationContext {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            if (json == null) {
                throw new NullPointerException("json is null");
            }
            if (typeOfT == null) {
                throw new NullPointerException("typeOfT is null");
            }
            
            if (typeOfT == String.class) {
                return (T) json.getAsString();
            }
            throw new JsonParseException("Type mismatch");
        }
    }
}