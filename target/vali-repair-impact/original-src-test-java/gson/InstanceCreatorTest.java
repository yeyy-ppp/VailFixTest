package gson;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import static org.junit.jupiter.api.Assertions.*;

public class InstanceCreatorTest {
    @Test
    void testCreateInstance_ConcreteClass() {
        class ConcreteInstanceCreator implements InstanceCreator<String> {
            @Override
            public String createInstance(Type type) {
                return "test";
            }
        }
        InstanceCreator<String> creator = new ConcreteInstanceCreator();
        String result = creator.createInstance(String.class);
        assertEquals("test", result);
    }

    @Test
    void testCreateInstance_ParameterizedType() {
        class ParameterizedInstanceCreator implements InstanceCreator<Number> {
            @Override
            public Number createInstance(Type type) {
                return 42;
            }
        }
        ParameterizedType type = new ParameterizedType() {
            public Type[] getActualTypeArguments() { return new Type[]{Integer.class}; }
            public Type getRawType() { return Number.class; }
            public Type getOwnerType() { return null; }
        };
        InstanceCreator<Number> creator = new ParameterizedInstanceCreator();
        Number result = creator.createInstance(type);
        assertEquals(42, result);
    }

    @Test
    void testCreateInstance_WildcardType() {
        class WildcardInstanceCreator implements InstanceCreator<Object> {
            @Override
            public Object createInstance(Type type) {
                return new Object();
            }
        }
        WildcardType wildcardType = new WildcardType() {
            public Type[] getUpperBounds() { return new Type[]{Object.class}; }
            public Type[] getLowerBounds() { return new Type[0]; }
        };
        InstanceCreator<Object> creator = new WildcardInstanceCreator();
        Object result = creator.createInstance(wildcardType);
        assertNotNull(result);
    }

    @Test
    void testCreateInstance_GenericArrayType() {
        class ArrayInstanceCreator implements InstanceCreator<String[]> {
            @Override
            public String[] createInstance(Type type) {
                return new String[]{"a", "b"};
            }
        }
        Type arrayType = String[].class;
        InstanceCreator<String[]> creator = new ArrayInstanceCreator();
        String[] result = creator.createInstance(arrayType);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    void testCreateInstance_NullType() {
        class NullTypeCreator implements InstanceCreator<Object> {
            @Override
            public Object createInstance(Type type) {
                return null;
            }
        }
        InstanceCreator<Object> creator = new NullTypeCreator();
        Object result = creator.createInstance(null);
        assertNull(result);
    }
}