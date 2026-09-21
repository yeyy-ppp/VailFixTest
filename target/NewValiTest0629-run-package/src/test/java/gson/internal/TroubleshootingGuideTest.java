package gson.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;

public class TroubleshootingGuideTest {

    @Test
    void createUrl_withNormalId() {
        String id = "connection-timeout";
        String expected = "https://github.com/google/gson/blob/main/Troubleshooting.md#connection-timeout";
        String actual = TroubleshootingGuide.createUrl(id);
        assertEquals(expected, actual);
    }

    @Test
    void createUrl_withEmptyId() {
        String id = "";
        String expected = "https://github.com/google/gson/blob/main/Troubleshooting.md#";
        String actual = TroubleshootingGuide.createUrl(id);
        assertEquals(expected, actual);
    }

    @Test
    void createUrl_withNullId() {
        String id = null;
        String expected = "https://github.com/google/gson/blob/main/Troubleshooting.md#null";
        String actual = TroubleshootingGuide.createUrl(id);
        assertEquals(expected, actual);
    }

    @Test
    void createUrl_withSpecialCharacters() {
        String id = "user@domain";
        String expected = "https://github.com/google/gson/blob/main/Troubleshooting.md#user@domain";
        String actual = TroubleshootingGuide.createUrl(id);
        assertEquals(expected, actual);
    }

    @Test
    void constructorIsPrivate() throws NoSuchMethodException {
        Constructor<TroubleshootingGuide> constructor = TroubleshootingGuide.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
    }

}