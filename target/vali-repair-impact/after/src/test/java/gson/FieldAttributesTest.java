package gson;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FieldAttributesTest {

    // ... 其他保持不变 ...

    @Test
    void testConstructorSetsField() throws NoSuchFieldException {
        Field field = TestFields.class.getField("publicField");
        FieldAttributes publicFieldAttributes = new FieldAttributes(field);
        assertEquals(field.getName(), publicFieldAttributes.getName());
    }

    // ... 其他保持不变 ...
}

class TestFields {
    public int publicField;
    private String[] privateField;
    @Deprecated
    public Object deprecatedField;
    public static final String CONSTANT_FIELD = "constant";
}
