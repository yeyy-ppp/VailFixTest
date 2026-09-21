package gson.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JavaVersionTest {

    @Test
    void testParseDotted() throws Exception {
        Method method = JavaVersion.class.getDeclaredMethod("parseDotted", String.class);
        method.setAccessible(true);
        
        assertEquals(8, method.invoke(null, "1.8.0_291"));
        assertEquals(9, method.invoke(null, "9.0.1"));
        assertEquals(11, method.invoke(null, "11.0.2"));
        assertEquals(1, method.invoke(null, "1"));
        assertEquals(5, method.invoke(null, "5.0"));
        assertEquals(-1, method.invoke(null, "1.x"));
        assertEquals(-1, method.invoke(null, "1_anything"));
        assertEquals(-1, method.invoke(null, ""));
        assertEquals(-1, method.invoke(null, "abc"));
        assertEquals(-1, method.invoke(null, "1."));
        assertEquals(-1, method.invoke(null, "1.a"));
    }

    @Test
    void testExtractBeginningInt() throws Exception {
        Method method = JavaVersion.class.getDeclaredMethod("extractBeginningInt", String.class);
        method.setAccessible(true);
        
        assertEquals(15, method.invoke(null, "15-ea"));
        assertEquals(10, method.invoke(null, "10"));
        assertEquals(8, method.invoke(null, "8u291"));
        assertEquals(123, method.invoke(null, "123abc"));
        assertEquals(1, method.invoke(null, "1"));
        assertEquals(1, method.invoke(null, "1xyz"));
        assertEquals(-1, method.invoke(null, ""));
        assertEquals(-1, method.invoke(null, "abc"));
        assertEquals(-1, method.invoke(null, "-10"));
    }

    @Test
    void testParseMajorJavaVersion() {
        assertEquals(8, JavaVersion.parseMajorJavaVersion("1.8"));
        assertEquals(9, JavaVersion.parseMajorJavaVersion("9"));
        assertEquals(11, JavaVersion.parseMajorJavaVersion("11.0.2"));
        assertEquals(10, JavaVersion.parseMajorJavaVersion("10"));
        assertEquals(15, JavaVersion.parseMajorJavaVersion("15-ea"));
        assertEquals(6, JavaVersion.parseMajorJavaVersion("invalid"));
        assertEquals(6, JavaVersion.parseMajorJavaVersion(""));
        assertEquals(1, JavaVersion.parseMajorJavaVersion("1."));
    }

    @Test
    void testGetMajorJavaVersion() throws Exception {
        Field field = JavaVersion.class.getDeclaredField("majorJavaVersion");
        field.setAccessible(true);
        
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        
        int original = field.getInt(null);
        try {
            field.setInt(null, 8);
            assertEquals(8, JavaVersion.getMajorJavaVersion());
            
            field.setInt(null, 11);
            assertEquals(11, JavaVersion.getMajorJavaVersion());
        } finally {
            field.setInt(null, original);
        }
    }

    @Test
    void testIsJava9OrLater() throws Exception {
        Field field = JavaVersion.class.getDeclaredField("majorJavaVersion");
        field.setAccessible(true);
        
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        
        int original = field.getInt(null);
        try {
            field.setInt(null, 8);
            assertFalse(JavaVersion.isJava9OrLater());
            
            field.setInt(null, 9);
            assertTrue(JavaVersion.isJava9OrLater());
            
            field.setInt(null, 11);
            assertTrue(JavaVersion.isJava9OrLater());
        } finally {
            field.setInt(null, original);
        }
    }
}