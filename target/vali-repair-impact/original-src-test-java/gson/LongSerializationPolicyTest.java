package gson;

import gson.internal.bind.TypeAdapters;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LongSerializationPolicyTest {

    @Test
    void serializeDefault_WhenNull_ReturnsJsonNull() {
        JsonElement result = LongSerializationPolicy.DEFAULT.serialize(null);
        assertSame(JsonNull.INSTANCE, result);
    }

    @Test
    void serializeDefault_WhenNonNull_ReturnsJsonPrimitive() {
        Long[] values = {0L, 123L, -456L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (Long value : values) {
            JsonElement result = LongSerializationPolicy.DEFAULT.serialize(value);
            assertTrue(result.isJsonPrimitive());
            assertEquals(value, result.getAsLong());
        }
    }

    @Test
    void serializeString_WhenNull_ReturnsJsonNull() {
        JsonElement result = LongSerializationPolicy.STRING.serialize(null);
        assertSame(JsonNull.INSTANCE, result);
    }

    @Test
    void serializeString_WhenNonNull_ReturnsStringPrimitive() {
        Long[] values = {0L, 123L, -456L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (Long value : values) {
            JsonElement result = LongSerializationPolicy.STRING.serialize(value);
            assertTrue(result.isJsonPrimitive());
            assertEquals(String.valueOf(value), result.getAsString());
        }
    }

    @Test
    void typeAdapterDefault_ReturnsLongTypeAdapter() {
        TypeAdapter<Number> adapter = LongSerializationPolicy.DEFAULT.typeAdapter();
        assertSame(TypeAdapters.LONG, adapter);
    }

    @Test
    void typeAdapterString_ReturnsLongAsStringTypeAdapter() {
        TypeAdapter<Number> adapter = LongSerializationPolicy.STRING.typeAdapter();
        assertSame(TypeAdapters.LONG_AS_STRING, adapter);
    }
}