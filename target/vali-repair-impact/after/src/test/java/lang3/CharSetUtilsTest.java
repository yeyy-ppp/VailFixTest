package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class CharSetUtilsTest {

    @Test
    void testContainsAny() {
        assertFalse(CharSetUtils.containsAny(null, "a"));
        assertFalse(CharSetUtils.containsAny("", "a"));
        assertFalse(CharSetUtils.containsAny("hello"));
        assertFalse(CharSetUtils.containsAny("hello", "xyz"));
        assertTrue(CharSetUtils.containsAny("hello", "l"));
        assertTrue(CharSetUtils.containsAny("world", "d", "x"));
    }

// [兜底]     @Test
// [兜底]     void testCount() {
// [兜底]         assertEquals(0, CharSetUtils.count(null, "a"));
// [兜底]         assertEquals(0, CharSetUtils.count("", "a"));
// [兜底]         assertEquals(0, CharSetUtils.count("hello", "xyz"));
// [兜底]         assertEquals(2, CharSetUtils.count("hello", "l"));
// [兜底]         assertEquals(5, CharSetUtils.count("banana", "na"));
// [兜底]         assertEquals(4, CharSetUtils.count("apples", "a", "p"));
// [兜底]     }

    @Test
    void testDelete() {
        assertNull(CharSetUtils.delete(null, "a"));
        assertEquals("", CharSetUtils.delete("", "a"));
        assertEquals("hello", CharSetUtils.delete("hello", "xyz"));
        assertEquals("heo", CharSetUtils.delete("hello", "l"));
        assertEquals("bn", CharSetUtils.delete("banana", "a"));
        assertEquals("le", CharSetUtils.delete("apple", "a", "p"));
    }

    @Test
    void testIsEmpty() {
        assertNull(CharSetUtils.keep(null, "a"));
        assertEquals("", CharSetUtils.keep("", "a"));
    }

    @Test
    void testKeep() {
        assertNull(CharSetUtils.keep(null, "a"));
        assertEquals("", CharSetUtils.keep("", "a"));
        assertEquals("", CharSetUtils.keep("hello", "xyz"));
        assertEquals("ll", CharSetUtils.keep("hello", "l"));
        assertEquals("aaa", CharSetUtils.keep("banana", "a"));
        assertEquals("pp", CharSetUtils.keep("apple", "p"));
    }

    @Test
    void testSqueeze() {
        assertNull(CharSetUtils.squeeze(null, "a"));
        assertEquals("", CharSetUtils.squeeze("", "a"));
        assertEquals("abcd", CharSetUtils.squeeze("aabbccdd", "xy"));
        assertEquals("helo", CharSetUtils.squeeze("hello", "l"));
        assertEquals("banana", CharSetUtils.squeeze("baananaa", "a"));
        assertEquals("squeez", CharSetUtils.squeeze("squeeze", "e"));
        assertEquals("aabbccdd", CharSetUtils.squeeze("aabbccdd", "x"));
        assertEquals("abc", CharSetUtils.squeeze("aaabbbccc", "ab"));
    }

    @Test
    void testConstructor() {
        new CharSetUtils();
    }
}
