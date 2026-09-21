package codeing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AddTest {

    private Add add;

    @BeforeEach
    public void setUp() {
        add = new Add();
    }

    @Test
    public void testCountChar_NullString() {
        int result = Add.countChar(null, 'a');
        assertEquals(0, result);
    }

    @Test
    public void testCountChar_EmptyString() {
        int result = Add.countChar("", 'a');
        assertEquals(0, result);
    }

    @Test
    public void testCountChar_SingleMatch() {
        int result = Add.countChar("apple", 'a');
        assertEquals(1, result);
    }

    @Test
    public void testCountChar_MultipleMatches() {
        int result = Add.countChar("banana", 'a');
        assertEquals(3, result);
    }

    @Test
    public void testCountChar_NoMatches() {
        int result = Add.countChar("hello", 'z');
        assertEquals(0, result);
    }

    @Test
    public void testCountChar_CaseSensitivity() {
        int result = Add.countChar("Java", 'J');
        assertEquals(1, result);
    }

    @Test
    public void testCountChar_LowerCaseInMixedCase() {
        int result = Add.countChar("Java", 'a');
        assertEquals(2, result);
    }

    @Test
    public void testCountChar_SpecialChar() {
        int result = Add.countChar("a,b,c", ',');
        assertEquals(2, result);
    }

    @Test
    public void testReverse_NullInput() {
        String result = add.reverse(null);
        assertEquals("", result);
    }

    @Test
    public void testReverse_EmptyString() {
        String result = add.reverse("");
        assertEquals("", result);
    }

    @Test
    public void testReverse_SingleChar() {
        String result = add.reverse("a");
        assertEquals("a", result);
    }

    @Test
    public void testReverse_EvenLength() {
        String result = add.reverse("abcd");
        assertEquals("dcba", result);
    }

    @Test
    public void testReverse_OddLength() {
        String result = add.reverse("abc");
        assertEquals("cba", result);
    }

    @Test
    public void testCharAt_NullString() {
        assertThrows(IllegalArgumentException.class, () -> {
            add.charAt(null, 1);
        });
    }

    @Test
    public void testCharAt_IndexZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            add.charAt("hello", 0);
        });
    }

    @Test
    public void testCharAt_IndexNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            add.charAt("hello", -1);
        });
    }

    @Test
    public void testCharAt_IndexExceedLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            add.charAt("hi", 3);
        });
    }

    @Test
    public void testCharAt_FirstIndex() {
        char result = add.charAt("hello", 1);
        assertEquals('h', result);
    }

    @Test
    public void testCharAt_LastIndex() {
        char result = add.charAt("world", 5);
        assertEquals('d', result);
    }

    @Test
    public void testCharAt_MiddleIndex() {
        char result = add.charAt("hello", 3);
        assertEquals('l', result);
    }

    @Test
    public void testCharAt_UnicodeChar() {
        char result = add.charAt("你好", 1);
        assertEquals('你', result);
    }

    @Test
    public void testMaxLength_NullArray() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, (Object) null);
        assertEquals(0, result);
    }

    @Test
    public void testMaxLength_EmptyArray() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{}});
        assertEquals(0, result);
    }

    @Test
    public void testMaxLength_AllNulls() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{null, null, null}});
        assertEquals(0, result);
    }

    @Test
    public void testMaxLength_WithNullsAndStrings() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{null, "a", "abc", null, "ab"}});
        assertEquals(3, result);
    }

    @Test
    public void testMaxLength_MultipleStrings() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{"apple", "banana", "cherry"}});
        assertEquals(6, result);
    }

    @Test
    public void testMaxLength_AllEmptyStrings() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{"", "", ""}});
        assertEquals(0, result);
    }

    @Test
    public void testMaxLength_MultipleSameMaxLength() throws Exception {
        java.lang.reflect.Method method = Add.class.getDeclaredMethod("maxLength", String[].class);
        method.setAccessible(true);
        int result = (int) method.invoke(add, new Object[]{new String[]{"cat", "dog", "fox"}});
        assertEquals(3, result);
    }
}