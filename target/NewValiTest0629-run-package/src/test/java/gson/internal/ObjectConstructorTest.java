package gson.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ObjectConstructorTest {

    @Test
    void testConstruct() {
        ObjectConstructor<Object> constructor = new TestObjectConstructor();
        Object result = constructor.construct();
        assertNotNull(result);
    }

    private static class TestObjectConstructor implements ObjectConstructor<Object> {
        @Override
        public Object construct() {
            return new Object();
        }
    }
}