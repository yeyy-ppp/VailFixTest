package lang3.builder;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AbstractSupplierTest {

    private static class ConcreteSupplier<T, E extends Throwable> {
        public ConcreteSupplier() {}
    }

    @Test
    void testConstructor() {
        ConcreteSupplier<String, Exception> supplier = new ConcreteSupplier<>();
        assertNotNull(supplier);
    }

}