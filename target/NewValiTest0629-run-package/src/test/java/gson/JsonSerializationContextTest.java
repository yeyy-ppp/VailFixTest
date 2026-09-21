package gson;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;
import gson.JsonElement;
import gson.JsonPrimitive;
import gson.JsonSerializationContext;

public class JsonSerializationContextTest {

    @Test
    void testSerialize_Object() {
        JsonSerializationContext context = new JsonSerializationContextImpl();
        JsonElement result = context.serialize(null);
        assertNotNull(result);
        
        result = context.serialize("testString");
        assertNotNull(result);
        
        result = context.serialize(123);
        assertNotNull(result);
    }

    @Test
    void testSerialize_ObjectAndType() {
        JsonSerializationContext context = new JsonSerializationContextImpl();
        JsonElement result = context.serialize(null, (Type) null);
        assertNotNull(result);
        
        result = context.serialize("testString", String.class);
        assertNotNull(result);
        
        result = context.serialize(123, Integer.class);
        assertNotNull(result);
        
        result = context.serialize(null, String.class);
        assertNotNull(result);
    }

    private static class JsonSerializationContextImpl implements JsonSerializationContext {
        @Override
        public JsonElement serialize(Object src) {
            return new JsonPrimitive(String.valueOf(src));
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc) {
            return new JsonPrimitive(String.valueOf(src));
        }
    }
}