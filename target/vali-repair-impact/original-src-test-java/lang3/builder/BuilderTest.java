package lang3.builder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    public void testBuild() {
        Builder<String> builder = () -> "test";
        assertEquals("test", builder.build());
    }

    @Test
    public void testBuildWithNull() {
        Builder<Object> builder = () -> null;
        assertNull(builder.build());
    }
}