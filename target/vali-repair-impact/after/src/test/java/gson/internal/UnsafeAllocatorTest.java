package gson.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractList;

import org.junit.jupiter.api.Test;

class UnsafeAllocatorTest {

    @Test
    void testNewInstance_Object() throws Exception {
        Object result = UnsafeAllocator.INSTANCE.newInstance(Object.class);
        assertNotNull(result);
    }

    @Test
    void testNewInstance_Interface() {
        assertThrows(AssertionError.class, () -> {
            UnsafeAllocator.INSTANCE.newInstance(Runnable.class);
        });
    }

    @Test
    void testNewInstance_AbstractClass() {
        assertThrows(AssertionError.class, () -> {
            UnsafeAllocator.INSTANCE.newInstance(AbstractList.class);
        });
    }

    @Test
    void testNewInstance_UnsupportedAllocator() {
        UnsafeAllocator allocator = new UnsafeAllocator() {
            @Override
            public <T> T newInstance(Class<T> c) {
                throw new UnsupportedOperationException();
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> {
            allocator.newInstance(Object.class);
        });
    }

    @Test
    void testCreate() throws Exception {
        UnsafeAllocator allocator = UnsafeAllocator.create();
        assertNotNull(allocator);
        assertNotNull(allocator.newInstance(Object.class));
    }
}