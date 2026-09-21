package gson.stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonTokenTest {

    @Test
    void testValues() {
        JsonToken[] tokens = JsonToken.values();
        assertEquals(10, tokens.length);
        assertEquals(JsonToken.BEGIN_ARRAY, tokens[0]);
        assertEquals(JsonToken.END_ARRAY, tokens[1]);
        assertEquals(JsonToken.BEGIN_OBJECT, tokens[2]);
        assertEquals(JsonToken.END_OBJECT, tokens[3]);
        assertEquals(JsonToken.NAME, tokens[4]);
        assertEquals(JsonToken.STRING, tokens[5]);
        assertEquals(JsonToken.NUMBER, tokens[6]);
        assertEquals(JsonToken.BOOLEAN, tokens[7]);
        assertEquals(JsonToken.NULL, tokens[8]);
        assertEquals(JsonToken.END_DOCUMENT, tokens[9]);
    }

    @Test
    void testValueOf_validName() {
        assertEquals(JsonToken.BEGIN_ARRAY, JsonToken.valueOf("BEGIN_ARRAY"));
        assertEquals(JsonToken.END_DOCUMENT, JsonToken.valueOf("END_DOCUMENT"));
    }

    @Test
    void testValueOf_invalidName() {
        assertThrows(IllegalArgumentException.class, () -> JsonToken.valueOf("INVALID_TOKEN"));
    }
}