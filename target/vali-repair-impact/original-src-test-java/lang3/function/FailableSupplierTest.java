package lang3.function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FailableSupplierTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testNul() {
        FailableSupplier<String, Exception> supplier1 = FailableSupplier.nul();
        FailableSupplier<Integer, RuntimeException> supplier2 = FailableSupplier.nul();
        
        assertNotNull(supplier1);
        assertNotNull(supplier2);
        assertSame(supplier1, supplier2);
    }

    @Test
    public void testGet() throws Exception {
        FailableSupplier<String, Exception> supplier = FailableSupplier.nul();
        String result = supplier.get();
        assertNull(result);
    }
}