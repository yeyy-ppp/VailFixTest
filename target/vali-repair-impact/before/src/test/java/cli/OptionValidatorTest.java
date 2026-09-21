package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class OptionValidatorTest {

    @Test
    void testValidate_NullInput() {
        assertNull(OptionValidator.validate(null));
    }

    @Test
    void testValidate_EmptyString() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            OptionValidator.validate("")
        );
        assertEquals("Empty option name.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(chars = {' ', '!', '=', '+'})
    void testValidate_InvalidFirstChar(char invalidChar) {
        String option = String.valueOf(invalidChar);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            OptionValidator.validate(option)
        );
        assertTrue(exception.getMessage().contains(String.format("Illegal option name '%s'.", invalidChar)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "Z", "0", "_", "?", "@"})
    void testValidate_ValidSingleChar(String validOption) {
        assertEquals(validOption, OptionValidator.validate(validOption));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a ", "a?", "a@", "a!"})
    void testValidate_InvalidSubsequentChar(String option) {
        char illegalChar = option.charAt(1);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            OptionValidator.validate(option)
        );
        assertTrue(exception.getMessage().contains(String.format("contains an illegal character : '%s'", illegalChar)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a0", "a_", "a-"})
    void testValidate_ValidMultiChar(String validOption) {
        assertEquals(validOption, OptionValidator.validate(validOption));
    }

    @Test
    void testSearch_CharFound() throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("search", char[].class, char.class);
        method.setAccessible(true);
        char[] array = {'a', 'b', 'c'};
        assertTrue((boolean) method.invoke(null, array, 'b'));
    }

    @Test
    void testSearch_CharNotFound() throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("search", char[].class, char.class);
        method.setAccessible(true);
        char[] array = {'a', 'b', 'c'};
        assertFalse((boolean) method.invoke(null, array, 'd'));
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', 'Z', '0', '_', '?', '@'})
    void testIsValidOpt_ValidChars(char validChar) throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("isValidOpt", char.class);
        method.setAccessible(true);
        assertTrue((boolean) method.invoke(null, validChar));
    }

    @ParameterizedTest
    @ValueSource(chars = {' ', '-', '!', '='})
    void testIsValidOpt_InvalidChars(char invalidChar) throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("isValidOpt", char.class);
        method.setAccessible(true);
        assertFalse((boolean) method.invoke(null, invalidChar));
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', 'Z', '0', '_', '-'})
    void testIsValidChar_ValidChars(char validChar) throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("isValidChar", char.class);
        method.setAccessible(true);
        assertTrue((boolean) method.invoke(null, validChar));
    }

    @ParameterizedTest
    @ValueSource(chars = {' ', '?', '@', '!', '='})
    void testIsValidChar_InvalidChars(char invalidChar) throws Exception {
        Method method = OptionValidator.class.getDeclaredMethod("isValidChar", char.class);
        method.setAccessible(true);
        assertFalse((boolean) method.invoke(null, invalidChar));
    }
}