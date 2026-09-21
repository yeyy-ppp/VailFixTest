package gson.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;

import lombok.var;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PreJava9DateFormatProviderTest {

    @Test
    void testConstructorAccessibility() throws Exception {
        var constructor = PreJava9DateFormatProvider.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testGetUsDateTimeFormatValidCombinations() {
        int[] styles = {DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
        for (int dateStyle : styles) {
            for (int timeStyle : styles) {
                DateFormat format = PreJava9DateFormatProvider.getUsDateTimeFormat(dateStyle, timeStyle);
                assertNotNull(format);
            }
        }
    }

    @Test
    void testGetUsDateTimeFormatInvalidDateStyle() {
        assertThrows(IllegalArgumentException.class, () -> 
            PreJava9DateFormatProvider.getUsDateTimeFormat(100, DateFormat.SHORT)
        );
    }

    @Test
    void testGetUsDateTimeFormatInvalidTimeStyle() {
        assertThrows(IllegalArgumentException.class, () -> 
            PreJava9DateFormatProvider.getUsDateTimeFormat(DateFormat.SHORT, 200)
        );
    }

    @Test
    void testGetDatePartOfDateTimePattern() throws Exception {
        Method method = PreJava9DateFormatProvider.class.getDeclaredMethod(
            "getDatePartOfDateTimePattern", int.class
        );
        method.setAccessible(true);
        
        assertEquals("M/d/yy", method.invoke(null, DateFormat.SHORT));
        assertEquals("MMM d, yyyy", method.invoke(null, DateFormat.MEDIUM));
        assertEquals("MMMM d, yyyy", method.invoke(null, DateFormat.LONG));
        assertEquals("EEEE, MMMM d, yyyy", method.invoke(null, DateFormat.FULL));
        
        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> 
            method.invoke(null, 100)
        );
        assertTrue(e.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testGetTimePartOfDateTimePattern() throws Exception {
        Method method = PreJava9DateFormatProvider.class.getDeclaredMethod(
            "getTimePartOfDateTimePattern", int.class
        );
        method.setAccessible(true);
        
        assertEquals("h:mm a", method.invoke(null, DateFormat.SHORT));
        assertEquals("h:mm:ss a", method.invoke(null, DateFormat.MEDIUM));
        assertEquals("h:mm:ss a z", method.invoke(null, DateFormat.LONG));
        assertEquals("h:mm:ss a z", method.invoke(null, DateFormat.FULL));
        
        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> 
            method.invoke(null, 200)
        );
        assertTrue(e.getCause() instanceof IllegalArgumentException);
    }

}