package gson;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import gson.Gson;
import gson.GsonBuilder;
import gson.JsonElement;
import gson.JsonNull;
import gson.JsonObject;
import gson.JsonPrimitive;

class JsonSerializerTest {

    @Test
    void serialize_NullObject_ReturnsJsonNull() {
        Gson gson = new Gson();
        JsonElement result = gson.toJsonTree(null);
        assertTrue(result instanceof JsonNull);
    }

    @Test
    void serialize_StringObject_ReturnsJsonPrimitive() {
        Gson gson = new Gson();
        JsonElement result = gson.toJsonTree("test");
        assertTrue(result instanceof JsonPrimitive);
        assertEquals("test", result.getAsString());
    }

    @Test
    void serialize_CustomType_HandlesTypeCorrectly() {
        Gson gson = new Gson();
        JsonElement result = gson.toJsonTree(new CustomType());
        assertTrue(result instanceof JsonObject);
    }

    @Test
    void serialize_WithContext_AppliesContextRules() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonElement result = gson.toJsonTree(new CircularReference());
        assertTrue(result instanceof JsonObject);
        assertFalse(result.getAsJsonObject().has("reference"));
    }

    private static class CustomType {
        public String field = "value";
    }

    private static class CircularReference {
        CircularReference reference = this;
    }
}