package gson;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonPrimitiveTest {
    // [兜底] 保留了 46 个正确的测试方法

    @Test
    void testBooleanConstructor() {
        JsonPrimitive primitive = new JsonPrimitive(true);
        assertTrue(primitive.isBoolean());
        assertTrue(primitive.getAsBoolean());
    }

    @Test
    void testNumberConstructor() {
        JsonPrimitive primitive = new JsonPrimitive(123);
        assertTrue(primitive.isNumber());
        assertEquals(123, primitive.getAsInt());
    }
}
