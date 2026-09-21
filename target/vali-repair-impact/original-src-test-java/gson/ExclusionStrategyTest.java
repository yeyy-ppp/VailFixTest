package gson;

import gson.ExclusionStrategy;
import gson.FieldAttributes;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

public class ExclusionStrategyTest {

    public String testField = "test";

    @Test
    void testShouldSkipField() throws Exception {
        Field field = this.getClass().getField("testField");
        FieldAttributes fieldAttributes = new FieldAttributes(field);
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        strategy.shouldSkipField(fieldAttributes);
    }

    @Test
    void testShouldSkipClass() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        strategy.shouldSkipClass(Object.class);
    }
}