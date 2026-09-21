package gson.internal.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class SqlTypesSupportTest {

    @Test
    void testPrivateConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<SqlTypesSupport> constructor = SqlTypesSupport.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    void testCannotInstantiateViaReflection() throws NoSuchMethodException {
        Constructor<SqlTypesSupport> constructor = SqlTypesSupport.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
    }
}
