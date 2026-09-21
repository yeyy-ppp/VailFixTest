package lang3.function;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.util.function.Supplier;

class SuppliersTest {

    @Test
    void testGetWithNullSupplier() {
        Object result = Suppliers.get(null);
        assertNull(result);
    }

    @Test
    void testGetWithNonNullSupplier() {
        String expected = "testValue";
        Supplier<String> supplier = () -> expected;
        Object result = Suppliers.get(supplier);
        assertEquals(expected, result);
    }

    @Test
    void testNulReturnsNull() {
        Supplier<Object> supplier = Suppliers.<Object>nul();
        Object result1 = supplier.get();
        Object result2 = supplier.get();
        assertNull(result1);
        assertNull(result2);
    }

    @Test
    void testSuppliersConstructor() {
        new Suppliers();
    }
}