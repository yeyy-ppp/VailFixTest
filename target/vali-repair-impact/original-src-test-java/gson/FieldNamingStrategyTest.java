package gson;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public class FieldNamingStrategyTest {

    @Test
    void testTranslateName_NormalField() throws Exception {
        class TestClass {
            public String sampleField;
        }
        Field field = TestClass.class.getField("sampleField");
        FieldNamingStrategy strategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field f) {
                return f.getName().toUpperCase();
            }
        };
        String result = strategy.translateName(field);
        assertEquals("SAMPLEFIELD", result);
    }

    @Test
    void testTranslateName_NullField() {
        FieldNamingStrategy strategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field f) {
                return f.getName();
            }
        };
        assertThrows(NullPointerException.class, () -> strategy.translateName(null));
    }

    @Test
    void testAlternateNames_DefaultImplementation() throws Exception {
        class TestClass {
            public String testField;
        }
        Field field = TestClass.class.getField("testField");
        FieldNamingStrategy strategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field f) {
                return "";
            }
        };
        List<String> result = strategy.alternateNames(field);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAlternateNames_NullField() {
        FieldNamingStrategy strategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field f) {
                return "";
            }
        };
        List<String> result = strategy.alternateNames(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAlternateNames_OverrideImplementation() throws Exception {
        class TestClass {
            public String overrideField;
        }
        Field field = TestClass.class.getField("overrideField");
        FieldNamingStrategy strategy = new FieldNamingStrategy() {
            @Override
            public String translateName(Field f) {
                return "";
            }
            
            @Override
            public List<String> alternateNames(Field f) {
                return Collections.unmodifiableList(Arrays.asList("altName1", "altName2"));
            }
        };
        List<String> result = strategy.alternateNames(field);
        assertEquals(2, result.size());
        assertTrue(result.contains("altName1"));
        assertTrue(result.contains("altName2"));
    }
}