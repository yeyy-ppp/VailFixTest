package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonNullTest {

    @Test
    void testConstructor() {
        JsonNull jsonNull = new JsonNull();
        assertNotNull(jsonNull);
        assertTrue(jsonNull instanceof JsonNull);
    }

    @Test
    void testDeepCopy() {
        JsonNull copy = JsonNull.INSTANCE.deepCopy();
        assertSame(JsonNull.INSTANCE, copy);
        JsonNull copy2 = new JsonNull().deepCopy();
        assertSame(JsonNull.INSTANCE, copy2);
    }

    @Test
    void testHashCode() {
        assertEquals(JsonNull.class.hashCode(), JsonNull.INSTANCE.hashCode());
        assertEquals(JsonNull.class.hashCode(), new JsonNull().hashCode());
        assertNotEquals(0, JsonNull.INSTANCE.hashCode());
    }

    @Test
    void testEquals() {
        assertTrue(JsonNull.INSTANCE.equals(JsonNull.INSTANCE));
        assertTrue(JsonNull.INSTANCE.equals(new JsonNull()));
        assertFalse(JsonNull.INSTANCE.equals(null));
        assertFalse(JsonNull.INSTANCE.equals("JsonNull"));
        assertFalse(JsonNull.INSTANCE.equals(new Object()));
    }
}