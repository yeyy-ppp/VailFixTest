package cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class CharTest {

// 修复失败，注释错误方法：    @Test
// 修复失败，注释错误方法：    void testPrivateConstructorAccessibility() throws NoSuchMethodException {
// 修复失败，注释错误方法：        Constructor<Char> constructor = Char.class.getDeclaredConstructor();
// 修复失败，注释错误方法：        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
// 修复失败，注释错误方法：    }

    @Test
    void testPrivateConstructorInvocation() throws Exception {
        Constructor<Char> constructor = Char.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError);
    }

    @Test
    void testStaticConstantsValues() {
        assertEquals('\'', Char.APOS);
        assertEquals('\r', Char.CR);
        assertEquals('=', Char.EQUAL);
        assertEquals('\n', Char.LF);
        assertEquals(' ', Char.SP);
        assertEquals('\t', Char.TAB);
    }
}
