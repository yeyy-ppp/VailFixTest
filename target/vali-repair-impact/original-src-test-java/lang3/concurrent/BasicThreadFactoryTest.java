package lang3.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicThreadFactoryTest {

    @Test
    void testBuild() {
        BasicThreadFactory.Builder builder = BasicThreadFactory.builder();
        builder.priority(5);
        BasicThreadFactory factory = builder.build();
        assertNotNull(factory);
        assertEquals(5, factory.getPriority().intValue());
    }
}